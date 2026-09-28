package com.zifang.z.qa.admin.service;

import com.zifang.z.qa.admin.domain.entity.QaRunDO;

/**
 * 通知服务 (FEATURE052 Phase 5).
 *
 * <p>负责 run 完成后的通知投递：
 * <ul>
 *   <li>in_app — 站内消息 (z-msg InAppChannel)</li>
 *   <li>email — 邮件 (z-msg EmailChannel)</li>
 * </ul>
 *
 * <p>失败重试 + 投递日志 (z_qa_notify_log) 在 z-msg 中实现。
 */
public interface QaNotifyService {
    /**
     * 通知套件/计划 owner：run 完成（无论通过失败，按需开关）。
     */
    void notifyRunFinished(QaRunDO run, String notifyChannels, boolean notifyOnPass, boolean notifyOnFail);
}
