package com.bluecone.app.controller.product;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.web.ResolvePublicId;
import com.bluecone.app.id.api.ResourceType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 商户侧商品接口（Public ID Governance 示范）。
 * 
 * <p>改造要点：</p>
 * <ul>
 *   <li>商品详情：/products/{productId} 使用 publicId（prd_xxx）</li>
 *   <li>SKU 详情：/products/{productId}/skus/{skuId} 使用 publicId（sku_xxx）</li>
 *   <li>批量查询：支持批量解析 publicId（避免 N+1）</li>
 *   <li>响应：仅返回 publicId，不暴露 Long 主键</li>
 * </ul>
 * 
 * <p>Scope Guard 策略：</p>
 * <ul>
 *   <li>PRODUCT/SKU：仅做租户级校验（商品归属租户）</li>
 *   <li>门店级校验：后续可扩展（商品归属门店）</li>
 * </ul>
 */
@Tag(name = "🏪 商户后台 > 商品管理", description = "商户侧商品管理接口")
@RestController
@RequestMapping("/api/merchant/products")
public class MerchantProductController {

    // 注：实际实现需要注入 ProductFacade 或 ProductService
    // private final ProductFacade productFacade;

    /**
     * 获取商品详情。
     * 
     * <p>请求示例：</p>
     * <pre>
     * GET /api/merchant/products/prd_01HN8X5K9G3QRST2VW4XYZ
     * </pre>
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>提取 productId：prd_01HN8X5K9G3QRST2VW4XYZ</li>
     *   <li>校验格式：前缀 prd_ + 26 位 ULID</li>
     *   <li>查询主键：SELECT id FROM bc_product WHERE tenant_id=? AND public_id=?</li>
     *   <li>Scope Guard：校验 tenantId（商品归属租户）</li>
     *   <li>注入参数：productPk = 12345（Long）</li>
     *   <li>调用服务：productService.getDetail(productPk)</li>
     * </ol>
     * 
     * @param productPk 商品主键（自动从 publicId 解析）
     * @return 商品详情
     */
    @GetMapping("/{productId}")
    public ApiResponse<ProductDetailResponse> detail(
            @PathVariable("productId") @ResolvePublicId(type = ResourceType.PRODUCT) Long productPk) {
        // productPk 已自动解析并通过 Scope Guard 校验
        
        // 模拟查询商品详情
        ProductDetailResponse response = new ProductDetailResponse(
                "prd_01HN8X5K9G3QRST2VW4XYZ",  // publicId
                "美式咖啡",
                "经典美式，香醇浓郁",
                new BigDecimal("25.00"),
                "https://cdn.example.com/product.jpg",
                List.of(
                        new SkuInfo("sku_01HN8X5K9G3QRST2VW4XYZ01", "中杯", new BigDecimal("25.00")),
                        new SkuInfo("sku_01HN8X5K9G3QRST2VW4XYZ02", "大杯", new BigDecimal("30.00"))
                )
        );
        
        return ApiResponse.success(response);
    }

    /**
     * 获取商品详情（使用 ResolvedPublicId 注入）。
     * 
     * <p>适用场景：需要同时使用 publicId 和主键</p>
     * 
     * @param resolved 完整解析结果
     * @return 商品详情
     */
    @GetMapping("/{productId}/full")
    public ApiResponse<ProductDetailWithMetaResponse> detailWithMeta(
            @PathVariable("productId") @ResolvePublicId(type = ResourceType.PRODUCT) ResolvedPublicId resolved) {
        Long productPk = resolved.asLong();
        String publicId = resolved.publicId();
        long tenantId = resolved.tenantId();
        
        // 模拟查询商品详情
        ProductDetailWithMetaResponse response = new ProductDetailWithMetaResponse(
                publicId,
                "美式咖啡",
                new BigDecimal("25.00"),
                Map.of(
                        "tenantId", tenantId,
                        "productPk", productPk,  // 仅用于演示，生产环境不应暴露
                        "resolvedAt", System.currentTimeMillis()
                )
        );
        
        return ApiResponse.success(response);
    }

