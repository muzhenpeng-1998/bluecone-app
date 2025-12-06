package com.bluecone.app.payment.infrastructure.repository.reconcile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecord;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecordRepository;
import com.bluecone.app.payment.domain.reconcile.ChannelTradeType;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.ChannelBillRecordDO;
import com.bluecone.app.payment.infrastructure.persistence.reconcile.ChannelBillRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class ChannelBillRecordRepositoryImpl implements ChannelBillRecordRepository {

    private final ChannelBillRecordMapper mapper;

    public ChannelBillRecordRepositoryImpl(ChannelBillRecordMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<ChannelBillRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (ChannelBillRecord record : records) {
            mapper.insert(toDO(record));
        }
    }

    @Override
    public List<ChannelBillRecord> findByChannelAndBillDate(String channelCode, java.time.LocalDate billDate) {
        LambdaQueryWrapper<ChannelBillRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelBillRecordDO::getChannelCode, channelCode)
                .eq(ChannelBillRecordDO::getBillDate, billDate);
        return mapper.selectList(wrapper).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteByChannelAndBillDate(String channelCode, java.time.LocalDate billDate) {
        LambdaQueryWrapper<ChannelBillRecordDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChannelBillRecordDO::getChannelCode, channelCode)
                .eq(ChannelBillRecordDO::getBillDate, billDate);
        mapper.delete(wrapper);
    }

    private ChannelBillRecordDO toDO(ChannelBillRecord record) {
        ChannelBillRecordDO doObj = new ChannelBillRecordDO();
        doObj.setId(record.getId());
        doObj.setChannelCode(record.getChannelCode());
        doObj.setBillDate(record.getBillDate());
        doObj.setTradeType(record.getTradeType() == null ? null : record.getTradeType().name());
        doObj.setChannelTradeNo(record.getChannelTradeNo());
        doObj.setMerchantOrderNo(record.getMerchantOrderNo());
        doObj.setMerchantRefundNo(record.getMerchantRefundNo());
        doObj.setAmount(record.getAmount());
        doObj.setCurrency(record.getCurrency());
        doObj.setTradeTime(record.getTradeTime());
        doObj.setFee(record.getFee());
        doObj.setRawLine(record.getRawLine());
        doObj.setCreatedAt(record.getCreatedAt());
        doObj.setUpdatedAt(record.getUpdatedAt());
        return doObj;
    }

    private ChannelBillRecord toDomain(ChannelBillRecordDO doObj) {
        if (doObj == null) {
            return null;
        }
        ChannelBillRecord record = new ChannelBillRecord();
        record.setId(doObj.getId());
        record.setChannelCode(doObj.getChannelCode());
        record.setBillDate(doObj.getBillDate());
        record.setTradeType(doObj.getTradeType() == null ? null : ChannelTradeType.valueOf(doObj.getTradeType()));
        record.setChannelTradeNo(doObj.getChannelTradeNo());
        record.setMerchantOrderNo(doObj.getMerchantOrderNo());
        record.setMerchantRefundNo(doObj.getMerchantRefundNo());
        record.setAmount(doObj.getAmount());
        record.setCurrency(doObj.getCurrency());
        record.setTradeTime(doObj.getTradeTime());
        record.setFee(doObj.getFee());
        record.setRawLine(doObj.getRawLine());
        record.setCreatedAt(doObj.getCreatedAt());
        record.setUpdatedAt(doObj.getUpdatedAt());
        return record;
    }
}
