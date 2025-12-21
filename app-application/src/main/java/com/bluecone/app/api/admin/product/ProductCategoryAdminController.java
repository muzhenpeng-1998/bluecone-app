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

import java.time.LocalDateTime;
import java.util.List;

/**
 * 商品分类管理后台接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商品分类的创建、修改、查询</li>
 *   <li>分类的显示/隐藏状态管理</li>
 *   <li>分类的排序管理（批量调整排序）</li>
 *   <li>分类的定时展示配置</li>
 * </ul>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>product-category:view</b> - 查看分类</li>
 *   <li><b>product-category:create</b> - 创建分类</li>
 *   <li><b>product-category:edit</b> - 编辑分类</li>
 *   <li><b>product-category:status</b> - 修改分类状态</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * POST   /api/admin/product-categories              - 创建分类
 * PUT    /api/admin/product-categories/{id}         - 更新分类
 * GET    /api/admin/product-categories              - 查询分类列表
 * PATCH  /api/admin/product-categories/{id}/status  - 修改分类状态（显示/隐藏）
 * POST   /api/admin/product-categories/reorder      - 批量调整分类排序
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Tag(name = "Admin/Product - 商品分类管理", description = "平台管理后台 - 商品分类管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/product-categories")
@RequiredArgsConstructor
public class ProductCategoryAdminController {
    
    private final AuditLogService auditLogService;
    
    // TODO: 注入分类应用服务（待实现）
    // private final ProductCategoryApplicationService categoryApplicationService;
    
    /**
     * 创建商品分类
     * 
     * <p>创建新的商品分类，支持设置图标、排序、启用状态、定时展示等配置。
     * 
     * @param request 创建请求
     * @return 创建的分类ID
     */
    @Operation(summary = "创建商品分类", description = "创建新的商品分类")
    @PostMapping
    @RequireAdminPermission("product-category:create")
    public ApiResponse<CreateCategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("创建商品分类: tenantId={}, request={}", tenantId, request);
        
        // TODO: 调用应用服务创建分类
        // Long categoryId = categoryApplicationService.createCategory(tenantId, request, operatorId);
        Long categoryId = 1L; // 临时返回
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("PRODUCT_CATEGORY")
                .resourceId(categoryId)
                .resourceName(request.getTitle())
                .operationDesc("创建商品分类")
                .dataAfter(request));
        
        log.info("商品分类创建成功: tenantId={}, categoryId={}", tenantId, categoryId);
        return ApiResponse.ok(new CreateCategoryResponse(categoryId));
    }
    
    /**
     * 更新商品分类
     * 
     * <p>更新商品分类的基本信息、图标、排序、启用状态、定时展示等配置。
     * 
     * @param id 分类ID
     * @param request 更新请求
     * @return 成功响应
     */
    @Operation(summary = "更新商品分类", description = "更新商品分类信息")
    @PutMapping("/{id}")
    @RequireAdminPermission("product-category:edit")
    public ApiResponse<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("更新商品分类: tenantId={}, categoryId={}, request={}", tenantId, id, request);
        
        // TODO: 调用应用服务更新分类
        // categoryApplicationService.updateCategory(tenantId, id, request, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("PRODUCT_CATEGORY")
                .resourceId(id)
                .resourceName(request.getTitle())
                .operationDesc("更新商品分类")
                .dataAfter(request));
        
        log.info("商品分类更新成功: tenantId={}, categoryId={}", tenantId, id);
        return ApiResponse.ok();
    }
    
    /**
     * 查询商品分类列表
     * 
     * <p>查询商品分类列表，支持按启用状态筛选、按定时展示时间过滤。
     * 
     * @param includeDisabled 是否包含禁用的分类（默认false，仅返回启用的）
     * @param filterByTime 是否按当前时间过滤定时展示（默认false，返回全部）
     * @return 分类列表
     */
    @Operation(summary = "查询商品分类列表", description = "查询商品分类列表，支持筛选和过滤")
    @GetMapping
    @RequireAdminPermission("product-category:view")
    public ApiResponse<List<CategoryView>> listCategories(
            @RequestParam(defaultValue = "false") Boolean includeDisabled,
            @RequestParam(defaultValue = "false") Boolean filterByTime) {
        Long tenantId = requireTenantId();
        
        log.info("查询商品分类列表: tenantId={}, includeDisabled={}, filterByTime={}", 
                tenantId, includeDisabled, filterByTime);
        
        // TODO: 调用应用服务查询分类列表
        // List<CategoryView> categories = categoryApplicationService.listCategories(
        //         tenantId, includeDisabled, filterByTime, LocalDateTime.now());
        List<CategoryView> categories = List.of(); // 临时返回空列表
        
        log.info("查询商品分类列表成功: tenantId={}, count={}", tenantId, categories.size());
        return ApiResponse.ok(categories);
    }
    
    /**
     * 修改分类状态（显示/隐藏）
     * 
     * <p>修改商品分类的启用状态，用于控制分类在C端的显示/隐藏。
     * 
     * @param id 分类ID
     * @param request 状态修改请求
     * @return 成功响应
     */
    @Operation(summary = "修改分类状态", description = "修改商品分类的显示/隐藏状态")
    @PatchMapping("/{id}/status")
    @RequireAdminPermission("product-category:status")
    public ApiResponse<Void> changeCategoryStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeCategoryStatusRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("修改分类状态: tenantId={}, categoryId={}, enabled={}", 
                tenantId, id, request.getEnabled());
        
        // TODO: 调用应用服务修改状态
        // categoryApplicationService.changeCategoryStatus(tenantId, id, request.getEnabled(), operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CHANGE_STATUS")
                .resourceType("PRODUCT_CATEGORY")
                .resourceId(id)
                .operationDesc(request.getEnabled() ? "显示分类" : "隐藏分类")
                .dataAfter(request));
        
        log.info("分类状态修改成功: tenantId={}, categoryId={}, enabled={}", 
                tenantId, id, request.getEnabled());
        return ApiResponse.ok();
    }
    
    /**
     * 批量调整分类排序
     * 
     * <p>批量调整商品分类的排序值，用于调整分类在C端的展示顺序。
     * 
     * @param request 批量排序请求
     * @return 成功响应
     */
    @Operation(summary = "批量调整分类排序", description = "批量调整商品分类的排序值")
    @PostMapping("/reorder")
    @RequireAdminPermission("product-category:edit")
    public ApiResponse<Void> reorderCategories(
            @Valid @RequestBody ReorderCategoriesRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("批量调整分类排序: tenantId={}, count={}", tenantId, request.getItems().size());
        
        // TODO: 调用应用服务批量调整排序
        // categoryApplicationService.reorderCategories(tenantId, request.getItems(), operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("REORDER")
                .resourceType("PRODUCT_CATEGORY")
                .operationDesc("批量调整分类排序")
                .dataAfter(request));
        
        log.info("分类排序调整成功: tenantId={}, count={}", tenantId, request.getItems().size());
        return ApiResponse.ok();
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
     * 创建分类请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateCategoryRequest {
        
        /**
         * 分类名称
         */
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过64个字符")
        private String title;
        
        /**
         * 父分类ID（0表示顶级分类）
         */
        @NotNull(message = "父分类ID不能为空")
        @Min(value = 0, message = "父分类ID不能小于0")
        private Long parentId;
        
        /**
         * 分类图标URL
         */
        @Size(max = 512, message = "图标URL不能超过512个字符")
        private String imageUrl;
        
        /**
         * 排序值（数值越大越靠前）
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用（true=显示，false=隐藏）
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间（null表示立即生效）
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间（null表示永久有效）
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 更新分类请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateCategoryRequest {
        
        /**
         * 分类名称
         */
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过64个字符")
        private String title;
        
        /**
         * 分类图标URL
         */
        @Size(max = 512, message = "图标URL不能超过512个字符")
        private String imageUrl;
        
        /**
         * 排序值（数值越大越靠前）
         */
        @NotNull(message = "排序值不能为空")
        @Min(value = 0, message = "排序值不能小于0")
        private Integer sortOrder;
        
        /**
         * 是否启用（true=显示，false=隐藏）
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
        
        /**
         * 定时展示开始时间（null表示立即生效）
         */
        private LocalDateTime displayStartAt;
        
        /**
         * 定时展示结束时间（null表示永久有效）
         */
        private LocalDateTime displayEndAt;
    }
    
    /**
     * 修改分类状态请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChangeCategoryStatusRequest {
        
        /**
         * 是否启用（true=显示，false=隐藏）
         */
        @NotNull(message = "启用状态不能为空")
        private Boolean enabled;
    }
    
    /**
     * 批量调整排序请求
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReorderCategoriesRequest {
        
        /**
         * 排序项列表
         */
        @NotEmpty(message = "排序项列表不能为空")
        @Size(min = 1, message = "至少需要一个排序项")
        private List<ReorderItem> items;
        
        /**
         * 排序项
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ReorderItem {
            
            /**
             * 分类ID
             */
            @NotNull(message = "分类ID不能为空")
            private Long categoryId;
            
            /**
             * 新的排序值
             */
            @NotNull(message = "排序值不能为空")
            @Min(value = 0, message = "排序值不能小于0")
            private Integer sortOrder;
        }
    }
    
    /**
     * 分类视图
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryView {
        
        /**
         * 分类ID
         */
        private Long id;
        
        /**
         * 父分类ID（0表示顶级分类）
         */
        private Long parentId;
        
        /**
         * 分类名称
         */
        private String title;
        
        /**
         * 分类图标URL
         */
        private String imageUrl;
        
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
     * 创建分类响应
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CreateCategoryResponse {
        
        /**
         * 创建的分类ID
         */
        private Long categoryId;
    }
}

