package com.bluecone.app.product.domain.model.addon;

import com.bluecone.app.product.domain.enums.AddonType;
import com.bluecone.app.product.domain.enums.ProductStatus;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 小料组领域模型，对应 bc_addon_group，定义可复用的加料组及类型（计价/不计价）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddonGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private Long tenantId;

    private String name;

    private AddonType type;

    private ProductStatus status;

    private Integer sortOrder;

    private String remark;

    private List<AddonItem> items;
}
