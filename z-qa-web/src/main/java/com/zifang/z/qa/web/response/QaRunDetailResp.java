package com.zifang.z.qa.web.response;

import java.util.Date;

import java.io.Serializable;

/**
 * 执行明细出参（lead 007 步骤 6）。
 */
public class QaRunDetailResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long runId;
    private Integer stepNo;
    private String stepName;
    private String method;
    private String requestUrl;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseHeaders;
    private String responseBody;
    private String variablesBefore;
    private String variablesAfter;
    private String extractedVars;
    private String assertResults;
    private String result;
    private String errorMessage;
    private Long durationMs;
    private Integer retryCount;
    private String mockEndpointCode;
    private String templateRequest;
    private String extractions;
    private String targetMode;
    private Long suiteId;
    private String defectType;
    private String tenantCode;
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
}
