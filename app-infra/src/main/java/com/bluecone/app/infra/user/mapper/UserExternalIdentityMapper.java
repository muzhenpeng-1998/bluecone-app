package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.UserExternalIdentityDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户外部身份绑定表 Mapper。
 */
@Mapper
public interface UserExternalIdentityMapper extends BaseMapper<UserExternalIdentityDO> {
}

