package com.bluecone.app.api.admin.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.infra.admin.service.AuditLogService;
import com.bluecone.app.product.dao.entity.BcProduct;
import com.bluecone.app.product.dao.entity.BcProductSku;
import com.bluecone.app.product.dao.mapper.BcProductMapper;
import com.bluecone.app.product.dao.mapper.BcProductSkuMapper;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.enums.ProductType;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.repository.ProductRepository;
import com.bluecone.app.security.admin.RequireAdminPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品管理后台接口（简化版）
 * 
 * <p><b>⚠️ 注意：</b>本 Controller 仅提供简单的商品 CRUD 功能，不支持完整的商品聚合管理（规格、属性、小料等）。
 * <p><b>推荐使用：</b>{@link ProductAggregateAdminController} 进行完整的商品聚合管理。
 * 
 * 提供商品的CRUD和状态管理功能：
 * - 分页查询商品列表
 * - 查看商品详情
 * - 创建商品（简化版，推荐使用 ProductAggregateAdminController）
 * - 修改商品信息
 * - 商品上线/下线
 * 
 * 权限要求：
 * - 查看：product:view
 * - 创建：product:create
 * - 编辑：product:edit
 * - 上下线：product:online
 */
@Tag(name = "🎛️ 平台管理后台 > 商品管理 > 商品基础管理（简化版）", description = "平台后台商品管理接口（简化版）")
@Slf4j
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {
    
    private final ProductRepository productRepository;
    private final BcProductMapper productMapper;
    private final BcProductSkuMapper skuMapper;
    private final IdService idService;
    private final AuditLogService auditLogService;
    
    /**
     * 分页查询商品列表
     * 
     * @param tenantId 租户ID
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @param name 商品名称（模糊搜索）
     * @param status 商品状态（0=草稿，1=启用，-1=禁用）
     * @return 商品分页列表
     */
    @GetMapping
    @RequireAdminPermission("product:view")
    public Page<ProductListView> listProducts(@RequestHeader("X-Tenant-Id") Long tenantId,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size,
                                              @RequestParam(required = false) String name,
                                              @RequestParam(required = false) Integer status) {
        log.info("查询商品列表: tenantId={}, page={}, size={}, name={}, status={}", 
                tenantId, page, size, name, status);
        
        LambdaQueryWrapper<BcProduct> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcProduct::getTenantId, tenantId);
        
        if (StringUtils.hasText(name)) {
            wrapper.like(BcProduct::getName, name);
        }
        if (status != null) {
            wrapper.eq(BcProduct::getStatus, status);
        }
        
        wrapper.orderByDesc(BcProduct::getCreatedAt);
        
        Page<BcProduct> productPage = productMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为视图对象
        Page<ProductListView> viewPage = new Page<>(page, size, productPage.getTotal());
        List<ProductListView> views = productPage.getRecords().stream()
                .map(this::toListView)
                .collect(Collectors.toList());
        viewPage.setRecords(views);
        
        return viewPage;
    }
    
    /**
     * 查询商品详情
     * 
     * @param tenantId 租户ID
     * @param id 商品ID
     * @return 商品详情
     */
    @GetMapping("/{id}")
    @RequireAdminPermission("product:view")
    public ProductDetailView getProduct(@RequestHeader("X-Tenant-Id") Long tenantId,
                                       @PathVariable Long id) {
        log.info("查询商品详情: tenantId={}, productId={}", tenantId, id);
        
        Product product = productRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在或无权访问"));
        
        return toDetailView(product);
    }
    
    /**
     * 创建商品（简化版）
     * 
     * <p><b>⚠️ 已废弃：</b>本方法仅支持简单的商品创建，不支持规格、属性、小料等完整功能。
     * <p><b>推荐使用：</b>ProductAggregateAdminController 进行完整的商品聚合管理。
     * 
     * @param tenantId 租户ID
     * @param request 创建请求
     * @return 商品ID
     * @deprecated 请使用 ProductAggregateAdminController 代替
     */
    @Deprecated
    @PostMapping
    @RequireAdminPermission("product:create")
    public CreateProductResponse createProduct(@RequestHeader("X-Tenant-Id") Long tenantId,
                                              @Valid @RequestBody CreateProductRequest request) {
        log.warn("⚠️ 使用已废弃的简化版商品创建接口，推荐使用 ProductAggregateAdminController: tenantId={}, request={}", 
                tenantId, request);
        
        Long operatorId = getCurrentUserId();
        
        // 构建商品聚合（不再使用 IdService 手动生成 ID，让 DB AUTO_INCREMENT 生成）
        Product product = Product.builder()
                // 不设置 id，让 DB AUTO_INCREMENT 生成
                .tenantId(tenantId)
                .productCode(request.getProductCode())
                .name(request.getName())
                .subtitle(request.getSubtitle())
                .productType(request.getProductType())
                .description(request.getDescription())
                .mainImage(request.getMainImage())
                .mediaGallery(request.getMediaGallery())
                .unit(request.getUnit())
                .status(ProductStatus.DRAFT) // 默认草稿状态
                .sortOrder(request.getSortOrder())
                .build();
        
        // 构建SKU列表（不再使用 IdService 手动生成 ID）
        List<ProductSku> skus = new ArrayList<>();
        if (request.getSkus() != null && !request.getSkus().isEmpty()) {
            for (CreateProductRequest.SkuRequest skuReq : request.getSkus()) {
                ProductSku sku = ProductSku.builder()
                        // 不设置 id，让 DB AUTO_INCREMENT 生成
                        .tenantId(tenantId)
                        // productId 将在 save 后设置
                        .skuCode(skuReq.getSkuCode())
                        .name(skuReq.getName())
                        .basePrice(skuReq.getBasePrice())
                        .marketPrice(skuReq.getMarketPrice())
                        .costPrice(skuReq.getCostPrice())
                        .barcode(skuReq.getBarcode())
                        .defaultSku(skuReq.isDefaultSku())
                        .status(ProductStatus.DRAFT)
                        .sortOrder(skuReq.getSortOrder())
                        .build();
                skus.add(sku);
            }
        }
        product.setSkus(skus);
        
        // 保存商品
        Long productId = productRepository.save(product);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("CREATE")
                .resourceType("PRODUCT")
                .resourceId(productId)
                .resourceName(request.getName())
                .operationDesc("创建商品（简化版）")
                .dataAfter(product));
        
        log.info("商品创建成功（简化版）: tenantId={}, productId={}", tenantId, productId);
        return new CreateProductResponse(productId);
    }
    
    /**
     * 更新商品信息
     * 
     * @param tenantId 租户ID
     * @param id 商品ID
     * @param request 更新请求
     */
    @PutMapping("/{id}")
    @RequireAdminPermission("product:edit")
    public void updateProduct(@RequestHeader("X-Tenant-Id") Long tenantId,
                             @PathVariable Long id,
                             @Valid @RequestBody UpdateProductRequest request) {
        log.info("更新商品信息: tenantId={}, productId={}, request={}", tenantId, id, request);
        
        Long operatorId = getCurrentUserId();
        
        // 查询原商品
        Product productBefore = productRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在或无权访问"));
        
        // 更新字段
        if (request.getName() != null) {
            productBefore.setName(request.getName());
        }
        if (request.getSubtitle() != null) {
            productBefore.setSubtitle(request.getSubtitle());
        }
        if (request.getDescription() != null) {
            productBefore.setDescription(request.getDescription());
        }
        if (request.getMainImage() != null) {
            productBefore.setMainImage(request.getMainImage());
        }
        if (request.getMediaGallery() != null) {
            productBefore.setMediaGallery(request.getMediaGallery());
        }
        if (request.getUnit() != null) {
            productBefore.setUnit(request.getUnit());
        }
        if (request.getSortOrder() != null) {
            productBefore.setSortOrder(request.getSortOrder());
        }
        
        // 执行更新
        productRepository.update(productBefore);
        
        // 查询更新后的商品
        Product productAfter = productRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("UPDATE")
                .resourceType("PRODUCT")
                .resourceId(id)
                .resourceName(productAfter.getName())
                .operationDesc("修改商品信息")
                .dataBefore(productBefore)
                .dataAfter(productAfter));
        
        log.info("商品更新成功: tenantId={}, productId={}", tenantId, id);
    }
    
    /**
     * 商品上线
     * 
     * @param tenantId 租户ID
     * @param id 商品ID
     */
    @PostMapping("/{id}/online")
    @RequireAdminPermission("product:online")
    public void onlineProduct(@RequestHeader("X-Tenant-Id") Long tenantId,
                             @PathVariable Long id) {
        log.info("商品上线: tenantId={}, productId={}", tenantId, id);
        
        Long operatorId = getCurrentUserId();
        
        // 查询商品
        Product product = productRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在或无权访问"));
        
        // 更新状态
        productRepository.changeStatus(tenantId, id, ProductStatus.ENABLED, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("ONLINE")
                .resourceType("PRODUCT")
                .resourceId(id)
                .resourceName(product.getName())
                .operationDesc("商品上线")
                .dataBefore(ProductStatus.DRAFT)
                .dataAfter(ProductStatus.ENABLED));
        
        log.info("商品上线成功: tenantId={}, productId={}", tenantId, id);
    }
    
    /**
     * 商品下线
     * 
     * @param tenantId 租户ID
     * @param id 商品ID
     */
    @PostMapping("/{id}/offline")
    @RequireAdminPermission("product:online")
    public void offlineProduct(@RequestHeader("X-Tenant-Id") Long tenantId,
                              @PathVariable Long id) {
        log.info("商品下线: tenantId={}, productId={}", tenantId, id);
        
        Long operatorId = getCurrentUserId();
        
        // 查询商品
        Product product = productRepository.findById(tenantId, id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在或无权访问"));
        
        // 更新状态
        productRepository.changeStatus(tenantId, id, ProductStatus.DISABLED, operatorId);
        
        // 记录审计日志
        auditLogService.log(auditLogService.builder(tenantId, operatorId)
                .action("OFFLINE")
                .resourceType("PRODUCT")
                .resourceId(id)
                .resourceName(product.getName())
                .operationDesc("商品下线")
                .dataBefore(ProductStatus.ENABLED)
                .dataAfter(ProductStatus.DISABLED));
        
        log.info("商品下线成功: tenantId={}, productId={}", tenantId, id);
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
     * 转换为列表视图
     */
    private ProductListView toListView(BcProduct entity) {
        ProductListView view = new ProductListView();
        view.setId(entity.getId());
        view.setProductCode(entity.getProductCode());
        view.setName(entity.getName());
        view.setSubtitle(entity.getSubtitle());
        view.setMainImage(entity.getMainImage());
        view.setUnit(entity.getUnit());
        view.setStatus(entity.getStatus());
        view.setStatusDesc(ProductStatus.fromCode(entity.getStatus()).getDescription());
        view.setCreatedAt(entity.getCreatedAt());
        view.setUpdatedAt(entity.getUpdatedAt());
        return view;
    }
    
    /**
     * 转换为详情视图
     */
    private ProductDetailView toDetailView(Product product) {
        ProductDetailView view = new ProductDetailView();
        view.setId(product.getId());
        view.setProductCode(product.getProductCode());
        view.setName(product.getName());
        view.setSubtitle(product.getSubtitle());
        view.setProductType(product.getProductType());
        view.setDescription(product.getDescription());
        view.setMainImage(product.getMainImage());
        view.setMediaGallery(product.getMediaGallery());
        view.setUnit(product.getUnit());
        view.setStatus(product.getStatus());
        view.setSortOrder(product.getSortOrder());
        
        // 转换SKU列表
        if (product.getSkus() != null) {
            List<ProductDetailView.SkuView> skuViews = product.getSkus().stream()
                    .map(sku -> {
                        ProductDetailView.SkuView skuView = new ProductDetailView.SkuView();
                        skuView.setId(sku.getId());
                        skuView.setSkuCode(sku.getSkuCode());
                        skuView.setName(sku.getName());
                        skuView.setBasePrice(sku.getBasePrice());
                        skuView.setMarketPrice(sku.getMarketPrice());
                        skuView.setCostPrice(sku.getCostPrice());
                        skuView.setBarcode(sku.getBarcode());
                        skuView.setDefaultSku(sku.isDefaultSku());
                        skuView.setStatus(sku.getStatus());
                        skuView.setSortOrder(sku.getSortOrder());
                        return skuView;
                    })
                    .collect(Collectors.toList());
            view.setSkus(skuViews);
        }
        
        return view;
    }
    
    // ===== DTO类 =====
    
    @Data
    public static class ProductListView {
        private Long id;
        private String productCode;
        private String name;
        private String subtitle;
        private String mainImage;
        private String unit;
        private Integer status;
        private String statusDesc;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    public static class ProductDetailView {
        private Long id;
        private String productCode;
        private String name;
        private String subtitle;
        private ProductType productType;
        private String description;
        private String mainImage;
        private List<String> mediaGallery;
        private String unit;
        private ProductStatus status;
        private Integer sortOrder;
        private List<SkuView> skus;
        
        @Data
        public static class SkuView {
            private Long id;
            private String skuCode;
            private String name;
            private BigDecimal basePrice;
            private BigDecimal marketPrice;
            private BigDecimal costPrice;
            private String barcode;
            private boolean defaultSku;
            private ProductStatus status;
            private Integer sortOrder;
        }
    }
    
    @Data
    public static class CreateProductRequest {
        private String productCode;
        private String name;
        private String subtitle;
        private ProductType productType;
        private String description;
        private String mainImage;
        private List<String> mediaGallery;
        private String unit;
        private Integer sortOrder;
        private List<SkuRequest> skus;
        
        @Data
        public static class SkuRequest {
            private String skuCode;
            private String name;
            private BigDecimal basePrice;
            private BigDecimal marketPrice;
            private BigDecimal costPrice;
            private String barcode;
            private boolean defaultSku;
            private Integer sortOrder;
        }
    }
    
    @Data
    public static class UpdateProductRequest {
        private String name;
        private String subtitle;
        private String description;
        private String mainImage;
        private List<String> mediaGallery;
        private String unit;
        private Integer sortOrder;
    }
    
    @Data
    @lombok.AllArgsConstructor
    public static class CreateProductResponse {
        private Long productId;
    }
}
