package com.zifang.z.qa.admin.service;

/**
 * 报告生成服务 (FEATURE052 Phase 4 + Phase 6).
 *
 * <p>职责：
 * <ul>
 *   <li>把 run + 详情渲染为 HTML（基于 Bootstrap，可内嵌到前端 iframe）</li>
 *   <li>提供 JSON 格式的全链路溯源（前端 LogicFlow 报告态可直接吃这份 JSON）</li>
 * </ul>
 */
public interface QaReportService {
    /**
     * 生成 HTML 报告，可内嵌/下载。
     */
    String renderHtml(String runCode);

    /**
     * 全链路 JSON 溯源，前端展示用。
     */
    Object renderChain(String runCode);
}
