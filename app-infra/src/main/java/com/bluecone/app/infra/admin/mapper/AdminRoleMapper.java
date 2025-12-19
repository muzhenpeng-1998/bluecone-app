package com.bluecone.app.infra.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.admin.entity.AdminRoleEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台角色Mapper
 */
@Mapper
public interface AdminRoleMapper extends BaseMapper<AdminRoleEntity> {
}
