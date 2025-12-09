package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.MerchantOrderCommandAppService;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import java.util.Objects;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商户操作订单的应用服务实现。
 */
@Service
@RequiredArgsConstructor
public class MerchantOrderCommandAppServiceImpl implements MerchantOrderCommandAppService {

    private final OrderRepository orderRepository;

    /**
     * 商户接单：只允许 WAIT_ACCEPT 状态的订单被接单。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView acceptOrder(MerchantAcceptOrderCommand command) {
        if (command == null || command.getTenantId() == null || command.getOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户/订单ID 不能为空");
        }
        Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        if (!Objects.equals(order.getStoreId(), command.getStoreId())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不属于当前门店");
        }
        order.accept(command.getOperatorId());
        orderRepository.update(order);
        return MerchantOrderView.from(order);
    }
}
