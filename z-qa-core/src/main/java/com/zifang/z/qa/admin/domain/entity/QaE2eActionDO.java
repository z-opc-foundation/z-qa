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
 * E2E 操作步骤 (对应 z_qa_e2e_action).
 * FEATURE052 §9.7
 */
@BizModel(name = "E2E 操作步骤 (对应 z_qa_e2e_action)", module = "z-qa", description = "E2E 操作步骤 (对应 z_qa_e2e_action). FEATURE052 §9.7")
@TableName("z_qa_e2e_action")
public class QaE2eActionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("session_id")
    @BizField(name = "关联会话 ID (z_qa_e2e_session.id)", type = BizField.FieldType.INTEGER)
    private Long sessionId;

    @TableField("step_no")
    @BizField(name = "操作序号", type = BizField.FieldType.INTEGER)
    private Integer stepNo;

    @TableField("action_type")
    @BizField(name = "navigate/click/fill/select/hover/wait/assert_text/assert_visible/screenshot", type = BizField.FieldType.STRING)
    private String actionType;

    @BizField(name = "元素定位器 (CSS/XPath)", type = BizField.FieldType.STRING)
    private String selector;

    @TableField("action_value")
    @BizField(name = "输入值/预期值", type = BizField.FieldType.STRING)
    private String actionValue;

    @BizField(name = "操作描述", type = BizField.FieldType.STRING)
    private String description;

    @TableField("screenshot_oss_key")
    @BizField(name = "截图 OSS 路径", type = BizField.FieldType.STRING)
    private String screenshotOssKey;

    /**
     * 是否截图 (FEATURE052 §9.7)
     */
    @TableField("e2e_screenshot")
    @BizField(name = "是否截图 (FEATURE052 §9.7)", type = BizField.FieldType.INTEGER)
    private Integer e2eScreenshot;

    @BizField(name = "pending/pass/fail/error", type = BizField.FieldType.STRING)
    private String result;

    @TableField("error_message")
    @BizField(name = "errorMessage", type = BizField.FieldType.STRING)
    private String errorMessage;

    @TableField("duration_ms")
    @BizField(name = "durationMs", type = BizField.FieldType.INTEGER)
    private Long durationMs;

    @TableField("started_at")
    @BizField(name = "startedAt", type = BizField.FieldType.DATETIME)
    private Date startedAt;

    @TableField("finished_at")
    @BizField(name = "finishedAt", type = BizField.FieldType.DATETIME)
    private Date finishedAt;

    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
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



