package com.zifang.z.qa.admin.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.io.Serializable;
import java.util.Date;

@BizModel(name = "z_qa_case", module = "z-qa", description = "z_qa_case")
@TableName("z_qa_case")
public class QaCaseDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "用例编码 (唯一)", type = BizField.FieldType.STRING)
    private String code;
    @BizField(name = "name", type = BizField.FieldType.STRING)
    private String name;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "P0/P1/P2/P3", type = BizField.FieldType.STRING)
    private String priority;
    @BizField(name = "category", type = BizField.FieldType.STRING)
    private String category;
    @BizField(name = "owner", type = BizField.FieldType.STRING)
    private String owner;
    @BizField(name = "tags", type = BizField.FieldType.STRING)
    private String tags;
    @BizField(name = "status", type = BizField.FieldType.INTEGER)
    private Integer status;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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



