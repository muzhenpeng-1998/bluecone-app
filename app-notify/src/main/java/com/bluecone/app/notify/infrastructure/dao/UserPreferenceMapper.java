package com.bluecone.app.notify.infrastructure.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户通知偏好 Mapper
 */
@Mapper
public interface UserPreferenceMapper extends BaseMapper<UserPreferenceDO> {
}
