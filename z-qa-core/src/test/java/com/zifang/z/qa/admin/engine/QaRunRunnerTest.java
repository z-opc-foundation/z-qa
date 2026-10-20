package com.zifang.z.qa.admin.engine;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * QaRunRunner 单元测试 —— 步骤循环里那几条最容易回归的策略。
 *
 * <p>不引 Mockito（组织口径：零 mock 框架，测试只吃 JDK 动态代理 + 反射注入），
 * 不用 @SpringBootTest。Mapper 全是接口，JDK 代理按方法名分派即可。
 *
 * <p>覆盖：failFast 的粒度是套件而非整轮 / abort 标志走 DB / 计数只写计数列 /
 * 计划里失效套件被跳过 / e2e 步骤不被降级成一次 HTTP。
 */
class QaRunRunnerTest {

    private final List<QaRunDetailDO> insertedDetails = new ArrayList<>();
    private final List<QaRunDO> runUpdates = new ArrayList<>();
    private final Map<Long, QaRunDO> runs = new HashMap<>();
    private final Map<Long, QaSuiteDO> suites = new HashMap<>();
    private final Map<Long, List<QaSuiteStepDO>> stepsBySuite = new HashMap<>();
    private final Map<Long, QaPlanDO> plans = new HashMap<>();
    private final AtomicInteger executorCalls = new AtomicInteger();

    /** 按 step.id 指定该步的结果；没指定的走 defaultResult。
     *  注意不能按 stepNo 做 key —— step_no 是**套件内**序号，两个套件都从 1 开始。 */
    private final Map<Long, String> resultByStepId = new HashMap<>();
    private String defaultResult = QaStepStatus.SUCCESS;
    /** 每步被调用时它看到的 ctx 键集合 —— 用来验"套件之间变量上下文重置"。 */
    private final Map<Long, Object> ctxSeenByStepId = new HashMap<>();

    /** 只覆写 execute，本测试不关心真实 HTTP（那一段由 ApiStepExecutorTest 打真实 socket 覆盖）。 */
    private final ApiStepExecutor executor = new ApiStepExecutor() {
        @Override
        public StepResult execute(QaSuiteStepDO step, QaEnvDO env, Map<String, Object> ctx) {
            executorCalls.incrementAndGet();
            ctxSeenByStepId.put(step.getId(), new HashMap<>(ctx));
            // 模拟"提取变量写进 ctx"，让套件间重置这条断言有东西可看
            if ("/extract".equals(step.getUrl())) {
                ctx.put("TOKEN", "from-step-" + step.getStepNo());
            }
            String result = resultByStepId.getOrDefault(step.getId(), defaultResult);
            StepResult sr = new StepResult();
            sr.setStepNo(step.getStepNo());
            sr.setStepName(step.getStepName());
            sr.setMethod(step.getMethod());
            sr.setRequestUrl(step.getUrl());
            sr.setResult(result);
            sr.setDurationMs(1L);
            if (!QaStepStatus.SUCCESS.equals(result)) {
                sr.setErrorMessage("stubbed " + result);
            }
            return sr;
        }
    };

    private QaRunRunner runner;

    @BeforeEach
    void setUp() {
        insertedDetails.clear();
        runUpdates.clear();
        runs.clear();
        suites.clear();
        stepsBySuite.clear();
        plans.clear();
        executorCalls.set(0);
        defaultResult = QaStepStatus.SUCCESS;
        resultByStepId.clear();
        ctxSeenByStepId.clear();
        runner = new QaRunRunner();
        inject("runMapper", runMapper());
        inject("runDetailMapper", runDetailMapper());
        inject("suiteMapper", suiteMapper());
        inject("stepMapper", stepMapper());
        inject("envMapper", envMapper());
        inject("planMapper", planMapper());
        inject("apiStepExecutor", executor);
        inject("notifyService", null);
    }

