package com.bluecone.app.growth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.GrowthCampaignPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 增长活动Mapper
 */
@Mapper
public interface GrowthCampaignMapper extends BaseMapper<GrowthCampaignPO> {
}