    /**
     * 获取 SKU 详情（嵌套路径示范）。
     * 
     * <p>请求示例：</p>
     * <pre>
     * GET /api/merchant/products/prd_xxx/skus/sku_xxx
     * </pre>
     * 
     * <p>说明：</p>
     * <ul>
     *   <li>同时解析 productId 和 skuId</li>
     *   <li>两者都会执行 Scope Guard 校验</li>
     *   <li>可用于校验 SKU 是否归属于指定 Product</li>
     * </ul>
     * 
     * @param productPk 商品主键（自动解析）
     * @param skuPk SKU 主键（自动解析）
     * @return SKU 详情
     */
    @GetMapping("/{productId}/skus/{skuId}")
    public ApiResponse<SkuDetailResponse> skuDetail(
            @PathVariable("productId") @ResolvePublicId(type = ResourceType.PRODUCT) Long productPk,
            @PathVariable("skuId") @ResolvePublicId(type = ResourceType.SKU) Long skuPk) {
        // productPk 和 skuPk 都已自动解析并通过 Scope Guard 校验
        
        // 业务逻辑：校验 SKU 是否归属于 Product
        // if (!skuBelongsToProduct(skuPk, productPk)) {
        //     throw new BusinessException("SKU 不属于该商品");
        // }
        
        // 模拟查询 SKU 详情
        SkuDetailResponse response = new SkuDetailResponse(
                "sku_01HN8X5K9G3QRST2VW4XYZ01",
                "prd_01HN8X5K9G3QRST2VW4XYZ",
                "中杯",
                new BigDecimal("25.00"),
                Map.of("size", "M", "temperature", "hot")
        );
        
        return ApiResponse.success(response);
    }

    /**
     * 批量查询商品详情（演示批量解析）。
     * 
     * <p>请求示例：</p>
     * <pre>
     * POST /api/merchant/products/batch
     * {
     *   "productIds": ["prd_xxx1", "prd_xxx2", "prd_xxx3"]
     * }
     * </pre>
     * 
     * <p>说明：</p>
     * <ul>
     *   <li>后续可扩展为使用 PublicIdGovernanceResolver.resolveBatch()</li>
     *   <li>避免 N+1 查询问题</li>
     *   <li>单次批量建议不超过 100 个</li>
     * </ul>
     * 
     * @param request 批量查询请求
     * @return 商品列表
     */
    @PostMapping("/batch")
    public ApiResponse<List<ProductSummary>> batchQuery(@RequestBody BatchQueryRequest request) {
        // 注：当前 @ResolvePublicId 不支持批量，需要手动调用 resolveBatch
        // 这里仅作为接口设计示范
        
        List<String> productIds = request.productIds();
        // Map<String, ResolvedPublicId> resolvedMap = governanceResolver.resolveBatch(tenantId, PRODUCT, productIds);
        
        // 模拟返回
        List<ProductSummary> products = productIds.stream()
                .map(publicId -> new ProductSummary(publicId, "商品名称", new BigDecimal("25.00")))
                .toList();
        
        return ApiResponse.success(products);
    }

    // ==================== 响应 DTO ====================

    public record ProductDetailResponse(
            String productPublicId,
            String name,
            String description,
            BigDecimal basePrice,
            String mainImage,
            List<SkuInfo> skus
    ) {}

    public record ProductDetailWithMetaResponse(
            String productPublicId,
            String name,
            BigDecimal basePrice,
            Map<String, Object> meta
    ) {}

    public record SkuInfo(
            String skuPublicId,
            String name,
            BigDecimal price
    ) {}

    public record SkuDetailResponse(
            String skuPublicId,
            String productPublicId,
            String name,
            BigDecimal price,
            Map<String, Object> specCombination
    ) {}

    public record ProductSummary(
            String productPublicId,
            String name,
            BigDecimal price
    ) {}

    public record BatchQueryRequest(
            List<String> productIds
    ) {}
}

