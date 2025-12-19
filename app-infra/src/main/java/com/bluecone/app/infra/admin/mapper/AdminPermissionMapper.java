package com.bluecone.app.infra.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.admin.entity.AdminPermissionEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 后台权限Mapper
 */
@Mapper
public interface AdminPermissionMapper extends BaseMapper<AdminPermissionEntity> {
}
