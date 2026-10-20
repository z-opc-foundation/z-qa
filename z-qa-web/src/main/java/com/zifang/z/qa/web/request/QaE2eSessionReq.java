package com.zifang.z.qa.web.request;


import java.io.Serializable;

/**
 * E2E 会话开立入参（lead 007 步骤 6）。
 */
public class QaE2eSessionReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long runId;
    private Long runDetailId;
    private String browserType;
    private String targetUrl;

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
}
