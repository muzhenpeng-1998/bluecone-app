package com.bluecone.app.order.application.generator;

import com.bluecone.app.order.domain.model.Order;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * 简单的订单号生成器，基于时间和主键拼接。
 * TODO: 与全局编码规范对齐。
 */
@Component
public class OrderNoGenerator {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public String generate(Order order) {
        LocalDateTime now = LocalDateTime.now();
        String timePart = now.format(FORMATTER);
        Long id = order.getId();
        String idPart = id == null ? "000000" : String.valueOf(id % 1_000_000);
        return "OC" + timePart + idPart;
    }
}
