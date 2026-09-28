package com.zifang.z.qa.admin.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * 失败定性单一真源 (蒸馏自 playwright-tc 的 TestExecutionService.classifyException +
 * DefectTypeClassifier)。
 *
 * <p>api 与 e2e 两条 StepExecutor 必须走同一口径，否则同一类失败在两种步骤下会被判成不同缺陷，
 * 报告与后续 AI 归因都会失真。
 */
public final class QaFailureClassifier {

    private static final Logger log = LogManager.getLogger(QaFailureClassifier.class);

    /** 被测系统自身缺陷 */
    public static final String DEFECT_SYSTEM = "system_defect";
    /** 测试脚本/步骤定义自身的问题 */
    public static final String DEFECT_SCRIPT = "script_defect";
    /** 环境或测试数据问题 */
    public static final String DEFECT_ENV_DATA = "env_data_defect";

    /**
     * 响应体里指向「系统自己报错」的结构标记。
     *
     * <p>刻意取保守小集合，且只在 HTTP 已经是 4xx/5xx 时才作为佐证 —— 避免把业务正常返回里
     * 一句「错误码：余额不足」误判成系统缺陷。源平台对应的是 UI 错误弹窗的三级识别，
     * 这里是它的 HTTP 等价面。
     */
    private static final String[] SYSTEM_ERROR_MARKERS = {"异常码", "错误码", "TID:", "Traceback", "StackOverflow"};

    private QaFailureClassifier() {
    }

    /**
     * 异常 → 细分失败状态。
     */
    public static String classifyThrown(Throwable error) {
        String msg = error.getMessage() != null ? error.getMessage() : "";
        String lower = msg.toLowerCase();

        if (error instanceof SocketTimeoutException || lower.contains("read timed out") || lower.contains("timeout")) {
            return QaStepStatus.FAIL_TIMEOUT;
        }
        if (error instanceof UnknownHostException || error instanceof ConnectException
                || error instanceof NoRouteToHostException || error instanceof MalformedURLException
                || lower.contains("unknown host") || lower.contains("connection refused")
                || lower.contains("connect") || lower.contains("navigation") || lower.contains("navigate")) {
            return QaStepStatus.FAIL_NAVIGATION;
        }
        if (error instanceof AssertionError) {
            return QaStepStatus.FAIL_ASSERT;
        }
        return QaStepStatus.FAIL_UNKNOWN;
    }

    /**
     * 判定这一步是不是「被测系统自己挂了」，返回系统错误的描述；不是则返回 null。
     *
     * <p>调用时机决定了这条规则的价值：必须在断言与异常分类**之前**问一次，否则 500 会被
     * 顺带失败的断言掩盖成 fail_assert，缺陷单直接开错方向。
     */
    public static String detectSystemError(int httpStatus, String responseBody) {
        if (httpStatus >= 500) {
            return "被测系统返回 HTTP " + httpStatus;
        }
        Integer envelopeCode = readEnvelopeCode(responseBody);
        if (envelopeCode != null && envelopeCode >= 500) {
            return "响应信封 code=" + envelopeCode + "，指向系统异常";
        }
        if (httpStatus >= 400 && responseBody != null) {
            for (String marker : SYSTEM_ERROR_MARKERS) {
                if (responseBody.contains(marker)) {
                    return "HTTP " + httpStatus + " 且响应含系统错误标记「" + marker + "」";
                }
            }
        }
        return null;
    }

    /**
     * 失败状态 → 缺陷类型；非失败态返回 null。口径与源平台 DefectTypeClassifier 一致。
     *
     * <p>fail_timeout / fail_navigation / fail_unknown 归脚本缺陷:步骤写法或等待策略问题居多；
     * fail_param_null 归环境数据缺陷:变量取不到是环境/数据没备齐，不是被测系统坏了。
     */
    public static String classifyDefectType(String status) {
        if (!QaStepStatus.isFail(status)) {
            return null;
        }
        switch (status) {
            case QaStepStatus.FAIL_ASSERT:
            case QaStepStatus.FAIL_SYSTEM_ERROR:
                return DEFECT_SYSTEM;
            case QaStepStatus.FAIL_PARAM_NULL:
                return DEFECT_ENV_DATA;
            case QaStepStatus.FAIL_TIMEOUT:
            case QaStepStatus.FAIL_NAVIGATION:
            case QaStepStatus.FAIL_UNKNOWN:
            default:
                return DEFECT_SCRIPT;
        }
    }

    /**
     * 缺陷三分类的中文口径 —— 报告头部与明细里显示的是「该开给谁」，不是英文码。
     */
    public static String defectLabel(String defectType) {
        if (DEFECT_SYSTEM.equals(defectType)) {
            return "系统缺陷";
        }
        if (DEFECT_SCRIPT.equals(defectType)) {
            return "脚本缺陷";
        }
        if (DEFECT_ENV_DATA.equals(defectType)) {
            return "环境/数据缺陷";
        }
        return defectType == null ? "" : defectType;
    }

    /**
     * 这一态值不值得重试。
     *
     * <p>断言不符与入参未解析重试也不会变，白等；系统 5xx 与超时/连不上往往是瞬时抖动，值得再来。
     * 源平台的重试在 action 层 (RetryAction)，步骤级重试是 z-qa 这一侧的等价改造。
     */
    public static boolean isRetryable(String status) {
        return QaStepStatus.FAIL_TIMEOUT.equals(status)
                || QaStepStatus.FAIL_NAVIGATION.equals(status)
                || QaStepStatus.FAIL_SYSTEM_ERROR.equals(status)
                || QaStepStatus.FAIL_UNKNOWN.equals(status);
    }

    private static Integer readEnvelopeCode(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return null;
        }
        String trimmed = responseBody.trim();
        if (!trimmed.startsWith("{")) {
            return null;
        }
        try {
            JsonObject obj = JsonUtil.parseObject(trimmed);
            Object code = obj.get("code");
            if (code == null) {
                return null;
            }
            return Integer.valueOf(String.valueOf(code).trim());
        } catch (Exception e) {
            log.debug("响应信封 code 解析失败: {}", e.getMessage());
            return null;
        }
    }
}
