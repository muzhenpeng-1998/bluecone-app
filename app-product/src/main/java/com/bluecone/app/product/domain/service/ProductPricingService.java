package com.bluecone.app.product.domain.service;

import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductSku;
import java.math.BigDecimal;

/**
 * 商品定价规则领域服务，占位用于封装 SKU 基础价、规格加价、小料加价等组合逻辑。
 */
public interface ProductPricingService {

    /**
     * 计算指定商品/SKU 的基础价格（占位，后续根据规格、小料等补充细节）。
     */
    BigDecimal calculateBasePrice(Product product, ProductSku sku);
}
