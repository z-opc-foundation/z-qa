package com.zifang.z.qa.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaScheduleDO;
import com.zifang.z.qa.admin.domain.mapper.QaScheduleMapper;
import com.zifang.z.qa.admin.engine.QaExecutionDispatcher;
import com.zifang.z.qa.admin.service.QaScheduleService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * QaScheduleService 实现 — FEATURE052 Phase 5 定时执行.
 *
 * <p>Cron 表达式支持：5 位 / 6 位 (quartz 风格, 含秒 + 年)；
 * 用一个轻量自实现的 nextFireTime() 来算下次触发时间，避免引入 cron-utils 依赖。
 *
 * <p>扫描频率：每分钟通过 @Scheduled cron 调 scanAndTrigger 一次。
 */
@Service
public class QaScheduleServiceImpl implements QaScheduleService {

    private static final Logger log = LogManager.getLogger(QaScheduleServiceImpl.class);

    @Autowired
    private QaScheduleMapper scheduleMapper;
    @Autowired
    private QaExecutionDispatcher dispatcher;

    /**
     * 轻量 cron 解析（5 位 + 6 位 quartz 风格）。失败时返回 null。
     * 支持 * / , - 字符，不支持 L W # 等。
     */
    public static Date computeNextFire(String expr, Date from) {
        if (expr == null) return null;
        try {
            String[] parts = expr.trim().split("\\s+");
            CronField sec, min, hour, dom, mon, dow;
            if (parts.length == 6) {
                sec = new CronField(parts[0], 0, 59);
                min = new CronField(parts[1], 0, 59);
                hour = new CronField(parts[2], 0, 23);
                dom = new CronField(parts[3], 1, 31);
                mon = new CronField(parts[4], 1, 12);
                dow = new CronField(parts[5], 1, 7);
            } else if (parts.length == 5) {
                sec = new CronField("0", 0, 59);
                min = new CronField(parts[0], 0, 59);
                hour = new CronField(parts[1], 0, 23);
                dom = new CronField(parts[2], 1, 31);
                mon = new CronField(parts[3], 1, 12);
                dow = new CronField(parts[4], 1, 7);
            } else {
                return null;
            }
            Calendar c = Calendar.getInstance();
            c.setTime(from);
            c.add(Calendar.MINUTE, 1);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
            for (int i = 0; i < 60 * 24 * 366; i++) {
                if (mon.match(c.get(Calendar.MONTH) + 1) && dom.match(c.get(Calendar.DAY_OF_MONTH))
                        && dow.match(c.get(Calendar.DAY_OF_WEEK))) {
                    if (hour.match(c.get(Calendar.HOUR_OF_DAY)) && min.match(c.get(Calendar.MINUTE))
                            && sec.match(c.get(Calendar.SECOND))) {
                        return c.getTime();
                    }
                }
                c.add(Calendar.MINUTE, 1);
            }
            return null;
        } catch (Exception ex) {
            log.warn("cron parse failed for [{}]: {}", expr, ex.getMessage());
            return null;
        }
    }

    @Override
    public Long create(QaScheduleDO schedule) {
        if (schedule.getEnabled() == null) schedule.setEnabled(1);
        schedule.setGmtCreate(new Date());
        schedule.setGmtModified(new Date());
        schedule.setNextRunTime(computeNextFire(schedule.getCronExpr(), new Date()));
        scheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Override
    public void update(QaScheduleDO schedule) {
        schedule.setGmtModified(new Date());
        schedule.setNextRunTime(computeNextFire(schedule.getCronExpr(), new Date()));
        scheduleMapper.updateById(schedule);
    }

    @Override
    public void delete(Long id) {
        scheduleMapper.deleteById(id);
    }

    @Override
    public void toggle(Long id, boolean enabled) {
        QaScheduleDO s = scheduleMapper.selectById(id);
        if (s == null) return;
        s.setEnabled(enabled ? 1 : 0);
        s.setGmtModified(new Date());
        if (enabled) s.setNextRunTime(computeNextFire(s.getCronExpr(), new Date()));
        scheduleMapper.updateById(s);
    }

    @Override
    public List<QaScheduleDO> list() {
        return scheduleMapper.selectList(new QueryWrapper<QaScheduleDO>().orderByDesc("gmt_create"));
    }

    @Override
    public QaScheduleDO get(Long id) {
        return scheduleMapper.selectById(id);
    }

    /**
     * 每分钟扫描一次.
     */
    @Scheduled(cron = "0 * * * * ?")
    @Override
    public int scanAndTrigger() {
        Date now = new Date();
        List<QaScheduleDO> enabled = scheduleMapper.selectList(
                new QueryWrapper<QaScheduleDO>().eq("enabled", 1));
        int triggered = 0;
        for (QaScheduleDO s : enabled) {
            try {
                if (s.getNextRunTime() != null && s.getNextRunTime().before(now)) {
                    triggerOne(s);
                    triggered++;
                } else if (s.getNextRunTime() == null) {
                    // 第一次：设置 next_run_time
                    s.setNextRunTime(computeNextFire(s.getCronExpr(), now));
                    scheduleMapper.updateById(s);
                }
            } catch (Exception ex) {
                log.warn("schedule[{}] scan failed: {}", s.getId(), ex.getMessage());
            }
        }
        return triggered;
    }

    private void triggerOne(QaScheduleDO s) {
        log.info("schedule[{}] triggered: {}", s.getId(), s.getName());
        String runCode = null;
        try {
            QaRunDO run = null;
            if ("plan".equalsIgnoreCase(s.getTargetType()) && s.getPlanId() != null) {
                run = dispatcher.triggerPlan(s.getPlanId(), s.getEnvCode(), "schedule@" + s.getId(), "cron");
            } else if (s.getSuiteId() != null) {
                run = dispatcher.triggerSuite(s.getSuiteId(), s.getEnvCode(), "schedule@" + s.getId(), "cron");
            }
            runCode = run == null ? null : run.getRunCode();
        } catch (Exception ex) {
            log.warn("schedule[{}] trigger failed: {}", s.getId(), ex.getMessage());
        }
        s.setLastRunTime(new Date());
        s.setLastRunResult(runCode == null ? "error" : "triggered");
        s.setNextRunTime(computeNextFire(s.getCronExpr(), new Date()));
        scheduleMapper.updateById(s);
    }

    private static class CronField {
        final List<int[]> ranges = new ArrayList<>();

        CronField(String expr, int min, int max) {
            // FEATURE052 补: cron 中 "?" 等价 "*" (常见于 quartz day-of-week 与 day-of-month)
            String normalized = "*".equals(expr) || "?".equals(expr) ? "*" : expr;
            for (String part : normalized.split(",")) {
                String[] range = part.split("/");
                int step = range.length == 2 ? Integer.parseInt(range[1]) : 1;
                String[] ab = range[0].split("-");
                int a = ("*".equals(ab[0]) || "?".equals(ab[0])) ? min : Integer.parseInt(ab[0]);
                int b = ab.length == 1
                        ? (("*".equals(ab[0]) || "?".equals(ab[0])) ? max : a)
                        : Integer.parseInt(ab[1]);
                ranges.add(new int[]{a, b, step});
            }
        }

        boolean match(int v) {
            for (int[] r : ranges) {
                // 修复: x+=step 在 b<a 时可能死循环。先 clamp step 至少为 1
                int step = Math.max(1, r[2]);
                for (int x = r[0]; x <= r[1]; x += step) {
                    if (x == v) return true;
                }
            }
            return false;
        }
    }
}
