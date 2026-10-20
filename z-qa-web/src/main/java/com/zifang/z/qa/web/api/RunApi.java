package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import com.zifang.z.qa.admin.domain.mapper.QaRunDetailMapper;
import com.zifang.z.qa.admin.domain.mapper.QaRunMapper;
import com.zifang.z.qa.admin.engine.QaExecutionDispatcher;
import com.zifang.z.qa.web.response.QaResult;
import com.zifang.z.qa.web.response.QaRunDetailResp;
import com.zifang.z.qa.web.response.QaRunResp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 执行 API（lead 007 步骤 6）。
 */
@RestController("qaRunController")
@RequestMapping("/api/qa")
public class RunApi {

    private static final Logger log = LogManager.getLogger(RunApi.class);

    /** run 级终态。中止只是软信号，跑完了的 run 没有可中止的东西。 */
    private static final Set<String> TERMINAL_RUN_STATUS =
            new HashSet<>(Arrays.asList("success", "failed", "cancelled"));

    @Autowired
    private QaRunMapper runMapper;
    @Autowired
    private QaRunDetailMapper runDetailMapper;
    @Autowired
    private QaExecutionDispatcher dispatcher;

    @GetMapping("/run/list")
    public QaResult<List<QaRunResp>> list(@RequestParam(required = false) String status,
                                           @RequestParam(defaultValue = "50") Integer limit) {
        QueryWrapper<QaRunDO> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            qw.eq("status", status);
        }
        qw.orderByDesc("gmt_create").last("LIMIT " + limit);
        return QaResult.ok(QaConverter.toList(runMapper.selectList(qw), QaRunResp.class));
    }

    @PostMapping("/run/trigger")
    public QaResult<QaRunResp> trigger(@RequestParam Long suiteId,
                                       @RequestParam(required = false) String envCode,
                                       @RequestParam(required = false) String triggerUser,
                                       @RequestParam(required = false) String triggerType) {
        try {
            QaRunDO run = dispatcher.triggerSuite(suiteId, envCode, triggerUser, triggerType);
            return QaResult.ok("已触发", QaConverter.to(run, QaRunResp.class));
        } catch (IllegalArgumentException e) {
            return QaResult.notFound(e.getMessage());
        }
    }

    /**
     * 触发计划执行 —— 一个 run 串起计划下的所有套件。
     */
    @PostMapping("/run/runPlan")
    public QaResult<QaRunResp> runPlan(@RequestParam Long planId,
                                       @RequestParam(required = false) String envCode,
                                       @RequestParam(required = false) String triggerUser,
                                       @RequestParam(required = false) String triggerType) {
        try {
            QaRunDO run = dispatcher.triggerPlan(planId, envCode, triggerUser, triggerType);
            return QaResult.ok("已触发", QaConverter.to(run, QaRunResp.class));
        } catch (IllegalArgumentException e) {
            return QaResult.notFound(e.getMessage());
        }
    }

    @GetMapping("/run/status")
    public QaResult<Map<String, Object>> status(@RequestParam Long runId) {
        String status = dispatcher.getRunStatus(runId);
        Integer progress = dispatcher.getRunProgress(runId);
        QaRunDO run = runMapper.selectById(runId);
        Map<String, Object> content = new HashMap<>();
        content.put("runId", runId);
        content.put("status", status);
        content.put("progress", progress);
        content.put("totalSteps", run != null ? run.getTotalSteps() : 0);
        content.put("passedSteps", run != null ? run.getPassedSteps() : 0);
        content.put("failedSteps", run != null ? run.getFailedSteps() : 0);
        content.put("abortRequested", run != null && Integer.valueOf(1).equals(run.getAbortRequested()));
        return QaResult.ok(content);
    }

    @GetMapping("/run/details")
    public QaResult<List<QaRunDetailResp>> details(@RequestParam Long runId) {
        return QaResult.ok(QaConverter.toList(runDetailMapper.selectByRunId(runId), QaRunDetailResp.class));
    }

    @GetMapping("/run/get")
    public QaResult<QaRunResp> get(@RequestParam Long id) {
        QaRunDO run = runMapper.selectById(id);
        if (run == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(QaConverter.to(run, QaRunResp.class));
    }

    @PostMapping("/runs/{runCode}/abort")
    public QaResult<QaRunResp> abortByRunCode(@PathVariable String runCode) {
        QaRunDO run = runMapper.selectOne(new QueryWrapper<QaRunDO>().eq("run_code", runCode));
        if (run == null) {
            return QaResult.notFound("run not found");
        }
        return doAbort(run.getId());
    }

    @PostMapping("/run/abort")
    public QaResult<QaRunResp> abort(@RequestParam Long runId) {
        return doAbort(runId);
    }

    private QaResult<QaRunResp> doAbort(Long runId) {
        QaRunDO run = runMapper.selectById(runId);
        if (run == null) {
            return QaResult.notFound("run not found");
        }
        if (TERMINAL_RUN_STATUS.contains(run.getStatus())) {
            return QaResult.badRequest("run 已是终态 " + run.getStatus() + "，无需中止");
        }
        // 只置标志，不抢写终态:跑这条 run 的线程要在剩余步骤落完 skip 后才自己翻成 cancelled
        QaRunDO upd = new QaRunDO();
        upd.setId(runId);
        upd.setAbortRequested(1);
        runMapper.updateById(upd);
        log.info("run[{}] abort requested", runId);
        return QaResult.ok("abort requested; status will flip to cancelled", QaConverter.to(run, QaRunResp.class));
    }
}
