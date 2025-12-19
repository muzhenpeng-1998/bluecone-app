package com.bluecone.app.growth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.RewardIssueLogPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 奖励发放日志Mapper
 */
@Mapper
public interface RewardIssueLogMapper extends BaseMapper<RewardIssueLogPO> {
}
