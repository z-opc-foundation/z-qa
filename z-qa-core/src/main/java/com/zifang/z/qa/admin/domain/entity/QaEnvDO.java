package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

@BizModel(name = "z_qa_env", module = "z-qa", description = "z_qa_env")
@TableName("z_qa_env")
public class QaEnvDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "code", type = BizField.FieldType.STRING)
    private String code;
    @BizField(name = "name", type = BizField.FieldType.STRING)
    private String name;
    @TableField("base_url")
    @BizField(name = "baseUrl", type = BizField.FieldType.STRING)
    private String baseUrl;
    @TableField("headers_json")
    @BizField(name = "默认请求头 JSON", type = BizField.FieldType.STRING)
    private String headersJson;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "排序, 越大越靠前", type = BizField.FieldType.INTEGER)
    private Integer priority;
    @TableField("tenant_code")
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @TableField("gmt_create")
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private Date gmtCreate;
    @TableField("gmt_modified")
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private Date gmtModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getHeadersJson() {
        return headersJson;
    }

    public void setHeadersJson(String headersJson) {
        this.headersJson = headersJson;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public Date getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(Date gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public Date getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(Date gmtModified) {
        this.gmtModified = gmtModified;
    }
}



