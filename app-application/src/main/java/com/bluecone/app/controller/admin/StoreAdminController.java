package com.bluecone.app.controller.admin;

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 门店管理后台接口
 * 
 * 提供门店信息的查询和管理功能：
 * - 查看门店详情
 * - 修改门店基本信息（名称、地址、联系方式等）
 * - 修改营业时间
 * 
 * 权限要求：
 * - 查看：store:view
 * - 编辑：store:edit
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
public class StoreAdminController {
    
    private final StoreFacade storeFacade;
    private final BcStoreMapper storeMapper;
    private final AuditLogService auditLogService;
    
    /**
     * 查询门店详情
     * 
     * @param tenantId 租户ID（从请求头注入）
     * @param id 门店ID
     * @return 门店详情
     */
    @GetMapping("/{id}")
    @RequireAdminPermission("store:view")
    public StoreBaseView getStore(@RequestHeader("X-Tenant-Id") Long tenantId,
                                  @PathVariable Long id) {
        log.info("查询门店详情: tenantId={}, storeId={}", tenantId, id);
        
        // 租户隔离校验
        BcStore store = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (store == null) {
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        return storeFacade.getStoreBase(tenantId, id);
    }
    
    /**
     * 更新门店基本信息
     * 
     * @param tenantId 租户ID（从请求头注入）
     * @param id 门店ID
     * @param request 更新请求
     */
    @PutMapping("/{id}")
    @RequireAdminPermission("store:edit")
    public StoreBaseView updateStore(@RequestHeader("X-Tenant-Id") Long tenantId,
                                     @PathVariable Long id,
                                     @Valid @RequestBody UpdateStoreRequest request) {
        log.info("更新门店信息: tenantId={}, storeId={}, request={}", tenantId, id, request);
        
        // 租户隔离校验
        BcStore storeBefore = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (storeBefore == null) {
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        // 构建更新命令
        UpdateStoreBaseCommand command = UpdateStoreBaseCommand.builder()
                .tenantId(tenantId)
                .storeId(id)
                .name(request.getName())
                .shortName(request.getShortName())
                .cityCode(request.getCityCode())
                .expectedConfigVersion(storeBefore.getConfigVersion())
                .build();
        
        // 执行更新
        storeFacade.updateStoreBase(command);
        
        // 查询更新后的数据
        BcStore storeAfter = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        // 记录审计日志
        Long operatorId = getCurrentUserId();
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("STORE")
                .resourceId(id)
                .resourceName(storeAfter.getName())
                .operationDesc("修改门店基本信息")
                .dataBefore(storeBefore)
                .dataAfter(storeAfter));
        
        return storeFacade.getStoreBase(tenantId, id);
    }
    
    /**
     * 更新门店营业时间
     * 
     * @param tenantId 租户ID（从请求头注入）
     * @param id 门店ID
     * @param command 营业时间更新命令
     */
    @PutMapping("/{id}/opening-hours")
    @RequireAdminPermission("store:edit")
    public void updateOpeningHours(@RequestHeader("X-Tenant-Id") Long tenantId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateStoreOpeningHoursCommand command) {
        log.info("更新门店营业时间: tenantId={}, storeId={}", tenantId, id);
        
        // 租户隔离校验
        BcStore store = storeMapper.selectOne(new LambdaQueryWrapper<BcStore>()
                .eq(BcStore::getTenantId, tenantId)
                .eq(BcStore::getId, id)
                .eq(BcStore::getIsDeleted, false));
        
        if (store == null) {
            throw new IllegalArgumentException("门店不存在或无权访问");
        }
        
        // 设置租户ID和门店ID
        command.setTenantId(tenantId);
        command.setStoreId(id);
        
        // 执行更新
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
        
        log.info("门店营业时间更新成功: tenantId={}, storeId={}", tenantId, id);
    }
    
    /**
     * 获取当前操作人ID
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
            log.error("获取当前用户ID失败", e);
        }
        return null;
    }
    
    /**
     * 门店更新请求DTO
     */
    @lombok.Data
    public static class UpdateStoreRequest {
        private String name;
        private String shortName;
        private String address;
        private String provinceCode;
        private String cityCode;
        private String districtCode;
        private java.math.BigDecimal longitude;
        private java.math.BigDecimal latitude;
        private String contactPhone;
        private String logoUrl;
        private String coverUrl;
    }
}
