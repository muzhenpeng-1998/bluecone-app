package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.OrderCancelAppService;
import com.bluecone.app.order.application.RefundAppService;
import com.bluecone.app.order.application.command.ApplyRefundCommand;
import com.bluecone.app.order.application.command.CancelOrderCommand;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.enums.PayStatus;
import com.bluecone.app.order.domain.enums.RefundChannel;
import com.bluecone.app.order.application.service.WalletPaymentService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.domain.repository.OrderActionLogRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 订单取消应用服务实现。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>用户/系统取消订单</li>
 *   <li>支持幂等性（action_log 或 idem_key）</li>
 *   <li>支持并发控制（乐观锁版本号）</li>
 *   <li>自动触发退款（已支付订单取消时）</li>
 *   <li>M5：自动释放钱包冻结余额（钱包支付订单取消时）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCancelAppServiceImpl implements OrderCancelAppService {
    
    private final OrderRepository orderRepository;
    private final OrderActionLogRepository orderActionLogRepository;
    private final RefundAppService refundAppService;
    private final WalletPaymentService walletPaymentService;
    
    /**
     * 取消订单。
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>WAIT_PAY：直接取消（不退款） -> CANCELED</li>
     *   <li>WAIT_ACCEPT：允许取消 -> CANCELED，已支付则触发退款</li>
     *   <li>ACCEPTED：允许取消 -> CANCELED，已支付则触发退款</li>
     *   <li>IN_PROGRESS/READY/COMPLETED：不允许取消（M4 先不支持）</li>
     * </ul>
     * 
     * @param command 取消订单命令
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(CancelOrderCommand command) {
        log.info("开始取消订单：tenantId={}, storeId={}, orderId={}, requestId={}, expectedVersion={}, reasonCode={}",
                command.getTenantId(), command.getStoreId(), command.getOrderId(), 
                command.getRequestId(), command.getExpectedVersion(), command.getReasonCode());
        
        // 1. 参数校验
        if (command.getTenantId() == null || command.getOrderId() == null || command.getRequestId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId/requestId 不能为空");
        }
        
        // 2. 幂等性检查：根据 action_log 判断是否已执行
        String actionKey = buildActionKey(command.getTenantId(), command.getStoreId(), command.getOrderId(), command.getRequestId());
        OrderActionLog existingLog = orderActionLogRepository.findByActionKey(command.getTenantId(), actionKey);
        if (existingLog != null) {
            log.info("订单取消幂等返回（已存在 action_log）：tenantId={}, orderId={}, requestId={}, actionKey={}",
                    command.getTenantId(), command.getOrderId(), command.getRequestId(), actionKey);
            return;
        }
        
        // 3. 查询订单
        Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        
        // 4. 权限校验（用户取消时）
        if (command.getUserId() != null && !command.getUserId().equals(order.getUserId())) {
            log.warn("用户无权取消该订单：tenantId={}, userId={}, orderUserId={}, orderId={}",
                    command.getTenantId(), command.getUserId(), order.getUserId(), command.getOrderId());
            throw new BizException(CommonErrorCode.FORBIDDEN, "无权操作该订单");
        }
        
        // 5. 幂等性：如果订单已取消，直接返回
        OrderStatus canonical = order.getStatus() != null ? order.getStatus().normalize() : null;
        if (OrderStatus.CANCELED.equals(canonical)) {
            log.info("订单已取消，幂等返回：tenantId={}, orderId={}, status={}, canceledAt={}",
                    command.getTenantId(), command.getOrderId(), order.getStatus(), order.getCanceledAt());
            
            // 记录 action_log（避免重复执行）
            saveActionLog(command, order, "CANCELED_ALREADY");
            return;
        }
        
        // 6. 乐观锁检查
        if (command.getExpectedVersion() != null && !command.getExpectedVersion().equals(order.getVersion())) {
            log.warn("订单版本号不匹配（乐观锁冲突）：tenantId={}, orderId={}, expectedVersion={}, actualVersion={}",
                    command.getTenantId(), command.getOrderId(), command.getExpectedVersion(), order.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "订单状态已变更，请刷新后重试");
        }
        
        // 7. 状态约束检查
        if (!order.getStatus().canCancel()) {
            String msg = String.format("订单状态不允许取消：当前状态=%s", order.getStatus().getCode());
            log.warn("订单状态不允许取消：tenantId={}, orderId={}, status={}",
                    command.getTenantId(), command.getOrderId(), order.getStatus());
            throw new BizException(CommonErrorCode.BAD_REQUEST, msg);
        }
        
        // 8. 取消订单（调用领域模型方法）
        LocalDateTime now = LocalDateTime.now();
        order.cancel(command.getReasonCode(), command.getReasonDesc(), now);
        
        // 9. 保存订单（乐观锁更新）
        orderRepository.update(order);
        
        // 10. 记录 action_log
        saveActionLog(command, order, "SUCCESS");
        
        log.info("订单取消成功：tenantId={}, orderId={}, reasonCode={}, canceledAt={}",
                command.getTenantId(), command.getOrderId(), command.getReasonCode(), now);
        
        // 10.5. M5：如果订单未支付，释放钱包冻结余额（如果有）
        if (OrderStatus.WAIT_PAY.equals(order.getStatus().normalize())) {
            log.info("订单未支付，尝试释放钱包冻结余额：tenantId={}, orderId={}",
                    command.getTenantId(), command.getOrderId());
            try {
                walletPaymentService.releaseWalletFreeze(
                        command.getTenantId(), 
                        order.getUserId(), 
                        command.getOrderId()
                );
            } catch (Exception e) {
                log.error("释放钱包冻结余额失败（不影响订单取消）：tenantId={}, orderId={}", 
                        command.getTenantId(), command.getOrderId(), e);
                // 释放失败不影响订单取消流程
            }
        }
        
        // 11. 如果订单已支付，自动触发退款
        if (PayStatus.PAID.equals(order.getPayStatus())) {
            log.info("订单已支付，自动触发退款：tenantId={}, orderId={}, payableAmount={}",
                    command.getTenantId(), command.getOrderId(), order.getPayableAmount());
            
            // 构建退款命令
            ApplyRefundCommand refundCommand = ApplyRefundCommand.builder()
                    .tenantId(command.getTenantId())
                    .storeId(command.getStoreId())
                    .orderId(command.getOrderId())
                    .publicOrderNo(order.getOrderNo())
                    .requestId(command.getRequestId() + "_refund") // 退款 requestId 复用取消 requestId + 后缀
                    .expectedVersion(null) // 退款不需要版本号（退款单是新创建的）
                    .channel(RefundChannel.MOCK) // M4 使用 Mock 渠道
                    .refundAmount(order.getPayableAmount())
                    .reasonCode(command.getReasonCode())
                    .reasonDesc(command.getReasonDesc())
                    .payOrderId(null) // TODO: 从订单扩展字段获取 payOrderId
                    .payNo(null) // TODO: 从订单扩展字段获取 payNo
                    .build();
            
            // 发起退款（同步调用）
            refundAppService.applyRefund(refundCommand);
        } else {
            log.info("订单未支付，无需退款：tenantId={}, orderId={}, payStatus={}",
                    command.getTenantId(), command.getOrderId(), order.getPayStatus());
        }
    }
    
    /**
     * 构建 actionKey（幂等键）。
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param requestId 请求ID
     * @return actionKey
     */
    private String buildActionKey(Long tenantId, Long storeId, Long orderId, String requestId) {
        return OrderActionLog.buildActionKey(tenantId, storeId, orderId, "CANCEL", requestId);
    }
    
    /**
     * 保存 action_log（幂等性记录）。
     * 
     * @param command 取消订单命令
     * @param order 订单聚合根
     * @param resultCode 结果码
     */
    private void saveActionLog(CancelOrderCommand command, Order order, String resultCode) {
        String actionKey = buildActionKey(command.getTenantId(), command.getStoreId(), command.getOrderId(), command.getRequestId());
        
        OrderActionLog actionLog = OrderActionLog.builder()
                .tenantId(command.getTenantId())
                .storeId(command.getStoreId())
                .orderId(command.getOrderId())
                .actionType("CANCEL")
                .actionKey(actionKey)
                .operatorId(command.getUserId())
                .status(resultCode)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        
        orderActionLogRepository.save(actionLog);
        
        log.debug("保存 action_log 成功：tenantId={}, orderId={}, actionKey={}, resultCode={}",
                command.getTenantId(), command.getOrderId(), actionKey, resultCode);
    }
}
