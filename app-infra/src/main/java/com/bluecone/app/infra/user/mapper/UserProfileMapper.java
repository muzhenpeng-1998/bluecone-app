package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.UserProfileDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_user_profile 表对应的 Mapper。
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfileDO> {
}
