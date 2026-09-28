package com.zifang.z.qa.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.util.json.JsonUtil;
import com.zifang.z.qa.admin.domain.entity.QaEnvDO;
import com.zifang.z.qa.admin.domain.entity.QaPlanDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDetailDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import com.zifang.z.qa.admin.domain.mapper.QaEnvMapper;
import com.zifang.z.qa.admin.domain.mapper.QaPlanMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteStepMapper;
import com.zifang.z.qa.admin.service.QaNotifyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * run 的步骤循环 —— retry / failFast / abort / 缺陷定性 / 逐步落库这些策略都收在这里。
 *
 * <p>必须是独立 bean 而非 dispatcher 的方法:{@code @Async} 靠代理生效,同类内 this 调用会绕过代理,
 * 整条 run 就同步跑在发起请求的线程上 —— 那样连「谁来检查 abort」都不成立。
 *
 * <p>failFast 的粒度是**套件**:触发后只跳过该套件剩余步骤,计划里的后续套件照常执行。
 * 对齐源平台「abort the scenario，下一个 scenario 继续」的口径,而非整轮止损。
 */
@Component
public class QaRunRunner {

    private static final Logger log = LogManager.getLogger(QaRunRunner.class);

    /** 重试前的固定等待:超时/连不上多是瞬时抖动,立刻重发大概率还是失败 */
    private static final long RETRY_PAUSE_MS = 1000L;

    /** 响应体入库截断长度 (沿用改造前的口径) */
    private static final int RESPONSE_BODY_MAX = 8000;

    private static final String STATUS_RUNNING = "running";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_CANCELLED = "cancelled";

    private static final String MODE_E2E = "e2e";

    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private QaRunDetailMapper runDetailMapper;
    @Autowired
    private QaSuiteMapper suiteMapper;
    @Autowired
    private QaSuiteStepMapper stepMapper;
    @Autowired
    private QaEnvMapper envMapper;
    @Autowired
    private QaPlanMapper planMapper;
    @Autowired
    private ApiStepExecutor apiStepExecutor;
    @Autowired
    @Lazy
    private QaNotifyService notifyService;

    @Async
    public void runSuiteAsync(Long runId) {
        QaRunDO run = runMapper.selectById(runId);
        if (run == null) {
            log.warn("run[{}] 不存在，跳过", runId);
            return;
        }
        QaSuiteDO suite = suiteMapper.selectById(run.getSuiteId());
        if (suite == null) {
            finishAsError(run, "套件不存在: " + run.getSuiteId());
            return;
        }
        execute(run, planItems(run, Collections.singletonList(suite)));
    }

    @Async
    public void runPlanAsync(Long runId) {
        QaRunDO run = runMapper.selectById(runId);
        if (run == null) {
            log.warn("run[{}] 不存在，跳过", runId);
            return;
        }
        QaPlanDO plan = planMapper.selectById(run.getPlanId());
        if (plan == null) {
            finishAsError(run, "计划不存在: " + run.getPlanId());
            return;
        }
        List<QaSuiteDO> suites = new ArrayList<>();
        for (Long suiteId : parseSuiteIds(plan.getSuiteIds())) {
            QaSuiteDO suite = suiteMapper.selectById(suiteId);
            if (suite == null) {
                log.warn("plan[{}] 引用了不存在的 suite[{}]，忽略", plan.getId(), suiteId);
                continue;
            }
            suites.add(suite);
        }
        if (suites.isEmpty()) {
            finishAsError(run, "计划下没有可执行的套件 (suite_ids 为空或全部失效)");
            return;
        }
        execute(run, planItems(run, suites));
    }

    // ===== 核心循环 =====

