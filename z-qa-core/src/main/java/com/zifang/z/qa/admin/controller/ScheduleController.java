package com.zifang.z.qa.admin.controller;

import com.zifang.z.qa.admin.domain.entity.QaScheduleDO;
import com.zifang.z.qa.admin.service.QaScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试定时 Controller (FEATURE052 Phase 5 / §6.4).
 */
@RestController("qaScheduleController")
@RequestMapping("/api/qa/schedules")
public class ScheduleController {

    @Autowired
    private QaScheduleService scheduleService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        Map<String, Object> r = new HashMap<>();
        List<QaScheduleDO> all = scheduleService.list();
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", all);
        r.put("success", true);
        return r;
    }

    @GetMapping("/get")
    public Map<String, Object> get(@RequestParam Long id) {
        Map<String, Object> r = new HashMap<>();
        QaScheduleDO s = scheduleService.get(id);
        r.put("code", s != null ? 200 : 404);
        r.put("msg", s != null ? "success" : "not found");
        r.put("content", s);
        r.put("success", s != null);
        return r;
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody QaScheduleDO schedule) {
        Map<String, Object> r = new HashMap<>();
        Long id = scheduleService.create(schedule);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("content", id);
        r.put("success", true);
        return r;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody QaScheduleDO schedule) {
        Map<String, Object> r = new HashMap<>();
        scheduleService.update(schedule);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("success", true);
        return r;
    }

    @PostMapping("/remove")
    public Map<String, Object> remove(@RequestParam Long id) {
        Map<String, Object> r = new HashMap<>();
        scheduleService.delete(id);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("success", true);
        return r;
    }

    @PostMapping("/toggle")
    public Map<String, Object> toggle(@RequestParam Long id, @RequestParam Boolean enabled) {
        Map<String, Object> r = new HashMap<>();
        scheduleService.toggle(id, enabled);
        r.put("code", 200);
        r.put("msg", "success");
        r.put("success", true);
        return r;
    }

    @PostMapping("/scan")
    public Map<String, Object> scan() {
        Map<String, Object> r = new HashMap<>();
        int n = scheduleService.scanAndTrigger();
        r.put("code", 200);
        r.put("msg", "scanned");
        r.put("content", n);
        r.put("success", true);
        return r;
    }
}
