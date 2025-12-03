package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 营业时间规则聚合，统一封装常规营业时间与特殊日（对应 bc_store_opening_hours 与 bc_store_special_day）。
 * <p>StoreOpenStateService 将依赖此模型做「是否营业」判断，高并发下通过 StoreConfig 快照直接复用。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOpeningSchedule {

    /**
     * 常规营业时间列表。
     */
    private List<OpeningHoursItem> regularHours;

    /**
     * 特殊日配置，例如节假日延时、停业等。
     */
    private List<SpecialDayItem> specialDays;

    /**
     * 判断给定时间是否营业，具体规则后续补充。
     *
     * @param dateTime 当前时间
     * @return true 表示营业，false 表示不营业
     */
    public boolean isOpenAt(LocalDateTime dateTime) {
        // TODO 后续补充具体判断逻辑：先判断特殊日，再回落到常规营业时间
        return false;
    }

    /**
     * 常规营业时间值对象。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHoursItem {
        private Byte weekday;
        private LocalTime startTime;
        private LocalTime endTime;
        private String periodType;
    }

    /**
     * 特殊日配置值对象。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpecialDayItem {
        private LocalDate date;
        private String specialType;
        private LocalTime startTime;
        private LocalTime endTime;
    }
}
