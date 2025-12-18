package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.order.application.OrderPreCheckService;
import com.bluecone.app.order.domain.error.OrderErrorCode;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 订单前置校验服务实现。
 * <p>通过 StoreFacade 调用门店能力，保持模块边界清晰，订单侧不直接依赖门店表结构。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderPreCheckServiceImpl implements OrderPreCheckService {

    private final StoreFacade storeFacade;

    /**
     * 订单提交前置校验。
     * <p>调用门店侧的可接单校验，如果不可接单则抛出 BizException，异常信息中携带门店返回的 reasonCode 和 detail。</p>
     *
     * @param tenantId     租户 ID
     * @param storeId      门店 ID
     * @param channelType  渠道类型（可选）
     * @param now          当前时间（可选，默认使用当前系统时间）
     * @param cartSummary  购物车摘要（预留扩展）
     * @throws BizException 当门店不可接单时抛出，异常中携带 reasonCode（来自门店侧的 StoreErrorCode）
     */
    @Override
    public void preCheck(Long tenantId, Long storeId, String channelType, LocalDateTime now, CartSummary cartSummary) {
        if (tenantId == null || storeId == null) {
            throw new BizException(OrderErrorCode.ORDER_INVALID, "租户ID和门店ID不能为空");
        }

        LocalDateTime checkTime = now != null ? now : LocalDateTime.now();

        // 调用门店侧的可接单校验
        // 注意：capability 参数暂时不传，后续可根据业务需要扩展
        StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(tenantId, storeId, null, checkTime, channelType);

        // 如果不可接单，抛出异常，携带门店返回的 reasonCode 和 detail
        // 说明：reasonCode 来自 StoreErrorCode，可用于前端提示和埋点统计
        if (!result.isAcceptable()) {
            String detail = result.getDetail() != null ? result.getDetail() : result.getReasonMessage();
            log.warn("门店前置校验失败：tenantId={}, storeId={}, channelType={}, reasonCode={}, detail={}",
                    tenantId, storeId, channelType, result.getReasonCode(), detail);
            // 使用统一的订单错误码，但携带门店返回的 reasonCode 作为 detail 的一部分，便于前端和埋点使用
            throw new BizException(OrderErrorCode.STORE_NOT_ACCEPTABLE,
                    String.format("门店当前不可接单：%s（原因码：%s）", result.getReasonMessage(), result.getReasonCode()));
        }

        log.debug("门店前置校验通过：tenantId={}, storeId={}, channelType={}", tenantId, storeId, channelType);
    }
}
