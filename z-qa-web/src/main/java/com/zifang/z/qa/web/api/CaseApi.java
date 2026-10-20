package com.zifang.z.qa.web.api;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zifang.z.qa.admin.domain.entity.QaCaseDO;
import com.zifang.z.qa.admin.domain.mapper.QaCaseMapper;
import com.zifang.z.qa.web.request.QaCaseReq;
import com.zifang.z.qa.web.response.QaCaseResp;
import com.zifang.z.qa.web.response.QaResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

/**
 * 用例 API（lead 007 步骤 6）。list 走 POST 以承载长 keyword，URL 不变。
 */
@RestController("qaCaseController")
@RequestMapping("/api/qa/case")
public class CaseApi {

    @Autowired
    private QaCaseMapper caseMapper;

    @PostMapping("/list")
    public QaResult<List<QaCaseResp>> list(@RequestBody(required = false) QaCaseReq req) {
        QueryWrapper<QaCaseDO> qw = new QueryWrapper<>();
        if (req != null) {
            String keyword = req.getKeyword();
            if (keyword != null && !keyword.isEmpty()) {
                qw.and(w -> w.like("code", keyword).or().like("name", keyword));
            }
            String priority = req.getPriority();
            if (priority != null && !priority.isEmpty()) {
                qw.eq("priority", priority);
            }
            String category = req.getCategory();
            if (category != null && !category.isEmpty()) {
                qw.eq("category", category);
            }
        }
        qw.orderByDesc("gmt_create");
        return QaResult.ok(QaConverter.toList(caseMapper.selectList(qw), QaCaseResp.class));
    }

    @PostMapping("/add")
    public QaResult<QaCaseResp> add(@RequestBody QaCaseReq req) {
        QaCaseDO c = new QaCaseDO();
        BeanUtils.copyProperties(req, c);
        if (c.getPriority() == null) {
            c.setPriority("P1");
        }
        if (c.getStatus() == null) {
            c.setStatus(1);
        }
        Date now = new Date();
        c.setGmtCreate(now);
        c.setGmtModified(now);
        caseMapper.insert(c);
        return QaResult.ok(QaConverter.to(c, QaCaseResp.class));
    }

    @PostMapping("/update")
    public QaResult<QaCaseResp> update(@RequestBody QaCaseReq req) {
        QaCaseDO c = new QaCaseDO();
        BeanUtils.copyProperties(req, c);
        c.setGmtModified(new Date());
        caseMapper.updateById(c);
        return QaResult.ok(QaConverter.to(c, QaCaseResp.class));
    }

    @PostMapping("/remove")
    public QaResult<Void> remove(@RequestParam Long id) {
        caseMapper.deleteById(id);
        return QaResult.ok();
    }
}
