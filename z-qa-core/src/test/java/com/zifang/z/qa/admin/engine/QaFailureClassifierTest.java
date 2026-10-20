package com.zifang.z.qa.admin.engine;

import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * QaFailureClassifier 单元测试 —— 十态状态机里"什么算失败、失败算谁的锅"的单一真源。
 *
 * <p>覆盖：异常分类 / 系统错误优先于断言 / 缺陷三分类映射 / 可重试判定。
 */
class QaFailureClassifierTest {

    // ===== classifyThrown =====

    @Test
    void thrown_socket_timeout_is_fail_timeout() {
        assertEquals(QaStepStatus.FAIL_TIMEOUT,
                QaFailureClassifier.classifyThrown(new SocketTimeoutException("read timed out")));
    }

    @Test
    void thrown_unknown_host_is_fail_navigation() {
        assertEquals(QaStepStatus.FAIL_NAVIGATION,
                QaFailureClassifier.classifyThrown(new UnknownHostException("qa.internal")));
    }

    @Test
    void thrown_assertion_error_is_fail_assert() {
        assertEquals(QaStepStatus.FAIL_ASSERT,
                QaFailureClassifier.classifyThrown(new AssertionError("期望 200 实得 500")));
    }

    @Test
    void thrown_anything_else_is_fail_unknown() {
        assertEquals(QaStepStatus.FAIL_UNKNOWN,
                QaFailureClassifier.classifyThrown(new IllegalStateException("boom")));
    }

    // ===== detectSystemError：必须在断言之前判，否则 5xx 会被 fail_assert 掩盖 =====

    @Test
    void system_error_http_500_detected() {
        assertTrue(QaFailureClassifier.detectSystemError(500, "internal error").contains("HTTP 500"));
    }

    @Test
    void system_error_envelope_code_500_detected_even_on_http_200() {
        String msg = QaFailureClassifier.detectSystemError(200, "{\"code\":500,\"msg\":\"boom\"}");
        assertTrue(msg != null && msg.contains("code=500"));
    }

    @Test
    void system_error_marker_only_counts_on_4xx_5xx() {
        // 200 + 业务文案里带「错误码」不是系统缺陷，否则余额不足这类正常返回会被误判
        assertNull(QaFailureClassifier.detectSystemError(200, "{\"msg\":\"错误码：余额不足\"}"));
        assertTrue(QaFailureClassifier.detectSystemError(400, "{\"msg\":\"错误码：余额不足\"}")
                .contains("错误码"));
    }

    @Test
    void not_system_error_on_healthy_response() {
        assertNull(QaFailureClassifier.detectSystemError(200, "{\"code\":200,\"msg\":\"success\"}"));
    }

    @Test
    void system_error_tolerates_non_json_and_null_body() {
        assertNull(QaFailureClassifier.detectSystemError(200, null));
        assertNull(QaFailureClassifier.detectSystemError(200, "<html>ok</html>"));
        assertNull(QaFailureClassifier.detectSystemError(200, "not-json-at-all"));
    }

    // ===== classifyDefectType =====

    @Test
    void non_fail_status_has_no_defect_type() {
        assertNull(QaFailureClassifier.classifyDefectType(QaStepStatus.SUCCESS));
        assertNull(QaFailureClassifier.classifyDefectType(QaStepStatus.SKIP));
    }

    @Test
    void assert_and_system_error_are_system_defects() {
        assertEquals(QaFailureClassifier.DEFECT_SYSTEM,
                QaFailureClassifier.classifyDefectType(QaStepStatus.FAIL_ASSERT));
        assertEquals(QaFailureClassifier.DEFECT_SYSTEM,
                QaFailureClassifier.classifyDefectType(QaStepStatus.FAIL_SYSTEM_ERROR));
    }

    @Test
    void param_null_is_env_data_defect() {
        assertEquals(QaFailureClassifier.DEFECT_ENV_DATA,
                QaFailureClassifier.classifyDefectType(QaStepStatus.FAIL_PARAM_NULL));
    }

    @Test
    void timeout_navigation_unknown_are_script_defects() {
        for (String s : new String[]{QaStepStatus.FAIL_TIMEOUT, QaStepStatus.FAIL_NAVIGATION,
                QaStepStatus.FAIL_UNKNOWN}) {
            assertEquals(QaFailureClassifier.DEFECT_SCRIPT, QaFailureClassifier.classifyDefectType(s));
        }
    }

    // ===== isRetryable：断言不符与入参缺失重试也不会变 =====

    @Test
    void retryable_excludes_assert_and_param_null() {
        assertFalse(QaFailureClassifier.isRetryable(QaStepStatus.FAIL_ASSERT));
        assertFalse(QaFailureClassifier.isRetryable(QaStepStatus.FAIL_PARAM_NULL));
        assertTrue(QaFailureClassifier.isRetryable(QaStepStatus.FAIL_TIMEOUT));
        assertTrue(QaFailureClassifier.isRetryable(QaStepStatus.FAIL_NAVIGATION));
        assertTrue(QaFailureClassifier.isRetryable(QaStepStatus.FAIL_SYSTEM_ERROR));
    }

    // ===== defectLabel =====

    @Test
    void defect_labels_are_chinese() {
        assertEquals("系统缺陷", QaFailureClassifier.defectLabel(QaFailureClassifier.DEFECT_SYSTEM));
        assertEquals("脚本缺陷", QaFailureClassifier.defectLabel(QaFailureClassifier.DEFECT_SCRIPT));
        assertEquals("环境/数据缺陷", QaFailureClassifier.defectLabel(QaFailureClassifier.DEFECT_ENV_DATA));
        assertEquals("", QaFailureClassifier.defectLabel(null));
    }
}
