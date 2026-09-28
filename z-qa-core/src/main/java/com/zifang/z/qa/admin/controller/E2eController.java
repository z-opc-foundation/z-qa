package com.zifang.z.qa.admin.controller;

import com.zifang.z.qa.admin.domain.entity.QaE2eActionDO;
import com.zifang.z.qa.admin.domain.entity.QaE2eSessionDO;
import com.zifang.z.qa.admin.service.QaE2eService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * E2E 浏览器会话/操作管理 (FEATURE052 Phase 7 / §6.5).
 */
@RestController("qaE2eController")
@RequestMapping("/api/qa/e2e")
public class E2eController {

    @Autowired
    private QaE2eService e2eService;

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static Long asLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number) return ((Number) o).longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private static int asInt(Object o, int d) {
        if (o == null) return d;
        if (o instanceof Number) return ((Number) o).intValue();
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception ex) {
            return d;
        }
    }

    /**
     * 开启一次会话.
     */
    @PostMapping("/sessions/open")
    public Map<String, Object> open(@RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        Long runId = asLong(body.get("runId"));
        Long runDetailId = asLong(body.get("runDetailId"));
        String browser = asString(body.get("browserType"));
        String targetUrl = asString(body.get("targetUrl"));
        Long sessionId = e2eService.openSession(runId, runDetailId, browser, targetUrl);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", sessionId);
        r.put("success", true);
        return r;
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public Map<String, Object> finish(@PathVariable Long sessionId,
                                      @RequestBody(required = false) Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        String status = body == null ? "done" : asString(body.get("status"));
        String video = body == null ? null : asString(body.get("videoOssKey"));
        String trace = body == null ? null : asString(body.get("traceOssKey"));
        String har = body == null ? null : asString(body.get("harOssKey"));
        e2eService.finishSession(sessionId, status, video, trace, har);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("success", true);
        return r;
    }

    @GetMapping("/sessions/{sessionId}")
    public Map<String, Object> session(@PathVariable Long sessionId) {
        Map<String, Object> r = new HashMap<>();
        QaE2eSessionDO s = e2eService.getSession(sessionId);
        r.put("code", s != null ? 200 : 404);
        r.put("msg", s != null ? "success" : "not found");
        r.put("content", s);
        r.put("success", s != null);
        return r;
    }

    @GetMapping("/sessions/{sessionId}/actions")
    public Map<String, Object> actions(@PathVariable Long sessionId) {
        Map<String, Object> r = new HashMap<>();
        List<QaE2eActionDO> actions = e2eService.listActions(sessionId);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", actions);
        r.put("success", true);
        return r;
    }

    /**
     * 同步执行一步（前端调试用）。
     */
    @PostMapping("/actions/execute")
    public Map<String, Object> execAction(@RequestBody Map<String, Object> body) {
        Map<String, Object> r = new HashMap<>();
        Long sessionId = asLong(body.get("sessionId"));
        QaE2eSessionDO s = e2eService.getSession(sessionId);
        if (s == null) {
            r.put("code", 404);
            r.put("msg", "session not found");
            return r;
        }
        QaE2eActionDO a = new QaE2eActionDO();
        a.setSessionId(sessionId);
        a.setStepNo(asInt(body.get("stepNo"), 1));
        a.setActionType(asString(body.get("actionType")));
        a.setSelector(asString(body.get("selector")));
        a.setActionValue(asString(body.get("value")));
        a.setDescription(asString(body.get("description")));
        a.setE2eScreenshot(asInt(body.get("screenshot"), 1));
        a.setStartedAt(new java.util.Date());
        boolean ok = e2eService.executeAction(a, s);
        r.put("code", 200);
        r.put("msg", ok ? "success" : "fail");
        r.put("content", a);
        r.put("success", ok);
        return r;
    }
}
