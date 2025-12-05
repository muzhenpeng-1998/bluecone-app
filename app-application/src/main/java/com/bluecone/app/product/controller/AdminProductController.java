package com.bluecone.app.product.controller;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.product.application.query.ProductQueryApplicationService;
import com.bluecone.app.product.dto.view.ProductDetailView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端商品查询接口，后续可扩展创建/编辑等写操作。
 */
@RestController
@RequestMapping("/api/admin/product")
@Validated
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductQueryApplicationService productQueryApplicationService;

    /**
     * 商品详情。
     */
    @GetMapping("/detail")
    public ApiResponse<ProductDetailView> getDetail(@RequestParam Long productId) {
        ProductDetailView view = productQueryApplicationService.getProductDetail(productId);
        return ApiResponse.success(view);
    }

    /**
     * 商品列表，可按分类筛选。
     */
    @GetMapping("/list")
    public ApiResponse<List<ProductDetailView>> listProducts(@RequestParam(required = false) Long categoryId) {
        List<ProductDetailView> list = productQueryApplicationService.listProductsByCategory(categoryId);
        return ApiResponse.success(list);
    }
}
