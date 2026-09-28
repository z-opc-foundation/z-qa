package com.zifang.z.qa.admin.engine;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.model.JsonObject;
import com.zifang.z.qa.admin.domain.entity.QaEnvDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTTP 步骤执行器 — 真实调用外部 HTTP API (含变量替换 + JSONPath 提取 + 断言).
 * <p>
 * 一次调用 = 一个 attempt。failFast / retry / abort 是策略，归 QaRunRunner。
 * <p>
 * 支持 GET/POST/PUT/DELETE;
 * body/headers/url 支持 ${VAR} 占位符 (从 ctx 替换，取不到即定性 fail_param_null);
 * 响应 body 解析为 JSON, 提取 JSONPath → ${VAR};
 * 断言规则: {"status": 200} 或 {"$.code": 200} 或 {"$.msg": "success"};
 * 结果态取值见 {@link QaStepStatus}。
 */

@Component
public class ApiStepExecutor {

    private static final Logger log = LogManager.getLogger(ApiStepExecutor.class);

    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * 执行单个 HTTP 步骤.
     *
     * @param step 步骤定义
     * @param env  环境 (拿 baseUrl)
     * @param ctx  变量上下文 (会被 step 写入新变量)
     * @return 单步执行结果
     */
    public StepResult execute(QaSuiteStepDO step, QaEnvDO env, Map<String, Object> ctx) {
        StepResult result = new StepResult();
        result.setStepNo(step.getStepNo());
        result.setStepName(step.getStepName());
        result.setMethod(step.getMethod());
        JsonObject beforeCtx = new JsonObject();
        if (ctx != null) {
            for (Map.Entry<String, Object> e : ctx.entrySet()) {
                beforeCtx.put(e.getKey(), e.getValue());
            }
        }
        result.setVariablesBefore(JsonUtil.toJson(beforeCtx));

        long t0 = System.currentTimeMillis();
        try {
            // 1. 替换变量
            Set<String> missingVars = new LinkedHashSet<>();
            String url = replaceVars(step.getUrl(), ctx, missingVars);
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                // 相对路径 → 用 env.baseUrl 拼接
                String base = env != null && env.getBaseUrl() != null ? env.getBaseUrl() : "http://127.0.0.1:8888";
                url = base + (url.startsWith("/") ? url : "/" + url);
            }
            result.setRequestUrl(url);

            String body = replaceVars(step.getBody(), ctx, missingVars);
            result.setRequestBody(body);

            Map<String, String> headers = parseJsonObject(step.getHeadersJson());
            for (Map.Entry<String, String> e : headers.entrySet()) {
                e.setValue(replaceVars(e.getValue(), ctx, missingVars));
            }
            // 默认加 Content-Type
            if (body != null && !body.isEmpty() && !headers.containsKey("Content-Type")) {
                headers.put("Content-Type", "application/json;charset=UTF-8");
            }
            result.setRequestHeaders(JsonUtil.toJson(headers));

            // 取不到的 ${VAR} 当环境数据问题定性，不发请求。
            // 原先静默替换成空串，会把 "/api/x?id=" 这种残破请求发出去，报出来的是一行断言失败，
            // 看不出是变量没备齐。
            if (!missingVars.isEmpty()) {
                result.setResult(QaStepStatus.FAIL_PARAM_NULL);
                result.setErrorMessage("上下文缺少变量，未发起请求: " + missingVars);
                result.setAssertResults("{\"passed\": false, \"items\": [], \"skipped\": \"param_unresolved\"}");
                return result;
            }

            // 2. 真正发请求
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(step.getMethod() != null ? step.getMethod() : "GET");
            conn.setConnectTimeout(step.getTimeoutMs() != null ? step.getTimeoutMs() : 10000);
            conn.setReadTimeout(step.getTimeoutMs() != null ? step.getTimeoutMs() : 10000);
            conn.setDoInput(true);
            for (Map.Entry<String, String> h : headers.entrySet()) {
                conn.setRequestProperty(h.getKey(), h.getValue());
            }
            if (body != null && !body.isEmpty() && !"GET".equalsIgnoreCase(step.getMethod())) {
                conn.setDoOutput(true);
                conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String respBody = readBody(conn);
            result.setResponseStatus(status);
            result.setResponseBody(respBody);

            // 3. 提取变量
            if (step.getExtractRules() != null && !step.getExtractRules().isEmpty()) {
                Map<String, Object> extracted = extractVariables(respBody, step.getExtractRules());
                ctx.putAll(extracted);
                result.setExtractedVars(JsonUtil.toJson(extracted));
            }

            // 4. 系统错误优先于断言:5xx 若被顺带失败的断言吃掉，缺陷会直接开错方向
            String systemError = QaFailureClassifier.detectSystemError(status, respBody);
            if (systemError != null) {
                result.setResult(QaStepStatus.FAIL_SYSTEM_ERROR);
                result.setErrorMessage(systemError);
                result.setAssertResults("{\"passed\": false, \"items\": [], \"skipped\": \"system_error_preempts_assert\"}");
                return result;
            }

            // 5. 断言
            String assertResult = "{\"passed\": true, \"items\": []}";
            if (step.getAssertRules() != null && !step.getAssertRules().isEmpty()) {
                assertResult = runAssertions(status, respBody, step.getAssertRules());
                JsonObject ar = JsonUtil.parseObject(assertResult);
                if (!ar.getBoolean("passed")) {
                    result.setResult(QaStepStatus.FAIL_ASSERT);
                    result.setErrorMessage("断言失败: " + ar.getJsonArray("items"));
                } else {
                    result.setResult(QaStepStatus.SUCCESS);
                }
            } else {
                // 默认断言: 状态码 2xx = pass
                if (status >= 200 && status < 300) {
                    result.setResult(QaStepStatus.SUCCESS);
                } else {
                    result.setResult(QaStepStatus.FAIL_ASSERT);
                    result.setErrorMessage("默认状态码断言未通过 (期望 2xx), 实得: " + status);
                }
            }
            result.setAssertResults(assertResult);
        } catch (Exception e) {
            log.error("Step execution failed: step={}, url={}", step.getStepNo(), result.getRequestUrl(), e);
            String classified = QaFailureClassifier.classifyThrown(e);
            result.setResult(classified);
            result.setErrorMessage("执行异常[" + classified + "]: "
                    + e.getClass().getSimpleName() + " " + e.getMessage());
        } finally {
            result.setDurationMs(System.currentTimeMillis() - t0);
            JsonObject afterCtx = new JsonObject();
            for (Map.Entry<String, Object> e : ctx.entrySet()) {
                afterCtx.put(e.getKey(), e.getValue());
            }
            result.setVariablesAfter(JsonUtil.toJson(afterCtx));
        }
        return result;
    }

