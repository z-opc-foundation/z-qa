package com.zifang.z.qa.web.api;

import com.zifang.z.qa.admin.domain.entity.QaScheduleDO;
import com.zifang.z.qa.admin.service.QaScheduleService;
import com.zifang.z.qa.web.request.QaScheduleReq;
import com.zifang.z.qa.web.response.QaResult;
import com.zifang.z.qa.web.response.QaScheduleResp;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 定时任务 API（lead 007 步骤 6）。
 */
@RestController("qaScheduleController")
@RequestMapping("/api/qa/schedules")
public class ScheduleApi {

    @Autowired
    private QaScheduleService scheduleService;

    @GetMapping("/list")
    public QaResult<List<QaScheduleResp>> list() {
        return QaResult.ok(QaConverter.toList(scheduleService.list(), QaScheduleResp.class));
    }

    @GetMapping("/get")
    public QaResult<QaScheduleResp> get(@RequestParam Long id) {
        QaScheduleDO s = scheduleService.get(id);
        if (s == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(QaConverter.to(s, QaScheduleResp.class));
    }

    @PostMapping("/add")
    public QaResult<Long> add(@RequestBody QaScheduleReq req) {
        QaScheduleDO schedule = new QaScheduleDO();
        BeanUtils.copyProperties(req, schedule);
        return QaResult.ok(scheduleService.create(schedule));
    }

    @PostMapping("/update")
    public QaResult<Void> update(@RequestBody QaScheduleReq req) {
        QaScheduleDO schedule = new QaScheduleDO();
        BeanUtils.copyProperties(req, schedule);
        scheduleService.update(schedule);
        return QaResult.ok();
    }

    @PostMapping("/remove")
    public QaResult<Void> remove(@RequestParam Long id) {
        scheduleService.delete(id);
        return QaResult.ok();
    }

    @PostMapping("/toggle")
    public QaResult<Void> toggle(@RequestParam Long id, @RequestParam Boolean enabled) {
        scheduleService.toggle(id, enabled);
        return QaResult.ok();
    }

    @PostMapping("/scan")
    public QaResult<Integer> scan() {
        return QaResult.ok("scanned", scheduleService.scanAndTrigger());
    }
}
