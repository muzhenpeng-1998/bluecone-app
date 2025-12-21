package com.bluecone.app.product.dto.view.unified;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一选项项视图（Unified Option Item View）。
 * <p>
 * 用于表示规格选项（Spec Option）、属性选项（Attribute Option）、小料项（Addon Item）的统一视图，
 * 便于 Admin 回显编辑与 C 端菜单快照渲染。
 * <p>
 * 字段说明：
 * <ul>
 *   <li>itemId：选项/小料项的内部 ID（Long）</li>
 *   <li>title：选项/小料项的显示名称</li>
 *   <li>priceDelta：价格增量（相对于基础价格的加价/减价），支持小数</li>
 *   <li>sortOrder：排序值，数值越大越靠前</li>
 *   <li>enabled：是否启用（true/false），禁用的选项不展示给 C 端</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionItemView {

    /**
     * 选项/小料项的内部 ID（Long）。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格选项：BcProductSpecOption.id</li>
     *   <li>属性选项：BcProductAttrOption.id</li>
     *   <li>小料项：BcAddonItem.id</li>
     * </ul>
     */
    private Long itemId;

    /**
     * 选项/小料项的显示名称。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格选项：BcProductSpecOption.name</li>
     *   <li>属性选项：BcProductAttrOption.name</li>
     *   <li>小料项：BcAddonItem.name</li>
     * </ul>
     */
    private String title;

    /**
     * 价格增量（相对于基础价格的加价/减价），支持小数。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格选项：BcProductSpecOption.priceDelta（如有）</li>
     *   <li>属性选项：BcProductAttrRel.overridePrice（商品级覆盖）或 BcProductAttrOption.defaultPrice（素材库默认）</li>
     *   <li>小料项：BcProductAddonRel.overridePrice（商品级覆盖）或 BcAddonItem.defaultPrice（素材库默认）</li>
     * </ul>
     */
    private BigDecimal priceDelta;

    /**
     * 排序值，数值越大越靠前。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格选项：BcProductSpecOption.sortOrder</li>
     *   <li>属性选项：BcProductAttrRel.sortOrder（商品级覆盖）或 BcProductAttrOption.sortOrder（素材库默认）</li>
     *   <li>小料项：BcProductAddonRel.sortOrder（商品级覆盖）或 BcAddonItem.sortOrder（素材库默认）</li>
     * </ul>
     */
    private Integer sortOrder;

    /**
     * 是否启用（true/false），禁用的选项不展示给 C 端。
     * <p>
     * 对应：
     * <ul>
     *   <li>规格选项：BcProductSpecOption.status == 1</li>
     *   <li>属性选项：BcProductAttrRel.status（商品级覆盖）或 BcProductAttrOption.status（素材库默认）</li>
     *   <li>小料项：BcProductAddonRel.status（商品级覆盖）或 BcAddonItem.status（素材库默认）</li>
     * </ul>
     */
    private Boolean enabled;
}

