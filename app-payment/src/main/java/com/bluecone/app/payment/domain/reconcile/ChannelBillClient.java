package com.bluecone.app.payment.domain.reconcile;

import java.time.LocalDate;
import java.util.List;

/**
 * 渠道账单获取客户端（可对接文件下载或 SDK），当前为占位实现。
 */
public interface ChannelBillClient {

    boolean supports(String channelCode);

    List<String> fetchBillLines(LocalDate billDate);
}
