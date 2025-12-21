package com.bluecone.app.product.infrastructure.assembler;

import com.bluecone.app.product.dao.entity.*;
import com.bluecone.app.product.dto.view.unified.OptionGroupKind;
import com.bluecone.app.product.dto.view.unified.OptionGroupView;
import com.bluecone.app.product.dto.view.unified.OptionItemView;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 统一选项组装配器（Unified Option Group Assembler）。
 * <p>
 * 将商品的规格组（SPEC）、属性组（ATTR）、小料组（ADDON）统一装配为 {@link OptionGroupView} 视图模型，
 * 用于 Admin 回显编辑与 C 端菜单快照渲染。
 * <p>
 * 装配规则：
 * <ul>
 *   <li><b>规格组（SPEC）</b>：直接从 {@code bc_product_spec_group} 和 {@code bc_product_spec_option} 构建，
 *       无需商品级覆盖（规格组直接属于商品）</li>
 *   <li><b>属性组（ATTR）</b>：从 {@code bc_product_attr_group}（素材库）+ {@code bc_product_attr_group_rel}（商品级组规则）
 *       + {@code bc_product_attr_option}（素材库选项）+ {@code bc_product_attr_rel}（商品级选项覆盖）构建</li>
 *   <li><b>小料组（ADDON）</b>：从 {@code bc_addon_group}（素材库）+ {@code bc_product_addon_group_rel}（商品级组规则）
 *       + {@code bc_addon_item}（素材库小料项）+ {@code bc_product_addon_rel}（商品级小料项覆盖）构建</li>
 * </ul>
 * <p>
 * 覆盖优先级：
 * <ul>
 *   <li>组级规则：商品绑定表（*_group_rel）的覆盖字段 > 素材库默认字段（若存在）</li>
 *   <li>选项/小料项级规则：商品绑定表（*_rel）的覆盖字段 > 素材库默认字段</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@Component
public class UnifiedOptionGroupAssembler {

