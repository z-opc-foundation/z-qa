package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

@BizModel(name = "z_qa_plan", module = "z-qa", description = "z_qa_plan")
@TableName("z_qa_plan")
public class QaPlanDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "计划编码 (唯一)", type = BizField.FieldType.STRING)
    private String code;
    @BizField(name = "name", type = BizField.FieldType.STRING)
    private String name;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
    @TableField("suite_ids")
    @BizField(name = "JSON 数组: 关联套件 ID 列表", type = BizField.FieldType.STRING)
    private String suiteIds;
    @TableField("case_ids")
    @BizField(name = "JSON 数组: 关联用例 ID 列表", type = BizField.FieldType.STRING)
    private String caseIds;
    @TableField("cron_expr")
    @BizField(name = "cron 表达式 (空=手动)", type = BizField.FieldType.STRING)
    private String cronExpr;
    @TableField("notify_type")
    @BizField(name = "none/inapp/email/all", type = BizField.FieldType.STRING)
    private String notifyType;
    /**
     * FEATURE052 §9.5 — 通知渠道 (in_app,email)
     */
    @TableField("notify_channels")
    @BizField(name = "FEATURE052 §9.5 — 通知渠道 (in_app,email)", type = BizField.FieldType.STRING)
    private String notifyChannels;
    @TableField("notify_on_pass")
    @BizField(name = "notifyOnPass", type = BizField.FieldType.INTEGER)
    private Integer notifyOnPass;
    @TableField("notify_on_fail")
    @BizField(name = "notifyOnFail", type = BizField.FieldType.INTEGER)
    private Integer notifyOnFail;
    @BizField(name = "status", type = BizField.FieldType.INTEGER)
    private Integer status;
    @BizField(name = "owner", type = BizField.FieldType.STRING)
    private String owner;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuiteIds() {
        return suiteIds;
    }

    public void setSuiteIds(String suiteIds) {
        this.suiteIds = suiteIds;
    }

    public String getCaseIds() {
        return caseIds;
    }

    public void setCaseIds(String caseIds) {
        this.caseIds = caseIds;
    }

    public String getCronExpr() {
        return cronExpr;
    }

    public void setCronExpr(String cronExpr) {
        this.cronExpr = cronExpr;
    }

    public String getNotifyType() {
        return notifyType;
    }

    public void setNotifyType(String notifyType) {
        this.notifyType = notifyType;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
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



