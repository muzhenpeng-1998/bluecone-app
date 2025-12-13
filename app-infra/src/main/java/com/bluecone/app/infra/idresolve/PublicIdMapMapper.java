package com.bluecone.app.infra.idresolve;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公共 ID 映射表 Mapper。
 */
@Mapper
public interface PublicIdMapMapper extends BaseMapper<PublicIdMapDO> {
}

