package com.zifang.z.qa.web.api;

import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.qa.admin.service.QaReportService;
import com.zifang.z.qa.web.response.QaResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 报告 API（lead 007 步骤 6）。chain 出参是 service 生成的聚合结构，键名由前端消费，故保留 Object。
 */
@RestController("qaReportController")
@RequestMapping("/api/qa/report")
public class ReportApi {

    @Autowired
    private QaReportService reportService;

    @GetMapping(value = "/html/{runCode}", produces = MediaType.TEXT_HTML_VALUE)
    @BizApi(summary = "html", description = "", tags = { "z-qa" })
    public String html(@PathVariable @BizParam(name = "runCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String runCode) {
        return reportService.renderHtml(runCode);
    }

    @GetMapping("/chain/{runCode}")
    @BizApi(summary = "chain", description = "", tags = { "z-qa" })
    public QaResult<Object> chain(@PathVariable @BizParam(name = "runCode", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.PATH) String runCode) {
        Object chain = reportService.renderChain(runCode);
        if (chain == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(chain);
    }
}