    // ===== JDK 代理 =====

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> iface, java.lang.reflect.InvocationHandler h) {
        return (T) Proxy.newProxyInstance(QaRunRunnerTest.class.getClassLoader(), new Class<?>[]{iface}, h);
    }

    private static Object defaultFor(Class<?> rt) {
        if (rt == int.class) return 0;
        if (rt == long.class) return 0L;
        if (rt == boolean.class) return Boolean.FALSE;
        return null;
    }

    private QaRunMapper runMapper() {
        return proxy(QaRunMapper.class, (p, m, a) -> {
            switch (m.getName()) {
                case "selectById":
                    return runs.get(a[0]);
                case "updateById":
                    runUpdates.add((QaRunDO) a[0]);
                    return 1;
                default:
                    return defaultFor(m.getReturnType());
            }
        });
    }

    private QaRunDetailMapper runDetailMapper() {
        return proxy(QaRunDetailMapper.class, (p, m, a) -> {
            if ("insert".equals(m.getName())) {
                insertedDetails.add((QaRunDetailDO) a[0]);
                return 1;
            }
            return defaultFor(m.getReturnType());
        });
    }

    private QaSuiteMapper suiteMapper() {
        return proxy(QaSuiteMapper.class, (p, m, a) -> {
            if ("selectById".equals(m.getName())) {
                return suites.get(a[0]);
            }
            return defaultFor(m.getReturnType());
        });
    }

    private QaSuiteStepMapper stepMapper() {
        return proxy(QaSuiteStepMapper.class, (p, m, a) -> {
            if ("selectBySuiteId".equals(m.getName())) {
                return stepsBySuite.getOrDefault(a[0], new ArrayList<>());
            }
            return defaultFor(m.getReturnType());
        });
    }

    private QaEnvMapper envMapper() {
        QaEnvDO local = new QaEnvDO();
        local.setId(1L);
        local.setCode("local");
        local.setBaseUrl("http://127.0.0.1:0");
        return proxy(QaEnvMapper.class, (p, m, a) -> {
            if ("selectOne".equals(m.getName())) {
                return local;
            }
            return defaultFor(m.getReturnType());
        });
    }

    private QaPlanMapper planMapper() {
        return proxy(QaPlanMapper.class, (p, m, a) -> {
            if ("selectById".equals(m.getName())) {
                return plans.get(a[0]);
            }
            return defaultFor(m.getReturnType());
        });
    }

    private void inject(String field, Object value) {
        try {
            Field f = QaRunRunner.class.getDeclaredField(field);
            f.setAccessible(true);
            f.set(runner, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("注入 " + field + " 失败", e);
        }
    }

    // ===== 夹具 =====

    private QaSuiteDO suite(long id, int failFast) {
        QaSuiteDO s = new QaSuiteDO();
        s.setId(id);
        s.setCode("suite-" + id);
        s.setName("suite-" + id);
        s.setFailFast(failFast);
        suites.put(id, s);
        return s;
    }

    private QaSuiteStepDO step(long suiteId, int no, String mode) {
        QaSuiteStepDO s = new QaSuiteStepDO();
        s.setId(suiteId * 100 + no);
        s.setSuiteId(suiteId);
        s.setStepNo(no);
        s.setStepName("s" + no);
        s.setExecutionMode(mode);
        s.setMethod("GET");
        s.setUrl("/x");
        s.setTargetMode("real");
        s.setRetryCount(0);
        stepsBySuite.computeIfAbsent(suiteId, k -> new ArrayList<>()).add(s);
        return s;
    }

    private QaRunDO run(long id, Long suiteId, Long planId) {
        QaRunDO r = new QaRunDO();
        r.setId(id);
        r.setSuiteId(suiteId);
        r.setPlanId(planId);
        runs.put(id, r);
        return r;
    }

    private List<QaRunDetailDO> detailsOfSuite(long suiteId) {
        List<QaRunDetailDO> out = new ArrayList<>();
        for (QaRunDetailDO d : insertedDetails) {
            if (Long.valueOf(suiteId).equals(d.getSuiteId())) {
                out.add(d);
            }
        }
        return out;
    }

    // ===== 用例 =====

    @Test
    void all_pass_makes_run_success() {
        suite(1L, 0);
        step(1L, 1, "api");
        step(1L, 2, "api");
        run(100L, 1L, null);

        runner.runSuiteAsync(100L);

        assertEquals(2, insertedDetails.size());
        assertEquals("success", terminalStatus());
        assertEquals(QaStepStatus.SUCCESS, insertedDetails.get(0).getResult());
    }

    @Test
    void fail_fast_halts_only_the_failing_suite_not_the_whole_plan() {
        // 套件 A 开遇错即停，套件 B 不开；A 的第 1 步失败
        suite(1L, 1);
        QaSuiteStepDO a1 = step(1L, 1, "api");
        step(1L, 2, "api");
        suite(2L, 0);
        step(2L, 1, "api");

        QaPlanDO plan = new QaPlanDO();
        plan.setId(50L);
        plan.setSuiteIds("[1,2]");
        plans.put(50L, plan);

        run(100L, null, 50L);
        resultByStepId.put(a1.getId(), QaStepStatus.FAIL_ASSERT);

        runner.runPlanAsync(100L);

        List<QaRunDetailDO> a = detailsOfSuite(1L);
        assertEquals(2, a.size());
        assertEquals(QaStepStatus.FAIL_ASSERT, a.get(0).getResult());
        assertEquals(QaStepStatus.SKIP, a.get(1).getResult());

        // 关键：套件 B 照常执行，不被 A 的失败连坐
        List<QaRunDetailDO> b = detailsOfSuite(2L);
        assertEquals(1, b.size());
        assertEquals(QaStepStatus.SUCCESS, b.get(0).getResult());

        assertEquals("failed", terminalStatus());
    }

    @Test
    void abort_requested_in_db_turns_remaining_steps_into_skip_and_run_cancelled() {
        suite(1L, 0);
        step(1L, 1, "api");
        step(1L, 2, "api");
        step(1L, 3, "api");
        run(100L, 1L, null);
        // abort 标志走 DB：发起中止的实例未必是跑这条 run 的实例
        runs.get(100L).setAbortRequested(1);

        runner.runSuiteAsync(100L);

        assertEquals(3, insertedDetails.size());
        for (QaRunDetailDO d : insertedDetails) {
            assertEquals(QaStepStatus.SKIP, d.getResult());
        }
        assertEquals("cancelled", terminalStatus());
        assertEquals(0, executorCalls.get());
    }

    @Test
    void counter_updates_never_carry_abort_flag_or_other_columns() {
        // 整行回写会把 abort_requested 覆盖回内存快照的旧值，中止请求就丢了
        suite(1L, 0);
        step(1L, 1, "api");
        run(100L, 1L, null);

        runner.runSuiteAsync(100L);

        QaRunDO counterUpdate = null;
        for (QaRunDO u : runUpdates) {
            if (u.getPassedSteps() != null) {
                counterUpdate = u;
            }
        }
        assertNotNull(counterUpdate, "应有只写计数列的 update");
        assertEquals(1, counterUpdate.getPassedSteps().intValue());
        assertEquals(null, counterUpdate.getStatus());
        assertEquals(null, counterUpdate.getAbortRequested());
    }

    @Test
    void plan_referencing_missing_suite_is_skipped_not_fatal() {
        suite(2L, 0);
        step(2L, 1, "api");
        QaPlanDO plan = new QaPlanDO();
        plan.setId(50L);
        plan.setSuiteIds("[999,2]");
        plans.put(50L, plan);
        run(100L, null, 50L);

        runner.runPlanAsync(100L);

        assertEquals(1, insertedDetails.size());
        assertEquals("success", terminalStatus());
    }

    @Test
    void plan_with_no_executable_suite_finishes_as_error_not_hanging_running() {
        QaPlanDO plan = new QaPlanDO();
        plan.setId(50L);
        plan.setSuiteIds("[999]");
        plans.put(50L, plan);
        run(100L, null, 50L);

        runner.runPlanAsync(100L);

        assertEquals("failed", terminalStatus());
        assertEquals(0, insertedDetails.size());
    }

    @Test
    void e2e_step_is_never_degraded_into_one_http_call() {
        // 改造前 e2e 步骤会拿空 url 去拼 baseUrl，报出一行莫名的 HTTP 失败
        suite(1L, 0);
        step(1L, 1, "e2e");
        run(100L, 1L, null);

        runner.runSuiteAsync(100L);

        assertEquals(0, executorCalls.get());
        assertEquals(1, insertedDetails.size());
        assertEquals(QaStepStatus.FAIL_UNKNOWN, insertedDetails.get(0).getResult());
    }

    @Test
    void detail_rows_carry_defect_type_for_ai_attribution() {
        suite(1L, 0);
        QaSuiteStepDO only = step(1L, 1, "api");
        run(100L, 1L, null);
        resultByStepId.put(only.getId(), QaStepStatus.FAIL_SYSTEM_ERROR);

        runner.runSuiteAsync(100L);

        assertEquals(QaFailureClassifier.DEFECT_SYSTEM, insertedDetails.get(0).getDefectType());
    }

    private String terminalStatus() {
        String status = null;
        for (QaRunDO u : runUpdates) {
            if (u.getStatus() != null) {
                status = u.getStatus();
            }
        }
        return status;
    }

    @Test
    void variable_context_is_reset_between_suites() {
        // 上一个套件提取的 ${TOKEN} 不该漏进下一个套件
        suite(1L, 0);
        QaSuiteStepDO s1 = step(1L, 1, "api");
        s1.setUrl("/extract");
        QaSuiteStepDO s1b = step(1L, 2, "api");
        s1b.setUrl("/uses-token");
        suite(2L, 0);
        QaSuiteStepDO s2 = step(2L, 1, "api");
        s2.setUrl("/uses-token");
        QaPlanDO plan = new QaPlanDO();
        plan.setId(50L);
        plan.setSuiteIds("[1,2]");
        plans.put(50L, plan);
        run(100L, null, 50L);

        runner.runPlanAsync(100L);

        // 提取确实落进了 ctx（同套件下一步看得见），但**不跨套件**泄漏
        assertEquals(Collections.emptySet(), ((Map<?, ?>) ctxSeenByStepId.get(s1.getId())).keySet());
        assertEquals(Collections.singleton("TOKEN"), ((Map<?, ?>) ctxSeenByStepId.get(s1b.getId())).keySet());
        assertEquals(Collections.emptySet(), ((Map<?, ?>) ctxSeenByStepId.get(s2.getId())).keySet());
    }
}
