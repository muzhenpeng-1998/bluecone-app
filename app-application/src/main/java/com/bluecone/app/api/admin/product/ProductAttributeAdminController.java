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
 * 商品属性素材库管理后台接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>属性组的创建、修改、删除、查询</li>
 *   <li>属性选项的创建、修改、删除、查询</li>
 *   <li>属性组和属性选项的排序管理</li>
 *   <li>属性的定时展示配置</li>
 * </ul>
 * 
 * <h3>💡 设计说明：</h3>
 * <p>属性素材库是租户级别的可复用资源，商品可以通过绑定关系引用属性组，并在商品级别覆盖属性的规则和价格。</p>
 * <p>属性通常用于表示商品的口味、做法、温度等可选配置，与规格（Spec）不同，属性不影响SKU的生成。</p>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>attr:view</b> - 查看属性</li>
 *   <li><b>attr:create</b> - 创建属性</li>
 *   <li><b>attr:edit</b> - 编辑属性</li>
 *   <li><b>attr:delete</b> - 删除属性</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * POST   /api/admin/attr-groups                       - 创建属性组
 * PUT    /api/admin/attr-groups/{groupId}             - 更新属性组
 * DELETE /api/admin/attr-groups/{groupId}             - 删除属性组
 * GET    /api/admin/attr-groups                       - 查询属性组列表
 * 
 * POST   /api/admin/attr-groups/{groupId}/options     - 创建属性选项
 * PUT    /api/admin/attr-groups/{groupId}/options/{id} - 更新属性选项
 * DELETE /api/admin/attr-groups/{groupId}/options/{id} - 删除属性选项
 * GET    /api/admin/attr-groups/{groupId}/options     - 查询属性选项列表
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Tag(name = "🎛️ 平台管理后台 > 商品管理 > 属性素材库管理", description = "平台管理后台 - 商品属性素材库管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/attr-groups")
@RequiredArgsConstructor
public class ProductAttributeAdminController {
    
    private final AuditLogService auditLogService;
    private final com.bluecone.app.product.application.service.ProductAttributeAdminApplicationService attributeAdminApplicationService;
    
    // ===== 属性组管理 =====
    
    /**
     * 创建属性组
     * 
     * <p>创建新的属性组，用于组织和管理属性选项。
     * 
     * @param request 创建请求
     * @return 创建的属性组ID
     */
    @Operation(summary = "创建属性组", description = "创建新的属性组")
    @PostMapping
    @RequireAdminPermission("attr:create")
    public ApiResponse<CreateAttrGroupResponse> createAttrGroup(
            @Valid @RequestBody CreateAttrGroupRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("创建属性组: tenantId={}, request={}", tenantId, request);
        
        // 转换为命令并调用应用服务
        com.bluecone.app.product.application.dto.attr.CreateAttrGroupCommand command = 
                com.bluecone.app.product.application.dto.attr.CreateAttrGroupCommand.builder()
                .title(request.getTitle())
                .sortOrder(request.getSortOrder())
                .enabled(request.getEnabled())
                .displayStartAt(request.getDisplayStartAt())
                .displayEndAt(request.getDisplayEndAt())
                .build();
        
        Long groupId = attributeAdminApplicationService.createAttrGroup(tenantId, command, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("ATTR_GROUP")
                .resourceId(groupId)
                .resourceName(request.getTitle())
                .operationDesc("创建属性组")
                .dataAfter(request));
        
        log.info("属性组创建成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok(new CreateAttrGroupResponse(groupId));
    }
    
    /**
     * 更新属性组
     * 
     * <p>更新属性组的基本信息。
     * 
     * @param groupId 属性组ID
     * @param request 更新请求
     * @return 成功响应
     */
    @Operation(summary = "更新属性组", description = "更新属性组信息")
    @PutMapping("/{groupId}")
    @RequireAdminPermission("attr:edit")
    public ApiResponse<Void> updateAttrGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody UpdateAttrGroupRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("更新属性组: tenantId={}, groupId={}, request={}", tenantId, groupId, request);
        
        // 转换为命令并调用应用服务
        com.bluecone.app.product.application.dto.attr.UpdateAttrGroupCommand command = 
                com.bluecone.app.product.application.dto.attr.UpdateAttrGroupCommand.builder()
                .title(request.getTitle())
                .sortOrder(request.getSortOrder())
                .enabled(request.getEnabled())
                .displayStartAt(request.getDisplayStartAt())
                .displayEndAt(request.getDisplayEndAt())
                .build();
        
        attributeAdminApplicationService.updateAttrGroup(tenantId, groupId, command, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("ATTR_GROUP")
                .resourceId(groupId)
                .resourceName(request.getTitle())
                .operationDesc("更新属性组")
                .dataAfter(request));
        
        log.info("属性组更新成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok();
    }
    
    /**
     * 删除属性组
     * 
     * <p>删除属性组（软删除），同时会删除该组下的所有属性选项。
     * 
     * @param groupId 属性组ID
     * @return 成功响应
     */
    @Operation(summary = "删除属性组", description = "删除属性组（软删除）")
    @DeleteMapping("/{groupId}")
    @RequireAdminPermission("attr:delete")
    public ApiResponse<Void> deleteAttrGroup(@PathVariable Long groupId) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("删除属性组: tenantId={}, groupId={}", tenantId, groupId);
        
        // 调用应用服务修改状态为禁用（软删除）
        attributeAdminApplicationService.changeAttrGroupStatus(tenantId, groupId, false, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("DELETE")
                .resourceType("ATTR_GROUP")
                .resourceId(groupId)
                .operationDesc("删除属性组"));
        
        log.info("属性组删除成功: tenantId={}, groupId={}", tenantId, groupId);
        return ApiResponse.ok();
    }
    
