package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaPlanDO;
import com.zifang.z.qa.admin.domain.mapper.QaPlanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试计划 Controller.
 */
@RestController("qaPlanController")
@RequestMapping("/api/qa/plan")
public class PlanController {

    @Autowired
    private QaPlanMapper planMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<QaPlanDO> list = planMapper.selectList(new QueryWrapper<QaPlanDO>().orderByDesc("gmt_create"));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("content", list);
        result.put("success", true);
        return result;
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam Long id) {
        QaPlanDO plan = planMapper.selectById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", plan != null ? 200 : 404);
        result.put("content", plan);
        result.put("success", plan != null);
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody QaPlanDO plan) {
        plan.setGmtCreate(new Date());
        plan.setGmtModified(new Date());
        if (plan.getStatus() == null) plan.setStatus(1);
        planMapper.insert(plan);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("content", plan);
        result.put("success", true);
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QaPlanDO plan) {
        plan.setGmtModified(new Date());
        planMapper.updateById(plan);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("success", true);
        return result;
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long id) {
        planMapper.deleteById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("success", true);
        return result;
    }
}