package com.zifang.z.qa.admin.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.qa.admin.domain.entity.QaSuiteStepDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QaSuiteStepMapper extends BaseMapper<QaSuiteStepDO> {
    List<QaSuiteStepDO> selectBySuiteId(@Param("suiteId") Long suiteId);
}