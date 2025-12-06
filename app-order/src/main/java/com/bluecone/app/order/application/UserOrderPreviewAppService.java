package com.bluecone.app.order.application;

import com.bluecone.app.order.api.dto.ConfirmOrderPreviewRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;

public interface UserOrderPreviewAppService {

    /**
     * 小程序用户确认订单预览。
     */
    ConfirmOrderPreviewResponse preview(ConfirmOrderPreviewRequest request);
}
