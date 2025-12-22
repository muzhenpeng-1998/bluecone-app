package com.bluecone.app.api.admin.product;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.admin.service.AuditLogService;
import com.bluecone.app.security.admin.RequireAdminPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小料素材库管理后台接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>小料组的创建、修改、删除、查询</li>
 *   <li>小料项的创建、修改、删除、查询</li>
 *   <li>小料组和小料项的排序管理</li>
 *   <li>小料的定时展示配置</li>
 * </ul>
 * 
 * <h3>💡 设计说明：</h3>
 * <p>小料素材库是租户级别的可复用资源，商品可以通过绑定关系引用小料组，并在商品级别覆盖小料的规则和价格。</p>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>addon:view</b> - 查看小料</li>
 *   <li><b>addon:create</b> - 创建小料</li>
 *   <li><b>addon:edit</b> - 编辑小料</li>
 *   <li><b>addon:delete</b> - 删除小料</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * POST   /api/admin/addon-groups                       - 创建小料组
 * PUT    /api/admin/addon-groups/{groupId}             - 更新小料组
 * DELETE /api/admin/addon-groups/{groupId}             - 删除小料组
 * GET    /api/admin/addon-groups                       - 查询小料组列表
 * 
 * POST   /api/admin/addon-groups/{groupId}/items       - 创建小料项
 * PUT    /api/admin/addon-groups/{groupId}/items/{id}  - 更新小料项
 * DELETE /api/admin/addon-groups/{groupId}/items/{id}  - 删除小料项
 * GET    /api/admin/addon-groups/{groupId}/items       - 查询小料项列表
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Tag(name = "🎛️ 平台管理后台 > 商品管理 > 小料素材库管理", description = "平台管理后台 - 小料素材库管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/addon-groups")
@RequiredArgsConstructor
public class AddonAdminController {
    
    private final AuditLogService auditLogService;
    
    // TODO: 注入小料应用服务（待实现）
    // private final AddonApplicationService addonApplicationService;
    
    // ===== 小料组管理 =====
    
    /**
     * 创建小料组
     * 
     * <p>创建新的小料组，用于组织和管理小料项。
     * 
     * @param request 创建请求
     * @return 创建的小料组ID
     */
    @Operation(summary = "创建小料组", description = "创建新的小料组")
    @PostMapping
    @RequireAdminPermission("addon:create")
    public ApiResponse<CreateAddonGroupResponse> createAddonGroup(
            @Valid @RequestBody CreateAddonGroupRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("创建小料组: tenantId={}, request={}", tenantId, request);
        
        // TODO: 调用应用服务创建小料组
        // Long groupId = addonApplicationService.createAddonGroup(tenantId, request, operatorId);
        Long groupId = 1L; // 临时返回
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("ADDON_GROUP")
                .resourceId(groupId)
                .resourceName(request.getTitle())
                .operationDesc("创建小料组")
                .dataAfter(request));
        
        log.info("小料组创建成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok(new CreateAddonGroupResponse(groupId));
    }
    
    /**
     * 更新小料组
     * 
     * <p>更新小料组的基本信息。
     * 
     * @param groupId 小料组ID
     * @param request 更新请求
     * @return 成功响应
     */
    @Operation(summary = "更新小料组", description = "更新小料组信息")
    @PutMapping("/{groupId}")
    @RequireAdminPermission("addon:edit")
    public ApiResponse<Void> updateAddonGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateAddonGroupRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("更新小料组: tenantId={}, groupId={}, request={}", tenantId, groupId, request);
        
