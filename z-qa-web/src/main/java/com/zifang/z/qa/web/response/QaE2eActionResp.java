package com.zifang.z.qa.web.response;

import java.util.Date;

import java.io.Serializable;

/**
 * E2E 操作出参（lead 007 步骤 6）。
 */
public class QaE2eActionResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long sessionId;
    private Integer stepNo;
    private String actionType;
    private String selector;
    private String actionValue;
    private String description;
    private String screenshotOssKey;
    private Integer e2eScreenshot;
    private String result;
    private String errorMessage;
    private Long durationMs;
    private Date startedAt;
    private Date finishedAt;
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
