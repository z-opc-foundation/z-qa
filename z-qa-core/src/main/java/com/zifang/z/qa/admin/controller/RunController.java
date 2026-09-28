package com.zifang.z.qa.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行管理 Controller.
 *
 * <p>API (FEATURE052 §6.2):
 * <ul>
 *   <li>GET    /api/qa/run/list        — 执行记录列表</li>
 *   <li>GET    /api/qa/run/get        — 单个执行详情 (by id)</li>
 *   <li>POST   /api/qa/run/trigger    — 触发套件执行 (suiteId)</li>
 *   <li>POST   /api/qa/run/runPlan    — 触发计划执行 (planId)</li>
 *   <li>GET    /api/qa/run/status     — 轮询状态 (runId)</li>
 *   <li>GET    /api/qa/run/details    — 执行明细 (runId)</li>
 *   <li>POST   /api/qa/run/abort      — 中止执行 (runId)</li>
 *   <li>POST   /api/qa/runs/{runCode}/abort — 中止执行 (runCode, FEATURE052 §6.2)</li>
 * </ul>
 */
@RestController("qaRunController")
@RequestMapping("/api/qa")
public class RunController {

    private static final Logger log = LogManager.getLogger(RunController.class);

    /** run 级终态。中止只是软信号，跑完了的 run 没有可中止的东西。 */
    private static final Set<String> TERMINAL_RUN_STATUS =
            new HashSet<>(Arrays.asList("success", "failed", "cancelled"));

    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper runDetailMapper;
    @Autowired
    private com.zifang.z.qa.admin.engine.QaExecutionDispatcher dispatcher;

    @GetMapping("/run/list")
    public Map<String, Object> list(@RequestParam(required = false) String status,
                                    @RequestParam(defaultValue = "50") Integer limit) {
        QueryWrapper<QaRunDO> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) qw.eq("status", status);
        qw.orderByDesc("gmt_create").last("LIMIT " + limit);
        List<QaRunDO> list = runMapper.selectList(qw);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("content", list);
        result.put("success", true);
        return result;
    }

    @PostMapping("/run/trigger")
    public Map<String, Object> trigger(@RequestParam Long suiteId,
                                       @RequestParam(required = false) String envCode,
                                       @RequestParam(required = false) String triggerUser,
                                       @RequestParam(required = false) String triggerType) {
        Map<String, Object> result = new HashMap<>();
        try {
            QaRunDO run = dispatcher.triggerSuite(suiteId, envCode, triggerUser, triggerType);
            result.put("code", 200);
            result.put("msg", "已触发");
            result.put("content", run);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("code", 404);
            result.put("msg", e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    /**
     * 触发计划执行 (FEATURE052 §6.4 复用) —— 一个 run 串起计划下的所有套件。
     *
     * <p>改造前这里只 new 了一个不落库的 QaRunDO 返回给前端:没有 run 行、没有执行、
     * 前端拿到的 runCode 也查不到任何状态。
     */
    @PostMapping("/run/runPlan")
    public Map<String, Object> runPlan(@RequestParam Long planId,
                                       @RequestParam(required = false) String envCode,
                                       @RequestParam(required = false) String triggerUser,
                                       @RequestParam(required = false) String triggerType) {
        Map<String, Object> result = new HashMap<>();
        try {
            QaRunDO run = dispatcher.triggerPlan(planId, envCode, triggerUser, triggerType);
            result.put("code", 200);
            result.put("msg", "已触发");
            result.put("content", run);
            result.put("success", true);
        } catch (IllegalArgumentException e) {
            result.put("code", 404);
            result.put("msg", e.getMessage());
            result.put("success", false);
        }
        return result;
    }

    @GetMapping("/run/status")
    public Map<String, Object> status(@RequestParam Long runId) {
        String status = dispatcher.getRunStatus(runId);
        Integer progress = dispatcher.getRunProgress(runId);
        QaRunDO run = runMapper.selectById(runId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        Map<String, Object> content = new HashMap<>();
        content.put("runId", runId);
        content.put("status", status);
        content.put("progress", progress);
        content.put("totalSteps", run != null ? run.getTotalSteps() : 0);
        content.put("passedSteps", run != null ? run.getPassedSteps() : 0);
        content.put("failedSteps", run != null ? run.getFailedSteps() : 0);
        content.put("abortRequested", run != null && Integer.valueOf(1).equals(run.getAbortRequested()));
        result.put("content", content);
        result.put("success", true);
        return result;
    }

    @GetMapping("/run/details")
    public Map<String, Object> details(@RequestParam Long runId) {
        java.util.List<com.zifang.z.qa.admin.domain.entity.QaRunDetailDO> details =
                runDetailMapper.selectByRunId(runId);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("msg", "success");
        result.put("content", details);
        result.put("success", true);
        return result;
    }

    @GetMapping("/run/get")
    public Map<String, Object> get(@RequestParam Long id) {
        QaRunDO run = runMapper.selectById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", run != null ? 200 : 404);
        result.put("content", run);
        result.put("success", run != null);
        return result;
    }

    /**
     * 中止执行 (FEATURE052 §6.2).
     */
    @PostMapping("/runs/{runCode}/abort")
    public Map<String, Object> abortByRunCode(@PathVariable String runCode) {
        QaRunDO run = runMapper.selectOne(new QueryWrapper<QaRunDO>().eq("run_code", runCode));
        Map<String, Object> result = new HashMap<>();
        if (run == null) {
            result.put("code", 404);
            result.put("msg", "run not found");
            result.put("success", false);
            return result;
        }
        return doAbort(run.getId());
    }

    /**
     * 中止 by runId (与前端 runService API 对齐).
     */
    @PostMapping("/run/abort")
    public Map<String, Object> abort(@RequestParam Long runId) {
        return doAbort(runId);
    }

    private Map<String, Object> doAbort(Long runId) {
        Map<String, Object> result = new HashMap<>();
        QaRunDO run = runMapper.selectById(runId);
        if (run == null) {
            result.put("code", 404);
            result.put("msg", "run not found");
            result.put("success", false);
            return result;
        }
        if (TERMINAL_RUN_STATUS.contains(run.getStatus())) {
            result.put("code", 400);
            result.put("msg", "run 已是终态 " + run.getStatus() + "，无需中止");
            result.put("success", false);
            return result;
        }
        // 只置标志，不抢写终态:跑这条 run 的线程要在剩余步骤落完 skip 后才自己翻成 cancelled
        QaRunDO upd = new QaRunDO();
        upd.setId(runId);
        upd.setAbortRequested(1);
        runMapper.updateById(upd);
        log.info("run[{}] abort requested", runId);
        result.put("code", 200);
        result.put("msg", "abort requested; status will flip to cancelled");
        result.put("content", run);
        result.put("success", true);
        return result;
    }
}
