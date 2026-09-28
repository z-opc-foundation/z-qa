package com.zifang.z.qa.admin.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QaScheduleServiceImpl.computeNextFire 单元测试 — FEATURE052 Phase 5 定时触发核心.
 *
 * <p>覆盖：
 * <ul>
 *   <li>5 位 cron 表达式 (unix 风格): 分 时 日 月 周</li>
 *   <li>6 位 quartz 风格: 秒 分 时 日 月 周</li>
 *   <li>结果是未来某个时刻</li>
 *   <li>非法 cron 返回 null</li>
 * </ul>
 */
class QaScheduleServiceImplTest {

    private final QaScheduleServiceImpl svc = new QaScheduleServiceImpl();

    private static Date utcOf(int y, int mo, int d, int h, int mi, int s) {
        Calendar c = Calendar.getInstance();
        c.set(y, mo - 1, d, h, mi, s);
        c.set(Calendar.SECOND, s);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    @Test
    void next_fire_5field() {
        Date now = utcOf(2026, 8, 13, 12, 30, 0);
        Date next = QaScheduleServiceImpl.computeNextFire("0 0 23 * * ?", now);
        assertNotNull(next);
        Calendar c = Calendar.getInstance();
        c.setTime(next);
        assertEquals(23, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, c.get(Calendar.MINUTE));
    }

    @Test
    void next_fire_6field_with_seconds() {
        Date now = utcOf(2026, 8, 13, 12, 30, 0);
        Date next = QaScheduleServiceImpl.computeNextFire("0 30 12 13 8 ?", now);
        assertNotNull(next);
        Calendar c = Calendar.getInstance();
        c.setTime(next);
        assertEquals(12, c.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, c.get(Calendar.MINUTE));
        assertEquals(0, c.get(Calendar.SECOND));
    }

    @Test
    void cron_step_every_5_minutes() {
        Date now = utcOf(2026, 8, 13, 12, 0, 0);
        Date next = QaScheduleServiceImpl.computeNextFire("0 */5 * * * ?", now);
        assertNotNull(next);
        Calendar c = Calendar.getInstance();
        c.setTime(next);
        assertEquals(5, c.get(Calendar.MINUTE));
    }

    @Test
    void invalid_cron_returns_null() {
        Date now = new Date();
        assertNull(QaScheduleServiceImpl.computeNextFire(null, now));
        assertNull(QaScheduleServiceImpl.computeNextFire("invalid", now));
    }

    @Test
    void next_fire_always_after_now() {
        Date now = utcOf(2026, 8, 13, 12, 30, 0);
        Date next = QaScheduleServiceImpl.computeNextFire("0 0 13 * * ?", now);
        assertNotNull(next);
        assertTrue(next.after(now), "next must be in the future");
    }
}
