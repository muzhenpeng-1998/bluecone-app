package com.bluecone.app.order.infra.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderDraftRepository;
import com.bluecone.app.order.infra.persistence.converter.OrderConverter;
import com.bluecone.app.order.infra.persistence.mapper.OrderItemMapper;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.order.infra.persistence.po.OrderItemPO;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单草稿仓储实现，复用现有 OrderMapper / OrderConverter 的 PO ↔ Domain 转换逻辑。
 */
@Repository
@RequiredArgsConstructor
public class OrderDraftRepositoryImpl implements OrderDraftRepository {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderIdGenerator orderIdGenerator;

    @Override
    public Optional<Order> findDraft(Long tenantId,
                                     Long storeId,
                                     Long userId,
                                     String channel,
                                     String scene) {
        validateContext(tenantId, storeId, userId, channel);
        LambdaQueryWrapper<OrderPO> wrapper = buildDraftWrapper(tenantId, storeId, userId, channel, scene);
        wrapper.orderByDesc(OrderPO::getCreatedAt).last("LIMIT 1");
        OrderPO orderPO = orderMapper.selectOne(wrapper);
        if (orderPO == null) {
            return Optional.empty();
        }
        List<OrderItemPO> itemPOList = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, tenantId)
                .eq(OrderItemPO::getStoreId, storeId)
                .eq(OrderItemPO::getOrderId, orderPO.getId()));
        return Optional.of(OrderConverter.toDomain(orderPO, itemPOList));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order saveDraft(Order draft) {
        requireDraft(draft);
        ensureContext(draft);
        if (draft.getId() == null || draft.getId() == 0L) {
            return insertDraft(draft);
        }
        return updateDraft(draft);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long orderId) {
        if (orderId == null) {
            return;
        }
        OrderPO po = orderMapper.selectById(orderId);
        if (po == null || (!OrderStatus.DRAFT.getCode().equals(po.getStatus())
                && !OrderStatus.LOCKED_FOR_CHECKOUT.getCode().equals(po.getStatus()))) {
            return;
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, po.getTenantId())
                .eq(OrderItemPO::getStoreId, po.getStoreId())
                .eq(OrderItemPO::getOrderId, orderId));
        orderMapper.delete(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, po.getTenantId())
                .eq(OrderPO::getStoreId, po.getStoreId())
                .eq(OrderPO::getId, orderId));
    }

    private LambdaQueryWrapper<OrderPO> buildDraftWrapper(Long tenantId,
                                                          Long storeId,
                                                          Long userId,
                                                          String channel,
                                                          String scene) {
        LambdaQueryWrapper<OrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderPO::getTenantId, tenantId)
                .eq(OrderPO::getStoreId, storeId)
                .eq(OrderPO::getUserId, userId)
                .eq(OrderPO::getChannel, channel)
                .in(OrderPO::getStatus,
                        OrderStatus.DRAFT.getCode(),
                        OrderStatus.LOCKED_FOR_CHECKOUT.getCode());
        if (StringUtils.hasText(scene)) {
            wrapper.eq(OrderPO::getOrderSource, scene);
        }
        return wrapper;
    }

    private Order insertDraft(Order draft) {
        LocalDateTime now = LocalDateTime.now();
        draft.setId(orderIdGenerator.nextId());
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        draft.setVersion(0);
        OrderPO po = OrderConverter.toPO(draft);
        po.setVersion(0);
        orderMapper.insert(po);
        saveItems(draft, now);
        draft.setVersion(po.getVersion() == null ? 0 : po.getVersion());
        return draft;
    }

    private Order updateDraft(Order draft) {
        if (draft.getId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "草稿订单 ID 缺失");
        }
        Integer currentVersion = draft.getVersion();
        if (currentVersion == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "草稿订单版本缺失");
        }
        LocalDateTime now = LocalDateTime.now();
        OrderPO po = OrderConverter.toPO(draft);
        po.setUpdatedAt(now);
        po.setVersion(currentVersion + 1);
        LambdaUpdateWrapper<OrderPO> update = new LambdaUpdateWrapper<OrderPO>()
                .eq(OrderPO::getId, draft.getId())
                .eq(OrderPO::getTenantId, draft.getTenantId())
                .eq(OrderPO::getStoreId, draft.getStoreId())
                .eq(OrderPO::getUserId, draft.getUserId())
                .eq(OrderPO::getChannel, draft.getChannel())
                .eq(OrderPO::getOrderSource, codeOrNull(draft.getOrderSource()))
                .eq(OrderPO::getStatus, OrderStatus.DRAFT.getCode())
                .eq(OrderPO::getVersion, currentVersion);
        int updated = orderMapper.update(po, update);
        if (updated == 0) {
            throw new BizException(CommonErrorCode.CONFLICT, "订单草稿已被修改，请刷新后重试");
        }
        deleteItems(draft);
        saveItems(draft, now);
        draft.setVersion(po.getVersion());
        return draft;
    }

    private void saveItems(Order draft, LocalDateTime now) {
        List<OrderItem> items = draft.getItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        for (OrderItem item : items) {
            if (item == null) {
                continue;
            }
            if (item.getId() == null) {
                item.setId(orderIdGenerator.nextId());
            }
            item.setOrderId(draft.getId());
            item.setTenantId(draft.getTenantId());
            item.setStoreId(draft.getStoreId());
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(now);
            }
            item.setUpdatedAt(now);
            item.recalculateAmounts();
            orderItemMapper.insert(OrderConverter.toItemPO(draft, item));
        }
    }

    private void deleteItems(Order draft) {
        if (draft.getId() == null) {
            return;
        }
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItemPO>()
                .eq(OrderItemPO::getTenantId, draft.getTenantId())
                .eq(OrderItemPO::getStoreId, draft.getStoreId())
                .eq(OrderItemPO::getOrderId, draft.getId()));
    }

    private void ensureContext(Order draft) {
        if (draft.getTenantId() == null
                || draft.getStoreId() == null
                || draft.getUserId() == null
                || draft.getChannel() == null
                || draft.getChannel().isBlank()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "草稿订单缺少租户/门店/用户/渠道信息");
        }
    }

    private void validateContext(Long tenantId, Long storeId, Long userId, String channel) {
        if (tenantId == null || storeId == null || userId == null || !StringUtils.hasText(channel)) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "查询草稿需提供 tenantId/storeId/userId/channel");
        }
    }

    private void requireDraft(Order draft) {
        if (draft == null || draft.getStatus() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "仅允许操作草稿订单");
        }
        if (!OrderStatus.DRAFT.equals(draft.getStatus()) && !OrderStatus.LOCKED_FOR_CHECKOUT.equals(draft.getStatus())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "仅允许操作草稿订单");
        }
    }

    private String codeOrNull(OrderSource source) {
        return source == null ? null : source.getCode();
    }
}
