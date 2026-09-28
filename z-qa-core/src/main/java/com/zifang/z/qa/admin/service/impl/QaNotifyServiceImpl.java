package com.zifang.z.qa.admin.service.impl;

import com.zifang.z.msg.api.EmailMessage;
import com.zifang.z.msg.api.MessageGateway;
import com.zifang.z.msg.api.MessageSendResult;
import com.zifang.z.msg.core.domain.entity.InAppMessage;
import com.zifang.z.msg.core.domain.mapper.InAppMessageMapper;
import com.zifang.z.qa.admin.domain.entity.QaNotifyLogDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.mapper.QaNotifyLogMapper;
import com.zifang.z.qa.admin.service.QaNotifyService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知服务实现 — FEATURE052 Phase 5.
 *
 * <p>已对接真实 z-msg 模块（缺陷修复：原来是 stub log）：
 * <ul>
 *   <li>in_app — 直接插入 z_inapp_message (InAppMessageMapper)，用户登录后即可看到</li>
 *   <li>email — 通过 MessageGateway 走 z-msg 的 SMTP EmailSender</li>
 * </ul>
 *
 * <p>投递结果写入 z_qa_notify_log (本模块) 便于追溯 / 重试。
 */
@Service
public class QaNotifyServiceImpl implements QaNotifyService {

    private static final Logger log = LogManager.getLogger(QaNotifyServiceImpl.class);

    @Autowired
    private QaNotifyLogMapper notifyLogMapper;
    /**
     * 通过 z-msg-spring-boot-starter 自动注入 (DefaultMessageGateway).
     */
    @Autowired(required = false)
    private MessageGateway messageGateway;
    /**
     * 站内消息表 (z_inapp_message) 由 z-msg-core 维护 — 直接插入即可
     */
    @Autowired(required = false)
    private InAppMessageMapper inAppMessageMapper;

    @Value("${z-qa.notify.email-receiver-suffix:@example.com}")
    private String emailReceiverSuffix;
    @Value("${z-qa.notify.biz-type:z-qa-run-finished}")
    private String bizType;
    @Value("${z-qa.notify.default-user-id:1}")
    private Long defaultUserId;

    private static String buildInAppContent(QaRunDO run) {
        StringBuilder sb = new StringBuilder();
        sb.append("Run: ").append(run.getName()).append("\n");
        sb.append("Status: ").append(run.getStatus()).append("\n");
        sb.append("Steps: ").append(run.getPassedSteps()).append("/").append(run.getTotalSteps()).append(" passed");
        if (run.getFailedSteps() != null && run.getFailedSteps() > 0) {
            sb.append(", ").append(run.getFailedSteps()).append(" failed");
        }
        if (run.getDurationMs() != null) sb.append("\nDuration: ").append(run.getDurationMs()).append(" ms");
        if (run.getErrorMessage() != null) sb.append("\nError: ").append(run.getErrorMessage());
        return sb.toString();
    }

    private static Long parseLong(String s, Long d) {
        if (s == null) return d;
        try {
            return Long.parseLong(s);
        } catch (Exception ex) {
            return d;
        }
    }

    @Override
    public void notifyRunFinished(QaRunDO run, String notifyChannels, boolean notifyOnPass, boolean notifyOnFail) {
        // 只有 success 算通过:中止 cancelled 也是没跑成，按失败侧通知才不会被当成绿灯
        boolean pass = "success".equalsIgnoreCase(run.getStatus());
        if (pass && !notifyOnPass) return;
        if (!pass && !notifyOnFail) return;

        String channels = notifyChannels == null || notifyChannels.isEmpty()
                ? "in_app"
                : notifyChannels;
        Map<String, Object> payload = basePayload(run);
        for (String ch : channels.split(",")) {
            if ("in_app".equalsIgnoreCase(ch.trim())) {
                sendInApp(run, payload);
            } else if ("email".equalsIgnoreCase(ch.trim())) {
                sendEmail(run, payload);
            }
        }
    }

