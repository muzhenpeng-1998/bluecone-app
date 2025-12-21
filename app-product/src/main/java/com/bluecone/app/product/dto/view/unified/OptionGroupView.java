package com.bluecone.app.product.dto.view.unified;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一选项组视图（Unified Option Group View）。
 * <p>
 * 用于表示规格组（Spec Group）、属性组（Attribute Group）、小料组（Addon Group）的统一视图，
 * 便于 Admin 回显编辑与 C 端菜单快照渲染。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>kind：选项组类型（SPEC/ATTR/ADDON）</li>
 *   <li>groupId：组的内部 ID（Long）</li>
 *   <li>title：组的显示名称</li>
 *   <li>required：是否必选（true/false）</li>
 *   <li>minSelect：最小选择数量，0 表示不限制</li>
 *   <li>maxSelect：最大选择数量，NULL 表示不限制</li>
 *   <li>maxTotal：总可选上限（仅 ADDON 使用，表示小料总数量上限），支持小数</li>
 *   <li>sortOrder：排序值，数值越大越靠前</li>
 *   <li>enabled：是否启用（true/false），禁用的组不展示给 C 端</li>
 *   <li>displayStartAt：定时展示开始时间，NULL 表示立即生效</li>
 *   <li>displayEndAt：定时展示结束时间，NULL 表示永久有效</li>
 *   <li>items：选项/小料项列表</li>
 * </ul>
 * <p>
 * 组级规则优先级：
 * <ul>
 *   <li>商品绑定表（*_group_rel）的覆盖字段 > 素材库默认字段（若存在）</li>
 *   <li>定时展示/启用状态同样遵循：商品级覆盖 > 素材默认</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionGroupView {

    /**
     * 选项组类型（SPEC/ATTR/ADDON）。
     * <p>
     * 用于区分规格组、属性组、小料组。
     */
    private OptionGroupKind kind;

    /**
     * 组的内部 ID（Long）。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.id</li>
     *   <li>属性组：BcProductAttrGroup.id</li>
     *   <li>小料组：BcAddonGroup.id</li>
     * </ul>
     */
    private Long groupId;

    /**
     * 组的显示名称。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.name</li>
     *   <li>属性组：BcProductAttrGroup.name</li>
     *   <li>小料组：BcAddonGroup.name</li>
     * </ul>
     */
    private String title;

    /**
     * 是否必选（true/false）。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.required</li>
     *   <li>属性组：BcProductAttrGroupRel.required（商品级覆盖）或 BcProductAttrGroup.required（素材库默认）</li>
     *   <li>小料组：BcProductAddonGroupRel.required（商品级覆盖）</li>
     * </ul>
     */
    private Boolean required;

    /**
     * 最小选择数量，0 表示不限制。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：0（规格组通常无 minSelect）</li>
     *   <li>属性组：BcProductAttrGroupRel.minSelect（商品级覆盖）</li>
     *   <li>小料组：BcProductAddonGroupRel.minSelect（商品级覆盖）</li>
     * </ul>
     */
    private Integer minSelect;

    /**
     * 最大选择数量，NULL 表示不限制。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.maxSelect</li>
     *   <li>属性组：BcProductAttrGroupRel.maxSelect（商品级覆盖）或 BcProductAttrGroup.maxSelect（素材库默认）</li>
     *   <li>小料组：BcProductAddonGroupRel.maxSelect（商品级覆盖）</li>
     * </ul>
     */
    private Integer maxSelect;

    /**
     * 总可选上限（仅 ADDON 使用，表示小料总数量上限），支持小数。
     * <p>
     * 对应：
     * <ul>
     *   <li>小料组：BcProductAddonGroupRel.maxTotalQuantity（商品级覆盖）</li>
     *   <li>规格组/属性组：NULL</li>
     * </ul>
     */
    private BigDecimal maxTotal;

    /**
     * 排序值，数值越大越靠前。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.sortOrder</li>
     *   <li>属性组：BcProductAttrGroupRel.sortOrder（商品级覆盖）或 BcProductAttrGroup.sortOrder（素材库默认）</li>
     *   <li>小料组：BcProductAddonGroupRel.sortOrder（商品级覆盖）或 BcAddonGroup.sortOrder（素材库默认）</li>
     * </ul>
     */
    private Integer sortOrder;

    /**
     * 是否启用（true/false），禁用的组不展示给 C 端。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecGroup.status == 1</li>
     *   <li>属性组：BcProductAttrGroupRel.status（商品级覆盖）或 BcProductAttrGroup.status（素材库默认）</li>
     *   <li>小料组：BcProductAddonGroupRel.status（商品级覆盖）或 BcAddonGroup.status（素材库默认）</li>
     * </ul>
     */
    private Boolean enabled;

    /**
     * 定时展示开始时间，NULL 表示立即生效。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：NULL（规格组通常无定时展示）</li>
     *   <li>属性组：BcProductAttrGroupRel.displayStartAt（商品级覆盖）</li>
     *   <li>小料组：BcProductAddonGroupRel.displayStartAt（商品级覆盖）</li>
     * </ul>
     */
    private LocalDateTime displayStartAt;

    /**
     * 定时展示结束时间，NULL 表示永久有效。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：NULL（规格组通常无定时展示）</li>
     *   <li>属性组：BcProductAttrGroupRel.displayEndAt（商品级覆盖）</li>
     *   <li>小料组：BcProductAddonGroupRel.displayEndAt（商品级覆盖）</li>
     * </ul>
     */
    private LocalDateTime displayEndAt;

    /**
     * 选项/小料项列表。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格组：BcProductSpecOption 列表</li>
     *   <li>属性组：BcProductAttrOption 列表（经过 BcProductAttrRel 覆盖）</li>
     *   <li>小料组：BcAddonItem 列表（经过 BcProductAddonRel 覆盖）</li>
     * </ul>
     */
    private List<OptionItemView> items;
}

