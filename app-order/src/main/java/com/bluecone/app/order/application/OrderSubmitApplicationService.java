package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.OrderSubmitRequest;
import com.bluecone.app.order.api.dto.OrderSubmitResponse;

/**
 * 订单提交单应用服务接口。
 * <p>负责订单提交单的业务编排，包括幂等检查、关键校验、落库等。</p>
 */
public interface OrderSubmitApplicationService {

    /**
     * 订单提交单（M0）。
     * <p>业务流程：</p>
     * <ol>
     *   <li>幂等检查（基于 tenantId + storeId + userId + clientRequestId）</li>
     *   <li>重做关键校验（至少：门店可接单 + 商品有效 + 价格版本一致）</li>
     *   <li>生成 publicOrderNo（对齐公共 ID 治理）</li>
     *   <li>落库订单与明细</li>
     *   <li>返回结果（WAIT_PAY）</li>
     * </ol>
     *
     * @param request 提交单请求
     * @return 提交单响应（包含订单ID、publicOrderNo、状态等）
     */
    OrderSubmitResponse submit(OrderSubmitRequest request);
}
