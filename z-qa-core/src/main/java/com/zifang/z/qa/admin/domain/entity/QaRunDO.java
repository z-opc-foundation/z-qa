package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;


@TableName("z_qa_run")
public class QaRunDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("run_code")
    private String runCode;
    @TableField("plan_id")
    private Long planId;
    @TableField("suite_id")
    private Long suiteId;
    private String name;
    @TableField("trigger_type")
    private String triggerType;
    private String status;
    @TableField("total_steps")
    private Integer totalSteps;
    @TableField("passed_steps")
    private Integer passedSteps;
    @TableField("failed_steps")
    private Integer failedSteps;
    @TableField("duration_ms")
    private Long durationMs;
    @TableField("started_at")
    private Date startedAt;
    @TableField("finished_at")
    private Date finishedAt;
    @TableField("error_message")
    private String errorMessage;
    @TableField("trigger_user")
    private String triggerUser;
    /**
     * 执行环境 (FK 到 z_qa_env.code)
     */
    @TableField("env_code")
    private String envCode;
    /**
     * 执行环境 baseUrl 快照
     */
    @TableField("env_base_url")
    private String envBaseUrl;
    /**
     * FEATURE052 Phase 4 CI 集成 — 完成后回调 URL
     */
    @TableField("callback_url")
    private String callbackUrl;
    /**
     * 中止请求标志。走 DB 而不是内存 flag:多实例下 abort 打到的实例可能不是跑这条 run 的实例，
     * 且内存 flag 无处回收。
     */
    @TableField("abort_requested")
    private Integer abortRequested;
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
