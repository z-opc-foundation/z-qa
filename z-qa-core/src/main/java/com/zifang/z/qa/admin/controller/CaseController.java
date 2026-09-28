package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaCaseDO;
import com.zifang.z.qa.admin.domain.mapper.QaCaseMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用例 Controller (FEATURE052 §7.1 用例管理).
 *
 * <p>API:
 * <ul>
 *   <li>POST /api/qa/case/list  — 列表 (POST 兼容长 keyword)</li>
 *   <li>POST /api/qa/case/add   — 新增</li>
 *   <li>POST /api/qa/case/update — 更新</li>
 *   <li>POST /api/qa/case/remove — 删除</li>
 * </ul>
 */
@RestController("qaCaseController")
@RequestMapping("/api/qa/case")
public class CaseController {

    @Autowired
    private QaCaseMapper caseMapper;

    private static Map<String, Object> wrap(Object content) {
        Map<String, Object> r = new HashMap<>();
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", content);
        r.put("success", true);
        return r;
    }

    @PostMapping("/list")
    public Map<String, Object> list(@RequestBody(required = false) Map<String, Object> body) {
        QueryWrapper<QaCaseDO> qw = new QueryWrapper<>();
        if (body != null) {
            String keyword = (String) body.get("keyword");
            if (keyword != null && !keyword.isEmpty()) {
                qw.and(w -> w.like("code", keyword).or().like("name", keyword));
            }
            String priority = (String) body.get("priority");
            if (priority != null && !priority.isEmpty()) qw.eq("priority", priority);
            String category = (String) body.get("category");
            if (category != null && !category.isEmpty()) qw.eq("category", category);
        }
        qw.orderByDesc("gmt_create");
        List<QaCaseDO> list = caseMapper.selectList(qw);
        return wrap(list);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody QaCaseDO c) {
        if (c.getPriority() == null) c.setPriority("P1");
        if (c.getStatus() == null) c.setStatus(1);
        c.setGmtCreate(new Date());
        c.setGmtModified(new Date());
        caseMapper.insert(c);
        return wrap(c);
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QaCaseDO c) {
        c.setGmtModified(new Date());
        caseMapper.updateById(c);
        return wrap(c);
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long id) {
        caseMapper.deleteById(id);
        return wrap(null);
    }
}
