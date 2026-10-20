package com.zifang.z.qa.web.response;

import java.util.Date;

import java.io.Serializable;

/**
 * 执行出参（lead 007 步骤 6）。字段与 QaRunDO 同名，便于 copyProperties。
 */
public class QaRunResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String runCode;
    private Long planId;
    private Long suiteId;
    private String name;
    private String triggerType;
    private String status;
    private Integer totalSteps;
    private Integer passedSteps;
    private Integer failedSteps;
    private Long durationMs;
    private Date startedAt;
    private Date finishedAt;
    private String errorMessage;
    private String triggerUser;
    private String envCode;
    private String envBaseUrl;
    private String callbackUrl;
    private Integer abortRequested;
    private String tenantCode;
    private Date gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunCode() {
        return runCode;
    }

    public void setRunCode(String runCode) {
        this.runCode = runCode;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalSteps() {
        return totalSteps;
    }

    public void setTotalSteps(Integer totalSteps) {
        this.totalSteps = totalSteps;
    }

    public Integer getPassedSteps() {
        return passedSteps;
    }

    public void setPassedSteps(Integer passedSteps) {
        this.passedSteps = passedSteps;
    }

    public Integer getFailedSteps() {
        return failedSteps;
    }

    public void setFailedSteps(Integer failedSteps) {
        this.failedSteps = failedSteps;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getTriggerUser() {
        return triggerUser;
    }

    public void setTriggerUser(String triggerUser) {
        this.triggerUser = triggerUser;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getEnvBaseUrl() {
        return envBaseUrl;
    }

    public void setEnvBaseUrl(String envBaseUrl) {
        this.envBaseUrl = envBaseUrl;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public Integer getAbortRequested() {
        return abortRequested;
    }

    public void setAbortRequested(Integer abortRequested) {
        this.abortRequested = abortRequested;
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
