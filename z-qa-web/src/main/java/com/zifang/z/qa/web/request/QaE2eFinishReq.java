package com.zifang.z.qa.web.request;


import java.io.Serializable;

/**
 * E2E 会话结束入参（lead 007 步骤 6）。
 */
public class QaE2eFinishReq implements Serializable {

    private static final long serialVersionUID = 1L;

    private String status;
    private String videoOssKey;
    private String traceOssKey;
    private String harOssKey;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
}
