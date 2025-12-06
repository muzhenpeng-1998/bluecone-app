package com.bluecone.app.payment.domain.reconcile;

import java.time.LocalDate;
import java.util.List;

public interface ReconcileResultRepository {

    void saveAll(List<ReconcileResult> results);

    List<ReconcileResult> findByChannelAndBillDate(String channelCode, LocalDate billDate);

    void deleteByChannelAndBillDate(String channelCode, LocalDate billDate);
}
