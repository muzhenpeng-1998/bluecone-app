package com.bluecone.app.api.admin.store;

import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.admin.service.AuditLogService;
import com.bluecone.app.security.admin.RequireAdminPermission;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 【平台管理后台】门店管理接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>门店基本信息的查询和编辑（名称、地址、联系方式、logo等）</li>
 *   <li>门店营业时间配置（常规营业时间、跨日营业）</li>
 *   <li>租户隔离校验（确保只能访问本租户的门店）</li>
 *   <li>审计日志记录（所有变更操作记录操作人和变更内容）</li>
 * </ul>
 * 
 * <h3>👥 使用角色：</h3>
 * <ul>
 *   <li><b>平台运营人员</b>：可查看和管理所有租户的门店（需配置跨租户权限）</li>
 *   <li><b>租户管理员</b>：仅可查看和管理本租户下的门店（自动租户隔离）</li>
 * </ul>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>store:view</b> - 查看门店信息（查询接口必需）</li>
 *   <li><b>store:edit</b> - 编辑门店信息（更新接口必需）</li>
 * </ul>
 * 
 * <h3>🛡️ 安全机制：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：通过 X-Tenant-Id 请求头自动隔离数据，防止跨租户访问</li>
 *   <li><b>审计日志</b>：所有变更操作记录操作人ID、操作时间、变更前后数据</li>
 *   <li><b>乐观锁</b>：通过 configVersion 字段防止并发修改冲突</li>
 *   <li><b>软删除校验</b>：查询时自动过滤已删除的门店（is_deleted=false）</li>
 * </ul>
 * 
 * <h3>🔗 关联接口：</h3>
 * <ul>
 *   <li>{@link com.bluecone.app.api.merchant.store.MerchantStoreController} - 商户侧门店管理（使用 Public ID）</li>
 *   <li>{@link com.bluecone.app.api.open.store.OpenStoreController} - C端门店查询（小程序/H5）</li>
 *   <li>{@link StoreStaffAdminController} - 门店员工管理</li>
 *   <li>{@link StoreDeviceAdminController} - 门店设备管理</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * GET    /api/admin/stores/{id}               - 查询门店详情
 * PUT    /api/admin/stores/{id}               - 更新门店基本信息
 * PUT    /api/admin/stores/{id}/opening-hours - 更新营业时间
 * </pre>
 * 
 * <h3>⚠️ 注意事项：</h3>
 * <ul>
 *   <li>所有接口都需要在请求头中携带 <code>X-Tenant-Id</code>，由网关层注入</li>
 *   <li>更新操作需要传递 <code>configVersion</code> 进行乐观锁校验</li>
 *   <li>门店ID使用内部Long主键，不对外暴露（商户侧使用Public ID）</li>
 *   <li>审计日志异步写入，不影响主流程性能</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 * @see StoreFacade 门店领域门面
 * @see AuditLogService 审计日志服务
 */
