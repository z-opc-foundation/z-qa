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
 * 通知投递日志 (对应 z_qa_notify_log).
 * FEATURE052 §9.6
 */
@BizModel(name = "通知投递日志 (对应 z_qa_notify_log)", module = "z-qa", description = "通知投递日志 (对应 z_qa_notify_log). FEATURE052 §9.6")
@TableName("z_qa_notify_log")
public class QaNotifyLogDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("run_id")
    @BizField(name = "关联执行 ID", type = BizField.FieldType.INTEGER)
    private Long runId;

    @BizField(name = "in_app/email", type = BizField.FieldType.STRING)
    private String channel;

    @BizField(name = "收件方", type = BizField.FieldType.STRING)
    private String target;

    @BizField(name = "消息负载 JSON", type = BizField.FieldType.STRING)
    private String payload;

    @BizField(name = "pending/sent/failed", type = BizField.FieldType.STRING)
    private String status;

    @TableField("error_message")
    @BizField(name = "errorMessage", type = BizField.FieldType.STRING)
    private String errorMessage;

    @TableField("sent_at")
    @BizField(name = "sentAt", type = BizField.FieldType.DATETIME)
    private Date sentAt;

    @TableField("gmt_create")
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private Date gmtCreate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }
}



