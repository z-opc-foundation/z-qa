package com.zifang.z.qa.admin.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.entity.QaRunDetailDO;
import com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import com.zifang.z.qa.admin.engine.QaFailureClassifier;
import com.zifang.z.qa.admin.engine.QaStepStatus;
import com.zifang.z.qa.admin.service.QaReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 报告生成 — FEATURE052 Phase 4 + Phase 6.
 *
 * <p>前端 /tools/qa/runs/{runCode}/report 路由调用此服务得到 HTML，
 * 内嵌 LogicFlow 报告态通过 /tools/qa/runs/{runCode}/chain 拿到 JSON 全链路溯源。
 */
@Service
public class QaReportServiceImpl implements QaReportService {

    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private QaRunDetailMapper runDetailMapper;

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** 十态收敛到报告里四个色位:系统错误单独成色,别和断言失败混成一片红。 */
    private static String cssClass(String result) {
        if (QaStepStatus.SUCCESS.equals(result)) return "pass";
        if (QaStepStatus.FAIL_SYSTEM_ERROR.equals(result)) return "error";
        if (QaStepStatus.isFail(result)) return "fail";
        return "skip";
    }

    @Override
    public String renderHtml(String runCode) {
        Map<String, Object> chain = renderChain(runCode);
        if (chain == null) {
            return "<h2>run not found</h2>";
        }
        QaRunDO run = (QaRunDO) chain.get("run");
        @SuppressWarnings("unchecked")
        List<QaRunDetailDO> details = (List<QaRunDetailDO>) chain.get("details");
        long total = details == null ? 0 : details.size();
        long pass = details == null ? 0 : details.stream().filter(d -> QaStepStatus.SUCCESS.equals(d.getResult())).count();
        long fail = details == null ? 0 : details.stream().filter(d -> QaStepStatus.isFail(d.getResult())).count();
        long skip = details == null ? 0 : details.stream().filter(d -> QaStepStatus.SKIP.equals(d.getResult())).count();
        // 缺陷三分类:看 run 报告要回答的是「该开给谁」,而不只是「挂了几步」
        List<String> defectTally = new ArrayList<>();
        if (details != null) {
            Map<String, Integer> counted = new LinkedHashMap<>();
            for (QaRunDetailDO d : details) {
                String dt = d.getDefectType();
                if (dt != null && !dt.isEmpty()) counted.merge(dt, 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : counted.entrySet()) {
                defectTally.add(QaFailureClassifier.defectLabel(e.getKey()) + " " + e.getValue());
            }
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset='utf-8'><title>QA Report ").append(runCode).append("</title>");
        sb.append("<style>");
        sb.append("body{font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif;background:#f6f8fa;color:#24292f;margin:0;padding:24px}");
        sb.append(".card{background:#fff;border:1px solid #d0d7de;border-radius:6px;padding:16px;margin:12px 0}");
        sb.append(".step{display:flex;align-items:center;padding:10px;border-bottom:1px solid #e1e4e8}");
        sb.append(".pill{padding:2px 8px;border-radius:12px;font-size:12px;color:#fff;display:inline-block}");
        sb.append(".pass{background:#1a7f37} .fail{background:#cf222e} .error{background:#bc4c00} .skip{background:#6e7781}");
        sb.append(".defect{background:#57606a}");
        sb.append(".body{font-family:Menlo,Consolas,monospace;font-size:12px;background:#f6f8fa;padding:8px;border-radius:4px;white-space:pre-wrap;word-break:break-all}");
        sb.append("details>summary{cursor:pointer;color:#0969da}");
        sb.append("</style></head><body>");

        // header
        sb.append("<div class='card'><h2>").append(runCode).append(" &mdash; ").append(run == null ? "" : run.getName()).append("</h2>");
        sb.append("<p>Result: <b>").append(run == null ? "?" : run.getStatus()).append("</b> &nbsp; ");
        sb.append("Steps: <b>").append(total).append("</b> (pass ").append(pass).append(" / fail ").append(fail).append(" / skip ").append(skip).append(")");
        if (!defectTally.isEmpty())
            sb.append(" &nbsp; 缺陷归类: <b>").append(escape(String.join(" · ", defectTally))).append("</b>");
        if (run != null && run.getDurationMs() != null)
            sb.append(" &nbsp; Duration: <b>").append(run.getDurationMs()).append(" ms</b>");
        if (run != null && run.getStartedAt() != null)
            sb.append(" &nbsp; Started: <b>").append(sdf.format(run.getStartedAt())).append("</b>");
        sb.append("</p></div>");

        // JSON 报告 (供前端 LogicFlow 报告态使用)
        sb.append("<div class='card'>");
        sb.append("<h3>JSON Full Chain (FEATURE052 §3.3 溯源)</h3>");
        sb.append("<textarea class='body' rows='10' id='chain-json'>")
                .append(escape(com.zifang.util.json.JsonUtil.toJson(details)))
                .append("</textarea>");
        sb.append("</div>");

        // step-by-step HTML
        if (details != null) {
            for (QaRunDetailDO d : details) {
                sb.append("<div class='card step'>");
                String cls = cssClass(d.getResult());
                sb.append("<span class='pill ").append(cls).append("'>").append(orEmpty(d.getResult())).append("</span>");
                if (d.getDefectType() != null && !d.getDefectType().isEmpty())
                    sb.append(" &nbsp;<span class='pill defect'>").append(escape(QaFailureClassifier.defectLabel(d.getDefectType()))).append("</span>");
                sb.append("<b>&nbsp;Step ").append(d.getStepNo()).append(": ").append(escape(orEmpty(d.getStepName()))).append("</b>");
                if (d.getDurationMs() != null)
                    sb.append(" &nbsp; <small>").append(d.getDurationMs()).append("ms</small>");
                sb.append("<details style='margin-left:auto'><summary>溯源</summary>");
                sb.append("<div><b>Method:</b> ").append(d.getMethod()).append(" &nbsp; <b>URL:</b> ").append(d.getRequestUrl()).append("</div>");
                sb.append("<div><b>Target:</b> ").append(d.getTargetMode()).append(" &nbsp; ").append(d.getMockEndpointCode() == null ? "" : "mock=" + d.getMockEndpointCode()).append("</div>");
                sb.append("<h4>Request</h4><pre class='body'>").append(escape(orEmpty(d.getRequestHeaders()))).append("\n").append(escape(orEmpty(d.getRequestBody()))).append("</pre>");
                sb.append("<h4>Response</h4><pre class='body'>").append(d.getResponseStatus()).append("\n").append(escape(orEmpty(d.getResponseHeaders()))).append("\n").append(escape(orEmpty(d.getResponseBody()))).append("</pre>");
                sb.append("<h4>Variables (before)</h4><pre class='body'>").append(escape(orEmpty(d.getVariablesBefore()))).append("</pre>");
                sb.append("<h4>Variables (after)</h4><pre class='body'>").append(escape(orEmpty(d.getVariablesAfter()))).append("</pre>");
                sb.append("<h4>Extractions</h4><pre class='body'>").append(escape(orEmpty(d.getExtractions()))).append("</pre>");
                sb.append("<h4>Assertions</h4><pre class='body'>").append(escape(orEmpty(d.getAssertResults()))).append("</pre>");
                if (d.getErrorMessage() != null)
                    sb.append("<h4>Error</h4><pre class='body'>").append(escape(d.getErrorMessage())).append("</pre>");
                sb.append("</details></div>");
            }
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    @Override
    public Map<String, Object> renderChain(String runCode) {
        QaRunDO run = runMapper.selectOne(new QueryWrapper<QaRunDO>().eq("run_code", runCode));
        if (run == null) return null;
        List<QaRunDetailDO> details = runDetailMapper.selectList(
                new QueryWrapper<QaRunDetailDO>().eq("run_id", run.getId()).orderByAsc("id"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("run", run);
        out.put("details", details);
        // 进一步加工成 graph_data 供 LogicFlow 报告态使用
        return out;
    }
}