    /**
     * 查询属性组列表
     * 
     * <p>查询属性组列表，支持按启用状态筛选。
     * 
     * @param includeDisabled 是否包含禁用的属性组（默认false）
     * @return 属性组列表
     */
    @Operation(summary = "查询属性组列表", description = "查询属性组列表")
    @GetMapping
    @RequireAdminPermission("attr:view")
    public ApiResponse<List<AttrGroupView>> listAttrGroups(
            @RequestParam(defaultValue = "false") Boolean includeDisabled) {
        Long tenantId = requireTenantId();
        
        log.info("查询属性组列表: tenantId={}, includeDisabled={}", tenantId, includeDisabled);
        
        // 调用应用服务查询属性组列表
        List<com.bluecone.app.product.application.dto.attr.AttrGroupAdminView> serviceViews = 
                attributeAdminApplicationService.listAttrGroups(tenantId, includeDisabled, false, java.time.LocalDateTime.now());
        
        // 转换为 Controller 的 DTO
        List<AttrGroupView> groups = serviceViews.stream()
                .map(v -> AttrGroupView.builder()
                        .id(v.getId())
                        .title(v.getTitle())
                        .sortOrder(v.getSortOrder())
                        .enabled(v.getEnabled())
                        .displayStartAt(v.getDisplayStartAt())
                        .displayEndAt(v.getDisplayEndAt())
                        .createdAt(v.getCreatedAt())
                        .updatedAt(v.getUpdatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        log.info("查询属性组列表成功: tenantId={}, count={}", tenantId, groups.size());
        return ApiResponse.ok(groups);
    }
    
    // ===== 属性选项管理 =====
    
    /**
     * 创建属性选项
     * 
     * <p>在指定属性组下创建新的属性选项。
     * 
     * @param groupId 属性组ID
     * @param request 创建请求
     * @return 创建的属性选项ID
     */
    @Operation(summary = "创建属性选项", description = "在指定属性组下创建新的属性选项")
    @PostMapping("/{groupId}/options")
    @RequireAdminPermission("attr:create")
    public ApiResponse<CreateAttrOptionResponse> createAttrOption(
            @PathVariable Long groupId,
            @Valid @RequestBody CreateAttrOptionRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("创建属性选项: tenantId={}, groupId={}, request={}", tenantId, groupId, request);
        
        // 转换为命令并调用应用服务
        com.bluecone.app.product.application.dto.attr.CreateAttrOptionCommand command = 
                com.bluecone.app.product.application.dto.attr.CreateAttrOptionCommand.builder()
                .title(request.getTitle())
                .priceDelta(request.getPriceDelta())
                .sortOrder(request.getSortOrder())
                .enabled(request.getEnabled())
                .displayStartAt(request.getDisplayStartAt())
                .displayEndAt(request.getDisplayEndAt())
                .build();
        
        Long optionId = attributeAdminApplicationService.createAttrOption(tenantId, groupId, command, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("ATTR_OPTION")
                .resourceId(optionId)
                .resourceName(request.getTitle())
                .operationDesc("创建属性选项")
                .dataAfter(request));
        
        log.info("属性选项创建成功: tenantId={}, groupId={}, optionId={}", tenantId, groupId, optionId);
        return ApiResponse.ok(new CreateAttrOptionResponse(optionId));
    }
    
    /**
     * 更新属性选项
     * 
     * <p>更新属性选项的基本信息、价格、排序等。
     * 
     * @param groupId 属性组ID
     * @param id 属性选项ID
     * @param request 更新请求
     * @return 成功响应
     */
    @Operation(summary = "更新属性选项", description = "更新属性选项信息")
    @PutMapping("/{groupId}/options/{id}")
    @RequireAdminPermission("attr:edit")
    public ApiResponse<Void> updateAttrOption(
            @PathVariable Long groupId,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAttrOptionRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("更新属性选项: tenantId={}, groupId={}, optionId={}, request={}", 
                tenantId, groupId, id, request);
        
        // 转换为命令并调用应用服务
        com.bluecone.app.product.application.dto.attr.UpdateAttrOptionCommand command = 
                com.bluecone.app.product.application.dto.attr.UpdateAttrOptionCommand.builder()
                .title(request.getTitle())
                .priceDelta(request.getPriceDelta())
                .sortOrder(request.getSortOrder())
                .enabled(request.getEnabled())
                .displayStartAt(request.getDisplayStartAt())
                .displayEndAt(request.getDisplayEndAt())
                .build();
        
        attributeAdminApplicationService.updateAttrOption(tenantId, groupId, id, command, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("ATTR_OPTION")
                .resourceId(id)
                .resourceName(request.getTitle())
                .operationDesc("更新属性选项")
                .dataAfter(request));
        
        log.info("属性选项更新成功: tenantId={}, groupId={}, optionId={}", tenantId, groupId, id);
        return ApiResponse.ok();
    }
    
    /**
     * 删除属性选项
     * 
     * <p>删除属性选项（软删除）。
     * 
     * @param groupId 属性组ID
     * @param id 属性选项ID
     * @return 成功响应
     */
    @Operation(summary = "删除属性选项", description = "删除属性选项（软删除）")
    @DeleteMapping("/{groupId}/options/{id}")
    @RequireAdminPermission("attr:delete")
    public ApiResponse<Void> deleteAttrOption(
            @PathVariable Long groupId,
            @PathVariable Long id) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("删除属性选项: tenantId={}, groupId={}, optionId={}", tenantId, groupId, id);
        
        // 调用应用服务修改状态为禁用（软删除）
        attributeAdminApplicationService.changeAttrOptionStatus(tenantId, groupId, id, false, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("DELETE")
                .resourceType("ATTR_OPTION")
                .resourceId(id)
                .operationDesc("删除属性选项"));
        
        log.info("属性选项删除成功: tenantId={}, groupId={}, optionId={}", tenantId, groupId, id);
        return ApiResponse.ok();
    }
    
    /**
     * 查询属性选项列表
     * 
     * <p>查询指定属性组下的属性选项列表。
     * 
     * @param groupId 属性组ID
     * @param includeDisabled 是否包含禁用的属性选项（默认false）
     * @return 属性选项列表
     */
    @Operation(summary = "查询属性选项列表", description = "查询指定属性组下的属性选项列表")
    @GetMapping("/{groupId}/options")
    @RequireAdminPermission("attr:view")
    public ApiResponse<List<AttrOptionView>> listAttrOptions(
            @PathVariable Long groupId,
            @RequestParam(defaultValue = "false") Boolean includeDisabled) {
        Long tenantId = requireTenantId();
        
        log.info("查询属性选项列表: tenantId={}, groupId={}, includeDisabled={}", 
                tenantId, groupId, includeDisabled);
        
        // 调用应用服务查询属性选项列表
        List<com.bluecone.app.product.application.dto.attr.AttrOptionAdminView> serviceViews = 
                attributeAdminApplicationService.listAttrOptions(tenantId, groupId, includeDisabled, false, java.time.LocalDateTime.now());
        
        // 转换为 Controller 的 DTO
        List<AttrOptionView> options = serviceViews.stream()
                .map(v -> AttrOptionView.builder()
                        .id(v.getId())
                        .title(v.getTitle())
                        .priceDelta(v.getPriceDelta())
                        .sortOrder(v.getSortOrder())
                        .enabled(v.getEnabled())
                        .displayStartAt(v.getDisplayStartAt())
                        .displayEndAt(v.getDisplayEndAt())
                        .createdAt(v.getCreatedAt())
                        .updatedAt(v.getUpdatedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
        
        log.info("查询属性选项列表成功: tenantId={}, groupId={}, count={}", tenantId, groupId, options.size());
        return ApiResponse.ok(options);
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
     * 创建属性组请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAttrGroupRequest {
        
        /**
         * 属性组名称
         */
        @NotBlank(message = "属性组名称不能为空")
        @Size(max = 64, message = "属性组名称不能超过64个字符")
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
     * 更新属性组请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAttrGroupRequest {
        
        /**
         * 属性组名称
         */
        @NotBlank(message = "属性组名称不能为空")
        @Size(max = 64, message = "属性组名称不能超过64个字符")
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
     * 创建属性选项请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateAttrOptionRequest {
        
        /**
         * 属性选项名称
         */
        @NotBlank(message = "属性选项名称不能为空")
        @Size(max = 64, message = "属性选项名称不能超过64个字符")
        private String title;
        
        /**
         * 价格增量（可为0，表示不加价）
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
     * 更新属性选项请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAttrOptionRequest {
        
        /**
         * 属性选项名称
         */
        @NotBlank(message = "属性选项名称不能为空")
        @Size(max = 64, message = "属性选项名称不能超过64个字符")
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
     * 属性组视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrGroupView {
        
        /**
         * 属性组ID
         */
        private Long id;
        
        /**
         * 属性组名称
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
     * 属性选项视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttrOptionView {
        
        /**
         * 属性选项ID
         */
        private Long id;
        
        /**
         * 属性选项名称
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
     * 创建属性组响应
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateAttrGroupResponse {
        
        /**
         * 创建的属性组ID
         */
        private Long groupId;
    }
    
    /**
     * 创建属性选项响应
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateAttrOptionResponse {
        
        /**
         * 创建的属性选项ID
         */
        private Long optionId;
    }
}

