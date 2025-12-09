package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewRequest;
import com.bluecone.app.order.api.dto.ConfirmOrderPreviewResponse;
import com.bluecone.app.order.application.UserOrderPreviewAppService;
import com.bluecone.app.order.application.service.OrderPricingService;
import com.bluecone.app.order.application.service.OrderPricingService.PricingResult;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserOrderPreviewAppServiceImpl implements UserOrderPreviewAppService {

    private final OrderPricingService orderPricingService;

    @Override
    public ConfirmOrderPreviewResponse preview(ConfirmOrderPreviewRequest previewRequest) {
        validatePreviewRequest(previewRequest);
        List<ConfirmOrderItemDTO> items = previewRequest.getItems();
        PricingResult pricing = orderPricingService.priceItems(
                previewRequest.getTenantId(), previewRequest.getStoreId(), items);
        BigDecimal totalAmount = OrderPricingService.toDecimal(pricing.getTotalAmountCents());
        BigDecimal discountAmount = BigDecimal.ZERO;
        BigDecimal payableAmount = totalAmount.subtract(discountAmount);
        if (payableAmount.compareTo(BigDecimal.ZERO) < 0) {
            payableAmount = BigDecimal.ZERO;
        }
        ConfirmOrderPreviewResponse response = new ConfirmOrderPreviewResponse();
        response.setCanPlaceOrder(true);
        response.setTotalAmount(totalAmount);
        response.setDiscountAmount(discountAmount);
        response.setPayableAmount(payableAmount);
        response.setCurrency("CNY");
        response.setExpectedReadyTimeSeconds(600);
        response.setStoreOpenStatus("UNKNOWN");
        response.setMessage("当前为小程序 MVP 预览，后续由商品模块提供更多信息");
        response.setSessionVersion(previewRequest.getSessionVersion());
        response.setExt(previewRequest.getExt());
        return response;
    }

    private void validatePreviewRequest(ConfirmOrderPreviewRequest previewRequest) {
        if (previewRequest == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "订单预览请求不能为空");
        }
        if (previewRequest.getTenantId() == null || previewRequest.getStoreId() == null || previewRequest.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "tenantId/storeId/userId 必填");
        }
        if (!StringUtils.hasText(previewRequest.getBizType())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "bizType 不能为空");
        }
        if (!StringUtils.hasText(previewRequest.getOrderSource())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "orderSource 不能为空");
        }
        if (!StringUtils.hasText(previewRequest.getChannel())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "channel 不能为空");
        }
        if (CollectionUtils.isEmpty(previewRequest.getItems())) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "明细不能为空");
        }
        for (ConfirmOrderItemDTO item : previewRequest.getItems()) {
            if (item == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "每条明细数量必须大于 0");
            }
            if (item.getSkuId() == null) {
                throw new BizException(CommonErrorCode.BAD_REQUEST, "SKU ID 必填");
            }
        }
    }
}
