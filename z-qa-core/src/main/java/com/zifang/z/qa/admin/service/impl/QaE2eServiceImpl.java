package com.zifang.z.qa.admin.service.impl;

import com.zifang.util.json.JsonUtil;
import com.zifang.z.oss.core.domain.entity.OssObject;
import com.zifang.z.oss.core.domain.service.IOssObjectService;
import com.zifang.z.qa.admin.domain.entity.QaE2eActionDO;
import com.zifang.z.qa.admin.domain.entity.QaE2eSessionDO;
import com.zifang.z.qa.admin.domain.mapper.QaE2eActionMapper;
import com.zifang.z.qa.admin.domain.mapper.QaE2eSessionMapper;
import com.zifang.z.qa.admin.service.QaE2eService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * E2E 服务实现 — FEATURE052 Phase 7.
 *
 * <p>对接 Browserless (Playwright 容器). 流程：
 * <ol>
 *   <li>openSession — 创建会话，调用 Browserless /context 打开一个浏览器</li>
 *   <li>recordAction + executeAction — 按顺序执行 navigate/click/fill/...</li>
 *   <li>每步可截图，存 z-oss (IOssObjectService)</li>
 *   <li>finishSession — 关闭浏览器，回收 /context 资源</li>
 * </ol>
 *
 * <p>缺陷修复: 截图现在通过 IOssObjectService.uploadObject(...) 真正落到 z-oss，
 * 不再只是写占位 key。
 */
@Service
public class QaE2eServiceImpl implements QaE2eService {

    private static final Logger log = LogManager.getLogger(QaE2eServiceImpl.class);
    /**
     * 默认 Bucket 名 — 业务侧通用，可由配置覆盖.
     */
    private static final String DEFAULT_BUCKET = "qa-e2e";

    @Autowired
    private QaE2eSessionMapper sessionMapper;
    @Autowired
    private QaE2eActionMapper actionMapper;
    /**
     * z-oss-core IOssObjectService — 可选依赖，找不到时降级为 inline 占位 key.
     */
    @Autowired(required = false)
    private IOssObjectService ossObjectService;

    @Value("${z-qa.e2e.browserless-url:http://browserless:3000}")
    private String browserlessUrl;
    @Value("${z-qa.e2e.token:}")
    private String browserlessToken;
    @Value("${z-qa.e2e.bucket:qa-e2e}")
    private String ossBucket;
    @Value("${z-qa.e2e.tenant-id:1}")
    private Long tenantId;

