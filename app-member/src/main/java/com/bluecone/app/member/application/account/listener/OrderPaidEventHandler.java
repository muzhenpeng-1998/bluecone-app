package com.bluecone.app.user.application.account.listener;

import com.bluecone.app.core.user.domain.account.service.AccountDomainService;
import com.bluecone.app.core.user.domain.member.service.GrowthDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 订单支付事件处理入口，占位用于发放积分/扣减储值。
 */
@Component
@RequiredArgsConstructor
public class OrderPaidEventHandler {

    private final AccountDomainService accountDomainService;
    private final GrowthDomainService growthDomainService;

    public void onOrderPaid(OrderPaidEvent event) {
        // TODO: 根据租户积分规则计算 deltaPoints，然后调用 changePoints
        // accountDomainService.changePoints(event.getTenantId(), event.getMemberId(), deltaPoints, "ORDER_PAY_REWARD", event.getOrderNo(), "订单支付送积分");
        // TODO: 如订单包含储值支付部分，调用 changeBalance 扣减储值
        // 示例：按支付金额整数部分增加成长值
        int growthDelta = event.getPayAmount() != null ? event.getPayAmount().intValue() : 0;
        if (growthDelta > 0 && event.getMemberId() != null) {
            growthDomainService.increaseGrowthAndCheckLevel(event.getTenantId(), event.getMemberId(), growthDelta, "ORDER_PAY", event.getOrderNo());
        }
    }
}
