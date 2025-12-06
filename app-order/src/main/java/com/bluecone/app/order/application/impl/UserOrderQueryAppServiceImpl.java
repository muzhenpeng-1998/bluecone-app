package com.bluecone.app.order.application.impl;

import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;
import com.bluecone.app.order.api.dto.UserOrderDetailItemView;
import com.bluecone.app.order.api.dto.UserOrderDetailView;
import com.bluecone.app.order.api.dto.UserOrderListQuery;
import com.bluecone.app.order.api.dto.UserOrderSummaryView;
import com.bluecone.app.order.application.UserOrderPreviewAppService;
import com.bluecone.app.order.application.UserOrderQueryAppService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderItem;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
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
public class UserOrderQueryAppServiceImpl implements UserOrderQueryAppService {

    private final OrderRepository orderRepository;
    private final UserOrderPreviewAppService userOrderPreviewAppService;

    @Override
    @Transactional(readOnly = true)
    public ConfirmOrderPreviewResponse reorder(Long tenantId, Long userId, Long orderId) {
        if (tenantId == null || userId == null || orderId == null) {
            throw new IllegalArgumentException("tenantId/userId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("User reorder but order not found, tenantId={}, userId={}, orderId={}",
                    tenantId, userId, orderId);
            throw new IllegalStateException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            log.warn("User reorder but not owner, tenantId={}, userId={}, orderUserId={}, orderId={}",
                    tenantId, userId, order.getUserId(), orderId);
            throw new IllegalStateException("无权操作该订单");
        }
        List<OrderItem> orderItems = order.getItems();
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalStateException("订单明细为空，无法再来一单");
        }
        List<ConfirmOrderItemDTO> previewItems = orderItems.stream()
                .map(item -> {
                    ConfirmOrderItemDTO dto = new ConfirmOrderItemDTO();
                    dto.setProductId(item.getProductId());
                    dto.setSkuId(item.getSkuId());
                    dto.setProductName(item.getProductName());
                    dto.setSkuName(item.getSkuName());
                    dto.setProductCode(item.getProductCode());
                    dto.setQuantity(item.getQuantity());
                    dto.setClientUnitPrice(item.getUnitPrice());
                    dto.setClientSubtotalAmount(item.getPayableAmount());
                    dto.setAttrs(item.getAttrs());
                    dto.setRemark(item.getRemark());
                    return dto;
                })
                .collect(Collectors.toList());

        ConfirmOrderPreviewRequest previewRequest = new ConfirmOrderPreviewRequest();
        previewRequest.setTenantId(order.getTenantId());
        previewRequest.setStoreId(order.getStoreId());
        previewRequest.setUserId(order.getUserId());
        previewRequest.setBizType(order.getBizType() != null ? order.getBizType().getCode() : null);
        previewRequest.setOrderSource(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        previewRequest.setChannel(order.getChannel());
        previewRequest.setItems(previewItems);
        previewRequest.setClientPayableAmount(order.getPayableAmount());
        previewRequest.setRemark(order.getRemark());
        previewRequest.setSessionId(null);
        previewRequest.setSessionVersion(null);

        log.info("User reorder build preview request, tenantId={}, userId={}, orderId={}, itemCount={}",
                tenantId, userId, orderId, previewItems.size());

        return userOrderPreviewAppService.preview(previewRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserOrderSummaryView> listUserOrders(UserOrderListQuery query) {
        Long tenantId = query.getTenantId();
        Long userId = query.getUserId();
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

        long total = orderRepository.countUserOrders(tenantId, userId, statusList, fromTime, toTime);
        List<Order> orders = total > 0
                ? orderRepository.findUserOrders(tenantId, userId, statusList, fromTime, toTime, offset, pageSize)
                : Collections.emptyList();

        List<UserOrderSummaryView> list = orders.stream()
                .map(this::toUserOrderSummaryView)
                .collect(Collectors.toList());

        PageResult<UserOrderSummaryView> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public UserOrderDetailView getUserOrderDetail(Long tenantId, Long userId, Long orderId) {
        if (tenantId == null || userId == null || orderId == null) {
            throw new IllegalArgumentException("tenantId/userId/orderId 不能为空");
        }
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            log.warn("User get order detail but not found, tenantId={}, userId={}, orderId={}",
                    tenantId, userId, orderId);
            throw new IllegalStateException("订单不存在");
        }
        if (!userId.equals(order.getUserId())) {
            log.warn("User get order detail but not owner, tenantId={}, userId={}, orderUserId={}, orderId={}",
                    tenantId, userId, order.getUserId(), orderId);
            throw new IllegalStateException("无权查看该订单");
        }
        if (order.isUserDeleted()) {
            log.info("User get order detail but order is userDeleted, tenantId={}, userId={}, orderId={}",
                    tenantId, userId, orderId);
            throw new IllegalStateException("订单不存在或已删除");
        }
        return toUserOrderDetailView(order);
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

    private UserOrderSummaryView toUserOrderSummaryView(Order order) {
        UserOrderSummaryView view = new UserOrderSummaryView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setCreatedAt(order.getCreatedAt());
        view.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        view.setPayStatus(order.getPayStatus() != null ? order.getPayStatus().getCode() : null);
        view.setPayableAmount(order.getPayableAmount());
        view.setCurrency(order.getCurrency());
        view.setOrderSource(order.getOrderSource() != null ? order.getOrderSource().getCode() : null);
        view.setStoreName(null);
        view.setUserDeleted(order.isUserDeleted());
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

    private UserOrderDetailView toUserOrderDetailView(Order order) {
        UserOrderDetailView view = new UserOrderDetailView();
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
        view.setStoreName(null);
        view.setStoreAddress(null);
        if (order.getPayStatus() != null) {
            view.setRefundStatus(order.getPayStatus().getCode());
        }
        view.setRefundedAmount(null);
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            List<UserOrderDetailItemView> itemViews = order.getItems().stream()
                    .map(this::toUserOrderDetailItemView)
                    .collect(Collectors.toList());
            view.setItems(itemViews);
        } else {
            view.setItems(Collections.emptyList());
        }
        return view;
    }

    private UserOrderDetailItemView toUserOrderDetailItemView(OrderItem item) {
        UserOrderDetailItemView view = new UserOrderDetailItemView();
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
