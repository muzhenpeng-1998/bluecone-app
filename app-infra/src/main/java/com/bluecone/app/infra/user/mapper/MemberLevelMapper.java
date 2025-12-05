package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberLevelDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_level 表对应的 Mapper。
 */
@Mapper
public interface MemberLevelMapper extends BaseMapper<MemberLevelDO> {
}
