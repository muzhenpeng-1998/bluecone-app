package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;

/**
 * 商户命令用例：封装商户端的各种订单操作（当前仅实现接单）。
 */
public interface MerchantOrderCommandAppService {

    MerchantOrderView acceptOrder(MerchantAcceptOrderCommand command);
}
