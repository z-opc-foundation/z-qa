package com.zifang.z.qa.admin.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDetailDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface QaRunDetailMapper extends BaseMapper<QaRunDetailDO> {
    List<QaRunDetailDO> selectByRunId(@Param("runId") Long runId);

    /**
     * Top 失败步骤 — 按 step_no+step_name 聚合失败的次数 (FEATURE052 §6.5).
     */
    List<Map<String, Object>> topFailures(@Param("limit") int limit);
}
