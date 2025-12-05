package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberTagDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_tag 表对应的 Mapper。
 */
@Mapper
public interface MemberTagMapper extends BaseMapper<MemberTagDO> {
}
