package com.bluecone.app.payment.infrastructure.repository.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.reconcile.ReconcileDiffType;
import com.bluecone.app.payment.domain.reconcile.ReconcileResult;
import com.bluecone.app.payment.domain.reconcile.ReconcileResultRepository;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.ReconcileResultDO;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.ReconcileResultMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ReconcileResultRepositoryImpl implements ReconcileResultRepository {

    private final ReconcileResultMapper mapper;

    public ReconcileResultRepositoryImpl(ReconcileResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<ReconcileResult> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        for (ReconcileResult result : results) {
            mapper.insert(toDO(result));
        }
    }

    @Override
    public List<ReconcileResult> findByChannelAndBillDate(String channelCode, java.time.LocalDate billDate) {
        LambdaQueryWrapper<ReconcileResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileResultDO::getChannelCode, channelCode)
                .eq(ReconcileResultDO::getBillDate, billDate);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByChannelAndBillDate(String channelCode, java.time.LocalDate billDate) {
        LambdaQueryWrapper<ReconcileResultDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ReconcileResultDO::getChannelCode, channelCode)
                .eq(ReconcileResultDO::getBillDate, billDate);
        mapper.delete(wrapper);
    }

    private ReconcileResultDO toDO(ReconcileResult result) {
        ReconcileResultDO doObj = new ReconcileResultDO();
        doObj.setId(result.getId());
        doObj.setChannelCode(result.getChannelCode());
        doObj.setBillDate(result.getBillDate());
        doObj.setDiffType(result.getDiffType() == null ? null : result.getDiffType().name());
        doObj.setChannelTradeNo(result.getChannelTradeNo());
        doObj.setMerchantOrderNo(result.getMerchantOrderNo());
        doObj.setMerchantRefundNo(result.getMerchantRefundNo());
        doObj.setPaymentOrderId(result.getPaymentOrderId());
        doObj.setRefundOrderId(result.getRefundOrderId());
        doObj.setAmountDiff(result.getAmountDiff());
        doObj.setStatusDiff(result.getStatusDiff());
        doObj.setRemark(result.getRemark());
        doObj.setCreatedAt(result.getCreatedAt());
        doObj.setUpdatedAt(result.getUpdatedAt());
        return doObj;
    }

    private ReconcileResult toDomain(ReconcileResultDO doObj) {
        if (doObj == null) {
            return null;
        }
        ReconcileResult result = new ReconcileResult();
        result.setId(doObj.getId());
        result.setChannelCode(doObj.getChannelCode());
        result.setBillDate(doObj.getBillDate());
        result.setDiffType(doObj.getDiffType() == null ? null : ReconcileDiffType.valueOf(doObj.getDiffType()));
        result.setChannelTradeNo(doObj.getChannelTradeNo());
        result.setMerchantOrderNo(doObj.getMerchantOrderNo());
        result.setMerchantRefundNo(doObj.getMerchantRefundNo());
        result.setPaymentOrderId(doObj.getPaymentOrderId());
        result.setRefundOrderId(doObj.getRefundOrderId());
        result.setAmountDiff(doObj.getAmountDiff());
        result.setStatusDiff(doObj.getStatusDiff());
        result.setRemark(doObj.getRemark());
        result.setCreatedAt(doObj.getCreatedAt());
        result.setUpdatedAt(doObj.getUpdatedAt());
        return result;
    }
}
