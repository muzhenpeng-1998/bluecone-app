package com.bluecone.app.core.user.infra.persistence.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;

/**
 * 用户表 Mapper。
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
}
