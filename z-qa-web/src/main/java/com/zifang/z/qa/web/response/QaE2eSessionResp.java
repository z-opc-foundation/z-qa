package com.zifang.z.qa.web.response;

import java.util.Date;

import java.io.Serializable;

/**
 * E2E 会话出参（lead 007 步骤 6）。
 */
public class QaE2eSessionResp implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long runId;
    private Long runDetailId;
    private String browserType;
    private String targetUrl;
    private Integer viewportWidth;
    private Integer viewportHeight;
    private String videoOssKey;
    private String traceOssKey;
    private String harOssKey;
    private String status;
    private Date startedAt;
    private Date finishedAt;
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
