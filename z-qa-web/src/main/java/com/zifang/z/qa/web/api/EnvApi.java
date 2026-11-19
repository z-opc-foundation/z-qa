package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.meta.api.annotation.BizApi;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizParam;
import com.zifang.z.qa.admin.domain.entity.QaEnvDO;
import com.zifang.z.qa.admin.domain.mapper.QaEnvMapper;
import com.zifang.z.qa.web.request.QaEnvReq;
import com.zifang.z.qa.web.response.QaEnvResp;
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
 * 环境 API（lead 007 步骤 6）。
 */
@RestController("qaEnvController")
@RequestMapping("/api/qa/env")
public class EnvApi {

    @Autowired
    private QaEnvMapper envMapper;

    @GetMapping("/list")
    @BizApi(summary = "list", description = "", tags = { "z-qa" })
    public QaResult<List<QaEnvResp>> list() {
        List<QaEnvDO> list = envMapper.selectList(new QueryWrapper<QaEnvDO>().orderByDesc("priority"));
        return QaResult.ok(QaConverter.toList(list, QaEnvResp.class));
    }

    @PostMapping("/add")
    @BizApi(summary = "add", description = "", tags = { "z-qa" })
    public QaResult<QaEnvResp> add(@RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) QaEnvReq req) {
        QaEnvDO env = new QaEnvDO();
        BeanUtils.copyProperties(req, env);
        Date now = new Date();
        env.setGmtCreate(now);
        env.setGmtModified(now);
        envMapper.insert(env);
        return QaResult.ok(QaConverter.to(env, QaEnvResp.class));
    }

    @PostMapping("/update")
    @BizApi(summary = "update", description = "", tags = { "z-qa" })
    public QaResult<Void> update(@RequestBody @BizParam(name = "req", type = BizField.FieldType.STRING, description = "", position = BizParam.Position.BODY) QaEnvReq req) {
        QaEnvDO env = new QaEnvDO();
        BeanUtils.copyProperties(req, env);
        env.setGmtModified(new Date());
        envMapper.updateById(env);
        return QaResult.ok();
    }

    @PostMapping("/remove")
    @BizApi(summary = "remove", description = "", tags = { "z-qa" })
    public QaResult<Void> remove(@RequestParam @BizParam(name = "id", type = BizField.FieldType.INTEGER, description = "", position = BizParam.Position.QUERY) Long id) {
        envMapper.deleteById(id);
        return QaResult.ok();
    }
}



