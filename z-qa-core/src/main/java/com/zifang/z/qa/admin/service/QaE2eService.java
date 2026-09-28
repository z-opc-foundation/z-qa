package com.zifang.z.qa.admin.service;

import com.zifang.z.qa.admin.domain.entity.QaE2eActionDO;
import com.zifang.z.qa.admin.domain.entity.QaE2eSessionDO;

import java.util.List;

/**
 * E2E 浏览器测试服务 (FEATURE052 Phase 7).
 */
public interface QaE2eService {

    /**
     * 创建一次浏览器会话（异步启动）。返回 sessionId。
     */
    Long openSession(Long runId, Long runDetailId, String browserType, String targetUrl);

    /**
     * 记录一次操作。
     */
    Long recordAction(Long sessionId, int stepNo, String actionType,
                      String selector, String value, String description);

    /**
     * 标记会话完成。
     */
    void finishSession(Long sessionId, String status,
                       String videoOssKey, String traceOssKey, String harOssKey);

    /**
     * 取会话的所有操作。
     */
    List<QaE2eActionDO> listActions(Long sessionId);

    /**
     * 执行一次浏览器动作（HTTP 调 browserless）。
     */
    boolean executeAction(QaE2eActionDO action, QaE2eSessionDO session);

    /**
     * 拉取一次会话的元数据。
     */
    QaE2eSessionDO getSession(Long sessionId);
}
