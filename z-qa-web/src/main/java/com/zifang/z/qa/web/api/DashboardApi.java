package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.mapper.QaEnvMapper;
import com.zifang.z.qa.admin.domain.mapper.QaPlanMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteMapper;
import com.zifang.z.qa.web.response.QaResult;
import com.zifang.z.qa.web.response.QaRunResp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 质量看板 API（lead 007 步骤 6）。
 *
 * <p>看板是聚合视图，键名由前端 Dashboard.tsx 直接消费，故保留 Map 出参；
 * 其中内嵌的 run 列表已转成 QaRunResp，DO 不外泄。
 */
@RestController("qaDashboardController")
@RequestMapping("/api/qa/dashboard")
public class DashboardApi {

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

    @GetMapping("/stats")
    public QaResult<Map<String, Object>> stats() {
        Map<String, Object> result = new HashMap<>();
        Map<String, Long> byStatus = new HashMap<>();
        for (Map<String, Object> r : runMapper.countByStatus(null)) {
            byStatus.put(String.valueOf(r.get("status")), ((Number) r.get("cnt")).longValue());
        }
        result.put("byStatus", byStatus);
        result.put("totalSuites", suiteMapper.selectCount(null));
        result.put("totalPlans", planMapper.selectCount(null));
        result.put("totalEnvs", envMapper.selectCount(null));
        List<QaRunDO> recentRuns = runMapper.selectList(
                new QueryWrapper<QaRunDO>().orderByDesc("gmt_create").last("LIMIT 10"));
        result.put("recentRuns", QaConverter.toList(recentRuns, QaRunResp.class));
        return QaResult.ok(result);
    }

    /** 总览卡片：通过率 / 总步骤数 / 套件数 / 最近 24h。 */
    @GetMapping("/summary")
    public QaResult<Map<String, Object>> summary() {
        Map<String, Object> result = new HashMap<>();
        long totalRuns = runMapper.selectCount(null);
        long totalSuites = suiteMapper.selectCount(null);
        long totalEnvs = envMapper.selectCount(null);

        Long sumPass = runMapper.sumPassed();
        Long sumTotal = runMapper.sumTotal();
        double passRate = 0;
        if (sumPass != null && sumTotal != null && sumTotal > 0) {
            passRate = ((double) sumPass / sumTotal) * 100;
        }

        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -1);
        Date since = c.getTime();
        long runsLast24h = runMapper.selectCount(new QueryWrapper<QaRunDO>().ge("gmt_create", since));

        result.put("totalRuns", totalRuns);
        result.put("totalSuites", totalSuites);
        result.put("totalEnvs", totalEnvs);
        result.put("passRate", Math.round(passRate * 10) / 10.0);
        result.put("runsLast24h", runsLast24h);
        return QaResult.ok(result);
    }

    /** 近 14 天趋势。 */
    @GetMapping("/trend")
    public QaResult<List<Map<String, Object>>> trend() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -13);
        Date since = c.getTime();
        List<QaRunDO> runs = runMapper.selectList(new QueryWrapper<QaRunDO>().ge("gmt_create", since));
        Map<String, long[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            Calendar d = Calendar.getInstance();
            d.add(Calendar.DAY_OF_YEAR, -13 + i);
            buckets.put(sdf.format(d.getTime()), new long[3]);
        }
        for (QaRunDO r : runs) {
            if (r.getGmtCreate() == null) {
                continue;
            }
            long[] arr = buckets.get(sdf.format(r.getGmtCreate()));
            if (arr == null) {
                continue;
            }
            arr[0]++;
            if ("success".equalsIgnoreCase(r.getStatus())) {
                arr[1]++;
            } else if ("failed".equalsIgnoreCase(r.getStatus())) {
                arr[2]++;
            }
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
        return QaResult.ok(series);
    }

    /** 各套件健康度 — pass rate 倒序。 */
    @GetMapping("/suite-health")
    public QaResult<List<Map<String, Object>>> suiteHealth() {
        List<QaSuiteDO> suites = suiteMapper.selectList(null);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (QaSuiteDO s : suites) {
            long total = runMapper.selectCount(new QueryWrapper<QaRunDO>().eq("suite_id", s.getId()));
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
        return QaResult.ok(rows);
    }

    /** Top 失败步骤 — mapper 直出聚合行，键名由前端消费。 */
    @GetMapping("/top-failures")
    public QaResult<List<Map<String, Object>>> topFailures() {
        return QaResult.ok(runDetailMapper.topFailures(20));
    }
}
