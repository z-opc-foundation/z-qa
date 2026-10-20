package com.zifang.z.qa.web.request;


import java.io.Serializable;

/**
 * 定时任务入参（lead 007 步骤 6）。
 */
public class QaScheduleReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String targetType;
    private Long suiteId;
    private Long planId;
    private String cronExpr;
    private String envCode;
    private Integer enabled;
    private String notifyChannels;
    private Integer notifyOnPass;
    private Integer notifyOnFail;

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
}
