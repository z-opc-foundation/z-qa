package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 套件步骤.
 */

@TableName("z_qa_suite_step")
public class QaSuiteStepDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("suite_id")
    private Long suiteId;
    @TableField("step_no")
    private Integer stepNo;
    @TableField("step_name")
    private String stepName;
    @TableField("execution_mode")
    private String executionMode;
    private String method;
    private String url;
    @TableField("headers_json")
    private String headersJson;
    private String body;
    @TableField("target_mode")
    private String targetMode;
    @TableField("mock_endpoint")
    private String mockEndpoint;
    @TableField("extract_rules")
    private String extractRules;
    @TableField("assert_rules")
    private String assertRules;
    @TableField("timeout_ms")
    private Integer timeoutMs;
    @TableField("retry_count")
    private Integer retryCount;
    // FEATURE052 Phase 7 E2E 字段
    @TableField("e2e_action_type")
    private String e2eActionType;
    @TableField("e2e_selector")
    private String e2eSelector;
    @TableField("e2e_value")
    private String e2eValue;
    @TableField("e2e_screenshot")
    private Integer e2eScreenshot;
    @TableField("tenant_code")
    private String tenantCode;
    @TableField("gmt_create")
    private Date gmtCreate;
    @TableField("gmt_modified")
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
