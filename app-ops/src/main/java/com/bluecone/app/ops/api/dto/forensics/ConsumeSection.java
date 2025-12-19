package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 消费日志汇总
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsumeSection {
    
    /**
     * 消费日志列表
     */
    private List<ConsumeLogItem> logs;
    
    /**
     * 记录总数（用于判断是否被截断）
     */
    private Integer totalCount;
}
