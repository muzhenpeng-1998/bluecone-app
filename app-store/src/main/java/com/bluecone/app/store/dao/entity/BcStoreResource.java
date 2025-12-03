package com.bluecone.app.store.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;

import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Data
public class BcStoreResource implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private Long storeId;

    private String resourceType;

    private String code;

    private String name;

    private String area;

    private String status;

    private String metadataJson;

    private LocalDateTime createdAt;

    private Long createdBy;

    private LocalDateTime updatedAt;

    private Long updatedBy;

    private Boolean isDeleted;

}
