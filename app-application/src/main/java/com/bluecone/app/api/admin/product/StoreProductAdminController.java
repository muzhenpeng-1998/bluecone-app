package com.bluecone.app.api.admin.product;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.product.application.dto.StoreProductReorderRequest;
import com.bluecone.app.product.application.dto.StoreProductVisibilityRequest;
import com.bluecone.app.product.application.service.StoreProductAdminApplicationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 门店商品管理 Admin Controller
 * 
 * <h3>📋 接口列表：</h3>
 * <ul>
 *   <li>PUT /api/admin/stores/{storeId}/products/{productId}/visibility - 上架/下架</li>
 *   <li>POST /api/admin/stores/{storeId}/products/reorder - 批量排序</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Tag(name = "🎛️ 平台管理后台 > 商品管理 > 门店商品管理", description = "门店商品上下架和排序管理")
@RestController
@RequestMapping("/api/admin/stores")
@RequiredArgsConstructor
@Slf4j
public class StoreProductAdminController {
    
    private final StoreProductAdminApplicationService storeProductService;
    
    /**
     * 设置商品在门店的可见性（上架/下架）
     * 
     * <p>接口：PUT /api/admin/stores/{storeId}/products/{productId}/visibility
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>商品上架：visible=true</li>
     *   <li>商品下架：visible=false</li>
     *   <li>定时上架：设置 displayStartAt</li>
     *   <li>定时下架：设置 displayEndAt</li>
     * </ul>
     * 
     * @param storeId 门店ID
     * @param productId 商品ID
     * @param request 可见性设置请求
     * @return 成功响应
     */
    @PutMapping("/{storeId}/products/{productId}/visibility")
    public ApiResponse<Void> setProductVisibility(
            @PathVariable Long storeId,
            @PathVariable Long productId,
            @Valid @RequestBody StoreProductVisibilityRequest request) {
        
        log.info("设置商品可见性: storeId={}, productId={}, visible={}", 
                storeId, productId, request.getVisible());
        
        storeProductService.setProductVisibility(storeId, productId, request);
        
        return ApiResponse.success();
    }
    
    /**
     * 批量调整商品在门店的排序
     * 
     * <p>接口：POST /api/admin/stores/{storeId}/products/reorder
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>拖拽排序：前端传入新的排序列表</li>
     *   <li>置顶商品：设置较大的 sortOrder</li>
     * </ul>
     * 
     * <h3>排序规则：</h3>
     * <ul>
     *   <li>降序排列：sortOrder 值越大越靠前</li>
     *   <li>相同 sortOrder：按 id 升序</li>
     * </ul>
     * 
     * @param storeId 门店ID
     * @param request 排序请求
     * @return 成功响应
     */
    @PostMapping("/{storeId}/products/reorder")
    public ApiResponse<Void> reorderProducts(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreProductReorderRequest request) {
        
        log.info("批量调整商品排序: storeId={}, count={}", 
                storeId, request.getProducts().size());
        
        storeProductService.reorderProducts(storeId, request);
        
        return ApiResponse.success();
    }
}

