package com.bluecone.app.store.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

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
        if (dateTime == null) {
            return false;
        }
        // 1）拆解出日期/星期，weekday 取值 1~7（周一=1）
        LocalDate date = dateTime.toLocalDate();
        int weekday = dateTime.getDayOfWeek().getValue();
        LocalTime time = dateTime.toLocalTime();

        // 2）优先检查特殊日：命中 CLOSED 直接关店；命中 SPECIAL_TIME 则按时间段判定
        if (specialDays != null && !specialDays.isEmpty()) {
            for (SpecialDayItem item : specialDays) {
                if (item == null || item.getDate() == null) {
                    continue;
                }
                if (!item.getDate().isEqual(date)) {
                    continue;
                }
                if ("CLOSED".equalsIgnoreCase(item.getSpecialType())) {
                    return false;
                }
                if ("SPECIAL_TIME".equalsIgnoreCase(item.getSpecialType())) {
                    LocalTime start = item.getStartTime();
                    LocalTime end = item.getEndTime();
                    if (start != null && end != null) {
                        return !time.isBefore(start) && time.isBefore(end);
                    }
                    // 未配置时间段则视为全天营业
                    return true;
                }
                // 其他类型暂不处理，回落到常规营业时间
                break;
            }
        }

        // 3）常规营业时间：先找出匹配 weekday 的段，再判断是否在 REGULAR 且不落入 BREAK
        if (regularHours == null || regularHours.isEmpty()) {
            return false;
        }

        boolean inRegular = regularHours.stream()
                .filter(item -> item != null && item.getWeekday() == weekday)
                .filter(item -> "REGULAR".equalsIgnoreCase(item.getPeriodType()))
                .anyMatch(item -> within(time, item.getStartTime(), item.getEndTime()));
        if (!inRegular) {
            return false;
        }

        boolean inBreak = regularHours.stream()
                .filter(item -> item != null && item.getWeekday() == weekday)
                .filter(item -> "BREAK".equalsIgnoreCase(item.getPeriodType()))
                .anyMatch(item -> within(time, item.getStartTime(), item.getEndTime()));
        return !inBreak;
    }

    private boolean within(LocalTime time, LocalTime start, LocalTime end) {
        if (time == null || start == null || end == null) {
            return false;
        }
        // 半开区间：[start, end)，避免重叠边界重复判定
        return !time.isBefore(start) && time.isBefore(end);
    }

    /**
     * 获取指定日期的营业时间区间字符串（格式：HH:mm-HH:mm，多个时段用逗号分隔）。
     * <p>优先检查特殊日配置，如果命中特殊日且为 CLOSED，返回 null；如果命中 SPECIAL_TIME，返回特殊日时间段。</p>
     * <p>否则返回常规营业时间中该 weekday 的 REGULAR 时段（排除 BREAK 时段）。</p>
     *
     * @param date 查询日期
     * @return 营业时间区间字符串，如 "09:00-22:00" 或 "09:00-14:00,17:00-22:00"，如果不营业则返回 null
     */
    public String getOpeningHoursRange(LocalDate date) {
        if (date == null) {
            return null;
        }
        int weekday = date.getDayOfWeek().getValue();

        // 1）优先检查特殊日：命中 CLOSED 直接返回 null；命中 SPECIAL_TIME 则返回特殊日时间段
        if (specialDays != null && !specialDays.isEmpty()) {
            for (SpecialDayItem item : specialDays) {
                if (item == null || item.getDate() == null) {
                    continue;
                }
                if (!item.getDate().isEqual(date)) {
                    continue;
                }
                if ("CLOSED".equalsIgnoreCase(item.getSpecialType())) {
                    return null;
                }
                if ("SPECIAL_TIME".equalsIgnoreCase(item.getSpecialType())) {
                    LocalTime start = item.getStartTime();
                    LocalTime end = item.getEndTime();
                    if (start != null && end != null) {
                        return formatTimeRange(start, end);
                    }
                    // 未配置时间段则视为全天营业，返回 "00:00-23:59"
                    return "00:00-23:59";
                }
                // 其他类型暂不处理，回落到常规营业时间
                break;
            }
        }

        // 2）常规营业时间：找出匹配 weekday 的 REGULAR 时段（排除 BREAK 时段）
        if (regularHours == null || regularHours.isEmpty()) {
            return null;
        }

        List<OpeningHoursItem> regularItems = regularHours.stream()
                .filter(item -> item != null && item.getWeekday() == weekday)
                .filter(item -> "REGULAR".equalsIgnoreCase(item.getPeriodType()))
                .filter(item -> item.getStartTime() != null && item.getEndTime() != null)
                .sorted((a, b) -> {
                    // 按开始时间排序
                    int startCompare = a.getStartTime().compareTo(b.getStartTime());
                    if (startCompare != 0) {
                        return startCompare;
                    }
                    // 开始时间相同则按结束时间排序
                    return a.getEndTime().compareTo(b.getEndTime());
                })
                .collect(Collectors.toList());

        if (regularItems.isEmpty()) {
            return null;
        }

        // 合并连续的时段
        List<String> ranges = new java.util.ArrayList<>();
        for (OpeningHoursItem item : regularItems) {
            String range = formatTimeRange(item.getStartTime(), item.getEndTime());
            ranges.add(range);
        }

        return String.join(",", ranges);
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            return null;
        }
        return start.toString() + "-" + end.toString();
    }

    /**
     * 常规营业时间值对象。
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpeningHoursItem {
        private int weekday;
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
        /**
         * 备注信息，便于运营配置说明。
         */
        private String remark;
    }
}
