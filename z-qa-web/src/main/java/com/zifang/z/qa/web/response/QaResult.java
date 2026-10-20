package com.zifang.z.qa.web.response;

import java.io.Serializable;

/**
 * 统一响应信封（lead 007 步骤 6）。契约与前端 {@code services/api.ts} 的 QaResponse 一一对应：
 * code / msg / content / success 四个键，前端已在按 {@code res?.content} 取值。
 *
 * <p>禁 Jackson 注解（CEO 2026-10-16 裁定）——本仓不引 com.fasterxml.jackson，序列化由
 * 宿主 Spring 的 HttpMessageConverter 负责，DTO 只留 POJO。
 */
public class QaResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String msg;
    private T content;
    private boolean success;

    public QaResult() {
    }

    public QaResult(int code, String msg, T content, boolean success) {
        this.code = code;
        this.msg = msg;
        this.content = content;
        this.success = success;
    }

    public static <T> QaResult<T> ok(T content) {
        return new QaResult<>(200, "success", content, true);
    }

    public static <T> QaResult<T> ok(String msg, T content) {
        return new QaResult<>(200, msg, content, true);
    }

    /** 无返回体的成功（增删改走这条）。 */
    public static <T> QaResult<T> ok() {
        return new QaResult<>(200, "success", null, true);
    }

    public static <T> QaResult<T> notFound(String msg) {
        return new QaResult<>(404, msg, null, false);
    }

    public static <T> QaResult<T> badRequest(String msg) {
        return new QaResult<>(400, msg, null, false);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
