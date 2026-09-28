package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * E2E 操作步骤 (对应 z_qa_e2e_action).
 * FEATURE052 §9.7
 */
@TableName("z_qa_e2e_action")
public class QaE2eActionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("step_no")
    private Integer stepNo;

    @TableField("action_type")
    private String actionType;

    private String selector;

    @TableField("action_value")
    private String actionValue;

    private String description;

    @TableField("screenshot_oss_key")
    private String screenshotOssKey;

    /**
     * 是否截图 (FEATURE052 §9.7)
     */
    @TableField("e2e_screenshot")
    private Integer e2eScreenshot;

    private String result;

    @TableField("error_message")
    private String errorMessage;

    @TableField("duration_ms")
    private Long durationMs;

    @TableField("started_at")
    private Date startedAt;

    @TableField("finished_at")
    private Date finishedAt;

    @TableField("tenant_code")
    private String tenantCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getStepNo() {
        return stepNo;
    }

    public void setStepNo(Integer stepNo) {
        this.stepNo = stepNo;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getActionValue() {
        return actionValue;
    }

    public void setActionValue(String actionValue) {
        this.actionValue = actionValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getScreenshotOssKey() {
        return screenshotOssKey;
    }

    public void setScreenshotOssKey(String screenshotOssKey) {
        this.screenshotOssKey = screenshotOssKey;
    }

    public Integer getE2eScreenshot() {
        return e2eScreenshot;
    }

    public void setE2eScreenshot(Integer e2eScreenshot) {
        this.e2eScreenshot = e2eScreenshot;
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

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }
}