    /**
     * 从规格组和规格选项构建 SPEC 类型的 OptionGroupView 列表。
     * <p>
     * 规格组直接属于商品，无需商品级覆盖，直接从 {@code bc_product_spec_group} 和 {@code bc_product_spec_option} 构建。
     *
     * @param specGroups  规格组列表
     * @param specOptions 规格选项列表
     * @return SPEC 类型的 OptionGroupView 列表
     */
    public List<OptionGroupView> assembleSpecGroups(
            List<BcProductSpecGroup> specGroups,
            List<BcProductSpecOption> specOptions
    ) {
        if (specGroups == null || specGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 按 specGroupId 分组规格选项
        Map<Long, List<BcProductSpecOption>> optionsByGroupId = specOptions == null
                ? Collections.emptyMap()
                : specOptions.stream().collect(Collectors.groupingBy(BcProductSpecOption::getSpecGroupId));

        return specGroups.stream()
                .map(group -> {
                    List<BcProductSpecOption> groupOptions = optionsByGroupId.getOrDefault(group.getId(), Collections.emptyList());
                    List<OptionItemView> items = groupOptions.stream()
                            .map(this::toSpecOptionItemView)
                            .sorted(Comparator.comparing(OptionItemView::getSortOrder, Comparator.reverseOrder())
                                    .thenComparing(OptionItemView::getItemId))
                            .collect(Collectors.toList());

                    return OptionGroupView.builder()
                            .kind(OptionGroupKind.SPEC)
                            .groupId(group.getId())
                            .title(group.getName())
                            .required(group.getRequired())
                            .minSelect(0) // 规格组通常无 minSelect
                            .maxSelect(group.getMaxSelect())
                            .maxTotal(null) // 规格组无 maxTotal
                            .sortOrder(group.getSortOrder())
                            .enabled(isEnabled(group.getStatus()))
                            .displayStartAt(null) // 规格组通常无定时展示
                            .displayEndAt(null)
                            .items(items)
                            .build();
                })
                .sorted(Comparator.comparing(OptionGroupView::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(OptionGroupView::getGroupId))
                .collect(Collectors.toList());
    }

    /**
     * 从属性组素材库、商品级组规则、属性选项素材库、商品级选项覆盖构建 ATTR 类型的 OptionGroupView 列表。
     * <p>
     * 组级规则优先使用 {@code bc_product_attr_group_rel} 的覆盖字段，没有覆盖时使用 {@code bc_product_attr_group} 的默认字段。
     * 选项级规则优先使用 {@code bc_product_attr_rel} 的覆盖字段，没有覆盖时使用 {@code bc_product_attr_option} 的默认字段。
     *
     * @param attrGroups        属性组素材库列表
     * @param attrGroupRels     商品绑定属性组列表（组级规则）
     * @param attrOptions       属性选项素材库列表
     * @param productAttrRels   商品绑定属性选项列表（选项级覆盖）
     * @return ATTR 类型的 OptionGroupView 列表
     */
    public List<OptionGroupView> assembleAttrGroups(
            List<BcProductAttrGroup> attrGroups,
            List<BcProductAttrGroupRel> attrGroupRels,
            List<BcProductAttrOption> attrOptions,
            List<BcProductAttrRel> productAttrRels
    ) {
        if (attrGroupRels == null || attrGroupRels.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 attrGroupId -> BcProductAttrGroup 映射
        Map<Long, BcProductAttrGroup> attrGroupMap = attrGroups == null
                ? Collections.emptyMap()
                : attrGroups.stream().collect(Collectors.toMap(BcProductAttrGroup::getId, Function.identity()));

        // 按 attrGroupId 分组属性选项
        Map<Long, List<BcProductAttrOption>> optionsByGroupId = attrOptions == null
                ? Collections.emptyMap()
                : attrOptions.stream().collect(Collectors.groupingBy(BcProductAttrOption::getAttrGroupId));

        // 按 attrOptionId 索引商品级选项覆盖
        Map<Long, BcProductAttrRel> attrRelMap = productAttrRels == null
                ? Collections.emptyMap()
                : productAttrRels.stream().collect(Collectors.toMap(BcProductAttrRel::getAttrOptionId, Function.identity(), (a, b) -> a));

        return attrGroupRels.stream()
                .map(groupRel -> {
                    BcProductAttrGroup attrGroup = attrGroupMap.get(groupRel.getAttrGroupId());
                    if (attrGroup == null) {
                        return null; // 素材库中不存在该属性组，跳过
                    }

                    List<BcProductAttrOption> groupOptions = optionsByGroupId.getOrDefault(attrGroup.getId(), Collections.emptyList());
                    List<OptionItemView> items = groupOptions.stream()
                            .map(option -> toAttrOptionItemView(option, attrRelMap.get(option.getId())))
                            .filter(item -> item.getEnabled()) // 仅保留启用的选项
                            .sorted(Comparator.comparing(OptionItemView::getSortOrder, Comparator.reverseOrder())
                                    .thenComparing(OptionItemView::getItemId))
                            .collect(Collectors.toList());

                    return OptionGroupView.builder()
                            .kind(OptionGroupKind.ATTR)
                            .groupId(attrGroup.getId())
                            .title(attrGroup.getName())
                            // 组级规则：商品级覆盖 > 素材库默认
                            .required(coalesce(groupRel.getRequired(), attrGroup.getRequired(), false))
                            .minSelect(coalesce(groupRel.getMinSelect(), 0))
                            .maxSelect(coalesce(groupRel.getMaxSelect(), attrGroup.getMaxSelect()))
                            .maxTotal(null) // 属性组无 maxTotal
                            .sortOrder(coalesce(groupRel.getSortOrder(), attrGroup.getSortOrder(), 0))
                            .enabled(isEnabled(coalesce(groupRel.getStatus(), attrGroup.getStatus(), 1)))
                            .displayStartAt(groupRel.getDisplayStartAt())
                            .displayEndAt(groupRel.getDisplayEndAt())
                            .items(items)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OptionGroupView::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(OptionGroupView::getGroupId))
                .collect(Collectors.toList());
    }

    /**
     * 从小料组素材库、商品级组规则、小料项素材库、商品级小料项覆盖构建 ADDON 类型的 OptionGroupView 列表。
     * <p>
     * 组级规则优先使用 {@code bc_product_addon_group_rel} 的覆盖字段，没有覆盖时使用 {@code bc_addon_group} 的默认字段。
     * 小料项级规则优先使用 {@code bc_product_addon_rel} 的覆盖字段，没有覆盖时使用 {@code bc_addon_item} 的默认字段。
     *
     * @param addonGroups       小料组素材库列表
     * @param addonGroupRels    商品绑定小料组列表（组级规则）
     * @param addonItems        小料项素材库列表
     * @param productAddonRels  商品绑定小料项列表（小料项级覆盖）
     * @return ADDON 类型的 OptionGroupView 列表
     */
    public List<OptionGroupView> assembleAddonGroups(
            List<BcAddonGroup> addonGroups,
            List<BcProductAddonGroupRel> addonGroupRels,
            List<BcAddonItem> addonItems,
            List<BcProductAddonRel> productAddonRels
    ) {
        if (addonGroupRels == null || addonGroupRels.isEmpty()) {
            return Collections.emptyList();
        }

        // 构建 addonGroupId -> BcAddonGroup 映射
        Map<Long, BcAddonGroup> addonGroupMap = addonGroups == null
                ? Collections.emptyMap()
                : addonGroups.stream().collect(Collectors.toMap(BcAddonGroup::getId, Function.identity()));

        // 按 groupId 分组小料项
        Map<Long, List<BcAddonItem>> itemsByGroupId = addonItems == null
                ? Collections.emptyMap()
                : addonItems.stream().collect(Collectors.groupingBy(BcAddonItem::getGroupId));

        // 按 addonItemId 索引商品级小料项覆盖
        Map<Long, BcProductAddonRel> addonRelMap = productAddonRels == null
                ? Collections.emptyMap()
                : productAddonRels.stream().collect(Collectors.toMap(BcProductAddonRel::getAddonItemId, Function.identity(), (a, b) -> a));

        return addonGroupRels.stream()
                .map(groupRel -> {
                    BcAddonGroup addonGroup = addonGroupMap.get(groupRel.getAddonGroupId());
                    if (addonGroup == null) {
                        return null; // 素材库中不存在该小料组，跳过
                    }

                    List<BcAddonItem> groupItems = itemsByGroupId.getOrDefault(addonGroup.getId(), Collections.emptyList());
                    List<OptionItemView> items = groupItems.stream()
                            .map(item -> toAddonItemView(item, addonRelMap.get(item.getId())))
                            .filter(item -> item.getEnabled()) // 仅保留启用的小料项
                            .sorted(Comparator.comparing(OptionItemView::getSortOrder, Comparator.reverseOrder())
                                    .thenComparing(OptionItemView::getItemId))
                            .collect(Collectors.toList());

                    return OptionGroupView.builder()
                            .kind(OptionGroupKind.ADDON)
                            .groupId(addonGroup.getId())
                            .title(addonGroup.getName())
                            // 组级规则：商品级覆盖 > 素材库默认（小料组素材库无 required/minSelect/maxSelect，仅在商品级定义）
                            .required(coalesce(groupRel.getRequired(), false))
                            .minSelect(coalesce(groupRel.getMinSelect(), 0))
                            .maxSelect(groupRel.getMaxSelect())
                            .maxTotal(groupRel.getMaxTotalQuantity())
                            .sortOrder(coalesce(groupRel.getSortOrder(), addonGroup.getSortOrder(), 0))
                            .enabled(isEnabled(coalesce(groupRel.getStatus(), addonGroup.getStatus(), 1)))
                            .displayStartAt(groupRel.getDisplayStartAt())
                            .displayEndAt(groupRel.getDisplayEndAt())
                            .items(items)
                            .build();
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(OptionGroupView::getSortOrder, Comparator.reverseOrder())
                        .thenComparing(OptionGroupView::getGroupId))
                .collect(Collectors.toList());
    }

    /**
     * 将规格选项实体转换为 OptionItemView。
     */
    private OptionItemView toSpecOptionItemView(BcProductSpecOption option) {
        return OptionItemView.builder()
                .itemId(option.getId())
                .title(option.getName())
                .priceDelta(option.getPriceDelta())
                .sortOrder(option.getSortOrder())
                .enabled(isEnabled(option.getStatus()))
                .build();
    }

    /**
     * 将属性选项实体转换为 OptionItemView，应用商品级覆盖（如有）。
     *
     * @param option    属性选项素材库实体
     * @param attrRel   商品级选项覆盖实体（可为 null）
     * @return OptionItemView
     */
    private OptionItemView toAttrOptionItemView(BcProductAttrOption option, BcProductAttrRel attrRel) {
        return OptionItemView.builder()
                .itemId(option.getId())
                .title(option.getName())
                // 价格增量：商品级覆盖 > 素材库默认
                .priceDelta(attrRel != null && attrRel.getPriceDeltaOverride() != null
                        ? attrRel.getPriceDeltaOverride()
                        : option.getPriceDelta())
                // 排序：商品级覆盖 > 素材库默认
                .sortOrder(attrRel != null && attrRel.getSortOrder() != null
                        ? attrRel.getSortOrder()
                        : option.getSortOrder())
                // 启用状态：商品级覆盖 > 素材库默认
                .enabled(attrRel != null && attrRel.getStatus() != null
                        ? isEnabled(attrRel.getStatus())
                        : isEnabled(option.getStatus()))
                .build();
    }

    /**
     * 将小料项实体转换为 OptionItemView，应用商品级覆盖（如有）。
     *
     * @param item      小料项素材库实体
     * @param addonRel  商品级小料项覆盖实体（可为 null）
     * @return OptionItemView
     */
    private OptionItemView toAddonItemView(BcAddonItem item, BcProductAddonRel addonRel) {
        return OptionItemView.builder()
                .itemId(item.getId())
                .title(item.getName())
                // 价格：商品级覆盖 > 素材库默认
                .priceDelta(addonRel != null && addonRel.getPriceOverride() != null
                        ? addonRel.getPriceOverride()
                        : item.getPrice())
                // 排序：商品级覆盖 > 素材库默认
                .sortOrder(addonRel != null && addonRel.getSortOrder() != null
                        ? addonRel.getSortOrder()
                        : item.getSortOrder())
                // 启用状态：商品级覆盖 > 素材库默认
                .enabled(addonRel != null && addonRel.getStatus() != null
                        ? isEnabled(addonRel.getStatus())
                        : isEnabled(item.getStatus()))
                .build();
    }

    /**
     * 判断状态是否为启用（1）。
     */
    private boolean isEnabled(Integer status) {
        return status != null && status == 1;
    }

    /**
     * 返回第一个非 null 的值。
     */
    @SafeVarargs
    private final <T> T coalesce(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}

