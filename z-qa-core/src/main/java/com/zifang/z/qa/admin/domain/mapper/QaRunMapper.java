package com.zifang.z.qa.admin.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.qa.admin.domain.entity.QaRunDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface QaRunMapper extends BaseMapper<QaRunDO> {
    List<Map<String, Object>> countByStatus(@Param("tenantCode") String tenantCode);

    /**
     * 全部 passed_steps 之和 (FEATURE052 §6.5 Dashboard summary).
     */
    Long sumPassed();

    /**
     * 全部 total_steps 之和 (FEATURE052 §6.5 Dashboard summary).
     */
    Long sumTotal();
}
