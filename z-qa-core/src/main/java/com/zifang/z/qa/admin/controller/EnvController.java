package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaEnvDO;
import com.zifang.z.qa.admin.domain.mapper.QaEnvMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试环境管理 Controller.
 */
@RestController("qaEnvController")
@RequestMapping("/api/qa/env")
public class EnvController {

    @Autowired
    private QaEnvMapper envMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<QaEnvDO> list = envMapper.selectList(new QueryWrapper<QaEnvDO>().orderByDesc("priority"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("content", list);
        result.put("success", true);
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody QaEnvDO env) {
        env.setGmtCreate(new Date());
        env.setGmtModified(new Date());
        envMapper.insert(env);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("content", env);
        result.put("success", true);
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QaEnvDO env) {
        env.setGmtModified(new Date());
        envMapper.updateById(env);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("success", true);
        return result;
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long id) {
        envMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("success", true);
        return result;
    }
}