package com.bluecone.app.product.application.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.product.application.command.ChangeSkuPriceCommand;
import com.bluecone.app.product.application.command.PublishSkuCommand;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.event.ProductPriceChangedEvent;
import com.bluecone.app.product.domain.event.ProductPublishedEvent;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.repository.ProductRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商品管理应用服务，承接后台的上下架、改价等指令并发布领域事件。
 */
@Service
@RequiredArgsConstructor
public class ProductAdminApplicationService {

    private final ProductRepository productRepository;
    private final DomainEventPublisher eventPublisher;

    /**
     * 上架（或重新发布）某个 SKU。
     */
    @Transactional(rollbackFor = Exception.class)
    public void publishSku(PublishSkuCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getProductId(), "productId 不能为空");
        Objects.requireNonNull(command.getSkuId(), "skuId 不能为空");

        Product product = productRepository.findById(command.getTenantId(), command.getProductId())
                .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "商品不存在或已下线"));
        ProductSku sku = productRepository.findSkuById(command.getTenantId(), command.getSkuId())
                .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "SKU 不存在"));
        ensureSkuBelongsToProduct(product, sku);

        productRepository.updateSkuStatus(command.getTenantId(), command.getSkuId(), ProductStatus.ENABLED, command.getOperatorId());

        ProductPublishedEvent event = new ProductPublishedEvent(
                command.getTenantId(),
                command.getStoreId(),
                product.getId(),
                sku.getId(),
                product.getName(),
                toCentLong(sku.getBasePrice()),
                Boolean.TRUE
        );
        eventPublisher.publish(event);
    }

    /**
     * 修改 SKU 售价。
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeSkuPrice(ChangeSkuPriceCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getProductId(), "productId 不能为空");
        Objects.requireNonNull(command.getSkuId(), "skuId 不能为空");
        Objects.requireNonNull(command.getNewPrice(), "newPrice 不能为空");
        if (command.getNewPrice() < 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "newPrice 不能为负数");
        }

        Product product = productRepository.findById(command.getTenantId(), command.getProductId())
                .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "商品不存在或已下线"));
        ProductSku sku = productRepository.findSkuById(command.getTenantId(), command.getSkuId())
                .orElseThrow(() -> new BizException(CommonErrorCode.BAD_REQUEST, "SKU 不存在"));
        ensureSkuBelongsToProduct(product, sku);

        Long oldPrice = toCentLong(sku.getBasePrice());
        BigDecimal newPriceAmount = fromCent(command.getNewPrice());

        productRepository.updateSkuPrice(command.getTenantId(), command.getSkuId(), newPriceAmount, command.getOperatorId());

        ProductPriceChangedEvent event = new ProductPriceChangedEvent(
                command.getTenantId(),
                command.getStoreId(),
                product.getId(),
                sku.getId(),
                oldPrice,
                command.getNewPrice(),
                command.getOperatorId()
        );
        eventPublisher.publish(event);
    }

    private void ensureSkuBelongsToProduct(Product product, ProductSku sku) {
        if (product == null || sku == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "商品或 SKU 不存在");
        }
        if (!Objects.equals(product.getId(), sku.getProductId())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "SKU 不属于当前商品");
        }
    }

    private Long toCentLong(BigDecimal price) {
        if (price == null) {
            return null;
        }
        BigDecimal scaled = price.setScale(2, RoundingMode.HALF_UP);
        return scaled.movePointRight(2).longValue();
    }

    private BigDecimal fromCent(Long price) {
        if (price == null) {
            return null;
        }
        return BigDecimal.valueOf(price, 2);
    }
}