        // TODO: 调用应用服务更新小料组
        // addonApplicationService.updateAddonGroup(tenantId, groupId, request, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("ADDON_GROUP")
                .resourceId(groupId)
                .resourceName(request.getTitle())
                .operationDesc("更新小料组")
                .dataAfter(request));
        
        log.info("小料组更新成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok();
    }
    
    /**
     * 删除小料组
     * 
     * <p>删除小料组（软删除），同时会删除该组下的所有小料项。
     * 
     * @param groupId 小料组ID
     * @return 成功响应
     */
    @Operation(summary = "删除小料组", description = "删除小料组（软删除）")
    @DeleteMapping("/{groupId}")
    @RequireAdminPermission("addon:delete")
    public ApiResponse<Void> deleteAddonGroup(@PathVariable Long groupId) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("删除小料组: tenantId={}, groupId={}", tenantId, groupId);
        
        // TODO: 调用应用服务删除小料组
        // addonApplicationService.deleteAddonGroup(tenantId, groupId, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("DELETE")
                .resourceType("ADDON_GROUP")
                .resourceId(groupId)
                .operationDesc("删除小料组"));
        
        log.info("小料组删除成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok();
    }
    
    /**
     * 查询小料组列表
     * 
     * <p>查询小料组列表，支持按启用状态筛选。
     * 
     * @param includeDisabled 是否包含禁用的小料组（默认false）
     * @return 小料组列表
     */
    @Operation(summary = "查询小料组列表", description = "查询小料组列表")
    @GetMapping
    @RequireAdminPermission("addon:view")
    public ApiResponse<List<AddonGroupView>> listAddonGroups(
            @RequestParam(defaultValue = "false") Boolean includeDisabled) {
        Long tenantId = requireTenantId();
        
        log.info("查询小料组列表: tenantId={}, includeDisabled={}", tenantId, includeDisabled);
        
        // TODO: 调用应用服务查询小料组列表
        // List<AddonGroupView> groups = addonApplicationService.listAddonGroups(tenantId, includeDisabled);
        List<AddonGroupView> groups = List.of(); // 临时返回空列表
        
        log.info("查询小料组列表成功: tenantId={}, count={}", tenantId, groups.size());
        return ApiResponse.ok(groups);
    }
    
    // ===== 小料项管理 =====
    
    /**
     * 创建小料项
     * 
     * <p>在指定小料组下创建新的小料项。
     * 
     * @param groupId 小料组ID
     * @param request 创建请求
     * @return 创建的小料项ID
     */
    @Operation(summary = "创建小料项", description = "在指定小料组下创建新的小料项")
    @PostMapping("/{groupId}/items")
    @RequireAdminPermission("addon:create")
    public ApiResponse<CreateAddonItemResponse> createAddonItem(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateAddonItemRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("创建小料项: tenantId={}, groupId={}, request={}", tenantId, groupId, request);
        
        // TODO: 调用应用服务创建小料项
        // Long itemId = addonApplicationService.createAddonItem(tenantId, groupId, request, operatorId);
        Long itemId = 1L; // 临时返回
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("ADDON_ITEM")
                .resourceId(itemId)
                .resourceName(request.getTitle())
                .operationDesc("创建小料项")
                .dataAfter(request));
        
        log.info("小料项创建成功: tenantId={}, groupId={}, itemId={}", tenantId, groupId, itemId);
        return ApiResponse.ok(new CreateAddonItemResponse(itemId));
    }
    
    /**
     * 更新小料项
     * 
     * <p>更新小料项的基本信息、价格、排序等。
     * 
     * @param groupId 小料组ID
     * @param id 小料项ID
     * @param request 更新请求
     * @return 成功响应
     */
    @Operation(summary = "更新小料项", description = "更新小料项信息")
    @PutMapping("/{groupId}/items/{id}")
    @RequireAdminPermission("addon:edit")
    public ApiResponse<Void> updateAddonItem(
            @PathVariable Long groupId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddonItemRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("更新小料项: tenantId={}, groupId={}, itemId={}, request={}", 
                tenantId, groupId, id, request);
        
        // TODO: 调用应用服务更新小料项
        // addonApplicationService.updateAddonItem(tenantId, groupId, id, request, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("ADDON_ITEM")
                .resourceId(id)
                .resourceName(request.getTitle())
                .operationDesc("更新小料项")
                .dataAfter(request));
        
        log.info("小料项更新成功: tenantId={}, groupId={}, itemId={}", tenantId, groupId, id);
        return ApiResponse.ok();
    }
    
    /**
     * 删除小料项
     * 
     * <p>删除小料项（软删除）。
     * 
     * @param groupId 小料组ID
     * @param id 小料项ID
     * @return 成功响应
     */
    @Operation(summary = "删除小料项", description = "删除小料项（软删除）")
    @DeleteMapping("/{groupId}/items/{id}")
    @RequireAdminPermission("addon:delete")
    public ApiResponse<Void> deleteAddonItem(
            @PathVariable Long groupId,
            @PathVariable Long id) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("删除小料项: tenantId={}, groupId={}, itemId={}", tenantId, groupId, id);
        
        // TODO: 调用应用服务删除小料项
        // addonApplicationService.deleteAddonItem(tenantId, groupId, id, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("DELETE")
                .resourceType("ADDON_ITEM")
                .resourceId(id)
                .operationDesc("删除小料项"));
        
        log.info("小料项删除成功: tenantId={}, groupId={}, itemId={}", tenantId, groupId, id);
        return ApiResponse.ok();
    }
    
    /**
     * 查询小料项列表
     * 
     * <p>查询指定小料组下的小料项列表。
     * 
     * @param groupId 小料组ID
     * @param includeDisabled 是否包含禁用的小料项（默认false）
     * @return 小料项列表
     */
    @Operation(summary = "查询小料项列表", description = "查询指定小料组下的小料项列表")
    @GetMapping("/{groupId}/items")
    @RequireAdminPermission("addon:view")
    public ApiResponse<List<AddonItemView>> listAddonItems(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") Boolean includeDisabled) {
        Long tenantId = requireTenantId();
        
        log.info("查询小料项列表: tenantId={}, groupId={}, includeDisabled={}", 
                tenantId, groupId, includeDisabled);
        
        // TODO: 调用应用服务查询小料项列表
        // List<AddonItemView> items = addonApplicationService.listAddonItems(tenantId, groupId, includeDisabled);
        List<AddonItemView> items = List.of(); // 临时返回空列表
        
        log.info("查询小料项列表成功: tenantId={}, groupId={}, count={}", tenantId, groupId, items.size());
        return ApiResponse.ok(items);
    }
    
    /**
     * 获取当前租户ID
     */
    private Long requireTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new IllegalStateException("租户上下文未设置");
        }
        return Long.parseLong(tenantIdStr);
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
    
    // ===== DTO 类 =====
    
    /**
     * 创建小料组请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAddonGroupRequest {
        
        /**
         * 小料组名称
         */
        @NotBlank(message = "小料组名称不能为空")
        @Size(max = 64, message = "小料组名称不能超过64个字符")
        private String title;
        
        /**
         * 排序值（数值越大越靠前）
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 更新小料组请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAddonGroupRequest {
        
        /**
         * 小料组名称
         */
        @NotBlank(message = "小料组名称不能为空")
        @Size(max = 64, message = "小料组名称不能超过64个字符")
        private String title;
        
        /**
         * 排序值
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 创建小料项请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAddonItemRequest {
        
        /**
         * 小料项名称
         */
        @NotBlank(message = "小料项名称不能为空")
        @Size(max = 64, message = "小料项名称不能超过64个字符")
        private String title;
        
        /**
         * 价格增量（相对于基础价格的加价）
         */
        @NotNull(message = "价格增量不能为空")
        @DecimalMin(value = "0.00", message = "价格增量不能小于0")
        private BigDecimal priceDelta;
        
        /**
         * 排序值
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 更新小料项请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAddonItemRequest {
        
        /**
         * 小料项名称
         */
        @NotBlank(message = "小料项名称不能为空")
        @Size(max = 64, message = "小料项名称不能超过64个字符")
        private String title;
        
        /**
         * 价格增量
         */
        @NotNull(message = "价格增量不能为空")
        @DecimalMin(value = "0.00", message = "价格增量不能小于0")
        private BigDecimal priceDelta;
        
        /**
         * 排序值
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 小料组视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonGroupView {
        
        /**
         * 小料组ID
         */
        private Long id;
        
        /**
         * 小料组名称
         */
        private String title;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 创建时间
         */
        private LocalDateTime createdAt;
        
        /**
         * 更新时间
         */
        private LocalDateTime updatedAt;
    }
    
    /**
     * 小料项视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonItemView {
        
        /**
         * 小料项ID
         */
        private Long id;
        
        /**
         * 小料项名称
         */
        private String title;
        
        /**
         * 价格增量
         */
        private BigDecimal priceDelta;
        
        /**
         * 排序值
         */
        private Integer sortOrder;
        
        /**
         * 是否启用
         */
        private Boolean enabled;
        
        /**
         * 定时展示开始时间
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间
         */
        private LocalDateTime displayEndAt;
        
        /**
         * 创建时间
         */
        private LocalDateTime createdAt;
        
        /**
         * 更新时间
         */
        private LocalDateTime updatedAt;
    }
    
    /**
     * 创建小料组响应
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateAddonGroupResponse {
        
        /**
         * 创建的小料组ID
         */
        private Long groupId;
    }
    
    /**
     * 创建小料项响应
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateAddonItemResponse {
        
        /**
         * 创建的小料项ID
         */
        private Long itemId;
    }
}

