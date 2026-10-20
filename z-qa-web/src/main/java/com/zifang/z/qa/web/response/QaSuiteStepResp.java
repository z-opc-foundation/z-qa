package com.zifang.z.qa.web.response;

import java.util.Date;

import java.io.Serializable;

/**
 * 套件步骤出参（lead 007 步骤 6）。
 */
public class QaSuiteStepResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long suiteId;
    private Integer stepNo;
    private String stepName;
    private String executionMode;
    private String method;
    private String url;
    private String headersJson;
    private String body;
    private String targetMode;
    private String mockEndpoint;
    private String extractRules;
    private String assertRules;
    private Integer timeoutMs;
    private Integer retryCount;
    private String e2eActionType;
    private String e2eSelector;
    private String e2eValue;
    private Integer e2eScreenshot;
    private String tenantCode;
    private Date gmtCreate;
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
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

    public String getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(String executionMode) {
        this.executionMode = executionMode;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTargetMode() {
        return targetMode;
    }

    public void setTargetMode(String targetMode) {
        this.targetMode = targetMode;
    }

    public String getMockEndpoint() {
        return mockEndpoint;
    }

    public void setMockEndpoint(String mockEndpoint) {
        this.mockEndpoint = mockEndpoint;
    }

    public String getExtractRules() {
        return extractRules;
    }

    public void setExtractRules(String extractRules) {
        this.extractRules = extractRules;
    }

    public String getAssertRules() {
        return assertRules;
    }

    public void setAssertRules(String assertRules) {
        this.assertRules = assertRules;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getE2eActionType() {
        return e2eActionType;
    }

    public void setE2eActionType(String e2eActionType) {
        this.e2eActionType = e2eActionType;
    }

    public String getE2eSelector() {
        return e2eSelector;
    }

    public void setE2eSelector(String e2eSelector) {
        this.e2eSelector = e2eSelector;
    }

    public String getE2eValue() {
        return e2eValue;
    }

    public void setE2eValue(String e2eValue) {
        this.e2eValue = e2eValue;
    }

    public Integer getE2eScreenshot() {
        return e2eScreenshot;
    }

    public void setE2eScreenshot(Integer e2eScreenshot) {
        this.e2eScreenshot = e2eScreenshot;
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

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}
