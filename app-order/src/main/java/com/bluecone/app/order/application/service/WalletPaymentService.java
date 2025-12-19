package com.bluecone.app.order.application.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 钱包支付服务
 * 处理钱包余额支付的完整流程：提交冻结、标记订单已支付
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletPaymentService {
    
    private final WalletAssetFacade walletAssetFacade;
    private final OrderRepository orderRepository;
    
    /**
     * 钱包余额支付（提交冻结并标记订单已支付）
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>提交冻结余额（commit freeze）</li>
     *   <li>标记订单为已支付（WAIT_PAY -> PAID）</li>
     * </ol>
     * 
     * <h4>幂等性：</h4>
     * <ul>
     *   <li>通过幂等键保证重复调用不会重复扣款</li>
     *   <li>订单状态变更也是幂等的（已支付时直接返回）</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @return 支付是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean payWithWallet(Long tenantId, Long userId, Long orderId) {
        // 查询订单
        Order order = orderRepository.findById(tenantId, orderId);
        if (order == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单不存在");
        }
        
        // 幂等性检查：如果订单已支付，直接返回成功
        if (com.bluecone.app.order.domain.enums.OrderStatus.PAID.equals(order.getStatus()) ||
            com.bluecone.app.order.domain.enums.OrderStatus.WAIT_ACCEPT.equals(order.getStatus())) {
            log.info("订单已支付，幂等返回：orderId={}, status={}", orderId, order.getStatus());
            return true;
        }
        
        // 状态检查：只允许 WAIT_PAY 状态的订单支付
        if (!com.bluecone.app.order.domain.enums.OrderStatus.WAIT_PAY.equals(order.getStatus())) {
            log.warn("订单状态不允许支付：orderId={}, status={}", orderId, order.getStatus());
            throw new BizException(CommonErrorCode.BAD_REQUEST, 
                    "订单状态不允许支付：" + order.getStatus());
        }
        
        // 提交冻结余额（commit freeze）
        try {
            // 构造幂等键：{tenantId}:{userId}:{orderId}:commit
            String idempotencyKey = String.format("%d:%d:%d:commit", tenantId, userId, orderId);
            
            WalletAssetCommand commitCommand = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(order.getPayableAmount())
                    .bizType("ORDER_PAY")
                    .bizOrderId(orderId)
                    .bizOrderNo(order.getOrderNo())
                    .idempotencyKey(idempotencyKey)
                    .operatorId(userId)
                    .remark("订单支付扣款")
                    .build();
            
            WalletAssetResult result = walletAssetFacade.commit(commitCommand);
            if (!result.isSuccess()) {
                log.error("提交冻结余额失败：orderId={}, userId={}, error={}", 
                        orderId, userId, result.getErrorMessage());
                throw new BizException(CommonErrorCode.BAD_REQUEST, 
                        "钱包支付失败：" + result.getErrorMessage());
            }
            
            log.info("提交冻结余额成功：orderId={}, userId={}, ledgerNo={}, idempotent={}", 
                    orderId, userId, result.getLedgerNo(), result.isIdempotent());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("提交冻结余额异常：orderId={}, userId={}", orderId, userId, e);
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "钱包支付失败");
        }
        
        // 标记订单为已支付
        LocalDateTime now = LocalDateTime.now();
        order.markPaid(null, "WALLET", null, now);
        orderRepository.update(order);
        
        log.info("钱包余额支付成功：orderId={}, userId={}, amount={}", 
                orderId, userId, order.getPayableAmount());
        
        return true;
    }
    
    /**
     * 释放冻结余额（取消订单/超时）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param orderId 订单ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseWalletFreeze(Long tenantId, Long userId, Long orderId) {
        try {
            // 构造幂等键：{tenantId}:{userId}:{orderId}:release
            String idempotencyKey = String.format("%d:%d:%d:release", tenantId, userId, orderId);
            
            WalletAssetCommand releaseCommand = new WalletAssetCommand();
            releaseCommand.setTenantId(tenantId);
            releaseCommand.setUserId(userId);
            releaseCommand.setBizOrderId(orderId);
            releaseCommand.setIdempotencyKey(idempotencyKey);
            releaseCommand.setOperatorId(userId);
            
            WalletAssetResult result = walletAssetFacade.release(releaseCommand);
            if (!result.isSuccess()) {
                log.warn("释放冻结余额失败：orderId={}, userId={}, error={}", 
                        orderId, userId, result.getErrorMessage());
                // 释放失败不抛异常，只记录日志（可能冻结记录不存在或已释放）
                return;
            }
            
            log.info("释放冻结余额成功：orderId={}, userId={}", orderId, userId);
        } catch (Exception e) {
            log.error("释放冻结余额异常：orderId={}, userId={}", orderId, userId, e);
            // 释放失败不抛异常，避免影响订单取消流程
        }
    }
    
    /**
     * 回退余额变更（退款返还）
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param orderId 订单ID
     * @param refundAmount 退款金额
     * @param orderNo 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    public void revertWalletPayment(Long tenantId, Long userId, Long orderId, 
                                    java.math.BigDecimal refundAmount, String orderNo) {
        try {
            // 构造幂等键：{tenantId}:{userId}:{orderId}:refund
            String idempotencyKey = String.format("%d:%d:%d:refund", tenantId, userId, orderId);
            
            WalletAssetCommand revertCommand = WalletAssetCommand.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .amount(refundAmount)
                    .bizType("REFUND")
                    .bizOrderId(orderId)
                    .bizOrderNo(orderNo)
                    .idempotencyKey(idempotencyKey)
                    .operatorId(userId)
                    .remark("订单退款返还")
                    .build();
            
            WalletAssetResult result = walletAssetFacade.revert(revertCommand);
            if (!result.isSuccess()) {
                log.error("回退余额变更失败：orderId={}, userId={}, amount={}, error={}", 
                        orderId, userId, refundAmount, result.getErrorMessage());
                throw new BizException(CommonErrorCode.BAD_REQUEST, 
                        "退款失败：" + result.getErrorMessage());
            }
            
            log.info("回退余额变更成功：orderId={}, userId={}, amount={}, ledgerNo={}, idempotent={}", 
                    orderId, userId, refundAmount, result.getLedgerNo(), result.isIdempotent());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("回退余额变更异常：orderId={}, userId={}, amount={}", 
                    orderId, userId, refundAmount, e);
            throw new BizException(CommonErrorCode.SYSTEM_ERROR, "退款失败");
        }
    }
}
