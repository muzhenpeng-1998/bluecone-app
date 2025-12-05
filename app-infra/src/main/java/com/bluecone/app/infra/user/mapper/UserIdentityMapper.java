package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.UserIdentityDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_user_identity 表对应的 Mapper。
 */
@Mapper
public interface UserIdentityMapper extends BaseMapper<UserIdentityDO> {
}