    private Map<String, Object> basePayload(QaRunDO run) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("runCode", run.getRunCode());
        m.put("name", run.getName());
        m.put("status", run.getStatus());
        m.put("totalSteps", run.getTotalSteps());
        m.put("passedSteps", run.getPassedSteps());
        m.put("failedSteps", run.getFailedSteps());
        m.put("durationMs", run.getDurationMs());
        m.put("startedAt", run.getStartedAt());
        m.put("finishedAt", run.getFinishedAt());
        m.put("errorMessage", run.getErrorMessage());
        return m;
    }

    /**
     * 发送站内消息 — 直接插 InAppMessage 表 (z-msg 模块)。
     * 没有 InAppMessageMapper 时降级到本地 z_qa_notify_log。
     */
    private void sendInApp(QaRunDO run, Map<String, Object> payload) {
        QaNotifyLogDO log0 = new QaNotifyLogDO();
        log0.setRunId(run.getId());
        log0.setChannel("in_app");
        log0.setTarget(run.getTriggerUser());
        log0.setPayload(payload.toString());
        log0.setGmtCreate(new Date());

        try {
            // 方式 1（推荐）: 直接写 z_inapp_message 表，z-msg-web / z-msg API 提供前端查看
            if (inAppMessageMapper != null) {
                InAppMessage m = new InAppMessage();
                m.setUserId(parseLong(run.getTriggerUser(), defaultUserId));
                m.setEventType(bizType);
                m.setTitle("[z-qa] run " + run.getRunCode() + " " + run.getStatus());
                m.setContent(buildInAppContent(run));
                m.setIsRead(0);
                m.setTenantCode(run.getTenantCode());
                m.setCreatedTime(LocalDateTime.now());
                inAppMessageMapper.insert(m);
                log0.setStatus("sent");
                log0.setSentAt(new Date());
                log.info("in_app notify delivered for run[{}] user={}", run.getRunCode(), run.getTriggerUser());
            } else {
                log0.setStatus("failed");
                log0.setErrorMessage("InAppMessageMapper not on classpath (z-msg-core missing)");
                log.warn("in_app notify failed: z-msg-core not on classpath");
            }
        } catch (Exception ex) {
            log0.setStatus("failed");
            log0.setErrorMessage(ex.getMessage());
            log.warn("in_app notify failed: {}", ex.getMessage());
        }
        notifyLogMapper.insert(log0);
    }

    /**
     * 发送邮件 — 通过 z-msg MessageGateway（实际由 z-msg-spring-boot-starter 装配的
     * SmtpEmailSender 或 MockEmailSender 投递）。
     */
    private void sendEmail(QaRunDO run, Map<String, Object> payload) {
        QaNotifyLogDO log0 = new QaNotifyLogDO();
        log0.setRunId(run.getId());
        log0.setChannel("email");
        log0.setTarget((run.getTriggerUser() == null ? "qa" : run.getTriggerUser()) + emailReceiverSuffix);
        log0.setPayload(payload.toString());
        log0.setGmtCreate(new Date());

        try {
            if (messageGateway == null) {
                throw new IllegalStateException("MessageGateway not on classpath (z-msg-spring-boot-starter missing)");
            }
            // 构造模板参数，z-msg 可后续接 MessageTemplateEngine 做模板替换
            Map<String, String> tpl = new LinkedHashMap<>();
            tpl.put("runCode", run.getRunCode() == null ? "" : run.getRunCode());
            tpl.put("name", run.getName() == null ? "" : run.getName());
            tpl.put("status", run.getStatus() == null ? "" : run.getStatus());
            tpl.put("totalSteps", String.valueOf(run.getTotalSteps() == null ? 0 : run.getTotalSteps()));
            tpl.put("passedSteps", String.valueOf(run.getPassedSteps() == null ? 0 : run.getPassedSteps()));
            tpl.put("failedSteps", String.valueOf(run.getFailedSteps() == null ? 0 : run.getFailedSteps()));
            tpl.put("durationMs", String.valueOf(run.getDurationMs() == null ? 0 : run.getDurationMs()));

            EmailMessage em = EmailMessage.builder()
                    .to(log0.getTarget())
                    .subject("[z-qa] run " + run.getRunCode() + " " + run.getStatus())
                    .bizType(bizType)
                    .templateParams(tpl)
                    .build();
            MessageSendResult r = messageGateway.sendEmail(em);
            if (r != null && r.isSuccess()) {
                log0.setStatus("sent");
                log0.setSentAt(new Date());
                log.info("email notify sent for run[{}] to {}", run.getRunCode(), log0.getTarget());
            } else {
                log0.setStatus("failed");
                log0.setErrorMessage(r == null ? "null result" : r.getErrorMessage());
                log.warn("email notify failed: {}", r == null ? "null" : r.getErrorMessage());
            }
        } catch (Exception ex) {
            log0.setStatus("failed");
            log0.setErrorMessage(ex.getMessage());
            log.warn("email notify failed: {}", ex.getMessage());
        }
        notifyLogMapper.insert(log0);
    }
}
