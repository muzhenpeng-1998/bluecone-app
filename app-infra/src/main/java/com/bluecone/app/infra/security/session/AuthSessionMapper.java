package com.bluecone.app.infra.security.session;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 会话表 Mapper。
 */
@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSessionEntity> {
}
