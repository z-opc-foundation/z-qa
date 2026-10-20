package com.zifang.z.qa.admin.engine;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.zifang.z.qa.admin.domain.entity.QaEnvDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ApiStepExecutor 单元测试 —— 对 JDK 内置 HttpServer 打真实 HTTP 往返。
 *
 * <p>不打桩 HttpURLConnection：变量替换、JSONPath 提取、断言求值、系统错误优先这几段
 * 只有真走一次 socket 才算验过（stub 掉连接就等于把被测逻辑一起 stub 掉了）。
 *
 * <p>覆盖：相对路径拼 env.baseUrl / 断言通过与失败 / 5xx 抢在断言之前定性 /
 * 变量缺失不发请求 / 提取变量写进 ctx 供后续步骤用。
 */
class ApiStepExecutorTest {

    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    static void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();

        server.createContext("/ok", ex -> respond(ex, 200,
                "{\"code\":200,\"msg\":\"success\",\"data\":{\"token\":\"abc123\",\"user\":{\"id\":7}}}"));
        server.createContext("/echo", ex -> respond(ex, 200, "{\"code\":200,\"echo\":" + readBody(ex) + "}"));
        server.createContext("/boom", ex -> respond(ex, 500, "{\"code\":500,\"msg\":\"内部异常\"}"));
        server.createContext("/html", ex -> respond(ex, 200, "<html><body>ok</body></html>"));
        server.setExecutor(null);
        server.start();
    }

    @AfterAll
    static void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private static String readBody(HttpExchange ex) throws java.io.IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = ex.getRequestBody().read(buf)) > 0) {
            baos.write(buf, 0, n);
        }
        return new String(baos.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void respond(HttpExchange ex, int status, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private final ApiStepExecutor executor = new ApiStepExecutor();
    private final Map<String, Object> ctx = new HashMap<>();
    private QaEnvDO env;

    @BeforeEach
    void setUp() {
        ctx.clear();
        env = new QaEnvDO();
        env.setCode("local");
        env.setBaseUrl(baseUrl);
    }

    private QaSuiteStepDO step(int no, String method, String url) {
        QaSuiteStepDO s = new QaSuiteStepDO();
        s.setStepNo(no);
        s.setStepName("step-" + no);
        s.setMethod(method);
        s.setUrl(url);
        s.setExecutionMode("api");
        s.setTargetMode("real");
        s.setTimeoutMs(5000);
        s.setRetryCount(0);
        return s;
    }

    @Test
    void relative_url_is_joined_with_env_base_url_and_assert_passes() {
        QaSuiteStepDO s = step(1, "GET", "/ok");
        s.setAssertRules("{\"status\":200,\"$.code\":200,\"$.msg\":\"success\"}");

        ApiStepExecutor.StepResult r = executor.execute(s, env, ctx);

        assertEquals(QaStepStatus.SUCCESS, r.getResult());
        assertEquals(200, r.getResponseStatus().intValue());
        assertEquals(baseUrl + "/ok", r.getRequestUrl());
    }

    @Test
    void assert_mismatch_is_fail_assert() {
        QaSuiteStepDO s = step(2, "GET", "/ok");
        s.setAssertRules("{\"$.code\":404}");

        ApiStepExecutor.StepResult r = executor.execute(s, env, ctx);

        assertEquals(QaStepStatus.FAIL_ASSERT, r.getResult());
        assertTrue(r.getErrorMessage().contains("断言失败"));
    }

    @Test
    void http_500_preempts_assert_so_defect_points_at_system() {
        QaSuiteStepDO s = step(3, "GET", "/boom");
        // 断言本身会通过（code=200 的形状），但 5xx 必须先定性，不能被断言吃掉
        s.setAssertRules("{\"status\":200}");

        ApiStepExecutor.StepResult r = executor.execute(s, env, ctx);

        assertEquals(QaStepStatus.FAIL_SYSTEM_ERROR, r.getResult());
        assertEquals(QaFailureClassifier.DEFECT_SYSTEM, QaFailureClassifier.classifyDefectType(r.getResult()));
    }

    @Test
    void missing_variable_fails_as_param_null_without_sending_request() {
        QaSuiteStepDO s = step(4, "GET", "/echo?q=${NOT_SET}");

        ApiStepExecutor.StepResult r = executor.execute(s, env, ctx);

        assertEquals(QaStepStatus.FAIL_PARAM_NULL, r.getResult());
        assertEquals(QaFailureClassifier.DEFECT_ENV_DATA, QaFailureClassifier.classifyDefectType(r.getResult()));
        // 没发请求 ⇒ 没有响应状态
        assertEquals(null, r.getResponseStatus());
    }

    @Test
    void extracted_vars_land_in_ctx_and_feed_the_next_step() {
        QaSuiteStepDO login = step(5, "GET", "/ok");
        login.setExtractRules("{\"TOKEN\":\"$.data.token\",\"USER_ID\":\"$.data.user.id\"}");

        ApiStepExecutor.StepResult first = executor.execute(login, env, ctx);

        assertEquals(QaStepStatus.SUCCESS, first.getResult());
        assertEquals("abc123", ctx.get("TOKEN"));

        QaSuiteStepDO next = step(6, "GET", "/echo");
        next.setBody("{\"uid\":${USER_ID}}");
        ApiStepExecutor.StepResult second = executor.execute(next, env, ctx);

        assertEquals(QaStepStatus.SUCCESS, second.getResult());
        assertTrue(second.getRequestBody().contains("\"uid\":7"));
    }

    @Test
    void non_json_response_defaults_to_2xx_assert() {
        QaSuiteStepDO s = step(7, "GET", "/html");

        ApiStepExecutor.StepResult r = executor.execute(s, env, ctx);

        assertEquals(QaStepStatus.SUCCESS, r.getResult());
    }

    @Test
    void unreachable_port_is_classified_as_navigation_failure() {
        QaEnvDO dead = new QaEnvDO();
        dead.setBaseUrl("http://127.0.0.1:1");
        QaSuiteStepDO s = step(8, "GET", "/ok");
        s.setTimeoutMs(2000);

        ApiStepExecutor.StepResult r = executor.execute(s, dead, ctx);

        assertEquals(QaStepStatus.FAIL_NAVIGATION, r.getResult());
        assertTrue(QaFailureClassifier.isRetryable(r.getResult()));
    }
}