    /**
     * 步骤在开跑前展平成一条有序序列,整个 run 只有一个循环 ——
     * 计数、终止判定、skip 归属才不会在多套件的收尾里互相覆盖。
     */
    private void execute(QaRunDO run, List<PlannedStep> items) {
        run.setTotalSteps(items.size());
        run.setPassedSteps(0);
        run.setFailedSteps(0);

        Map<String, Object> ctx = new HashMap<>();
        Long ctxSuiteId = null;
        Long failFastHaltedSuiteId = null;
        boolean aborted = false;
        int passed = 0, failed = 0;

        try {
            markRunning(run, items.isEmpty() ? null : items.get(0).env);

            for (PlannedStep item : items) {
                if (!item.suiteId.equals(ctxSuiteId)) {
                    // 变量上下文按套件重置:上一步提取的 ${TOKEN} 不该漏进下一个套件
                    ctx = new HashMap<>();
                    ctxSuiteId = item.suiteId;
                }

                if (aborted) {
                    writeSkip(run, item, "收到中止请求，未执行");
                    continue;
                }
                if (item.suiteId.equals(failFastHaltedSuiteId)) {
                    writeSkip(run, item, "该套件前序步骤失败且开启遇错即停");
                    continue;
                }
                if (abortFlagSet(run.getId())) {
                    aborted = true;
                    writeSkip(run, item, "收到中止请求，未执行");
                    continue;
                }

                String result;
                String reason;
                try {
                    ApiStepExecutor.StepResult sr = executeWithRetry(run, item, ctx);
                    QaRunDetailDO detail = sr == null
                            ? plainDetail(run, item, QaStepStatus.FAIL_UNKNOWN,
                            "步骤模式无对应执行器: " + item.step.getExecutionMode())
                            : toDetail(run, item, sr, Math.max(0, item.attempts - 1));
                    runDetailMapper.insert(detail);
                    result = detail.getResult();
                    reason = detail.getErrorMessage();
                } catch (RuntimeException e) {
                    // 明细这一行可能落不下去，但这一步必须有定性 —— 否则计数与终止判定会漏掉它
                    log.error("run[{}] step[{}] 执行或落库异常", run.getId(), item.step.getStepNo(), e);
                    result = QaStepStatus.FAIL_SYSTEM_ERROR;
                    reason = e.getMessage();
                }
                if (QaStepStatus.SUCCESS.equals(result)) {
                    passed++;
                } else if (QaStepStatus.isFail(result)) {
                    failed++;
                }
                flushCounters(run, passed, failed);
                log.info("run[{}] suite[{}] step[{}] {} {}",
                        run.getId(), item.suiteId, item.step.getStepNo(), result, reason);

                if (item.failFast && QaStepStatus.isFail(result)) {
                    failFastHaltedSuiteId = item.suiteId;
                }
            }
        } catch (RuntimeException e) {
            log.error("run[{}] 执行链异常终止", run.getId(), e);
            finishCrashed(run, e);
            return;
        }

        String status = aborted ? STATUS_CANCELLED : (failed > 0 ? STATUS_FAILED : STATUS_SUCCESS);
        finish(run, status, aborted ? "执行中被中止；剩余步骤落 skip" : null);
        log.info("run[{}] 结束: status={}, passed={}/{}, failed={}",
                run.getId(), status, passed, items.size(), failed);

        notifyFinished(run);
    }

