package com.bluecone.app.api.admin.product;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.infra.admin.service.AuditLogService;
import com.bluecone.app.product.application.command.CreateProductAggregateCommand;
import com.bluecone.app.product.application.command.UpdateProductAggregateCommand;
import com.bluecone.app.product.application.dto.ProductDetailDTO;
import com.bluecone.app.product.application.service.ProductAggregateAdminApplicationService;
import com.bluecone.app.security.admin.RequireAdminPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 商品聚合管理后台接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商品聚合的创建（Product + SKU + Spec + Attr + Addon + Category）</li>
 *   <li>商品聚合的更新（子表全量覆盖 delete+insert 策略）</li>
 *   <li>商品详情查询（完整聚合结构回显）</li>
 *   <li>商品状态修改（草稿/启用/禁用）</li>
 * </ul>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>product:create</b> - 创建商品</li>
 *   <li><b>product:edit</b> - 编辑商品</li>
 *   <li><b>product:view</b> - 查看商品</li>
 *   <li><b>product:status</b> - 修改商品状态</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * POST   /api/admin/products/aggregate              - 创建商品聚合
 * PUT    /api/admin/products/aggregate/{productId}  - 更新商品聚合
 * GET    /api/admin/products/aggregate/{productId}  - 查询商品详情
 * PATCH  /api/admin/products/aggregate/{productId}/status - 修改商品状态
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Tag(name = "🎛️ 平台管理后台 > 商品管理 > 商品聚合管理", description = "平台管理后台 - 商品聚合管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/products/aggregate")
@RequiredArgsConstructor
public class ProductAggregateAdminController {
    
    private final AuditLogService auditLogService;
    private final ProductAggregateAdminApplicationService productAggregateAdminApplicationService;
    
    /**
     * 创建商品聚合
     * 
     * <p>创建完整的商品聚合，包括商品基本信息、SKU、规格、属性、小料、分类绑定。
     * 
     * @param command 创建命令
     * @return 创建的商品ID
     */
    @Operation(summary = "创建商品聚合", description = "创建完整的商品聚合")
    @PostMapping
    @RequireAdminPermission("product:create")
    public ApiResponse<CreateProductAggregateResponse> createProductAggregate(
            @Valid @RequestBody CreateProductAggregateCommand command) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        // 设置 tenantId 和 operatorId
        command.setTenantId(tenantId);
        command.setOperatorId(operatorId);
        
        log.info("创建商品聚合: tenantId={}, name={}, publishNow={}", 
                tenantId, command.getName(), command.getPublishNow());
        
        Long productId = productAggregateAdminApplicationService.create(command);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("PRODUCT_AGGREGATE")
                .resourceId(productId)
                .resourceName(command.getName())
                .operationDesc("创建商品聚合")
                .dataAfter(command));
        
        log.info("商品聚合创建成功: tenantId={}, productId={}", tenantId, productId);
        return ApiResponse.ok(new CreateProductAggregateResponse(productId));
    }
    
    /**
     * 更新商品聚合
     * 
     * <p>更新商品聚合，采用子表全量覆盖策略（delete+insert）。
     * 
     * @param productId 商品ID
     * @param command 更新命令
     * @return 成功响应
     */
    @Operation(summary = "更新商品聚合", description = "更新商品聚合（子表全量覆盖）")
    @PutMapping("/{productId}")
    @RequireAdminPermission("product:edit")
    public ApiResponse<Void> updateProductAggregate(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductAggregateCommand command) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        // 设置 tenantId 和 operatorId
        command.setTenantId(tenantId);
        command.setOperatorId(operatorId);
        
        log.info("更新商品聚合: tenantId={}, productId={}, name={}", 
                tenantId, productId, command.getName());
        
        productAggregateAdminApplicationService.update(productId, command);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("PRODUCT_AGGREGATE")
                .resourceId(productId)
                .resourceName(command.getName())
                .operationDesc("更新商品聚合")
                .dataAfter(command));
        
        log.info("商品聚合更新成功: tenantId={}, productId={}", tenantId, productId);
        return ApiResponse.ok();
    }
    
    /**
     * 查询商品详情
     * 
     * <p>查询完整的商品聚合结构，用于回显编辑。
     * 
     * @param productId 商品ID
     * @return 商品详情
     */
    @Operation(summary = "查询商品详情", description = "查询完整的商品聚合结构")
    @GetMapping("/{productId}")
    @RequireAdminPermission("product:view")
    public ApiResponse<ProductDetailDTO> getProductDetail(@PathVariable Long productId) {
        Long tenantId = requireTenantId();
        
        log.info("查询商品详情: tenantId={}, productId={}", tenantId, productId);
        
        ProductDetailDTO detail = productAggregateAdminApplicationService.getDetail(tenantId, productId);
        
        log.info("查询商品详情成功: tenantId={}, productId={}", tenantId, productId);
        return ApiResponse.ok(detail);
    }
    
    /**
     * 修改商品状态
     * 
     * <p>修改商品状态（草稿/启用/禁用）。
     * 
     * @param productId 商品ID
     * @param request 状态修改请求
     * @return 成功响应
     */
    @Operation(summary = "修改商品状态", description = "修改商品状态（0=草稿，1=启用，-1=禁用）")
    @PatchMapping("/{productId}/status")
    @RequireAdminPermission("product:status")
    public ApiResponse<Void> changeProductStatus(
            @PathVariable Long productId,
            @Valid @RequestBody ChangeProductStatusRequest request) {
        Long tenantId = requireTenantId();
        Long operatorId = getCurrentUserId();
        
        log.info("修改商品状态: tenantId={}, productId={}, status={}", 
                tenantId, productId, request.getStatus());
        
        productAggregateAdminApplicationService.changeStatus(tenantId, productId, request.getStatus(), operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CHANGE_STATUS")
                .resourceType("PRODUCT_AGGREGATE")
                .resourceId(productId)
                .operationDesc("修改商品状态: " + request.getStatus()));
        
        log.info("商品状态修改成功: tenantId={}, productId={}, status={}", 
                tenantId, productId, request.getStatus());
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
     * 创建商品聚合响应
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CreateProductAggregateResponse {
        /**
         * 创建的商品ID
         */
        private Long productId;
    }
    
    /**
     * 修改商品状态请求
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChangeProductStatusRequest {
        /**
         * 新状态（0=草稿，1=启用，-1=禁用）
         */
        @jakarta.validation.constraints.NotNull(message = "状态不能为空")
        private Integer status;
    }
}