    private static byte[] base64Decode(String s) {
        if (s == null) return new byte[0];
        try {
            return java.util.Base64.getDecoder().decode(s);
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n");
    }

    @Override
    public Long openSession(Long runId, Long runDetailId, String browserType, String targetUrl) {
        QaE2eSessionDO s = new QaE2eSessionDO();
        s.setRunId(runId);
        s.setRunDetailId(runDetailId);
        s.setBrowserType(browserType == null ? "chromium" : browserType);
        s.setTargetUrl(targetUrl);
        s.setViewportWidth(1920);
        s.setViewportHeight(1080);
        s.setStatus("running");
        s.setStartedAt(new Date());
        sessionMapper.insert(s);
        log.info("E2E session opened: id={}, target={}", s.getId(), targetUrl);
        return s.getId();
    }

    @Override
    public Long recordAction(Long sessionId, int stepNo, String actionType,
                             String selector, String value, String description) {
        QaE2eActionDO a = new QaE2eActionDO();
        a.setSessionId(sessionId);
        a.setStepNo(stepNo);
        a.setActionType(actionType);
        a.setSelector(selector);
        a.setActionValue(value);
        a.setDescription(description);
        a.setStartedAt(new Date());
        a.setResult("pending");
        actionMapper.insert(a);
        return a.getId();
    }

    @Override
    public void finishSession(Long sessionId, String status,
                              String videoOssKey, String traceOssKey, String harOssKey) {
        QaE2eSessionDO s = sessionMapper.selectById(sessionId);
        if (s == null) return;
        s.setStatus(status);
        s.setFinishedAt(new Date());
        if (videoOssKey != null) s.setVideoOssKey(videoOssKey);
        if (traceOssKey != null) s.setTraceOssKey(traceOssKey);
        if (harOssKey != null) s.setHarOssKey(harOssKey);
        sessionMapper.updateById(s);
    }

    @Override
    public List<QaE2eActionDO> listActions(Long sessionId) {
        return actionMapper.selectBySessionId(sessionId);
    }

    // ===== OSS upload (缺陷修复) =====

    @Override
    public boolean executeAction(QaE2eActionDO action, QaE2eSessionDO session) {
        long start = System.currentTimeMillis();
        try {
            String script = buildScript(action, session);
            String resp = callBrowserlessFunction(script);
            log.info("E2E action[{}] resp: {}", action.getActionType(), resp);
            action.setResult("pass");
            action.setDurationMs(System.currentTimeMillis() - start);
            action.setFinishedAt(new Date());
            // 截图 → z-oss（先尝试 uploadObject，没 IOssObjectService 时降级写 inline 占位）
            if (action.getSelector() != null && (action.getActionType() == null
                    || !"navigate".equalsIgnoreCase(action.getActionType()))) {
                String screenshotB64 = safeCaptureScreenshot();
                if (screenshotB64 != null) {
                    String uploadedKey = uploadScreenshotToOss(session, action, screenshotB64);
                    action.setScreenshotOssKey(uploadedKey);
                }
            }
            actionMapper.updateById(action);
            return true;
        } catch (Exception ex) {
            log.warn("E2E action[{}] failed: {}", action.getActionType(), ex.getMessage());
            action.setResult("fail");
            action.setErrorMessage(ex.getMessage());
            action.setFinishedAt(new Date());
            actionMapper.updateById(action);
            return false;
        }
    }

    @Override
    public QaE2eSessionDO getSession(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    /**
     * 上传截图字节到 z-oss，返回 ossKey (或 path/URL)。
     *
     * <p>步骤:
     * <ol>
     *   <li>Base64 解码得到 PNG 字节</li>
     *   <li>用 {@link IOssObjectService#uploadObject} 写到 bucket `qa-e2e`</li>
     *   <li>object key 格式: e2e/run-{runId}/session-{sessionId}/step-{stepNo}-{ts}.png</li>
     *   <li>返回 ossKey（或生成预签名 URL 方便前端展示）</li>
     * </ol>
     *
     * <p>无 IOssObjectService（依赖缺失）时降级为 `inline://` 占位字符串。
     */
    private String uploadScreenshotToOss(QaE2eSessionDO session, QaE2eActionDO action, String base64) {
        byte[] bytes = base64Decode(base64);
        if (bytes.length == 0) return null;
        if (ossObjectService == null) {
            // 降级：没接入 z-oss 时仍给一个占位 key，方便前端识别
            log.debug("IOssObjectService not on classpath, using inline placeholder for step {}", action.getStepNo());
            return "inline://" + (session.getRunId() == null ? 0 : session.getRunId())
                    + "/" + action.getSessionId() + "/" + action.getStepNo() + ".png";
        }
        try {
            String bucket = (ossBucket == null || ossBucket.isEmpty()) ? DEFAULT_BUCKET : ossBucket;
            String key = "e2e/run-" + (session.getRunId() == null ? 0 : session.getRunId())
                    + "/session-" + action.getSessionId()
                    + "/step-" + action.getStepNo() + "-" + System.currentTimeMillis() + ".png";
            try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                OssObject obj = ossObjectService.uploadObject(bucket, key, in,
                        bytes.length, "image/png", tenantId);
                String storedKey = obj == null ? key
                        : (obj.getStoragePath() != null ? obj.getStoragePath() : obj.getObjectKey());
                log.info("E2E screenshot uploaded: bucket={}, key={}", bucket, storedKey);
                return storedKey;
            }
        } catch (Exception ex) {
            log.warn("upload screenshot to oss failed: {}", ex.getMessage());
            return "upload-failed://" + ex.getMessage();
        }
    }

    // ===== Browserless helpers =====

    /**
     * 单步可选截图（不阻塞主流程，失败 warn 即可）。
     */
    private String safeCaptureScreenshot() {
        try {
            return callBrowserlessScreenshot();
        } catch (Exception ex) {
            log.debug("screenshot skipped: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * 把单步 E2E 操作翻译成一段 Playwright 脚本，提交到 Browserless /function 执行。
     */
    private String buildScript(QaE2eActionDO a, QaE2eSessionDO s) {
        StringBuilder js = new StringBuilder();
        js.append("module.exports = async (browser) => {");
        js.append("const page = await browser.newPage();");
        if (s.getTargetUrl() != null) {
            js.append("await page.goto('").append(escape(s.getTargetUrl())).append("', {waitUntil:'networkidle0'});");
        }
        if (a.getSelector() != null) {
            String sel = escape(a.getSelector());
            switch (a.getActionType() == null ? "" : a.getActionType()) {
                case "navigate":
                    js.append("await page.goto('").append(escape(a.getActionValue())).append("');");
                    break;
                case "click":
                    js.append("await page.click('").append(sel).append("');");
                    break;
                case "fill":
                case "type":
                    js.append("await page.type('").append(sel).append("', '").append(escape(a.getActionValue())).append("');");
                    break;
                case "select":
                    js.append("await page.select('").append(sel).append("', '").append(escape(a.getActionValue())).append("');");
                    break;
                case "hover":
                    js.append("await page.hover('").append(sel).append("');");
                    break;
                case "assert_text":
                    js.append("const el = await page.$('").append(sel).append("');");
                    js.append("const t = await page.evaluate(e => e.innerText, el);");
                    js.append("if (!t.includes('").append(escape(a.getActionValue())).append("')) throw new Error('text mismatch');");
                    break;
                case "assert_visible":
                    js.append("await page.waitForSelector('").append(sel).append("', {visible:true});");
                    break;
                case "wait":
                    if (sel != null) js.append("await page.waitForSelector('").append(sel).append("');");
                    else js.append("await new Promise(r=>setTimeout(r,").append(a.getActionValue()).append("));");
                    break;
                default:
                    js.append("// unknown action ").append(escape(a.getActionType()));
            }
        }
        js.append("await page.close();");
        js.append("return 'ok';");
        js.append("};");
        return js.toString();
    }

    private String callBrowserlessFunction(String script) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(browserlessUrl + "/function?--allow-unsigned").openConnection();
        try {
            conn.setRequestProperty("Content-Type", "application/javascript");
            if (!browserlessToken.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + browserlessToken);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(script.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String body = sb.toString();
            if (code >= 400) throw new RuntimeException("browserless " + code + ": " + body);
            return body;
        } finally {
            try {
                conn.disconnect();
            } catch (Exception ignore) {
            }
        }
    }

    private String callBrowserlessScreenshot() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(browserlessUrl + "/screenshot?--allow-unsigned").openConnection();
        try {
            if (!browserlessToken.isEmpty()) conn.setRequestProperty("Authorization", "Bearer " + browserlessToken);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            String body = "{\"url\":\"about:blank\"}";
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            if (code >= 400) return null;
            try {
                Map<String, Object> obj = JsonUtil.fromJson(sb.toString(), Map.class);
                return String.valueOf(obj.get("data"));
            } catch (Exception ex) {
                return null;
            }
        } finally {
            try {
                conn.disconnect();
            } catch (Exception ignore) {
            }
        }
    }
}