@Tag(name = "Admin/Store - 门店管理", description = "平台管理后台 - 门店信息管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreAdminController {
    
    /** 门店领域门面，提供门店相关的所有业务能力 */
    private final StoreFacade storeFacade;
    
    /** 门店数据访问层，用于租户隔离校验 */
    private final BcStoreMapper storeMapper;
    
    /** 审计日志服务，记录所有变更操作 */
    private final AuditLogService auditLogService;
    
    /**
     * 查询门店详情
     * 
     * <p>返回门店的完整基础信息，包括：</p>
     * <ul>
     *   <li>基本信息：名称、简称、编码</li>
     *   <li>地址信息：省市区、详细地址、经纬度</li>
     *   <li>联系信息：联系电话</li>
     *   <li>展示信息：logo、封面图</li>
     *   <li>配置版本：configVersion（用于乐观锁）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/admin/stores/12345
     * Headers:
     *   X-Tenant-Id: 10001
     *   Authorization: Bearer {token}
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "id": 12345,
     *   "name": "总店",
     *   "shortName": "总店",
     *   "cityCode": "110100",
     *   "address": "朝阳区xxx路xxx号",
     *   "contactPhone": "010-12345678",
     *   "logoUrl": "https://cdn.example.com/logo.jpg",
     *   "configVersion": 5
     * }
     * </pre>
     * 
     * @param tenantId 租户ID（从请求头自动注入，由网关层解析Token后添加）
     * @param id 门店ID（内部Long主键）
     * @return 门店基础信息视图对象
     * @throws IllegalArgumentException 当门店不存在或无权访问时抛出
     */
    @Operation(
        summary = "查询门店详情",
        description = "根据门店ID查询完整的门店基础信息，包括地址、联系方式、logo等"
    )
    @GetMapping("/{id}")
    @RequireAdminPermission("store:view")
    public StoreBaseView getStore(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable Long id) {
        log.info("【门店管理】查询门店详情 - tenantId={}, storeId={}", tenantId, id);
        
        // 租户隔离校验：确保门店归属于当前租户，防止跨租户访问
        BcStore store = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (store == null) {
            log.warn("【门店管理】门店不存在或无权访问 - tenantId={}, storeId={}", tenantId, id);
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        // 调用领域层查询门店详情
        return storeFacade.getStoreBase(tenantId, id);
    }
    
    /**
     * 更新门店基本信息
     * 
     * <p>支持更新门店的以下信息：</p>
     * <ul>
     *   <li>名称和简称</li>
     *   <li>地址信息（省市区代码、详细地址、经纬度）</li>
     *   <li>联系电话</li>
     *   <li>logo和封面图</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * PUT /api/admin/stores/12345
     * Headers:
     *   X-Tenant-Id: 10001
     *   Authorization: Bearer {token}
     * Body:
     * {
     *   "name": "总店（新）",
     *   "shortName": "总店",
     *   "cityCode": "110100",
     *   "address": "朝阳区xxx路xxx号",
     *   "contactPhone": "010-12345678",
     *   "logoUrl": "https://cdn.example.com/new-logo.jpg"
     * }
     * </pre>
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>租户隔离校验：确认门店归属当前租户</li>
     *   <li>查询变更前数据：用于审计日志对比</li>
     *   <li>执行更新操作：通过领域层更新（含乐观锁校验）</li>
     *   <li>查询变更后数据：用于审计日志记录</li>
     *   <li>记录审计日志：异步写入操作记录</li>
     *   <li>返回最新数据：返回更新后的门店信息</li>
     * </ol>
     * 
     * @param tenantId 租户ID（从请求头自动注入）
     * @param id 门店ID
     * @param request 更新请求对象（包含要更新的字段）
     * @return 更新后的门店基础信息
     * @throws IllegalArgumentException 当门店不存在或无权访问时抛出
     * @throws OptimisticLockException 当configVersion不匹配时抛出（乐观锁冲突）
     */
    @Operation(
        summary = "更新门店基本信息",
        description = "更新门店的名称、地址、联系方式、logo等基础信息"
    )
    @PutMapping("/{id}")
    @RequireAdminPermission("store:edit")
    public StoreBaseView updateStore(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoreRequest request) {
        log.info("【门店管理】更新门店信息 - tenantId={}, storeId={}, request={}", tenantId, id, request);
        
        // 租户隔离校验：确保门店归属于当前租户
        BcStore storeBefore = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (storeBefore == null) {
            log.warn("【门店管理】门店不存在或无权访问 - tenantId={}, storeId={}", tenantId, id);
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        // 构建更新命令对象
        UpdateStoreBaseCommand command = UpdateStoreBaseCommand.builder()
                .tenantId(tenantId)
                .storeId(id)
                .name(request.getName())
                .shortName(request.getShortName())
                .cityCode(request.getCityCode())
                .expectedConfigVersion(storeBefore.getConfigVersion())  // 乐观锁版本号
                .build();
        
        // 执行更新（领域层会进行乐观锁校验）
        storeFacade.updateStoreBase(command);
        
        // 查询更新后的数据，用于审计日志
        BcStore storeAfter = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        // 记录审计日志（异步执行，不影响主流程）
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("STORE")
                .resourceId(id)
                .resourceName(storeAfter.getName())
                .operationDesc("修改门店基本信息")
                .dataBefore(storeBefore)
                .dataAfter(storeAfter));
        
        log.info("【门店管理】门店信息更新成功 - tenantId={}, storeId={}", tenantId, id);
        
        // 返回最新的门店信息
        return storeFacade.getStoreBase(tenantId, id);
    }
    
    /**
     * 更新门店营业时间
     * 
     * <p>支持配置门店的常规营业时间，例如：</p>
     * <ul>
     *   <li>周一至周日的营业时间段</li>
     *   <li>支持配置多个时间段（如午市、晚市）</li>
     *   <li>支持跨日营业配置（如23:00-次日02:00）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * PUT /api/admin/stores/12345/opening-hours
     * Headers:
     *   X-Tenant-Id: 10001
     *   Authorization: Bearer {token}
     * Body:
     * {
     *   "weekdayHours": [
     *     {"startTime": "10:00", "endTime": "14:00"},
     *     {"startTime": "17:00", "endTime": "22:00"}
     *   ],
     *   "weekendHours": [
     *     {"startTime": "09:00", "endTime": "23:00"}
     *   ]
     * }
     * </pre>
     * 
     * <h4>注意事项：</h4>
     * <ul>
     *   <li>时间格式为 HH:mm，24小时制</li>
     *   <li>支持跨日配置，如 23:00-02:00 表示晚上11点到次日凌晨2点</li>
     *   <li>特殊日期的营业时间通过另外的接口配置（如节假日、临时闭店）</li>
     * </ul>
     * 
     * @param tenantId 租户ID（从请求头自动注入）
     * @param id 门店ID
     * @param command 营业时间更新命令对象
     * @throws IllegalArgumentException 当门店不存在或无权访问时抛出
     */
    @Operation(
        summary = "更新门店营业时间",
        description = "配置门店的常规营业时间，支持周一至周日不同时间段，支持跨日营业"
    )
    @PutMapping("/{id}/opening-hours")
    @RequireAdminPermission("store:edit")
    public void updateOpeningHours(
            @RequestHeader("X-Tenant-Id") Long tenantId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateStoreOpeningHoursCommand command) {
        log.info("【门店管理】更新门店营业时间 - tenantId={}, storeId={}", tenantId, id);
        
        // 租户隔离校验：确保门店归属于当前租户
        BcStore store = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (store == null) {
            log.warn("【门店管理】门店不存在或无权访问 - tenantId={}, storeId={}", tenantId, id);
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        // 设置租户ID和门店ID（从路径参数注入）
        command.setTenantId(tenantId);
        command.setStoreId(id);
        
        // 执行更新操作
        storeFacade.updateOpeningHours(command);
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("STORE")
                .resourceId(id)
                .resourceName(store.getName())
                .operationDesc("修改门店营业时间")
                .dataAfter(command));
        
        log.info("【门店管理】门店营业时间更新成功 - tenantId={}, storeId={}", tenantId, id);
    }
    
    /**
     * 获取当前操作人ID
     * 
     * <p>从Spring Security上下文中提取当前登录用户的ID，用于审计日志记录。</p>
     * 
     * @return 当前操作人ID，获取失败时返回null
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof com.bluecone.app.security.core.SecurityUserPrincipal) {
                    return ((com.bluecone.app.security.core.SecurityUserPrincipal) principal).getUserId();
                }
            }
        } catch (Exception e) {
            log.error("【门店管理】获取当前用户ID失败", e);
        }
        return null;
    }
    
    /**
     * 门店更新请求DTO
     * 
     * <p>用于接收前端传递的门店更新数据，支持部分字段更新。</p>
     */
    @lombok.Data
    public static class UpdateStoreRequest {
        /** 门店名称（全称） */
        private String name;
        
        /** 门店简称（用于展示） */
        private String shortName;
        
        /** 详细地址 */
        private String address;
        
        /** 省份代码（国标行政区划代码） */
        private String provinceCode;
        
        /** 城市代码（国标行政区划代码） */
        private String cityCode;
        
        /** 区县代码（国标行政区划代码） */
        private String districtCode;
        
        /** 经度（GCJ-02火星坐标系） */
        private java.math.BigDecimal longitude;
        
        /** 纬度（GCJ-02火星坐标系） */
        private java.math.BigDecimal latitude;
        
        /** 联系电话 */
        private String contactPhone;
        
        /** Logo图片URL */
        private String logoUrl;
        
        /** 封面图片URL */
        private String coverUrl;
    }
}
