package com.bluecone.app.infra.admin.service;

import com.bluecone.app.infra.admin.mapper.AdminRolePermissionMapper;
import com.bluecone.app.infra.admin.mapper.AdminUserRoleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 后台权限校验服务
 * 
 * 提供基于RBAC的权限校验功能，支持：
 * - 用户权限查询（带缓存）
 * - 单个权限校验
 * - 批量权限校验
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthorizationService {
    
    private final AdminUserRoleMapper userRoleMapper;
    private final AdminRolePermissionMapper rolePermissionMapper;
    
    /**
     * 检查用户是否拥有指定权限
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param permissionCode 权限代码（如：product:edit）
     * @return true-有权限，false-无权限
     */
    public boolean hasPermission(Long tenantId, Long userId, String permissionCode) {
        Set<String> permissions = getUserPermissions(tenantId, userId);
        return permissions.contains(permissionCode);
    }
    
    /**
     * 检查用户是否拥有任一权限（OR关系）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param permissionCodes 权限代码列表
     * @return true-拥有任一权限，false-无权限
     */
    public boolean hasAnyPermission(Long tenantId, Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return false;
        }
        Set<String> userPermissions = getUserPermissions(tenantId, userId);
        for (String permissionCode : permissionCodes) {
            if (userPermissions.contains(permissionCode)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查用户是否拥有所有权限（AND关系）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param permissionCodes 权限代码列表
     * @return true-拥有所有权限，false-缺少权限
     */
    public boolean hasAllPermissions(Long tenantId, Long userId, String... permissionCodes) {
        if (permissionCodes == null || permissionCodes.length == 0) {
            return true;
        }
        Set<String> userPermissions = getUserPermissions(tenantId, userId);
        for (String permissionCode : permissionCodes) {
            if (!userPermissions.contains(permissionCode)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取用户的所有权限（带缓存）
     * 
     * 缓存策略：
     * - 缓存key: admin:permissions:{tenantId}:{userId}
     * - 过期时间: 5分钟（由Spring Cache配置）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 权限代码集合
     */
    @Cacheable(value = "admin:permissions", key = "#tenantId + ':' + #userId")
    public Set<String> getUserPermissions(Long tenantId, Long userId) {
        log.debug("查询用户权限: tenantId={}, userId={}", tenantId, userId);
        
        // 1. 查询用户的所有角色
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUser(tenantId, userId);
        if (CollectionUtils.isEmpty(roleIds)) {
            log.debug("用户没有分配角色: tenantId={}, userId={}", tenantId, userId);
            return Collections.emptySet();
        }
        
        // 2. 查询角色的所有权限
        List<String> permissionCodes = rolePermissionMapper.selectPermissionCodesByRoles(tenantId, roleIds);
        if (CollectionUtils.isEmpty(permissionCodes)) {
            log.debug("用户角色没有分配权限: tenantId={}, userId={}, roleIds={}", tenantId, userId, roleIds);
            return Collections.emptySet();
        }
        
        Set<String> permissions = permissionCodes.stream().collect(Collectors.toSet());
        log.debug("用户权限查询完成: tenantId={}, userId={}, permissions={}", tenantId, userId, permissions);
        return permissions;
    }
    
    /**
     * 清除用户权限缓存
     * 
     * 使用场景：
     * - 用户角色变更
     * - 角色权限变更
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     */
    public void clearUserPermissionsCache(Long tenantId, Long userId) {
        log.info("清除用户权限缓存: tenantId={}, userId={}", tenantId, userId);
        // 实际清除由CacheManager处理，这里只是记录日志
        // 可以通过 @CacheEvict 注解或 CacheManager.getCache().evict() 实现
    }
}
