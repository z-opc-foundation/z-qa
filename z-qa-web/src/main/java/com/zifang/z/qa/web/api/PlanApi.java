package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.qa.admin.domain.entity.QaPlanDO;
import com.zifang.z.qa.admin.domain.mapper.QaPlanMapper;
import com.zifang.z.qa.web.request.QaPlanReq;
import com.zifang.z.qa.web.response.QaPlanResp;
import com.zifang.z.qa.web.response.QaResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 计划 API（lead 007 步骤 6）。
 */
@RestController("qaPlanController")
@RequestMapping("/api/qa/plan")
public class PlanApi {

    @Autowired
    private QaPlanMapper planMapper;

    @GetMapping("/list")
    @BizApi(summary = "list", description = "", tags = { "z-qa" })
    public QaResult<List<QaPlanResp>> list() {
        List<QaPlanDO> list = planMapper.selectList(new QueryWrapper<QaPlanDO>().orderByDesc("gmt_create"));
        return QaResult.ok(QaConverter.toList(list, QaPlanResp.class));
    }

    @GetMapping("/get")
    @BizApi(summary = "get", description = "", tags = { "z-qa" })
    public QaResult<QaPlanResp> get(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long id) {
        QaPlanDO plan = planMapper.selectById(id);
        if (plan == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(QaConverter.to(plan, QaPlanResp.class));
    }

    @PostMapping("/add")
    @BizApi(summary = "add", description = "", tags = { "z-qa" })
    public QaResult<QaPlanResp> add(@RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) QaPlanReq req) {
        QaPlanDO plan = new QaPlanDO();
        BeanUtils.copyProperties(req, plan);
        Date now = new Date();
        plan.setGmtCreate(now);
        plan.setGmtModified(now);
        if (plan.getStatus() == null) {
            plan.setStatus(1);
        }
        planMapper.insert(plan);
        return QaResult.ok(QaConverter.to(plan, QaPlanResp.class));
    }

    @PostMapping("/update")
    @BizApi(summary = "update", description = "", tags = { "z-qa" })
    public QaResult<Void> update(@RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) QaPlanReq req) {
        QaPlanDO plan = new QaPlanDO();
        BeanUtils.copyProperties(req, plan);
        plan.setGmtModified(new Date());
        planMapper.updateById(plan);
        return QaResult.ok();
    }

    @PostMapping("/remove")
    @BizApi(summary = "remove", description = "", tags = { "z-qa" })
    public QaResult<Void> remove(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long id) {
        planMapper.deleteById(id);
        return QaResult.ok();
    }
}



