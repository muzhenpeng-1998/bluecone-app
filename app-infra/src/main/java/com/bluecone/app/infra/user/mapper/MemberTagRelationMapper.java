package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberTagRelationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_tag_relation 表对应的 Mapper。
 */
@Mapper
public interface MemberTagRelationMapper extends BaseMapper<MemberTagRelationDO> {
}
