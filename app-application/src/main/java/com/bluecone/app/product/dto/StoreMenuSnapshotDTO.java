package com.bluecone.app.product.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 门店菜单快照 DTO，供应用层和接口层返回。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuSnapshotDTO {

    private Long storeId;
    private String channel;
    private String orderScene;
    private Long version;
    private String menuJson;
    private LocalDateTime generatedAt;
}
