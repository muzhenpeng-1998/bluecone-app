package com.bluecone.app.wallet.api.facade;

import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;

/**
 * 钱包资产操作门面接口
 * 提供钱包资产的冻结、提交、释放、回退等操作能力
 * 所有操作都必须保证幂等性和原子性
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface WalletAssetFacade {
    
    /**
     * 冻结余额（下单锁定）
     * 将可用余额转为冻结状态，等待后续提交或释放
     * 
     * <h4>业务场景：</h4>
     * <ul>
     *   <li>订单下单时（Checkout）：锁定订单金额，防止重复使用</li>
     * </ul>
     * 
     * <h4>幂等性保证：</h4>
     * <ul>
     *   <li>通过 idempotencyKey 保证相同请求只冻结一次</li>
     *   <li>重复调用返回已冻结的结果（idempotent=true）</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>使用乐观锁（version）保证并发安全</li>
     *   <li>并发冻结时，只有一个成功，其他失败重试</li>
     * </ul>
     * 
     * @param command 冻结命令（必须包含幂等键）
     * @return 操作结果
     */
    WalletAssetResult freeze(WalletAssetCommand command);
    
    /**
     * 提交余额变更（支付成功后提交扣减）
     * 将冻结余额实际扣除，并写入账本流水
     * 
     * <h4>业务场景：</h4>
     * <ul>
     *   <li>订单支付成功（Pay）：提交扣减冻结余额</li>
     * </ul>
     * 
     * <h4>前提条件：</h4>
     * <ul>
     *   <li>必须先调用 freeze 冻结余额</li>
     *   <li>冻结记录状态必须是 FROZEN</li>
     * </ul>
     * 
     * <h4>幂等性保证：</h4>
     * <ul>
     *   <li>通过 idempotencyKey 保证相同请求只提交一次</li>
     *   <li>重复调用返回已提交的结果（idempotent=true）</li>
     * </ul>
     * 
     * <h4>账本化要求：</h4>
     * <ul>
     *   <li>必须写入 bc_wallet_ledger 流水表</li>
     *   <li>流水类型：ORDER_PAY（订单支付出账）</li>
     * </ul>
     * 
     * @param command 提交命令（必须包含幂等键）
     * @return 操作结果
     */
    WalletAssetResult commit(WalletAssetCommand command);
    
    /**
     * 释放冻结余额（取消订单/超时）
     * 将冻结余额恢复为可用余额
     * 
     * <h4>业务场景：</h4>
     * <ul>
     *   <li>订单取消（Cancel）：释放冻结余额</li>
     *   <li>订单超时（Timeout）：释放冻结余额</li>
     * </ul>
     * 
     * <h4>前提条件：</h4>
     * <ul>
     *   <li>必须先调用 freeze 冻结余额</li>
     *   <li>冻结记录状态必须是 FROZEN</li>
     * </ul>
     * 
     * <h4>幂等性保证：</h4>
     * <ul>
     *   <li>通过 idempotencyKey 保证相同请求只释放一次</li>
     *   <li>重复调用不报错，直接返回成功</li>
     * </ul>
     * 
     * <h4>账本化说明：</h4>
     * <ul>
     *   <li>释放操作不写入账本流水（只是状态恢复）</li>
     *   <li>如果需要记录，可写入一条"释放日志"（非资金流水）</li>
     * </ul>
     * 
     * @param command 释放命令（必须包含幂等键）
     * @return 操作结果
     */
    WalletAssetResult release(WalletAssetCommand command);
    
    /**
     * 回退余额变更（退款返还）
     * 按照原流水做反向操作，恢复余额
     * 
     * <h4>业务场景：</h4>
     * <ul>
     *   <li>订单退款（Refund）：退款金额返还到账户</li>
     * </ul>
     * 
     * <h4>幂等性保证：</h4>
     * <ul>
     *   <li>通过 idempotencyKey 保证相同请求只回退一次</li>
     *   <li>重复调用返回已回退的结果（idempotent=true）</li>
     * </ul>
     * 
     * <h4>账本化要求：</h4>
     * <ul>
     *   <li>必须写入 bc_wallet_ledger 流水表</li>
     *   <li>流水类型：REFUND（退款入账）</li>
     *   <li>金额为正数（入账）</li>
     * </ul>
     * 
     * @param command 回退命令（必须包含幂等键）
     * @return 操作结果
     */
    WalletAssetResult revert(WalletAssetCommand command);
}
