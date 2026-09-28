package com.zifang.z.qa.admin.controller;

import com.zifang.z.qa.admin.service.QaReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 报告 / 链路 JSON 控制器 (FEATURE052 Phase 6).
 */
@RestController("qaReportController")
@RequestMapping("/api/qa/report")
public class ReportController {

    @Autowired
    private QaReportService reportService;

    @GetMapping(value = "/html/{runCode}", produces = MediaType.TEXT_HTML_VALUE)
    public String html(@PathVariable String runCode) {
        return reportService.renderHtml(runCode);
    }

    @GetMapping("/chain/{runCode}")
    public Map<String, Object> chain(@PathVariable String runCode) {
        Map<String, Object> r = new HashMap<>();
        Object chain = reportService.renderChain(runCode);
        r.put("code", chain == null ? 404 : 200);
        r.put("msg", chain == null ? "not found" : "success");
        r.put("content", chain);
        r.put("success", chain != null);
        return r;
    }
}
