package com.bluecone.app.order.application;

import com.bluecone.app.order.application.command.CancelOrderCommand;

/**
 * 订单取消应用服务接口。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>用户/系统取消订单</li>
 *   <li>支持幂等性（同一 requestId 重复取消只执行一次）</li>
 *   <li>支持并发控制（乐观锁版本号）</li>
 *   <li>自动触发退款（已支付订单取消时）</li>
 * </ul>
 */
public interface OrderCancelAppService {
    
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
     * <h4>幂等性：</h4>
     * <ul>
     *   <li>通过 idemKey（{tenantId}:{storeId}:{orderId}:cancel:{requestId}）保证幂等</li>
     *   <li>同一 requestId 重复取消只执行一次</li>
     *   <li>已取消的订单再次取消直接返回（不报错）</li>
     * </ul>
     * 
     * <h4>并发控制：</h4>
     * <ul>
     *   <li>通过乐观锁（expectedVersion）保证并发安全</li>
     *   <li>版本号不匹配时抛出异常（提示用户刷新）</li>
     * </ul>
     * 
     * @param command 取消订单命令
     */
    void cancelOrder(CancelOrderCommand command);
}
