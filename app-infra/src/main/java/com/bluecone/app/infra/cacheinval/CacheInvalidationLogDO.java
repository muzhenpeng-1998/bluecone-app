package com.bluecone.app.infra.cacheinval;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_cache_invalidation_log")
public class CacheInvalidationLogDO implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDateTime occurredAt;

    private LocalDateTime receivedAt;

    private Long tenantId;

    private String scope;

    private String namespace;

    private String eventId;

    private Integer keysCount;

    private String keySamples;

    private Long configVersion;

    private String transport;

    private String instanceId;

    private String result;

    private String note;

    private String decision;

    private Integer stormMode;

    private Long epoch;
}
