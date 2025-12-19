package com.bluecone.app.payment.application.reconcile;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.payment.domain.reconcile.ChannelBillClient;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecord;
import com.bluecone.app.payment.domain.reconcile.ChannelBillRecordRepository;
import com.bluecone.app.payment.domain.reconcile.ChannelTradeType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChannelBillImportService {

    private static final Logger log = LoggerFactory.getLogger(ChannelBillImportService.class);

    private final List<ChannelBillClient> billClients;
    private final ChannelBillRecordRepository channelBillRecordRepository;

    public ChannelBillImportService(List<ChannelBillClient> billClients,
                                    ChannelBillRecordRepository channelBillRecordRepository) {
        this.billClients = billClients;
        this.channelBillRecordRepository = channelBillRecordRepository;
    }

    @Transactional
    public void importBill(String channelCode, LocalDate billDate) {
        ChannelBillClient client = resolveClient(channelCode);
        List<String> lines = client.fetchBillLines(billDate);
        if (lines == null || lines.isEmpty()) {
            log.warn("[bill-import] no bill lines channel={} billDate={}", channelCode, billDate);
            return;
        }
        channelBillRecordRepository.deleteByChannelAndBillDate(channelCode, billDate);
        List<ChannelBillRecord> records = parseLines(channelCode, billDate, lines);
        channelBillRecordRepository.saveAll(records);
        log.info("[bill-import] imported {} records channel={} billDate={}", records.size(), channelCode, billDate);
    }

    private ChannelBillClient resolveClient(String channelCode) {
        return billClients.stream()
                .filter(c -> c.supports(channelCode))
                .findFirst()
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "未找到渠道账单客户端: " + channelCode));
    }

    private List<ChannelBillRecord> parseLines(String channelCode, LocalDate billDate, List<String> lines) {
        List<ChannelBillRecord> result = new ArrayList<>();
        for (String line : lines) {
            if (StringUtils.isBlank(line) || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split(",");
            if (parts.length < 7) {
                continue;
            }
            ChannelBillRecord record = new ChannelBillRecord();
            record.setChannelCode(channelCode);
            record.setBillDate(billDate);
            record.setTradeType(parseTradeType(parts[0]));
            record.setChannelTradeNo(parts[1]);
            record.setMerchantOrderNo(parts[2]);
            record.setMerchantRefundNo(parts[3]);
            record.setAmount(safeAmount(parts[4]));
            record.setCurrency(parts[5]);
            record.setTradeTime(parseTime(parts[6], billDate));
            record.setRawLine(line);
            result.add(record);
        }
        return result;
    }

    private ChannelTradeType parseTradeType(String value) {
        try {
            return ChannelTradeType.valueOf(StringUtils.upperCase(value));
        } catch (Exception ex) {
            return ChannelTradeType.OTHER;
        }
    }

    private BigDecimal safeAmount(String value) {
        try {
            return new BigDecimal(value);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private LocalDateTime parseTime(String value, LocalDate billDate) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return billDate.atStartOfDay();
        }
    }
}
