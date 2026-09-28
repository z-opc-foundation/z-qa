package com.zifang.z.qa.admin.engine;

/**
 * 步骤执行的终态词表 (蒸馏自 playwright-tc 的 ui_execution_case_record.status)。
 *
 * <p>价值不在状态数量，而在「失败要能说明为什么失败」：原先 z-qa 只有 fail/error 两态，
 * 一个 500 崩溃和一个业务断言不符长得一样，缺陷无从分诊。
 *
 * <p>与源平台的口径差异:源平台状态是大写下划线(SUCCESS/FAIL_ASSERT)，z-qa 全库状态字段
 * 一贯小写，故此处统一为小写；源平台的 WAITING(排队中) 在 z-qa 由 run 级 pending 承担，不重复建。
 */
public final class QaStepStatus {

    /** 已建行、尚未执行 (failFast/abort 之前先落这一态，便于区分「没跑」和「跑挂了」) */
    public static final String PENDING = "pending";
    /** 执行中 (长步骤供前端轮询) */
    public static final String RUNNING = "running";
    /** 通过 */
    public static final String SUCCESS = "success";
    /** 被跳过:前序失败触发 failFast，或被 abort */
    public static final String SKIP = "skip";
    /** 断言不符 —— 期望与实得确实不一致 */
    public static final String FAIL_ASSERT = "fail_assert";
    /** 被测系统自身报错 (HTTP 5xx 或响应体里的系统错误标记) —— 优先级高于断言与异常 */
    public static final String FAIL_SYSTEM_ERROR = "fail_system_error";
    /** 超时 */
    public static final String FAIL_TIMEOUT = "fail_timeout";
    /** 目标不可达:连不上、DNS 失败、URL 非法 */
    public static final String FAIL_NAVIGATION = "fail_navigation";
    /** 入参未解析:步骤里引用了上下文不存在的 ${VAR} */
    public static final String FAIL_PARAM_NULL = "fail_param_null";
    /** 兜底 */
    public static final String FAIL_UNKNOWN = "fail_unknown";

    private QaStepStatus() {
    }

    public static boolean isFail(String status) {
        return status != null && status.startsWith("fail_");
    }

    /** 已出结论，不再会被改写 */
    public static boolean isTerminal(String status) {
        return SUCCESS.equals(status) || SKIP.equals(status) || isFail(status);
    }
}
