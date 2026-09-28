package com.zifang.z.qa.admin.engine;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaPlanDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDetailDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.mapper.QaPlanMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 执行入口 —— 只负责建 run 行与状态读取,真正的步骤循环在 {@link QaRunRunner}。
 *
 * <p>改造前这里同时握着循环、内存进度缓存和 @Async 自调用三件事:自调用让 @Async 失效
 * (整条 run 同步跑在 HTTP 请求线程上),内存缓存在多实例下各说各话且无处回收。
 * 现在 run 的状态与进度一律以 DB 为准。
 */
@Component
public class QaExecutionDispatcher {

    private static final Logger log = LogManager.getLogger(QaExecutionDispatcher.class);

    /** run_code 唯一键;毫秒时间戳 + 进程内序号，避免同毫秒并发触发撞 uk_run_code */
    private static final AtomicInteger RUN_SEQ = new AtomicInteger();
    private static final ThreadLocal<SimpleDateFormat> CODE_FMT =
            ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyyMMddHHmmssSSS"));

    @Autowired
    private QaSuiteMapper suiteMapper;
    @Autowired
    private QaPlanMapper planMapper;
    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private QaRunDetailMapper runDetailMapper;
    @Autowired
    private QaRunRunner runner;

    /**
     * 触发单套件执行。建 run 行 (pending) 后立刻把循环交给 runner 的异步代理，
     * 调用方拿到 run 行即可开始轮询。
     */
    public QaRunDO triggerSuite(Long suiteId, String envCode, String triggerUser, String triggerType) {
        QaSuiteDO suite = suiteMapper.selectById(suiteId);
        if (suite == null) {
            throw new IllegalArgumentException("套件不存在: " + suiteId);
        }
        QaRunDO run = newRun(triggerUser, triggerType, suite.getTenantCode());
        run.setSuiteId(suiteId);
        run.setName(suite.getName());
        run.setEnvCode(firstNonEmpty(envCode, suite.getEnvCode()));
        run.setTotalSteps(0);
        runMapper.insert(run);

        runner.runSuiteAsync(run.getId());
        return run;
    }

    /**
     * 触发计划执行:一个 run 串起计划里的所有套件，明细按 suite_id 归属。
     */
    public QaRunDO triggerPlan(Long planId, String envCode, String triggerUser, String triggerType) {
        QaPlanDO plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("计划不存在: " + planId);
        }
        QaRunDO run = newRun(triggerUser, triggerType, plan.getTenantCode());
        run.setPlanId(planId);
        run.setName(plan.getName());
        run.setEnvCode(envCode);
        run.setTotalSteps(0);
        runMapper.insert(run);

        runner.runPlanAsync(run.getId());
        return run;
    }

    public String getRunStatus(Long runId) {
        QaRunDO run = runMapper.selectById(runId);
        return run != null ? run.getStatus() : "unknown";
    }

    /** 进度 = 已落库的明细行数;落一条明细推一格，不用缓存也能跨实例看准。 */
    public int getRunProgress(Long runId) {
        Long counted = runDetailMapper.selectCount(
                new QueryWrapper<QaRunDetailDO>().eq("run_id", runId));
        return counted == null ? 0 : counted.intValue();
    }

    private QaRunDO newRun(String triggerUser, String triggerType, String tenantCode) {
        QaRunDO run = new QaRunDO();
        run.setRunCode("RUN-" + CODE_FMT.get().format(new Date()) + "-" + RUN_SEQ.incrementAndGet());
        run.setTriggerType(triggerType != null ? triggerType : "manual");
        run.setStatus("pending");
        run.setStartedAt(new Date());
        run.setPassedSteps(0);
        run.setFailedSteps(0);
        run.setTriggerUser(triggerUser);
        run.setTenantCode(tenantCode);
        run.setAbortRequested(0);
        run.setGmtCreate(new Date());
        return run;
    }

    private static String firstNonEmpty(String a, String b) {
        return a != null && !a.isEmpty() ? a : b;
    }
}
