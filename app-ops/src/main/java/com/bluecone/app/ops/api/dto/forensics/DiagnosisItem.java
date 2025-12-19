package com.bluecone.app.ops.api.dto.forensics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 诊断结论项
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisItem {
    
    /**
     * 诊断代码
     * 例如：MISSING_WALLET_COMMIT, MISSING_COUPON_REDEMPTION, OUTBOX_DELIVERY_FAILED
     */
    private String code;
    
    /**
     * 严重程度：ERROR, WARNING, INFO
     */
    private String severity;
    
    /**
     * 诊断消息（人类可读）
     */
    private String message;
    
    /**
     * 建议操作
     */
    private String suggestedAction;
    
    /**
     * 上下文数据（用于提供额外信息）
     */
    private Map<String, Object> context;
}
