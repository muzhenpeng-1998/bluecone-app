package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.command.CompleteOrderCommand;
import com.bluecone.app.order.application.command.MarkReadyCommand;
import com.bluecone.app.order.application.command.StartOrderCommand;

/**
 * 商户履约流转用例：封装商户端的履约操作（开始制作、出餐完成、订单完成）。
 * 
 * <h3>职责：</h3>
 * <ul>
 *   <li>开始制作：ACCEPTED → IN_PROGRESS</li>
 *   <li>出餐完成：IN_PROGRESS → READY</li>
 *   <li>订单完成：READY → COMPLETED</li>
 * </ul>
 * 
 * <h3>核心保障：</h3>
 * <ul>
 *   <li>幂等性：通过 requestId 保证同一请求只执行一次</li>
 *   <li>并发保护：通过 expectedVersion（乐观锁）防止并发冲突</li>
 *   <li>状态约束：通过聚合根方法保证状态流转正确</li>
 *   <li>审计追溯：通过 bc_order_action_log 记录每次操作的操作人、时间、结果</li>
 * </ul>
 */
public interface MerchantFulfillmentAppService {

    /**
     * 商户开始制作订单。
     * 
     * <h4>状态流转：</h4>
     * <p>ACCEPTED（已接单） → IN_PROGRESS（制作中）</p>
     * 
     * <h4>幂等性：</h4>
     * <p>同一 requestId 重复调用，返回已有结果，不产生副作用。</p>
     * 
     * <h4>并发保护：</h4>
     * <p>如果传了 expectedVersion，会校验订单版本号，版本冲突时抛出异常。</p>
     * 
     * @param command 开始制作命令
     * @return 订单视图
     */
    MerchantOrderView startOrder(StartOrderCommand command);

    /**
     * 商户标记订单出餐完成。
     * 
     * <h4>状态流转：</h4>
     * <p>IN_PROGRESS（制作中） → READY（已出餐/待取货）</p>
     * 
     * <h4>幂等性：</h4>
     * <p>同一 requestId 重复调用，返回已有结果，不产生副作用。</p>
     * 
     * <h4>并发保护：</h4>
     * <p>如果传了 expectedVersion，会校验订单版本号，版本冲突时抛出异常。</p>
     * 
     * @param command 出餐完成命令
     * @return 订单视图
     */
    MerchantOrderView markReady(MarkReadyCommand command);

    /**
     * 标记订单完成。
     * 
     * <h4>状态流转：</h4>
     * <p>READY（已出餐） → COMPLETED（已完成）</p>
     * 
     * <h4>幂等性：</h4>
     * <p>同一 requestId 重复调用，返回已有结果，不产生副作用。</p>
     * 
     * <h4>并发保护：</h4>
     * <p>如果传了 expectedVersion，会校验订单版本号，版本冲突时抛出异常。</p>
     * 
     * @param command 完成订单命令
     * @return 订单视图
     */
    MerchantOrderView completeOrder(CompleteOrderCommand command);
}
