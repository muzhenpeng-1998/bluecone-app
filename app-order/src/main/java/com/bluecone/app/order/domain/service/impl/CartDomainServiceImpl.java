package com.bluecone.app.order.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.service.CartDomainService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * 购物车领域服务实现，覆盖新增/更新/移除/清空草稿订单明细的常见场景。
 */
@Service
public class CartDomainServiceImpl implements CartDomainService {

    @Override
    public Order addItem(Order draft, ConfirmOrderItemDTO itemDTO) {
        requireDraft(draft);
        if (itemDTO == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "购物车明细不能为空");
        }
        if (itemDTO.getQuantity() == null || itemDTO.getQuantity() <= 0) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "明细数量必须大于0");
        }
        OrderItem item = toOrderItem(itemDTO);
        draft.addOrMergeItem(item);
        return draft;
    }

    @Override
    public Order changeItemQuantity(Order draft, Long skuId, Map<String, Object> attrs, int newQuantity) {
        requireDraft(draft);
        draft.changeItemQuantity(skuId, attrs, newQuantity);
        return draft;
    }

    @Override
    public Order removeItem(Order draft, Long skuId, Map<String, Object> attrs) {
        requireDraft(draft);
        draft.removeItem(skuId, attrs);
        return draft;
    }

    @Override
    public Order clearCart(Order draft) {
        requireDraft(draft);
        draft.clearItems();
        return draft;
    }

    private void requireDraft(Order draft) {
        if (draft == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "草稿订单不能为空");
        }
        draft.assertEditable();
    }

    private OrderItem toOrderItem(ConfirmOrderItemDTO dto) {
        OrderItem item = new OrderItem();
        item.setProductId(dto.getProductId());
        item.setSkuId(dto.getSkuId());
        item.setProductName(dto.getProductName());
        item.setSkuName(dto.getSkuName());
        item.setProductCode(dto.getProductCode());
        item.setQuantity(dto.getQuantity());
        item.setAttrs(defaultMap(dto.getAttrs()));
        item.setUnitPrice(defaultBigDecimal(dto.getClientUnitPrice()));
        item.setDiscountAmount(BigDecimal.ZERO);
        return item;
    }

    private Map<String, Object> defaultMap(Map<String, Object> source) {
        return source == null ? Collections.emptyMap() : source;
    }

    private BigDecimal defaultBigDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
