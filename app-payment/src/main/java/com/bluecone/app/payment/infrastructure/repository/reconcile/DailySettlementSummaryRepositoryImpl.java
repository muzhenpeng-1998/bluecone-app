package com.bluecone.app.payment.infrastructure.repository.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.reconcile.DailySettlementSummary;
import com.bluecone.app.payment.domain.reconcile.DailySettlementSummaryRepository;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.DailySettlementSummaryDO;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.DailySettlementSummaryMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class DailySettlementSummaryRepositoryImpl implements DailySettlementSummaryRepository {

    private final DailySettlementSummaryMapper mapper;

    public DailySettlementSummaryRepositoryImpl(DailySettlementSummaryMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<DailySettlementSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return;
        }
        for (DailySettlementSummary summary : summaries) {
            mapper.insert(toDO(summary));
        }
    }

    @Override
    public List<DailySettlementSummary> findByTenantStoreAndDate(Long tenantId, Long storeId, LocalDate from, LocalDate to) {
        LambdaQueryWrapper<DailySettlementSummaryDO> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) {
            wrapper.eq(DailySettlementSummaryDO::getTenantId, tenantId);
        }
        if (storeId != null) {
            wrapper.eq(DailySettlementSummaryDO::getStoreId, storeId);
        }
        if (from != null) {
            wrapper.ge(DailySettlementSummaryDO::getBillDate, from);
        }
        if (to != null) {
            wrapper.le(DailySettlementSummaryDO::getBillDate, to);
        }
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    private DailySettlementSummaryDO toDO(DailySettlementSummary summary) {
        DailySettlementSummaryDO doObj = new DailySettlementSummaryDO();
        doObj.setId(summary.getId());
        doObj.setTenantId(summary.getTenantId());
        doObj.setStoreId(summary.getStoreId());
        doObj.setChannelCode(summary.getChannelCode());
        doObj.setBillDate(summary.getBillDate());
        doObj.setTotalPaidAmount(summary.getTotalPaidAmount());
        doObj.setTotalRefundedAmount(summary.getTotalRefundedAmount());
        doObj.setNetAmount(summary.getNetAmount());
        doObj.setPayCount(summary.getPayCount());
        doObj.setRefundCount(summary.getRefundCount());
        doObj.setTotalFee(summary.getTotalFee());
        doObj.setGeneratedAt(summary.getGeneratedAt());
        doObj.setCreatedAt(summary.getCreatedAt());
        doObj.setUpdatedAt(summary.getUpdatedAt());
        return doObj;
    }

    private DailySettlementSummary toDomain(DailySettlementSummaryDO doObj) {
        if (doObj == null) {
            return null;
        }
        DailySettlementSummary summary = new DailySettlementSummary();
        summary.setId(doObj.getId());
        summary.setTenantId(doObj.getTenantId());
        summary.setStoreId(doObj.getStoreId());
        summary.setChannelCode(doObj.getChannelCode());
        summary.setBillDate(doObj.getBillDate());
        summary.setTotalPaidAmount(doObj.getTotalPaidAmount());
        summary.setTotalRefundedAmount(doObj.getTotalRefundedAmount());
        summary.setNetAmount(doObj.getNetAmount());
        summary.setPayCount(doObj.getPayCount());
        summary.setRefundCount(doObj.getRefundCount());
        summary.setTotalFee(doObj.getTotalFee());
        summary.setGeneratedAt(doObj.getGeneratedAt());
        summary.setCreatedAt(doObj.getCreatedAt());
        summary.setUpdatedAt(doObj.getUpdatedAt());
        return summary;
    }
}
