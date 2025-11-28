package com.bluecone.app.infra.configcenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.configcenter.entity.ConfigPropertyEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Mapper for configuration properties.
 */
@Mapper
public interface ConfigPropertyMapper extends BaseMapper<ConfigPropertyEntity> {
}
