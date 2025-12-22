package com.bluecone.app.product.domain.model.menu;

import com.bluecone.app.product.dto.view.unified.OptionGroupView;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 菜单商品视图，承载前端需要展示的核心字段（SPU 级别）。
 * <p>
 * Prompt 07: 使用统一的 {@link OptionGroupView} 替代原有的 specGroups/attrGroups/addonGroups，
 * 提供一致的前端渲染结构。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuProductView implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String name;
    private String subtitle;
    private String mainImage;
    private List<String> tags;
    private Map<String, Object> productMeta;
    
    /**
     * 门店维度排序值（从 bc_product_store_config.sort_order）
     * <p>用于分类下商品排序：优先门店排序，其次商品排序，最后 productId</p>
     */
    private Integer storeSortOrder;
    
    /**
     * 商品自身排序值（从 bc_product.sort_order）
     */
    private Integer productSortOrder;

    private List<StoreMenuSkuView> skus;
    
    /**
     * 统一选项组列表（包含 SPEC/ATTR/ADDON 三种类型）。
     * <p>
     * 使用 {@link OptionGroupView} 统一表示，kind 字段区分类型：
     * <ul>
     *   <li>SPEC - 规格组</li>
     *   <li>ATTR - 属性组</li>
     *   <li>ADDON - 小料组</li>
     * </ul>
     */
    private List<OptionGroupView> optionGroups;
}
