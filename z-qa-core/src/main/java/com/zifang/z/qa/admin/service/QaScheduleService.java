package com.zifang.z.qa.admin.service;

import com.zifang.z.qa.admin.domain.entity.QaScheduleDO;

import java.util.List;

/**
 * 测试定时服务 (FEATURE052 Phase 5).
 */
public interface QaScheduleService {
    Long create(QaScheduleDO schedule);

    void update(QaScheduleDO schedule);

    void delete(Long id);

    void toggle(Long id, boolean enabled);

    List<QaScheduleDO> list();

    QaScheduleDO get(Long id);

    /**
     * 遍历启用的 schedule, 检查是否需要触发 (next_run_time <= now)。
     * 由 z-schedule 定时调用 (每分钟一次)。
     *
     * @return 本轮触发的 schedule 数量
     */
    int scanAndTrigger();
}
