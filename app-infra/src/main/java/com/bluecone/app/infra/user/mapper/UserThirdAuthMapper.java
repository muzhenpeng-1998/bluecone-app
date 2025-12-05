package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.UserThirdAuthDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_user_third_auth 表对应的 Mapper。
 */
@Mapper
public interface UserThirdAuthMapper extends BaseMapper<UserThirdAuthDO> {
}
