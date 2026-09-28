package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;


@TableName("z_qa_run_detail")
public class QaRunDetailDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("run_id")
    private Long runId;
    @TableField("step_no")
    private Integer stepNo;
    @TableField("step_name")
    private String stepName;
    private String method;
    @TableField("request_url")
    private String requestUrl;
    @TableField("request_headers")
    private String requestHeaders;
    @TableField("request_body")
    private String requestBody;
    @TableField("response_status")
    private Integer responseStatus;
    @TableField("response_headers")
    private String responseHeaders;
    @TableField("response_body")
    private String responseBody;
    @TableField("variables_before")
    private String variablesBefore;
    @TableField("variables_after")
    private String variablesAfter;
    @TableField("extracted_vars")
    private String extractedVars;
    @TableField("assert_results")
    private String assertResults;
    private String result;
    @TableField("error_message")
    private String errorMessage;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("retry_count")
    private Integer retryCount;
    /**
     * FEATURE052 §3.3 — Mock 端点编码
     */
    @TableField("mock_endpoint_code")
    private String mockEndpointCode;
    /**
     * FEATURE052 §3.3 — 模板请求 (变量替换前)
     */
    @TableField("template_request")
    private String templateRequest;
    /**
     * FEATURE052 §3.3 — 变量提取过程明细
     */
    @TableField("extractions")
    private String extractions;
    /**
     * FEATURE052 §3.3 — 目标模式: real / mock
     */
    @TableField("target_mode")
    private String targetMode;
    /**
     * 计划 run 一次覆盖多个套件，明细必须能回溯到归属套件；step_no 在 run 内是全局序号，
     * 不再等于套件内的序号
     */
    @TableField("suite_id")
    private Long suiteId;
    /**
     * 失败态推导出的缺陷分类 (system_defect / script_defect / env_data_defect)，通过时为 null
     */
    @TableField("defect_type")
    private String defectType;
    @TableField("tenant_code")
    private String tenantCode;
    @TableField("gmt_create")
    private Date gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getStepName() {
        return stepName;
    }

    public void setStepName(String stepName) {
        this.stepName = stepName;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public Integer getResponseStatus() {
        return responseStatus;
    }

    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(String responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getVariablesBefore() {
        return variablesBefore;
    }

    public void setVariablesBefore(String variablesBefore) {
        this.variablesBefore = variablesBefore;
    }

    public String getVariablesAfter() {
        return variablesAfter;
    }

    public void setVariablesAfter(String variablesAfter) {
        this.variablesAfter = variablesAfter;
    }

    public String getExtractedVars() {
        return extractedVars;
    }

    public void setExtractedVars(String extractedVars) {
        this.extractedVars = extractedVars;
    }

    public String getAssertResults() {
        return assertResults;
    }

    public void setAssertResults(String assertResults) {
        this.assertResults = assertResults;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getMockEndpointCode() {
        return mockEndpointCode;
    }

    public void setMockEndpointCode(String mockEndpointCode) {
        this.mockEndpointCode = mockEndpointCode;
    }

    public String getTemplateRequest() {
        return templateRequest;
    }

    public void setTemplateRequest(String templateRequest) {
        this.templateRequest = templateRequest;
    }

    public String getExtractions() {
        return extractions;
    }

    public void setExtractions(String extractions) {
        this.extractions = extractions;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
    }

    public String getDefectType() {
        return defectType;
    }

    public void setDefectType(String defectType) {
        this.defectType = defectType;
    }
}
