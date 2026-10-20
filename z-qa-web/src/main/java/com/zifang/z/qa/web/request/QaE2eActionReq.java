package com.zifang.z.qa.web.request;


import java.io.Serializable;

/**
 * E2E 动作执行入参（lead 007 步骤 6）。
 */
public class QaE2eActionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long sessionId;
    private Integer stepNo;
    private String actionType;
    private String selector;
    private String value;
    private String description;
    private Integer screenshot;

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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getScreenshot() {
        return screenshot;
    }

    public void setScreenshot(Integer screenshot) {
        this.screenshot = screenshot;
    }
}
