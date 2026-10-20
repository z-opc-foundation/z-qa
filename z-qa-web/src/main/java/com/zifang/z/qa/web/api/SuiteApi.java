package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaSuiteDO;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteMapper;
import com.zifang.z.qa.admin.domain.mapper.QaSuiteStepMapper;
import com.zifang.z.qa.web.request.QaSuiteReq;
import com.zifang.z.qa.web.request.QaSuiteStepReq;
import com.zifang.z.qa.web.response.QaResult;
import com.zifang.z.qa.web.response.QaSuiteResp;
import com.zifang.z.qa.web.response.QaSuiteStepResp;
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
 * 套件 API（lead 007 步骤 6）。URL 与 param 名保持不变以不破前端。
 */
@RestController("qaSuiteController")
@RequestMapping("/api/qa/suite")
public class SuiteApi {

    @Autowired
    private QaSuiteMapper suiteMapper;
    @Autowired
    private QaSuiteStepMapper stepMapper;

    @GetMapping("/list")
    public QaResult<List<QaSuiteResp>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category) {
        QueryWrapper<QaSuiteDO> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(w -> w.like("code", keyword).or().like("name", keyword));
        }
        if (category != null && !category.isEmpty()) {
            qw.eq("category", category);
        }
        qw.orderByDesc("gmt_create");
        return QaResult.ok(QaConverter.toList(suiteMapper.selectList(qw), QaSuiteResp.class));
    }

    @GetMapping("/get")
    public QaResult<QaSuiteResp> get(@RequestParam Long id) {
        QaSuiteDO suite = suiteMapper.selectById(id);
        if (suite == null) {
            return QaResult.notFound("not found");
        }
        return QaResult.ok(QaConverter.to(suite, QaSuiteResp.class));
    }

    @PostMapping("/add")
    public QaResult<QaSuiteResp> add(@RequestBody QaSuiteReq req) {
        QaSuiteDO suite = new QaSuiteDO();
        BeanUtils.copyProperties(req, suite);
        Date now = new Date();
        suite.setGmtCreate(now);
        suite.setGmtModified(now);
        if (suite.getStatus() == null) {
            suite.setStatus(1);
        }
        if (suite.getVersion() == null) {
            suite.setVersion(1);
        }
        suiteMapper.insert(suite);
        return QaResult.ok("新增成功", QaConverter.to(suite, QaSuiteResp.class));
    }

    @PostMapping("/update")
    public QaResult<Void> update(@RequestBody QaSuiteReq req) {
        QaSuiteDO suite = new QaSuiteDO();
        BeanUtils.copyProperties(req, suite);
        suite.setGmtModified(new Date());
        suiteMapper.updateById(suite);
        return QaResult.ok("更新成功", null);
    }

    @PostMapping("/remove")
    public QaResult<Void> remove(@RequestParam Long id) {
        suiteMapper.deleteById(id);
        stepMapper.delete(new QueryWrapper<QaSuiteStepDO>().eq("suite_id", id));
        return QaResult.ok("删除成功", null);
    }

    @GetMapping("/steps")
    public QaResult<List<QaSuiteStepResp>> steps(@RequestParam Long suiteId) {
        return QaResult.ok(QaConverter.toList(stepMapper.selectBySuiteId(suiteId), QaSuiteStepResp.class));
    }

    @PostMapping("/steps/save")
    public QaResult<Integer> saveSteps(@RequestParam Long suiteId, @RequestBody List<QaSuiteStepReq> steps) {
        stepMapper.delete(new QueryWrapper<QaSuiteStepDO>().eq("suite_id", suiteId));
        for (QaSuiteStepReq s : steps) {
            QaSuiteStepDO step = new QaSuiteStepDO();
            BeanUtils.copyProperties(s, step);
            step.setSuiteId(suiteId);
            step.setId(null);
            Date now = new Date();
            step.setGmtCreate(now);
            step.setGmtModified(now);
            stepMapper.insert(step);
        }
        return QaResult.ok("保存成功", steps.size());
    }
}
