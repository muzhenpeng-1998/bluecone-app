package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;
import com.bluecone.app.order.application.command.MerchantRejectOrderCommand;

/**
 * 商户命令用例：封装商户端的各种订单操作（接单、拒单）。
 */
public interface MerchantOrderCommandAppService {

    /**
     * 商户接单。
     * 
     * @param command 接单命令
     * @return 订单视图
     */
    MerchantOrderView acceptOrder(MerchantAcceptOrderCommand command);

    /**
     * 商户拒单。
     * 
     * @param command 拒单命令
     * @return 订单视图
     */
    MerchantOrderView rejectOrder(MerchantRejectOrderCommand command);
}
