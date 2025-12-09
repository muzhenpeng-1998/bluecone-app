package com.bluecone.app.order.application.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.product.application.UserProductQueryAppService;
import com.bluecone.app.product.domain.model.menu.StoreSkuSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * 为订单提供权威的商品价格校验与明细填充。
 */
@Service
@RequiredArgsConstructor
public class OrderPricingService {

    private final UserProductQueryAppService userProductQueryAppService;

    public PricingResult priceItems(Long tenantId, Long storeId, List<ConfirmOrderItemDTO> items) {
        if (CollectionUtils.isEmpty(items)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单明细不能为空");
        }
        Set<Long> skuIds = items.stream()
                .map(ConfirmOrderItemDTO::getSkuId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (skuIds.isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "明细中必须包含 skuId");
        }
        Map<Long, StoreSkuSnapshot> snapshotMap = userProductQueryAppService.getStoreSkuSnapshotMap(tenantId, storeId, skuIds);
        if (snapshotMap == null || snapshotMap.isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "当前门店无可售商品");
        }
        long totalCents = 0L;
        for (ConfirmOrderItemDTO item : items) {
            if (item == null || item.getSkuId() == null) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "明细中存在无效 SKU");
            }
            StoreSkuSnapshot snapshot = snapshotMap.get(item.getSkuId());
            if (snapshot == null) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "SKU[" + item.getSkuId() + "] 不存在或不可售");
            }
            if (!Boolean.TRUE.equals(snapshot.getAvailable())) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "SKU[" + item.getSkuId() + "] 当前不可售");
            }
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            if (quantity <= 0) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "明细数量必须大于 0");
            }
            long unitPrice = snapshot.getSalePrice() == null ? 0L : snapshot.getSalePrice();
            long lineAmount = unitPrice * quantity;
            totalCents += lineAmount;
            item.setClientUnitPrice(toDecimal(unitPrice));
            item.setClientSubtotalAmount(toDecimal(lineAmount));
            item.setSkuName(snapshot.getSkuName());
        }
        return new PricingResult(totalCents, snapshotMap);
    }

    public static BigDecimal toDecimal(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(cents).movePointLeft(2);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class PricingResult {
        private final long totalAmountCents;
        private final Map<Long, StoreSkuSnapshot> snapshotMap;
    }
}
