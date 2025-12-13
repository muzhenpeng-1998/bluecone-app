package com.bluecone.app.infra.idempotency;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

/**
 * 幂等记录数据对象，对应表 bc_idempotency_record。
 */
@Data
@TableName("bc_idempotency_record")
public class IdempotencyRecordDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String bizType;

    private String idemKey;

    private String requestHash;

    /**
     * 状态：0=PROCESSING,1=SUCCEEDED,2=FAILED。
     */
    private Integer status;

    private String resultRef;

    private String resultJson;

    private String errorCode;

    private String errorMsg;

    private LocalDateTime expireAt;

    private LocalDateTime lockUntil;

    private Integer version;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

