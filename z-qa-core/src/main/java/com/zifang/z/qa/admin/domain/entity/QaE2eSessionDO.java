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
 * E2E 浏览器会话 (对应 z_qa_e2e_session).
 * FEATURE052 §9.7
 */
@BizModel(name = "E2E 浏览器会话 (对应 z_qa_e2e_session)", module = "z-qa", description = "E2E 浏览器会话 (对应 z_qa_e2e_session). FEATURE052 §9.7")
@TableName("z_qa_e2e_session")
public class QaE2eSessionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;

    @TableField("run_id")
    @BizField(name = "关联执行记录 ID (z_qa_run.id)", type = BizField.FieldType.INTEGER)
    private Long runId;

    @TableField("run_detail_id")
    @BizField(name = "关联步骤明细 ID (z_qa_run_detail.id)", type = BizField.FieldType.INTEGER)
    private Long runDetailId;

    @TableField("browser_type")
    @BizField(name = "chromium/firefox/webkit", type = BizField.FieldType.STRING)
    private String browserType;

    @TableField("target_url")
    @BizField(name = "目标 URL", type = BizField.FieldType.STRING)
    private String targetUrl;

    @TableField("viewport_width")
    @BizField(name = "视口宽", type = BizField.FieldType.INTEGER)
    private Integer viewportWidth;

    @TableField("viewport_height")
    @BizField(name = "视口高", type = BizField.FieldType.INTEGER)
    private Integer viewportHeight;

    @TableField("video_oss_key")
    @BizField(name = "视频 OSS 路径 (z-oss)", type = BizField.FieldType.STRING)
    private String videoOssKey;

    @TableField("trace_oss_key")
    @BizField(name = "Trace OSS 路径", type = BizField.FieldType.STRING)
    private String traceOssKey;

    @TableField("har_oss_key")
    @BizField(name = "HAR OSS 路径", type = BizField.FieldType.STRING)
    private String harOssKey;

    @BizField(name = "pending/running/done/failed", type = BizField.FieldType.STRING)
    private String status;

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

    public Long getRunId() {
        return runId;
    }

    public void setRunId(Long runId) {
        this.runId = runId;
    }

    public Long getRunDetailId() {
        return runDetailId;
    }

    public void setRunDetailId(Long runDetailId) {
        this.runDetailId = runDetailId;
    }

    public String getBrowserType() {
        return browserType;
    }

    public void setBrowserType(String browserType) {
        this.browserType = browserType;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Integer getViewportWidth() {
        return viewportWidth;
    }

    public void setViewportWidth(Integer viewportWidth) {
        this.viewportWidth = viewportWidth;
    }

    public Integer getViewportHeight() {
        return viewportHeight;
    }

    public void setViewportHeight(Integer viewportHeight) {
        this.viewportHeight = viewportHeight;
    }

    public String getVideoOssKey() {
        return videoOssKey;
    }

    public void setVideoOssKey(String videoOssKey) {
        this.videoOssKey = videoOssKey;
    }

    public String getTraceOssKey() {
        return traceOssKey;
    }

    public void setTraceOssKey(String traceOssKey) {
        this.traceOssKey = traceOssKey;
    }

    public String getHarOssKey() {
        return harOssKey;
    }

    public void setHarOssKey(String harOssKey) {
        this.harOssKey = harOssKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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



