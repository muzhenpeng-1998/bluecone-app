package com.bluecone.app.order.application;

import com.bluecone.app.order.application.command.ApplyRefundCommand;

/**
 * 退款应用服务接口。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>申请退款（创建退款单并发起退款请求）</li>
 *   <li>处理退款回调（更新退款单状态并推进订单状态）</li>
 *   <li>支持幂等性（同一 requestId 重复申请只创建一个退款单）</li>
 *   <li>支持并发控制（乐观锁版本号）</li>
 * </ul>
 */
public interface RefundAppService {
    
    /**
     * 申请退款。
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>幂等性检查：根据 idemKey 查询是否已存在退款单</li>
     *   <li>创建退款单：状态为 INIT/PROCESSING</li>
     *   <li>调用支付网关：发起退款请求（M4 使用 Mock 实现）</li>
     *   <li>更新退款单：根据支付网关响应更新状态</li>
     *   <li>推进订单：退款成功后推进订单为 REFUNDED</li>
     * </ol>
     * 
     * <h4>幂等性：</h4>
     * <ul>
     *   <li>通过 idemKey（{tenantId}:{storeId}:{orderId}:refund:{requestId}）保证幂等</li>
     *   <li>同一 requestId 重复申请只创建一个退款单</li>
     *   <li>已存在的退款单直接返回（不报错）</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>通过乐观锁（expectedVersion）保证并发安全</li>
     *   <li>版本号不匹配时抛出异常（提示用户刷新）</li>
     * </ul>
     * 
     * @param command 申请退款命令
     */
    void applyRefund(ApplyRefundCommand command);
    
    /**
     * 处理退款回调通知。
     * 
     * <h4>业务流程：</h4>
     * <ol>
     *   <li>幂等性检查：根据 notifyId 查询是否已处理</li>
     *   <li>解析回调报文：提取退款单号和退款状态</li>
     *   <li>更新退款单：标记为 SUCCESS/FAILED</li>
     *   <li>推进订单：退款成功后推进订单为 REFUNDED（使用乐观锁）</li>
     *   <li>记录回调日志：保存原始报文和处理结果</li>
     * </ol>
     * 
     * <h4>幂等性：</h4>
     * <ul>
     *   <li>通过 notifyId（唯一）保证幂等</li>
     *   <li>同一 notifyId 重复回调只处理一次</li>
     *   <li>已处理的回调直接返回（不报错）</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>通过乐观锁（version）保证并发安全</li>
     *   <li>退款单和订单都使用乐观锁更新</li>
     * </ul>
     * 
     * @param notifyId 通知ID（幂等键）
     * @param refundId 退款单号
     * @param refundNo 第三方退款单号
     * @param success 是否成功
     * @param errorMsg 失败原因（成功时为 null）
     */
    void onRefundNotify(String notifyId, String refundId, String refundNo, boolean success, String errorMsg);
}
