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
 * 定时执行 (对应 z_qa_schedule).
 * FEATURE052 §4.5 + §9.5
 */
@BizModel(name = "定时执行 (对应 z_qa_schedule)", module = "z-qa", description = "定时执行 (对应 z_qa_schedule). FEATURE052 §4.5 + §9.5")
@TableName("z_qa_schedule")
public class QaScheduleDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @BizField(name = "定时任务名称", type = BizField.FieldType.STRING)
    private String name;

    /**
     * suite=单套件 / plan=批量计划
     */
    @TableField("target_type")
    @BizField(name = "suite=单套件 plan=批量计划", type = BizField.FieldType.STRING)
    private String targetType;

    @TableField("suite_id")
    @BizField(name = "target_type=suite 时必填", type = BizField.FieldType.INTEGER)
    private Long suiteId;

    @TableField("plan_id")
    @BizField(name = "target_type=plan 时必填 (关联 z_qa_plan.id)", type = BizField.FieldType.INTEGER)
    private Long planId;

    @TableField("cron_expr")
    @BizField(name = "Cron 表达式 (例如 0 0 23 * * ?)", type = BizField.FieldType.STRING)
    private String cronExpr;

    @TableField("env_code")
    @BizField(name = "执行环境 (env.baseUrl)", type = BizField.FieldType.STRING)
    private String envCode;

    /**
     * 0=禁用 1=启用
     */
    @BizField(name = "是否启用", type = BizField.FieldType.INTEGER)
    private Integer enabled;

    @TableField("notify_channels")
    @BizField(name = "通知渠道 (逗号分隔: in_app,email)", type = BizField.FieldType.STRING)
    private String notifyChannels;

    @TableField("notify_on_pass")
    @BizField(name = "通过也通知", type = BizField.FieldType.INTEGER)
    private Integer notifyOnPass;

    @TableField("notify_on_fail")
    @BizField(name = "失败才通知", type = BizField.FieldType.INTEGER)
    private Integer notifyOnFail;

    @TableField("last_run_id")
    @BizField(name = "上次执行 ID", type = BizField.FieldType.INTEGER)
    private Long lastRunId;

    @TableField("last_run_time")
    @BizField(name = "上次执行时间", type = BizField.FieldType.DATETIME)
    private Date lastRunTime;

    @TableField("last_run_result")
    @BizField(name = "上次执行结果", type = BizField.FieldType.STRING)
    private String lastRunResult;

    @TableField("next_run_time")
    @BizField(name = "下次执行时间", type = BizField.FieldType.DATETIME)
    private Date nextRunTime;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getSuiteId() {
        return suiteId;
    }

    public void setSuiteId(Long suiteId) {
        this.suiteId = suiteId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public void setCronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public String getNotifyChannels() {
        return notifyChannels;
    }

    public void setNotifyChannels(String notifyChannels) {
        this.notifyChannels = notifyChannels;
    }

    public Integer getNotifyOnPass() {
        return notifyOnPass;
    }

    public void setNotifyOnPass(Integer notifyOnPass) {
        this.notifyOnPass = notifyOnPass;
    }

    public Integer getNotifyOnFail() {
        return notifyOnFail;
    }

    public void setNotifyOnFail(Integer notifyOnFail) {
        this.notifyOnFail = notifyOnFail;
    }

    public Long getLastRunId() {
        return lastRunId;
    }

    public void setLastRunId(Long lastRunId) {
        this.lastRunId = lastRunId;
    }

    public Date getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(Date lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public String getLastRunResult() {
        return lastRunResult;
    }

    public void setLastRunResult(String lastRunResult) {
        this.lastRunResult = lastRunResult;
    }

    public Date getNextRunTime() {
        return nextRunTime;
    }

    public void setNextRunTime(Date nextRunTime) {
        this.nextRunTime = nextRunTime;
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