    /**
     * 一次 attempt = 一次 apiStepExecutor.execute；重试与否看定性,不看有没有失败。
     *
     * <p>断言不符与入参未解析再来一次也不会变,白等；5xx 与超时/连不上多是瞬时抖动。
     */
    private ApiStepExecutor.StepResult executeWithRetry(QaRunDO run, PlannedStep item, Map<String, Object> ctx) {
        QaSuiteStepDO step = item.step;
        if (MODE_E2E.equalsIgnoreCase(step.getExecutionMode())) {
            // e2e 执行器在 P1 接入。此处绝不退化成发一次 HTTP 请求 —— 改造前正是这么干的，
            // 拿着 e2e 步骤的空 url 去拼 baseUrl，报出来的是一行莫名的 HTTP 失败。
            log.warn("run[{}] step[{}] 是 e2e 步骤，执行器尚未接入", run.getId(), step.getStepNo());
            return null;
        }
        int maxAttempts = 1 + Math.max(0, step.getRetryCount() == null ? 0 : step.getRetryCount());
        ApiStepExecutor.StepResult sr = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            sr = apiStepExecutor.execute(step, item.env, ctx);
            item.attempts = attempt;
            if (!QaFailureClassifier.isRetryable(sr.getResult())) {
                return sr;
            }
            if (attempt < maxAttempts) {
                log.info("run[{}] step[{}] 定性 {} 可重试，第 {} 次重试",
                        run.getId(), step.getStepNo(), sr.getResult(), attempt);
                sleep();
            }
        }
        return sr;
    }

    // ===== 明细 =====

    private QaRunDetailDO toDetail(QaRunDO run, PlannedStep item, ApiStepExecutor.StepResult sr, int retries) {
        QaRunDetailDO d = new QaRunDetailDO();
        d.setRunId(run.getId());
        d.setSuiteId(item.suiteId);
        d.setStepNo(sr.getStepNo());
        d.setStepName(sr.getStepName());
        d.setMethod(sr.getMethod());
        d.setRequestUrl(sr.getRequestUrl());
        d.setRequestHeaders(sr.getRequestHeaders());
        d.setRequestBody(sr.getRequestBody());
        d.setResponseStatus(sr.getResponseStatus());
        d.setResponseBody(truncate(sr.getResponseBody(), RESPONSE_BODY_MAX));
        d.setVariablesBefore(sr.getVariablesBefore());
        d.setVariablesAfter(sr.getVariablesAfter());
        d.setExtractedVars(sr.getExtractedVars());
        d.setAssertResults(sr.getAssertResults());
        d.setResult(sr.getResult());
        d.setErrorMessage(sr.getErrorMessage());
        d.setDurationMs(sr.getDurationMs());
        d.setRetryCount(retries);
        d.setTargetMode(item.step.getTargetMode());
        d.setDefectType(QaFailureClassifier.classifyDefectType(sr.getResult()));
        d.setTenantCode(run.getTenantCode());
        return d;
    }

    /** 没有真正执行过、因而没有请求/响应可记的明细 (跳过，或模式尚无执行器)。 */
    private QaRunDetailDO plainDetail(QaRunDO run, PlannedStep item, String result, String reason) {
        QaSuiteStepDO step = item.step;
        QaRunDetailDO d = new QaRunDetailDO();
        d.setRunId(run.getId());
        d.setSuiteId(item.suiteId);
        d.setStepNo(step.getStepNo());
        d.setStepName(step.getStepName());
        d.setMethod(step.getMethod());
        d.setRequestUrl(step.getUrl());
        d.setRequestBody(step.getBody());
        d.setResult(result);
        d.setErrorMessage(reason);
        d.setVariablesBefore("{}");
        d.setVariablesAfter("{}");
        d.setRetryCount(0);
        d.setTargetMode(step.getTargetMode());
        d.setDefectType(QaFailureClassifier.classifyDefectType(result));
        d.setTenantCode(run.getTenantCode());
        return d;
    }

    private void writeSkip(QaRunDO run, PlannedStep item, String reason) {
        runDetailMapper.insert(plainDetail(run, item, QaStepStatus.SKIP, reason));
    }

    // ===== run 行维护 =====

    private void markRunning(QaRunDO run, QaEnvDO env) {
        if (run.getStartedAt() == null) {
            run.setStartedAt(new Date());
        }
        if (env != null) {
            if (run.getEnvCode() == null) {
                run.setEnvCode(env.getCode());
            }
            run.setEnvBaseUrl(env.getBaseUrl());
        }
        run.setStatus(STATUS_RUNNING);

        QaRunDO upd = new QaRunDO();
        upd.setId(run.getId());
        upd.setStatus(STATUS_RUNNING);
        upd.setStartedAt(run.getStartedAt());
        upd.setTotalSteps(run.getTotalSteps());
        upd.setEnvCode(run.getEnvCode());
        upd.setEnvBaseUrl(run.getEnvBaseUrl());
        runMapper.updateById(upd);
    }

    private void flushCounters(QaRunDO run, int passed, int failed) {
        // 只写计数列:整行更新会把 abort_requested 覆盖回内存里的旧值，中止请求就丢了
        run.setPassedSteps(passed);
        run.setFailedSteps(failed);
        QaRunDO upd = new QaRunDO();
        upd.setId(run.getId());
        upd.setPassedSteps(passed);
        upd.setFailedSteps(failed);
        runMapper.updateById(upd);
    }

    /**
     * 终态只写自己那几列 —— 整行回写会拿这份内存快照把库里的 abort_requested 覆盖回 0，
     * 中止请求与"这条 run 是被中止的"这条事实就消失了。
     */
    private void finish(QaRunDO run, String status, String errorMessage) {
        QaRunDO upd = new QaRunDO();
        upd.setId(run.getId());
        upd.setStatus(status);
        upd.setFinishedAt(new Date());
        if (run.getStartedAt() != null) {
            upd.setDurationMs(System.currentTimeMillis() - run.getStartedAt().getTime());
        }
        upd.setErrorMessage(errorMessage);
        runMapper.updateById(upd);

        run.setStatus(status);
        run.setFinishedAt(upd.getFinishedAt());
        run.setDurationMs(upd.getDurationMs());
        if (errorMessage != null) {
            run.setErrorMessage(errorMessage);
        }
    }

    private void finishAsError(QaRunDO run, String message) {
        finish(run, STATUS_FAILED, message);
        log.error("run[{}] 无法执行: {}", run.getId(), message);
    }

    /**
     * 循环外泄的异常 (读中止标志、写计数这类基础设施失败) 兜底成 failed ——
     * 否则 run 永远停在 running，看板与通知都拿不到终态。
     */
    private void finishCrashed(QaRunDO run, RuntimeException cause) {
        QaRunDO fresh = runMapper.selectById(run.getId());
        if (fresh == null || isTerminalStatus(fresh.getStatus())) {
            return;
        }
        finish(run, STATUS_FAILED, "执行链异常终止: " + cause.getMessage());
        notifyFinished(run);
    }

    private static boolean isTerminalStatus(String status) {
        return STATUS_SUCCESS.equals(status) || STATUS_FAILED.equals(status) || STATUS_CANCELLED.equals(status);
    }

    /** abort 标志走 DB 而非内存 flag:发起中止的实例未必是跑这条 run 的实例。 */
    private boolean abortFlagSet(Long runId) {
        QaRunDO fresh = runMapper.selectById(runId);
        return fresh != null && fresh.getAbortRequested() != null && fresh.getAbortRequested() == 1;
    }

    private void notifyFinished(QaRunDO run) {
        if (notifyService == null) {
            return;
        }
        try {
            QaPlanDO plan = run.getPlanId() == null ? null : planMapper.selectById(run.getPlanId());
            if (plan != null) {
                notifyService.notifyRunFinished(run, plan.getNotifyChannels(),
                        isTrue(plan.getNotifyOnPass()), isTrue(plan.getNotifyOnFail()));
            } else {
                notifyService.notifyRunFinished(run, null, false, true);
            }
        } catch (Exception e) {
            log.warn("run[{}] 通知投递失败: {}", run.getId(), e.getMessage());
        }
    }

    // ===== 小工具 =====

    private List<PlannedStep> planItems(QaRunDO run, List<QaSuiteDO> suites) {
        List<PlannedStep> items = new ArrayList<>();
        for (QaSuiteDO suite : suites) {
            QaEnvDO env = resolveEnv(firstNonNull(run.getEnvCode(), suite.getEnvCode()));
            boolean failFast = suite.getFailFast() != null && suite.getFailFast() == 1;
            for (QaSuiteStepDO step : stepMapper.selectBySuiteId(suite.getId())) {
                items.add(new PlannedStep(suite.getId(), env, step, failFast));
            }
        }
        return items;
    }

    private QaEnvDO resolveEnv(String envCode) {
        if (envCode != null && !envCode.isEmpty()) {
            QaEnvDO env = envMapper.selectOne(new QueryWrapper<QaEnvDO>().eq("code", envCode));
            if (env != null) {
                return env;
            }
            log.warn("env_code={} 在 z_qa_env 里不存在，回落 local", envCode);
        }
        return envMapper.selectOne(new QueryWrapper<QaEnvDO>().eq("code", "local"));
    }

    private static List<Long> parseSuiteIds(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            Long[] ids = JsonUtil.fromJson(json, Long[].class);
            return ids == null ? Collections.<Long>emptyList() : Arrays.asList(ids);
        } catch (Exception e) {
            log.warn("suite_ids 不是合法 JSON 数组: {}", json);
            return Collections.emptyList();
        }
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    private static boolean isTrue(Integer v) {
        return v != null && v == 1;
    }

    private static void sleep() {
        try {
            Thread.sleep(RETRY_PAUSE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }

    /** 一个待执行步骤 + 它归属的套件与该套件解析出的环境。 */
    private static final class PlannedStep {
        final Long suiteId;
        final QaSuiteStepDO step;
        final boolean failFast;
        QaEnvDO env;
        int attempts;

        PlannedStep(Long suiteId, QaEnvDO env, QaSuiteStepDO step, boolean failFast) {
            this.suiteId = suiteId;
            this.env = env;
            this.step = step;
            this.failFast = failFast;
        }
    }
}
