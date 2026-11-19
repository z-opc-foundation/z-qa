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
 * 测试套件.
 */

@BizModel(name = "z_qa_suite", module = "z-qa", description = "z_qa_suite")
@TableName("z_qa_suite")
public class QaSuiteDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "套件编码 (唯一)", type = BizField.FieldType.STRING)
    private String code;
    @BizField(name = "套件名称", type = BizField.FieldType.STRING)
    private String name;
    @BizField(name = "套件描述", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "分类 (smoke/regression/api/e2e)", type = BizField.FieldType.STRING)
    private String category;
    @TableField("env_code")
    @BizField(name = "默认环境 (env.baseUrl)", type = BizField.FieldType.STRING)
    private String envCode;
    @BizField(name = "负责人", type = BizField.FieldType.STRING)
    private String owner;
    @BizField(name = "状态: 0=禁用 1=正常", type = BizField.FieldType.INTEGER)
    private Integer status;
    @BizField(name = "版本号", type = BizField.FieldType.INTEGER)
    private Integer version;
    @BizField(name = "逗号分隔标签", type = BizField.FieldType.STRING)
    private String tags;
    /**
     * 遇错即停:某步失败后剩余步骤直接落 skip 并结束本次 run
     */
    @TableField("fail_fast")
    @BizField(name = "failFast", type = BizField.FieldType.INTEGER)
    private Integer failFast;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getEnvCode() {
        return envCode;
    }

    public void setEnvCode(String envCode) {
        this.envCode = envCode;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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

    public Integer getFailFast() {
        return failFast;
    }

    public void setFailFast(Integer failFast) {
        this.failFast = failFast;
    }
}



