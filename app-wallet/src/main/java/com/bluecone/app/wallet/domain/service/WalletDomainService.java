package com.bluecone.app.wallet.domain.service;

import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.model.WalletFreeze;
import com.bluecone.app.wallet.domain.model.WalletLedger;

import java.math.BigDecimal;

/**
 * 钱包领域服务接口
 * 提供钱包资产操作的核心业务逻辑
 * 所有操作都必须保证幂等性和账本化
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletDomainService {
    
    /**
     * 冻结余额（下单锁定）
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>检查可用余额是否足够</li>
     *   <li>账户：available -> frozen</li>
     *   <li>写入冻结记录（bc_wallet_freeze）</li>
     *   <li>幂等：重复调用返回已冻结结果</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>使用乐观锁（version）保证账户并发安全</li>
     *   <li>使用唯一约束（idem_key）保证冻结记录幂等</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param amount 冻结金额
     * @param bizType 业务类型（ORDER_CHECKOUT）
     * @param bizOrderId 业务单ID（订单ID）
     * @param bizOrderNo 业务单号（订单号）
     * @param idempotencyKey 幂等键
     * @param operatorId 操作人ID
     * @return 冻结记录
     */
    WalletFreeze freeze(Long tenantId, Long userId, BigDecimal amount,
                       String bizType, Long bizOrderId, String bizOrderNo,
                       String idempotencyKey, Long operatorId);
    
    /**
     * 提交冻结余额（支付成功后扣减）
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>检查冻结记录状态必须是 FROZEN</li>
     *   <li>冻结记录：FROZEN -> COMMITTED</li>
     *   <li>账户：frozen -> 扣减（减少 frozen）</li>
     *   <li>写入账本流水（bc_wallet_ledger）：ORDER_PAY（出账）</li>
     *   <li>幂等：重复调用返回已提交结果</li>
     * </ul>
     * 
     * <h4>账本化要求：</h4>
     * <ul>
     *   <li>流水类型：ORDER_PAY</li>
     *   <li>金额：负数（出账）</li>
     *   <li>记录变更前后余额</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param bizOrderId 业务单ID（订单ID）
     * @param idempotencyKey 幂等键（commit操作的幂等键）
     * @param operatorId 操作人ID
     * @return 账本流水
     */
    WalletLedger commit(Long tenantId, Long userId, Long bizOrderId,
                       String idempotencyKey, Long operatorId);
    
    /**
     * 释放冻结余额（取消订单/超时）
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>检查冻结记录状态必须是 FROZEN</li>
     *   <li>冻结记录：FROZEN -> RELEASED</li>
     *   <li>账户：frozen -> available</li>
     *   <li>不写入账本流水（只是状态恢复，非资金流水）</li>
     *   <li>幂等：重复调用不报错，直接返回</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param bizOrderId 业务单ID（订单ID）
     * @param idempotencyKey 幂等键（release操作的幂等键）
     * @param operatorId 操作人ID
     */
    void release(Long tenantId, Long userId, Long bizOrderId,
                String idempotencyKey, Long operatorId);
    
    /**
     * 回退余额变更（退款返还）
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>账户：available 增加</li>
     *   <li>写入账本流水（bc_wallet_ledger）：REFUND（入账）</li>
     *   <li>幂等：重复调用返回已回退结果</li>
     * </ul>
     * 
     * <h4>账本化要求：</h4>
     * <ul>
     *   <li>流水类型：REFUND</li>
     *   <li>金额：正数（入账）</li>
     *   <li>记录变更前后余额</li>
     * </ul>
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param amount 退款金额
     * @param bizType 业务类型（REFUND）
     * @param bizOrderId 业务单ID（订单ID）
     * @param bizOrderNo 业务单号（订单号）
     * @param idempotencyKey 幂等键
     * @param operatorId 操作人ID
     * @return 账本流水
     */
    WalletLedger revert(Long tenantId, Long userId, BigDecimal amount,
                       String bizType, Long bizOrderId, String bizOrderNo,
                       String idempotencyKey, Long operatorId);
    
    /**
     * 查询或创建钱包账户
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 钱包账户
     */
    WalletAccount getOrCreateAccount(Long tenantId, Long userId);
    
    /**
     * 查询钱包账户
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 钱包账户（不存在返回null）
     */
    WalletAccount getAccount(Long tenantId, Long userId);
}
