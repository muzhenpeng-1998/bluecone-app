package com.bluecone.app.infra.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.admin.entity.AdminRolePermissionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色-权限关联Mapper
 */
@Mapper
public interface AdminRolePermissionMapper extends BaseMapper<AdminRolePermissionEntity> {
    
    /**
     * 查询角色的所有权限代码
     */
    @Select("SELECT p.permission_code FROM bc_admin_role_permission rp " +
            "INNER JOIN bc_admin_permission p ON rp.permission_id = p.id " +
            "WHERE rp.tenant_id = #{tenantId} AND rp.role_id = #{roleId} AND rp.status = 'ACTIVE' AND p.status = 'ACTIVE'")
    List<String> selectPermissionCodesByRole(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);
    
    /**
     * 批量查询多个角色的所有权限代码
     */
    @Select("<script>" +
            "SELECT DISTINCT p.permission_code FROM bc_admin_role_permission rp " +
            "INNER JOIN bc_admin_permission p ON rp.permission_id = p.id " +
            "WHERE rp.tenant_id = #{tenantId} AND rp.role_id IN " +
            "<foreach item='roleId' collection='roleIds' open='(' separator=',' close=')'>" +
            "#{roleId}" +
            "</foreach>" +
            " AND rp.status = 'ACTIVE' AND p.status = 'ACTIVE'" +
            "</script>")
    List<String> selectPermissionCodesByRoles(@Param("tenantId") Long tenantId, @Param("roleIds") List<Long> roleIds);
}
