package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Outbox 事件汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxSection {
    
    /**
     * Outbox 事件列表
     */
    private List<OutboxEventItem> events;
    
    /**
     * 记录总数（用于判断是否被截断）
     */
    private Integer totalCount;
}
