package com.bluecone.app.payment.domain.reconcile;

import java.time.LocalDate;
import java.util.List;

public interface ChannelBillRecordRepository {

    void saveAll(List<ChannelBillRecord> records);

    List<ChannelBillRecord> findByChannelAndBillDate(String channelCode, LocalDate billDate);

    void deleteByChannelAndBillDate(String channelCode, LocalDate billDate);
}