    private String readBody(HttpURLConnection conn) {
        try {
            java.io.InputStream is = conn.getResponseCode() >= 400 ? conn.getErrorStream() : conn.getInputStream();
            if (is == null) return "";
            byte[] buf = new byte[8192];
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            return baos.toString("UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private Map<String, String> parseJsonObject(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || json.isEmpty()) return result;
        try {
            JsonObject obj = JsonUtil.parseObject(json);
            for (Map.Entry<String, Object> e : obj.getAllKeyValue()) {
                Object v = e.getValue();
                result.put(e.getKey(), v == null ? "" : v.toString());
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private Map<String, Object> extractVariables(String body, String rulesJson) {
        Map<String, Object> result = new HashMap<>();
        if (body == null || body.isEmpty() || rulesJson == null || rulesJson.isEmpty()) return result;
        try {
            JsonObject bodyJson = JsonUtil.parseObject(body);
            JsonObject rules = JsonUtil.parseObject(rulesJson);
            for (Map.Entry<String, Object> e : rules.getAllKeyValue()) {
                String varName = e.getKey();
                String jsonPath = String.valueOf(e.getValue());
                Object value = evaluateJsonPath(bodyJson, jsonPath);
                if (value != null) {
                    result.put(varName, value);
                }
            }
        } catch (Exception e) {
            log.warn("extractVariables failed: {}", e.getMessage());
        }
        return result;
    }

    /**
     * 极简 JSONPath: 仅支持 $.key.subkey 和 $.array[0].field 形式.
     */
    private Object evaluateJsonPath(JsonObject json, String path) {
        if (path == null || !path.startsWith("$")) return null;
        String[] parts = path.substring(2).split("\\.");
        Object cur = json;
        for (String p : parts) {
            if (cur == null) return null;
            // 支持 array[idx]
            Matcher m = Pattern.compile("(\\w+)\\[(\\d+)\\]").matcher(p);
            if (m.find()) {
                String key = m.group(1);
                int idx = Integer.parseInt(m.group(2));
                if (cur instanceof JsonObject) {
                    cur = ((JsonObject) cur).get(key);
                }
                if (cur instanceof com.zifang.util.json.model.JsonArray) {
                    cur = ((com.zifang.util.json.model.JsonArray) cur).get(idx);
                }
            } else {
                if (cur instanceof JsonObject) {
                    cur = ((JsonObject) cur).get(p);
                } else {
                    return null;
                }
            }
        }
        return cur;
    }

    private String runAssertions(int status, String body, String rulesJson) {
        JsonObject result = new JsonObject();
        result.put("passed", true);
        com.zifang.util.json.model.JsonArray items = new com.zifang.util.json.model.JsonArray();
        try {
            // 先看 body 是不是 JSON (尝试 parse, 失败则当文本/html)
            JsonObject bodyJson = new JsonObject();
            boolean bodyIsJson = false;
            if (body != null && !body.isEmpty()) {
                String trimmed = body.trim();
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    try {
                        bodyJson = JsonUtil.parseObject(body);
                        bodyIsJson = true;
                    } catch (Exception ex) {
                        // 不是 JSON (可能是 HTML / 文本), 保持空对象
                        log.debug("response body is not JSON: {}", ex.getMessage());
                    }
                }
            }
            JsonObject rules = JsonUtil.parseObject(rulesJson);
            for (Map.Entry<String, Object> entry : rules.getAllKeyValue()) {
                String expr = entry.getKey();
                Object expected = entry.getValue();
                Object actual;
                if ("status".equals(expr)) {
                    actual = status;
                } else if (expr.startsWith("$")) {
                    // JSONPath 只能在 JSON 响应上工作
                    if (!bodyIsJson) {
                        actual = null;
                    } else {
                        actual = evaluateJsonPath(bodyJson, expr);
                    }
                } else {
                    // 简单 key 也只在 JSON 上工作
                    actual = bodyIsJson ? bodyJson.get(expr) : null;
                }
                boolean pass = expected == null ? actual == null : String.valueOf(expected).equals(String.valueOf(actual));
                JsonObject item = new JsonObject();
                item.put("expr", expr);
                item.put("expected", expected);
                item.put("actual", actual);
                item.put("pass", pass);
                items.add(item);
                if (!pass) result.put("passed", false);
            }
        } catch (Exception e) {
            result.put("passed", false);
            JsonObject item = new JsonObject();
            item.put("error", e.getMessage());
            items.add(item);
        }
        result.put("items", items);
        return JsonUtil.toJson(result);
    }

    private String replaceVars(String input, Map<String, Object> ctx, Set<String> missingOut) {
        if (input == null || input.isEmpty()) return input;
        Matcher m = VAR_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String varName = m.group(1);
            Object val = ctx.get(varName);
            if (val == null) {
                missingOut.add(varName);
            }
            String replacement = val == null ? "" : val.toString();
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }


    public static class StepResult {
        private Integer stepNo;
        private String stepName;
        private String method;
        private String requestUrl;
        private String requestHeaders;
        private String requestBody;
        private String variablesBefore;
        private String variablesAfter;
        private Integer responseStatus;
        private String responseBody;
        private String extractedVars;
        private String assertResults;
        private String result;
        private String errorMessage;
        private Long durationMs;

        public Integer getStepNo() {
            return stepNo;
        }

        public void setStepNo(Integer stepNo) {
            this.stepNo = stepNo;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        public String getRequestUrl() {
            return requestUrl;
        }

        public void setRequestUrl(String requestUrl) {
            this.requestUrl = requestUrl;
        }

        public String getRequestHeaders() {
            return requestHeaders;
        }

        public void setRequestHeaders(String requestHeaders) {
            this.requestHeaders = requestHeaders;
        }

        public String getRequestBody() {
            return requestBody;
        }

        public void setRequestBody(String requestBody) {
            this.requestBody = requestBody;
        }

        public String getVariablesBefore() {
            return variablesBefore;
        }

        public void setVariablesBefore(String variablesBefore) {
            this.variablesBefore = variablesBefore;
        }

        public String getVariablesAfter() {
            return variablesAfter;
        }

        public void setVariablesAfter(String variablesAfter) {
            this.variablesAfter = variablesAfter;
        }

        public Integer getResponseStatus() {
            return responseStatus;
        }

        public void setResponseStatus(Integer responseStatus) {
            this.responseStatus = responseStatus;
        }

        public String getResponseBody() {
            return responseBody;
        }

        public void setResponseBody(String responseBody) {
            this.responseBody = responseBody;
        }

        public String getExtractedVars() {
            return extractedVars;
        }

        public void setExtractedVars(String extractedVars) {
            this.extractedVars = extractedVars;
        }

        public String getAssertResults() {
            return assertResults;
        }

        public void setAssertResults(String assertResults) {
            this.assertResults = assertResults;
        }

        public String getResult() {
            return result;
        }

        public void setResult(String result) {
            this.result = result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public Long getDurationMs() {
            return durationMs;
        }

        public void setDurationMs(Long durationMs) {
            this.durationMs = durationMs;
        }
    }
}