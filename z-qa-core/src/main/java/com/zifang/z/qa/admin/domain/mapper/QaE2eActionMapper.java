package com.zifang.z.qa.admin.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zifang.z.qa.admin.domain.entity.QaE2eActionDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface QaE2eActionMapper extends BaseMapper<QaE2eActionDO> {
    List<QaE2eActionDO> selectBySessionId(@Param("sessionId") Long sessionId);
}
