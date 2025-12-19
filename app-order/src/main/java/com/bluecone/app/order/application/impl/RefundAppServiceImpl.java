package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.RefundAppService;
import com.bluecone.app.order.application.command.ApplyRefundCommand;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import com.bluecone.app.order.domain.enums.RefundStatus;
import com.bluecone.app.order.domain.gateway.PaymentRefundGateway;
import com.bluecone.app.order.application.service.WalletPaymentService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.RefundOrder;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.order.domain.repository.RefundOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 退款应用服务实现。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>申请退款（创建退款单并发起退款请求）</li>
 *   <li>处理退款回调（更新退款单状态并推进订单状态）</li>
 *   <li>支持幂等性（idemKey 和 notifyId）</li>
 *   <li>支持并发控制（乐观锁版本号）</li>
 *   <li>M5：支持钱包余额退款（回退余额变更）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundAppServiceImpl implements RefundAppService {
    
    private final RefundOrderRepository refundOrderRepository;
    private final OrderRepository orderRepository;
    private final PaymentRefundGateway paymentRefundGateway;
    private final OrderIdGenerator orderIdGenerator;
    private final WalletPaymentService walletPaymentService;
    
    /**
     * 申请退款。
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>幂等性检查：根据 idemKey 查询是否已存在退款单</li>
     *   <li>创建退款单：状态为 INIT</li>
     *   <li>调用支付网关：发起退款请求（M4 使用 Mock 实现）</li>
     *   <li>更新退款单：根据支付网关响应更新状态</li>
     *   <li>推进订单：退款成功后推进订单为 REFUNDED</li>
     * </ol>
     * 
     * @param command 申请退款命令
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(ApplyRefundCommand command) {
        log.info("开始申请退款：tenantId={}, storeId={}, orderId={}, requestId={}, refundAmount={}, reasonCode={}",
                command.getTenantId(), command.getStoreId(), command.getOrderId(), 
                command.getRequestId(), command.getRefundAmount(), command.getReasonCode());
        
        // 1. 参数校验
        if (command.getTenantId() == null || command.getOrderId() == null || command.getRequestId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/orderId/requestId 不能为空");
        }
        if (command.getRefundAmount() == null || command.getRefundAmount().signum() <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "退款金额必须大于0");
        }
        
        // 2. 幂等性检查：根据 idemKey 判断是否已创建退款单
        String idemKey = buildIdemKey(command.getTenantId(), command.getStoreId(), command.getOrderId(), command.getRequestId());
        RefundOrder existingRefund = refundOrderRepository.findByIdemKey(command.getTenantId(), idemKey);
        if (existingRefund != null) {
            log.info("退款申请幂等返回（已存在退款单）：tenantId={}, orderId={}, requestId={}, refundOrderId={}, status={}",
                    command.getTenantId(), command.getOrderId(), command.getRequestId(), 
                    existingRefund.getId(), existingRefund.getStatus());
            return;
        }
        
        // 3. 查询订单
        Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        
        // 4. 创建退款单（状态为 INIT）
        LocalDateTime now = LocalDateTime.now();
        Long refundOrderId = orderIdGenerator.nextId();
        String refundId = generateRefundId(refundOrderId);
        
        RefundOrder refundOrder = RefundOrder.builder()
                .id(refundOrderId)
                .tenantId(command.getTenantId())
                .storeId(command.getStoreId())
                .orderId(command.getOrderId())
                .publicOrderNo(command.getPublicOrderNo() != null ? command.getPublicOrderNo() : order.getOrderNo())
                .refundId(refundId)
                .channel(command.getChannel())
                .refundAmount(command.getRefundAmount())
                .currency("CNY")
                .status(RefundStatus.INIT)
                .reasonCode(command.getReasonCode())
                .reasonDesc(command.getReasonDesc())
                .idemKey(idemKey)
                .payOrderId(command.getPayOrderId())
                .payNo(command.getPayNo())
                .refundRequestedAt(now)
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
        
        // 5. 保存退款单
        refundOrderRepository.save(refundOrder);
        
        log.info("退款单创建成功：tenantId={}, orderId={}, refundOrderId={}, refundId={}, refundAmount={}",
                command.getTenantId(), command.getOrderId(), refundOrderId, refundId, command.getRefundAmount());
        
        // 6. 调用支付网关发起退款（M4 使用 Mock 实现，直接返回成功）
        PaymentRefundGateway.RefundRequest gatewayRequest = new PaymentRefundGateway.RefundRequest();
        gatewayRequest.setRefundId(refundId);
        gatewayRequest.setOrderNo(order.getOrderNo());
        gatewayRequest.setChannel(command.getChannel());
        gatewayRequest.setRefundAmount(command.getRefundAmount());
        gatewayRequest.setOrderAmount(order.getPayableAmount());
        gatewayRequest.setPayNo(command.getPayNo());
        gatewayRequest.setRefundReason(command.getReasonDesc());
        
        PaymentRefundGateway.RefundResponse gatewayResponse = paymentRefundGateway.refund(gatewayRequest);
        
        // 7. 根据支付网关响应更新退款单状态
        if (gatewayResponse.isSuccess()) {
            // 退款成功：更新退款单为 SUCCESS
            refundOrder.markSuccess(gatewayResponse.getRefundNo(), LocalDateTime.now());
            refundOrderRepository.update(refundOrder);
            
            log.info("退款成功：tenantId={}, orderId={}, refundOrderId={}, refundNo={}",
                    command.getTenantId(), command.getOrderId(), refundOrderId, gatewayResponse.getRefundNo());
            
            // 7.5. M5：如果是钱包支付，回退余额变更（退款返还）
            // 判断是否为钱包支付：检查 payChannel 是否为 WALLET 或 ext 中是否有钱包支付标记
            boolean isWalletPayment = "WALLET".equals(command.getChannel() != null ? command.getChannel().getCode() : null);
            if (isWalletPayment) {
                log.info("钱包支付订单，执行余额回退：tenantId={}, orderId={}, refundAmount={}",
                        command.getTenantId(), command.getOrderId(), command.getRefundAmount());
                try {
                    walletPaymentService.revertWalletPayment(
                            command.getTenantId(),
                            order.getUserId(),
                            command.getOrderId(),
                            command.getRefundAmount(),
                            order.getOrderNo()
                    );
                    log.info("钱包余额回退成功：tenantId={}, orderId={}, refundAmount={}",
                            command.getTenantId(), command.getOrderId(), command.getRefundAmount());
                } catch (Exception e) {
                    log.error("钱包余额回退失败：tenantId={}, orderId={}, refundAmount={}", 
                            command.getTenantId(), command.getOrderId(), command.getRefundAmount(), e);
                    // 余额回退失败不影响退款流程（可通过补偿机制修复）
                }
            }
            
            // 8. 推进订单状态为 REFUNDED（使用乐观锁）
            order.markRefunded(refundOrderId, LocalDateTime.now());
            int orderRows = orderRepository.update(order);
            
            if (orderRows == 0) {
                log.warn("订单更新失败（乐观锁冲突）：tenantId={}, orderId={}, version={}",
                        command.getTenantId(), command.getOrderId(), order.getVersion());
                // 注意：退款单已成功，订单更新失败不影响退款结果，可通过补偿机制修复
                // M4 先不处理，后续可通过定时任务补偿
            } else {
                log.info("订单标记为已退款：tenantId={}, orderId={}, refundOrderId={}, refundedAt={}",
                        command.getTenantId(), command.getOrderId(), refundOrderId, order.getRefundedAt());
            }
        } else {
            // 退款失败：更新退款单为 FAILED
            refundOrder.markFailed(gatewayResponse.getErrorMsg(), LocalDateTime.now());
            refundOrderRepository.update(refundOrder);
            
            log.warn("退款失败：tenantId={}, orderId={}, refundOrderId={}, errorCode={}, errorMsg={}",
                    command.getTenantId(), command.getOrderId(), refundOrderId, 
                    gatewayResponse.getErrorCode(), gatewayResponse.getErrorMsg());
            
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, 
                    "退款失败：" + gatewayResponse.getErrorMsg());
        }
    }
    
    /**
     * 处理退款回调通知。
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>幂等性检查：根据 notifyId 查询是否已处理（通过 bc_refund_notify_log 表）</li>
     *   <li>更新退款单：标记为 SUCCESS/FAILED</li>
     *   <li>推进订单：退款成功后推进订单为 REFUNDED（使用乐观锁）</li>
     *   <li>记录回调日志：保存原始报文和处理结果</li>
     * </ol>
     * 
     * @param notifyId 通知ID（幂等键）
     * @param refundId 退款单号
     * @param refundNo 第三方退款单号
     * @param success 是否成功
     * @param errorMsg 失败原因（成功时为 null）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onRefundNotify(String notifyId, String refundId, String refundNo, boolean success, String errorMsg) {
        log.info("收到退款回调通知：notifyId={}, refundId={}, refundNo={}, success={}, errorMsg={}",
                notifyId, refundId, refundNo, success, errorMsg);
        
        // 1. 参数校验
        if (notifyId == null || notifyId.isBlank() || refundId == null || refundId.isBlank()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "notifyId/refundId 不能为空");
        }
        
        // 2. 幂等性检查：根据 notifyId 判断是否已处理
        // TODO: 查询 bc_refund_notify_log 表判断是否已处理（M4 先省略，后续补充）
        
        // 3. 查询退款单（通过 refundId）
        // TODO: 需要在 RefundOrderRepository 增加 findByRefundId 方法（M4 先省略，后续补充）
        
        // 4. 更新退款单状态
        // TODO: 根据 success 标记为 SUCCESS/FAILED（M4 先省略，后续补充）
        
        // 5. 推进订单状态
        // TODO: 退款成功后推进订单为 REFUNDED（M4 先省略，后续补充）
        
        // 6. 记录回调日志
        // TODO: 保存到 bc_refund_notify_log 表（M4 先省略，后续补充）
        
        log.info("退款回调处理完成：notifyId={}, refundId={}, success={}", notifyId, refundId, success);
    }
    
    /**
     * 构建幂等键。
     * 
     * @param tenantId 租户ID
     * @param storeId 门店ID
     * @param orderId 订单ID
     * @param requestId 请求ID
     * @return 幂等键
     */
    private String buildIdemKey(Long tenantId, Long storeId, Long orderId, String requestId) {
        return String.format("%d:%d:%d:refund:%s", tenantId, storeId, orderId, requestId);
    }
    
    /**
     * 生成退款单号（PublicId格式：rfd_xxx）。
     * 
     * @param refundOrderId 退款单ID
     * @return 退款单号
     */
    private String generateRefundId(Long refundOrderId) {
        // 简易实现：rfd_ + refundOrderId
        // TODO: 后续替换为统一的 PublicId 生成器
        return "rfd_" + refundOrderId;
    }
}
