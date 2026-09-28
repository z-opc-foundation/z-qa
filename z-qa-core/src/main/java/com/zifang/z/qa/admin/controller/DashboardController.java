package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 测试平台 Dashboard Controller — FEATURE052 §6.5.
 *
 * <p>端点:
 * <ul>
 *   <li>GET /api/qa/dashboard/stats        — 旧版统计 (已有)</li>
 *   <li>GET /api/qa/dashboard/summary     — 总览卡片数据</li>
 *   <li>GET /api/qa/dashboard/trend       — 执行趋势 (近 14 天)</li>
 *   <li>GET /api/qa/dashboard/suite-health — 各套件健康度 (pass rate)</li>
 *   <li>GET /api/qa/dashboard/top-failures — Top 失败步骤</li>
 * </ul>
 */
@RestController("qaDashboardController")
@RequestMapping("/api/qa/dashboard")
public class DashboardController {

    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private QaRunDetailMapper runDetailMapper;
    @Autowired
    private QaSuiteMapper suiteMapper;
    @Autowired
    private QaPlanMapper planMapper;
    @Autowired
    private QaEnvMapper envMapper;

    private static Map<String, Object> wrap(Object content) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", content);
        r.put("success", true);
        return r;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> statusCounts = runMapper.countByStatus(null);
        Map<String, Long> byStatus = new HashMap<>();
        for (Map<String, Object> r : statusCounts) {
            byStatus.put(String.valueOf(r.get("status")), ((Number) r.get("cnt")).longValue());
        }
        result.put("byStatus", byStatus);
        result.put("totalSuites", suiteMapper.selectCount(null));
        result.put("totalPlans", planMapper.selectCount(null));
        result.put("totalEnvs", envMapper.selectCount(null));
        List<QaRunDO> recentRuns = runMapper.selectList(
                new QueryWrapper<QaRunDO>().orderByDesc("gmt_create").last("LIMIT 10"));
        result.put("recentRuns", recentRuns);
        return wrap(result);
    }

    /**
     * 总览卡片数据 — 通过率 / 总步骤数 / 套件健康度 / 最近 24h.
     */
    @GetMapping("/summary")
    public Map<String, Object> summary() {
        Map<String, Object> result = new HashMap<>();
        long totalRuns = runMapper.selectCount(null);
        long totalSuites = suiteMapper.selectCount(null);
        long totalEnvs = envMapper.selectCount(null);

        // 总通过率 = sum(passed) / sum(total)
        Long sumPass = runMapper.sumPassed();
        Long sumTotal = runMapper.sumTotal();
        double passRate = 0;
        if (sumPass != null && sumTotal != null && sumTotal > 0) {
            passRate = ((double) sumPass / sumTotal) * 100;
        }

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date since = c.getTime();
        long runsLast24h = runMapper.selectCount(
                new QueryWrapper<QaRunDO>().ge("gmt_create", since));

        result.put("totalRuns", totalRuns);
        result.put("totalSuites", totalSuites);
        result.put("totalEnvs", totalEnvs);
        result.put("passRate", Math.round(passRate * 10) / 10.0);  // 保留 1 位小数
        result.put("runsLast24h", runsLast24h);
        return wrap(result);
    }

    /**
     * 近 14 天趋势 (FEATURE052 §6.5).
     */
    @GetMapping("/trend")
    public Map<String, Object> trend() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -13); // 14 天前
        Date since = c.getTime();
        List<QaRunDO> runs = runMapper.selectList(
                new QueryWrapper<QaRunDO>().ge("gmt_create", since));
        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            Calendar d = Calendar.getInstance();
            d.add(Calendar.DAY_OF_YEAR, -13 + i);
            buckets.put(sdf.format(d.getTime()), new long[3]); // [total, pass, fail]
        }
        for (QaRunDO r : runs) {
            if (r.getGmtCreate() == null) continue;
            String key = sdf.format(r.getGmtCreate());
            long[] arr = buckets.get(key);
            if (arr == null) continue;
            arr[0]++;
            if ("success".equalsIgnoreCase(r.getStatus())) arr[1]++;
            else if ("failed".equalsIgnoreCase(r.getStatus())) arr[2]++;
        }
        List<Map<String, Object>> series = new ArrayList<>();
        for (Map.Entry<String, long[]> e : buckets.entrySet()) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("date", e.getKey());
            point.put("total", e.getValue()[0]);
            point.put("pass", e.getValue()[1]);
            point.put("fail", e.getValue()[2]);
            series.add(point);
        }
        return wrap(series);
    }

    /**
     * 各套件健康度 — pass rate 排序 (FEATURE052 §6.5).
     */
    @GetMapping("/suite-health")
    public Map<String, Object> suiteHealth() {
        List<QaSuiteDO> suites = suiteMapper.selectList(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QaSuiteDO s : suites) {
            long total = runMapper.selectCount(
                    new QueryWrapper<QaRunDO>().eq("suite_id", s.getId()));
            long pass = runMapper.selectCount(
                    new QueryWrapper<QaRunDO>().eq("suite_id", s.getId()).eq("status", "success"));
            double rate = total == 0 ? 0 : ((double) pass / total) * 100;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("suiteId", s.getId());
            row.put("suiteCode", s.getCode());
            row.put("suiteName", s.getName());
            row.put("totalRuns", total);
            row.put("passedRuns", pass);
            row.put("passRate", Math.round(rate * 10) / 10.0);
            rows.add(row);
        }
        rows.sort((a, b) -> Double.compare((double) b.get("passRate"), (double) a.get("passRate")));
        return wrap(rows);
    }

    /**
     * Top 失败步骤 — 按 step fail count 倒序 (FEATURE052 §6.5).
     */
    @GetMapping("/top-failures")
    public Map<String, Object> topFailures() {
        List<Map<String, Object>> raw = runDetailMapper.topFailures(20);
        // 简化: 直接返回 mapper 的结果, 不足 20 也能跑
        return wrap(raw);
    }
}
