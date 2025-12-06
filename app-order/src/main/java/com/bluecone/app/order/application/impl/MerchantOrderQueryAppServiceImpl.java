package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
import com.bluecone.app.order.api.dto.MerchantOrderDetailItemView;
import com.bluecone.app.order.api.dto.MerchantOrderDetailView;
import com.bluecone.app.order.api.dto.MerchantOrderListQuery;
import com.bluecone.app.order.api.dto.MerchantOrderSummaryView;
import com.bluecone.app.order.application.MerchantOrderQueryAppService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantOrderQueryAppServiceImpl implements MerchantOrderQueryAppService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResult<MerchantOrderSummaryView> listStoreOrders(MerchantOrderListQuery query) {
        if (query.getTenantId() == null || query.getStoreId() == null || query.getOperatorId() == null) {
            throw new IllegalArgumentException("tenantId/storeId/operatorId 不能为空");
        }
        int pageNo = query.getPageNo() == null ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null ? 20 : query.getPageSize();
        int offset = (pageNo - 1) * pageSize;

        List<String> statusList = null;
        if (StringUtils.hasText(query.getStatus())) {
            statusList = Arrays.stream(query.getStatus().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        LocalDateTime fromTime = parseDateTime(query.getFromTime());
        LocalDateTime toTime = parseDateTime(query.getToTime());

        long total = orderRepository.countStoreOrders(query.getTenantId(), query.getStoreId(),
                statusList, query.getOrderSource(), fromTime, toTime);
        List<Order> orders = total > 0
                ? orderRepository.findStoreOrders(query.getTenantId(), query.getStoreId(),
                statusList, query.getOrderSource(), fromTime, toTime, offset, pageSize)
                : Collections.emptyList();

        List<MerchantOrderSummaryView> list = orders.stream()
                .map(this::toMerchantOrderSummaryView)
                .collect(Collectors.toList());

        PageResult<MerchantOrderSummaryView> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public MerchantOrderDetailView getStoreOrderDetail(Long tenantId, Long storeId, Long operatorId, Long orderId) {
        if (tenantId == null || storeId == null || operatorId == null || orderId == null) {
            throw new IllegalArgumentException("tenantId/storeId/operatorId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("Merchant get order detail but order not found, tenantId={}, storeId={}, operatorId={}, orderId={}",
                    tenantId, storeId, operatorId, orderId);
            throw new IllegalStateException("订单不存在");
        }
        if (!storeId.equals(order.getStoreId())) {
            log.warn("Merchant get order detail but store mismatch, tenantId={}, storeId={}, orderStoreId={}, operatorId={}, orderId={}",
                    tenantId, storeId, order.getStoreId(), operatorId, orderId);
            throw new IllegalStateException("无权查看该订单");
        }
        return toMerchantOrderDetailView(order);
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return null;
        }
    }

    private MerchantOrderSummaryView toMerchantOrderSummaryView(Order order) {
        MerchantOrderSummaryView view = new MerchantOrderSummaryView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setTenantId(order.getTenantId());
        view.setStoreId(order.getStoreId());
        view.setCreatedAt(order.getCreatedAt());
        view.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        view.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        view.setPayableAmount(order.getPayableAmount());
        view.setCurrency(order.getCurrency());
        view.setOrderSource(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        view.setChannel(order.getChannel());
        view.setTableInfo(null);
        view.setUserNickname(null);
        view.setUserMobileMasked(null);
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            OrderItem first = order.getItems().get(0);
            int totalCount = order.getItems().stream()
                    .mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity())
                    .sum();
            view.setFirstItemName(first.getProductName());
            view.setTotalItemCount(totalCount);
        } else {
            view.setFirstItemName(null);
            view.setTotalItemCount(0);
        }
        return view;
    }

    private MerchantOrderDetailView toMerchantOrderDetailView(Order order) {
        MerchantOrderDetailView view = new MerchantOrderDetailView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setTenantId(order.getTenantId());
        view.setStoreId(order.getStoreId());
        view.setUserId(order.getUserId());
        view.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        view.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        view.setTotalAmount(order.getTotalAmount());
        view.setDiscountAmount(order.getDiscountAmount());
        view.setPayableAmount(order.getPayableAmount());
        view.setCurrency(order.getCurrency());
        view.setOrderSource(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        view.setChannel(order.getChannel());
        view.setRemark(order.getRemark());
        view.setCreatedAt(order.getCreatedAt());
        view.setPayTime(null);
        view.setCompletedAt(null);
        view.setUserNickname(null);
        view.setUserMobileMasked(null);
        view.setStoreName(null);
        view.setStoreAddress(null);
        view.setDeliveryType(null);
        view.setDeliveryStatus(null);
        view.setReceiverName(null);
        view.setReceiverMobileMasked(null);
        view.setReceiverAddress(null);
        if (order.getPayStatus() != null) {
            view.setRefundStatus(order.getPayStatus().getCode());
        }
        view.setRefundedAmount(null);
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<MerchantOrderDetailItemView> itemViews = order.getItems().stream()
                    .map(this::toMerchantOrderDetailItemView)
                    .collect(Collectors.toList());
            view.setItems(itemViews);
        } else {
            view.setItems(Collections.emptyList());
        }
        return view;
    }

    private MerchantOrderDetailItemView toMerchantOrderDetailItemView(OrderItem item) {
        MerchantOrderDetailItemView view = new MerchantOrderDetailItemView();
        view.setProductId(item.getProductId());
        view.setSkuId(item.getSkuId());
        view.setProductName(item.getProductName());
        view.setSkuName(item.getSkuName());
        view.setProductCode(item.getProductCode());
        view.setQuantity(item.getQuantity());
        view.setUnitPrice(item.getUnitPrice());
        view.setSubtotalAmount(item.getPayableAmount());
        view.setAttrs(item.getAttrs() != null ? item.getAttrs().toString() : null);
        view.setRemark(item.getRemark());
        return view;
    }
}
