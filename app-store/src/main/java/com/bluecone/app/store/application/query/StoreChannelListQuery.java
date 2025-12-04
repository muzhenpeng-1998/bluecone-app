package com.bluecone.app.store.application.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店渠道列表查询对象。
 * <p>预留 channelType/status 过滤与分页信息。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreChannelListQuery {

    private Long tenantId;
    private Long storeId;
    private String channelType;
    private String status;
    private Integer pageNo;
    private Integer pageSize;
}
