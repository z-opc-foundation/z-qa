package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * E2E 浏览器会话 (对应 z_qa_e2e_session).
 * FEATURE052 §9.7
 */
@TableName("z_qa_e2e_session")
public class QaE2eSessionDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("run_id")
    private Long runId;

    @TableField("run_detail_id")
    private Long runDetailId;

    @TableField("browser_type")
    private String browserType;

    @TableField("target_url")
    private String targetUrl;

    @TableField("viewport_width")
    private Integer viewportWidth;

    @TableField("viewport_height")
    private Integer viewportHeight;

    @TableField("video_oss_key")
    private String videoOssKey;

    @TableField("trace_oss_key")
    private String traceOssKey;

    @TableField("har_oss_key")
    private String harOssKey;

    private String status;

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
