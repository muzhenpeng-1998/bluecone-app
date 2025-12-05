package com.bluecone.app.product.domain.model.store;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店商品可售时间段的值对象，表示一天内的起止时间范围（HH:mm）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRange implements Serializable {

    private static final long serialVersionUID = 1L;

    private String fromTime;

    private String toTime;
}
