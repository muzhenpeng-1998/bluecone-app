package com.bluecone.app.payment.infrastructure.gateway.wechat;

import com.bluecone.app.payment.domain.reconcile.ChannelBillClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 微信账单拉取客户端（占位实现）。
 */
@Component
public class WechatBillClient implements ChannelBillClient {

    private static final Logger log = LoggerFactory.getLogger(WechatBillClient.class);

    @Override
    public boolean supports(String channelCode) {
        return "WECHAT".equalsIgnoreCase(channelCode);
    }

    @Override
    public List<String> fetchBillLines(LocalDate billDate) {
        log.info("[WechatBillClient] fetch stub bill for date={}", billDate);
        return Collections.emptyList();
    }
}
