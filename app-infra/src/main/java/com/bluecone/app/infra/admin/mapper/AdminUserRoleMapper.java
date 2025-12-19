package com.bluecone.app.infra.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.admin.entity.AdminUserRoleEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户-角色关联Mapper
 */
@Mapper
public interface AdminUserRoleMapper extends BaseMapper<AdminUserRoleEntity> {
    
    /**
     * 查询用户的所有角色ID
     */
    @Select("SELECT role_id FROM bc_admin_user_role " +
            "WHERE tenant_id = #{tenantId} AND user_id = #{userId} AND status = 'ACTIVE'")
    List<Long> selectRoleIdsByUser(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
