package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteStepMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 套件管理 Controller.
 * API: /api/qa/suite/list, /api/qa/suite/get, /api/qa/suite/add, /api/qa/suite/update, /api/qa/suite/remove
 * /api/qa/suite/steps?suiteId=
 */
@RestController("qaSuiteController")
@RequestMapping("/api/qa/suite")
public class SuiteController {

    @Autowired
    private QaSuiteMapper suiteMapper;
    @Autowired
    private QaSuiteStepMapper stepMapper;

    @GetMapping("/list")
    public Map<String, Object> list(@RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String category) {
        QueryWrapper<QaSuiteDO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("code", keyword).or().like("name", keyword));
        }
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        qw.orderByDesc("gmt_create");
        List<QaSuiteDO> list = suiteMapper.selectList(qw);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("content", list);
        result.put("success", true);
        return result;
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam Long id) {
        QaSuiteDO suite = suiteMapper.selectById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", suite != null ? 200 : 404);
        result.put("msg", suite != null ? "success" : "not found");
        result.put("content", suite);
        result.put("success", suite != null);
        return result;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody QaSuiteDO suite) {
        suite.setGmtCreate(new Date());
        suite.setGmtModified(new Date());
        if (suite.getStatus() == null) suite.setStatus(1);
        if (suite.getVersion() == null) suite.setVersion(1);
        suiteMapper.insert(suite);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "新增成功");
        result.put("content", suite);
        result.put("success", true);
        return result;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QaSuiteDO suite) {
        suite.setGmtModified(new Date());
        suiteMapper.updateById(suite);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "更新成功");
        result.put("success", true);
        return result;
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long id) {
        suiteMapper.deleteById(id);
        // 删 steps
        stepMapper.delete(new QueryWrapper<QaSuiteStepDO>().eq("suite_id", id));
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "删除成功");
        result.put("success", true);
        return result;
    }

    @GetMapping("/steps")
    public Map<String, Object> steps(@RequestParam Long suiteId) {
        List<QaSuiteStepDO> steps = stepMapper.selectBySuiteId(suiteId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("content", steps);
        result.put("success", true);
        return result;
    }

    @PostMapping("/steps/save")
    public Map<String, Object> saveSteps(@RequestParam Long suiteId,
                                         @RequestBody List<QaSuiteStepDO> steps) {
        stepMapper.delete(new QueryWrapper<QaSuiteStepDO>().eq("suite_id", suiteId));
        for (QaSuiteStepDO s : steps) {
            s.setSuiteId(suiteId);
            s.setId(null);
            s.setGmtCreate(new Date());
            s.setGmtModified(new Date());
            stepMapper.insert(s);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "保存成功");
        result.put("content", steps.size());
        result.put("success", true);
        return result;
    }
}