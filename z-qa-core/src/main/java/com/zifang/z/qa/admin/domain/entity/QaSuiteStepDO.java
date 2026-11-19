package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

/**
 * 套件步骤.
 */

@BizModel(name = "z_qa_suite_step", module = "z-qa", description = "z_qa_suite_step")
@TableName("z_qa_suite_step")
public class QaSuiteStepDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @TableField("suite_id")
    @BizField(name = "所属套件 ID", type = BizField.FieldType.INTEGER)
    private Long suiteId;
    @TableField("step_no")
    @BizField(name = "步骤序号 (从1开始)", type = BizField.FieldType.INTEGER)
    private Integer stepNo;
    @TableField("step_name")
    @BizField(name = "步骤名称", type = BizField.FieldType.STRING)
    private String stepName;
    @TableField("execution_mode")
    @BizField(name = "api=HTTP请求 e2e=浏览器操作", type = BizField.FieldType.STRING)
    private String executionMode;
    @BizField(name = "HTTP method: GET/POST/PUT/DELETE", type = BizField.FieldType.STRING)
    private String method;
    @BizField(name = "HTTP URL", type = BizField.FieldType.STRING)
    private String url;
    @TableField("headers_json")
    @BizField(name = "请求头 JSON", type = BizField.FieldType.STRING)
    private String headersJson;
    @BizField(name = "请求体 (可含 ${VAR} 变量)", type = BizField.FieldType.STRING)
    private String body;
    @TableField("target_mode")
    @BizField(name = "目标: real=真实服务 mock=Mock端点", type = BizField.FieldType.STRING)
    private String targetMode;
    @TableField("mock_endpoint")
    @BizField(name = "Mock 端点 code", type = BizField.FieldType.STRING)
    private String mockEndpoint;
    @TableField("extract_rules")
    @BizField(name = "JSONPath 变量提取规则 JSON", type = BizField.FieldType.STRING)
    private String extractRules;
    @TableField("assert_rules")
    @BizField(name = "断言规则 JSON (eq/gt/contains/regex)", type = BizField.FieldType.STRING)
    private String assertRules;
    @TableField("timeout_ms")
    @BizField(name = "单步超时", type = BizField.FieldType.INTEGER)
    private Integer timeoutMs;
    @TableField("retry_count")
    @BizField(name = "失败重试次数", type = BizField.FieldType.INTEGER)
    private Integer retryCount;
    // FEATURE052 Phase 7 E2E 字段
    @TableField("e2e_action_type")
    @BizField(name = "e2eActionType", type = BizField.FieldType.STRING)
    private String e2eActionType;
    @TableField("e2e_selector")
    @BizField(name = "e2eSelector", type = BizField.FieldType.STRING)
    private String e2eSelector;
    @TableField("e2e_value")
    @BizField(name = "e2eValue", type = BizField.FieldType.STRING)
    private String e2eValue;
    @TableField("e2e_screenshot")
    @BizField(name = "e2eScreenshot", type = BizField.FieldType.INTEGER)
    private Integer e2eScreenshot;
    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @TableField("gmt_create")
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private Date gmtCreate;
    @TableField("gmt_modified")
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
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



