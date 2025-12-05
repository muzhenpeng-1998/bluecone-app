package com.bluecone.app.inventory.infra.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("bc_inv_item")
public class InvItemDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long tenantId;

    private String itemType;

    private Long refId;

    private String externalCode;

    private String name;

    private String unit;

    private Integer status;

    private String remark;

    private String ext;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
