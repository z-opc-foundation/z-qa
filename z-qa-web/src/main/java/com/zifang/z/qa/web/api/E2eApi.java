package com.zifang.z.qa.web.api;

import com.zifang.z.qa.admin.domain.entity.QaE2eActionDO;
import com.zifang.z.qa.admin.domain.entity.QaE2eSessionDO;
import com.zifang.z.qa.admin.service.QaE2eService;
import com.zifang.z.qa.web.request.QaE2eActionReq;
import com.zifang.z.qa.web.request.QaE2eFinishReq;
import com.zifang.z.qa.web.request.QaE2eSessionReq;
import com.zifang.z.qa.web.response.QaE2eActionResp;
import com.zifang.z.qa.web.response.QaE2eSessionResp;
import com.zifang.z.qa.web.response.QaResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * E2E 浏览器会话 API（lead 007 步骤 6）。
 */
@RestController("qaE2eController")
@RequestMapping("/api/qa/e2e")
public class E2eApi {

    @Autowired
    private QaE2eService e2eService;

    @PostMapping("/sessions/open")
    public QaResult<Long> open(@RequestBody QaE2eSessionReq req) {
        Long sessionId = e2eService.openSession(
                req.getRunId(), req.getRunDetailId(), req.getBrowserType(), req.getTargetUrl());
        return QaResult.ok(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public QaResult<Void> finish(@PathVariable Long sessionId,
                                 @RequestBody(required = false) QaE2eFinishReq req) {
        String status = req == null || req.getStatus() == null ? "done" : req.getStatus();
        e2eService.finishSession(sessionId, status,
                req == null ? null : req.getVideoOssKey(),
                req == null ? null : req.getTraceOssKey(),
                req == null ? null : req.getHarOssKey());
        return QaResult.ok();
    }

    @GetMapping("/sessions/{sessionId}")
    public QaResult<QaE2eSessionResp> session(@PathVariable Long sessionId) {
        QaE2eSessionDO s = e2eService.getSession(sessionId);
        if (s == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(QaConverter.to(s, QaE2eSessionResp.class));
    }

    @GetMapping("/sessions/{sessionId}/actions")
    public QaResult<List<QaE2eActionResp>> actions(@PathVariable Long sessionId) {
        return QaResult.ok(QaConverter.toList(e2eService.listActions(sessionId), QaE2eActionResp.class));
    }

    /** 同步执行一步（前端调试用）。 */
    @PostMapping("/actions/execute")
    public QaResult<QaE2eActionResp> execAction(@RequestBody QaE2eActionReq req) {
        QaE2eSessionDO s = e2eService.getSession(req.getSessionId());
        if (s == null) {
            return QaResult.notFound("session not found");
        }
        QaE2eActionDO a = new QaE2eActionDO();
        BeanUtils.copyProperties(req, a);
        a.setSessionId(req.getSessionId());
        a.setActionValue(req.getValue());
        a.setE2eScreenshot(req.getScreenshot() == null ? 1 : req.getScreenshot());
        a.setStartedAt(new Date());
        boolean ok = e2eService.executeAction(a, s);
        if (!ok) {
            return new QaResult<>(200, "fail", QaConverter.to(a, QaE2eActionResp.class), false);
        }
        return QaResult.ok(QaConverter.to(a, QaE2eActionResp.class));
    }
}
