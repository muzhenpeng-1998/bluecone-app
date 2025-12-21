package com.bluecone.app.product.infrastructure.assembler;

import com.bluecone.app.product.dao.entity.*;
import com.bluecone.app.product.dto.view.unified.OptionGroupKind;
import com.bluecone.app.product.dto.view.unified.OptionGroupView;
import com.bluecone.app.product.dto.view.unified.OptionItemView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UnifiedOptionGroupAssembler 单元测试。
 * <p>
 * 通过假数据验证装配器输出结构的正确性，包括：
 * <ul>
 *   <li>规格组（SPEC）装配</li>
 *   <li>属性组（ATTR）装配（含组级规则覆盖与选项级覆盖）</li>
 *   <li>小料组（ADDON）装配（含组级规则覆盖与小料项级覆盖）</li>
 * </ul>
 */
class UnifiedOptionGroupAssemblerTest {

    private UnifiedOptionGroupAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new UnifiedOptionGroupAssembler();
    }

    /**
     * 测试规格组装配（SPEC）。
     * <p>
     * 规格组直接属于商品，无需商品级覆盖，直接从 {@code bc_product_spec_group} 和 {@code bc_product_spec_option} 构建。
     */
    @Test
    void testAssembleSpecGroups() {
        // 准备假数据：规格组
        BcProductSpecGroup specGroup1 = new BcProductSpecGroup();
        specGroup1.setId(1L);
        specGroup1.setTenantId(1000L);
        specGroup1.setProductId(100L);
        specGroup1.setName("容量");
        specGroup1.setSelectType(1); // 单选
        specGroup1.setRequired(true);
        specGroup1.setMaxSelect(1);
        specGroup1.setStatus(1); // 启用
        specGroup1.setSortOrder(10);

        // 准备假数据：规格选项
        BcProductSpecOption option1 = new BcProductSpecOption();
        option1.setId(101L);
        option1.setTenantId(1000L);
        option1.setProductId(100L);
        option1.setSpecGroupId(1L);
        option1.setName("大杯");
        option1.setPriceDelta(new BigDecimal("2.00"));
        option1.setIsDefault(false);
        option1.setStatus(1); // 启用
        option1.setSortOrder(20);

        BcProductSpecOption option2 = new BcProductSpecOption();
        option2.setId(102L);
        option2.setTenantId(1000L);
        option2.setProductId(100L);
        option2.setSpecGroupId(1L);
        option2.setName("中杯");
        option2.setPriceDelta(BigDecimal.ZERO);
        option2.setIsDefault(true);
        option2.setStatus(1); // 启用
        option2.setSortOrder(10);

        // 执行装配
        List<OptionGroupView> result = assembler.assembleSpecGroups(
                Arrays.asList(specGroup1),
                Arrays.asList(option1, option2)
        );

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        OptionGroupView group = result.get(0);
        assertEquals(OptionGroupKind.SPEC, group.getKind());
        assertEquals(1L, group.getGroupId());
        assertEquals("容量", group.getTitle());
        assertTrue(group.getRequired());
        assertEquals(0, group.getMinSelect()); // 规格组无 minSelect
        assertEquals(1, group.getMaxSelect());
        assertNull(group.getMaxTotal()); // 规格组无 maxTotal
        assertEquals(10, group.getSortOrder());
        assertTrue(group.getEnabled());
        assertNull(group.getDisplayStartAt()); // 规格组无定时展示
        assertNull(group.getDisplayEndAt());

        // 验证选项列表（按 sortOrder desc, itemId asc 排序）
        List<OptionItemView> items = group.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());

        OptionItemView item1 = items.get(0); // 大杯（sortOrder=20）
        assertEquals(101L, item1.getItemId());
        assertEquals("大杯", item1.getTitle());
        assertEquals(new BigDecimal("2.00"), item1.getPriceDelta());
        assertEquals(20, item1.getSortOrder());
        assertTrue(item1.getEnabled());

        OptionItemView item2 = items.get(1); // 中杯（sortOrder=10）
        assertEquals(102L, item2.getItemId());
        assertEquals("中杯", item2.getTitle());
        assertEquals(BigDecimal.ZERO, item2.getPriceDelta());
        assertEquals(10, item2.getSortOrder());
        assertTrue(item2.getEnabled());
    }

    /**
     * 测试属性组装配（ATTR），包含组级规则覆盖与选项级覆盖。
     * <p>
     * 组级规则优先使用 {@code bc_product_attr_group_rel} 的覆盖字段，没有覆盖时使用 {@code bc_product_attr_group} 的默认字段。
     * 选项级规则优先使用 {@code bc_product_attr_rel} 的覆盖字段，没有覆盖时使用 {@code bc_product_attr_option} 的默认字段。
     */
    @Test
    void testAssembleAttrGroups() {
        // 准备假数据：属性组素材库
        BcProductAttrGroup attrGroup1 = new BcProductAttrGroup();
        attrGroup1.setId(1L);
        attrGroup1.setTenantId(1000L);
        attrGroup1.setName("甜度");
        attrGroup1.setScope(1); // 口味
        attrGroup1.setSelectType(1); // 单选
        attrGroup1.setRequired(false); // 素材库默认：非必选
        attrGroup1.setMaxSelect(1);
        attrGroup1.setStatus(1); // 启用
        attrGroup1.setSortOrder(5); // 素材库默认排序

        // 准备假数据：商品绑定属性组（组级规则）
        BcProductAttrGroupRel groupRel1 = new BcProductAttrGroupRel();
        groupRel1.setId(1L);
        groupRel1.setTenantId(1000L);
        groupRel1.setProductId(100L);
        groupRel1.setAttrGroupId(1L);
        groupRel1.setRequired(true); // 商品级覆盖：必选
        groupRel1.setMinSelect(1);
        groupRel1.setMaxSelect(1);
        groupRel1.setStatus(1); // 启用
        groupRel1.setSortOrder(15); // 商品级覆盖排序
        groupRel1.setDisplayStartAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        groupRel1.setDisplayEndAt(LocalDateTime.of(2025, 12, 31, 23, 59));

        // 准备假数据：属性选项素材库
        BcProductAttrOption option1 = new BcProductAttrOption();
        option1.setId(101L);
        option1.setTenantId(1000L);
        option1.setAttrGroupId(1L);
        option1.setName("无糖");
        option1.setValueCode("NO_SUGAR");
        option1.setPriceDelta(BigDecimal.ZERO); // 素材库默认：不加价
        option1.setStatus(1); // 启用
        option1.setSortOrder(10); // 素材库默认排序

        BcProductAttrOption option2 = new BcProductAttrOption();
        option2.setId(102L);
        option2.setTenantId(1000L);
        option2.setAttrGroupId(1L);
        option2.setName("少糖");
        option2.setValueCode("LESS_SUGAR");
        option2.setPriceDelta(new BigDecimal("1.00")); // 素材库默认：加价 1 元
        option2.setStatus(1); // 启用
        option2.setSortOrder(5); // 素材库默认排序

        // 准备假数据：商品绑定属性选项（选项级覆盖）
        BcProductAttrRel attrRel1 = new BcProductAttrRel();
        attrRel1.setId(1L);
        attrRel1.setTenantId(1000L);
        attrRel1.setProductId(100L);
        attrRel1.setAttrGroupId(1L);
        attrRel1.setAttrOptionId(102L); // 覆盖"少糖"选项
        attrRel1.setPriceDeltaOverride(new BigDecimal("2.00")); // 商品级覆盖：加价 2 元
        attrRel1.setStatus(1); // 启用
        attrRel1.setSortOrder(20); // 商品级覆盖排序

        // 执行装配
        List<OptionGroupView> result = assembler.assembleAttrGroups(
                Arrays.asList(attrGroup1),
                Arrays.asList(groupRel1),
                Arrays.asList(option1, option2),
                Arrays.asList(attrRel1)
        );

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        OptionGroupView group = result.get(0);
        assertEquals(OptionGroupKind.ATTR, group.getKind());
        assertEquals(1L, group.getGroupId());
        assertEquals("甜度", group.getTitle());
        assertTrue(group.getRequired()); // 商品级覆盖：必选
        assertEquals(1, group.getMinSelect()); // 商品级覆盖
        assertEquals(1, group.getMaxSelect());
        assertNull(group.getMaxTotal()); // 属性组无 maxTotal
        assertEquals(15, group.getSortOrder()); // 商品级覆盖排序
        assertTrue(group.getEnabled());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), group.getDisplayStartAt());
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59), group.getDisplayEndAt());

        // 验证选项列表（按 sortOrder desc, itemId asc 排序）
        List<OptionItemView> items = group.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());

        OptionItemView item1 = items.get(0); // 少糖（sortOrder=20，商品级覆盖）
        assertEquals(102L, item1.getItemId());
        assertEquals("少糖", item1.getTitle());
        assertEquals(new BigDecimal("2.00"), item1.getPriceDelta()); // 商品级覆盖：加价 2 元
        assertEquals(20, item1.getSortOrder()); // 商品级覆盖排序
        assertTrue(item1.getEnabled());

        OptionItemView item2 = items.get(1); // 无糖（sortOrder=10，素材库默认）
        assertEquals(101L, item2.getItemId());
        assertEquals("无糖", item2.getTitle());
        assertEquals(BigDecimal.ZERO, item2.getPriceDelta()); // 素材库默认：不加价
        assertEquals(10, item2.getSortOrder()); // 素材库默认排序
        assertTrue(item2.getEnabled());
    }

    /**
     * 测试小料组装配（ADDON），包含组级规则覆盖与小料项级覆盖。
     * <p>
     * 组级规则优先使用 {@code bc_product_addon_group_rel} 的覆盖字段，没有覆盖时使用 {@code bc_addon_group} 的默认字段。
     * 小料项级规则优先使用 {@code bc_product_addon_rel} 的覆盖字段，没有覆盖时使用 {@code bc_addon_item} 的默认字段。
     */
    @Test
    void testAssembleAddonGroups() {
        // 准备假数据：小料组素材库
        BcAddonGroup addonGroup1 = new BcAddonGroup();
        addonGroup1.setId(1L);
        addonGroup1.setTenantId(1000L);
        addonGroup1.setName("奶茶配料");
        addonGroup1.setType(1); // 计价小料
        addonGroup1.setStatus(1); // 启用
        addonGroup1.setSortOrder(5); // 素材库默认排序

        // 准备假数据：商品绑定小料组（组级规则）
        BcProductAddonGroupRel groupRel1 = new BcProductAddonGroupRel();
        groupRel1.setId(1L);
        groupRel1.setTenantId(1000L);
        groupRel1.setProductId(100L);
        groupRel1.setAddonGroupId(1L);
        groupRel1.setRequired(false); // 非必选
        groupRel1.setMinSelect(0);
        groupRel1.setMaxSelect(3);
        groupRel1.setMaxTotalQuantity(new BigDecimal("5.0")); // 总可选上限 5 份
        groupRel1.setStatus(1); // 启用
        groupRel1.setSortOrder(20); // 商品级覆盖排序
        groupRel1.setDisplayStartAt(LocalDateTime.of(2025, 1, 1, 0, 0));
        groupRel1.setDisplayEndAt(LocalDateTime.of(2025, 12, 31, 23, 59));

        // 准备假数据：小料项素材库
        BcAddonItem item1 = new BcAddonItem();
        item1.setId(101L);
        item1.setTenantId(1000L);
        item1.setGroupId(1L);
        item1.setName("珍珠");
        item1.setPrice(new BigDecimal("3.00")); // 素材库默认：3 元
        item1.setMaxQuantity(new BigDecimal("2.0"));
        item1.setFreeLimit(BigDecimal.ZERO);
        item1.setStatus(1); // 启用
        item1.setSortOrder(10); // 素材库默认排序

        BcAddonItem item2 = new BcAddonItem();
        item2.setId(102L);
        item2.setTenantId(1000L);
        item2.setGroupId(1L);
        item2.setName("燕麦奶");
        item2.setPrice(new BigDecimal("5.00")); // 素材库默认：5 元
        item2.setMaxQuantity(new BigDecimal("1.0"));
        item2.setFreeLimit(BigDecimal.ZERO);
        item2.setStatus(1); // 启用
        item2.setSortOrder(5); // 素材库默认排序

        // 准备假数据：商品绑定小料项（小料项级覆盖）
        BcProductAddonRel addonRel1 = new BcProductAddonRel();
        addonRel1.setId(1L);
        addonRel1.setTenantId(1000L);
        addonRel1.setProductId(100L);
        addonRel1.setAddonGroupId(1L);
        addonRel1.setAddonItemId(101L); // 覆盖"珍珠"小料项
        addonRel1.setPriceOverride(new BigDecimal("4.00")); // 商品级覆盖：4 元
        addonRel1.setMaxQuantityOverride(new BigDecimal("3.0")); // 商品级覆盖：最多 3 份
        addonRel1.setStatus(1); // 启用
        addonRel1.setSortOrder(25); // 商品级覆盖排序

        // 执行装配
        List<OptionGroupView> result = assembler.assembleAddonGroups(
                Arrays.asList(addonGroup1),
                Arrays.asList(groupRel1),
                Arrays.asList(item1, item2),
                Arrays.asList(addonRel1)
        );

        // 验证结果
        assertNotNull(result);
        assertEquals(1, result.size());

        OptionGroupView group = result.get(0);
        assertEquals(OptionGroupKind.ADDON, group.getKind());
        assertEquals(1L, group.getGroupId());
        assertEquals("奶茶配料", group.getTitle());
        assertFalse(group.getRequired()); // 非必选
        assertEquals(0, group.getMinSelect());
        assertEquals(3, group.getMaxSelect());
        assertEquals(new BigDecimal("5.0"), group.getMaxTotal()); // 总可选上限 5 份
        assertEquals(20, group.getSortOrder()); // 商品级覆盖排序
        assertTrue(group.getEnabled());
        assertEquals(LocalDateTime.of(2025, 1, 1, 0, 0), group.getDisplayStartAt());
        assertEquals(LocalDateTime.of(2025, 12, 31, 23, 59), group.getDisplayEndAt());

        // 验证小料项列表（按 sortOrder desc, itemId asc 排序）
        List<OptionItemView> items = group.getItems();
        assertNotNull(items);
        assertEquals(2, items.size());

        OptionItemView itemView1 = items.get(0); // 珍珠（sortOrder=25，商品级覆盖）
        assertEquals(101L, itemView1.getItemId());
        assertEquals("珍珠", itemView1.getTitle());
        assertEquals(new BigDecimal("4.00"), itemView1.getPriceDelta()); // 商品级覆盖：4 元
        assertEquals(25, itemView1.getSortOrder()); // 商品级覆盖排序
        assertTrue(itemView1.getEnabled());

        OptionItemView itemView2 = items.get(1); // 燕麦奶（sortOrder=5，素材库默认）
        assertEquals(102L, itemView2.getItemId());
        assertEquals("燕麦奶", itemView2.getTitle());
        assertEquals(new BigDecimal("5.00"), itemView2.getPriceDelta()); // 素材库默认：5 元
        assertEquals(5, itemView2.getSortOrder()); // 素材库默认排序
        assertTrue(itemView2.getEnabled());
    }

    /**
     * 测试禁用选项过滤。
     * <p>
     * 验证装配器仅保留启用的选项/小料项（status=1），禁用的选项/小料项不应出现在结果中。
     */
    @Test
    void testFilterDisabledOptions() {
        // 准备假数据：规格组
        BcProductSpecGroup specGroup1 = new BcProductSpecGroup();
        specGroup1.setId(1L);
        specGroup1.setTenantId(1000L);
        specGroup1.setProductId(100L);
        specGroup1.setName("容量");
        specGroup1.setSelectType(1);
        specGroup1.setRequired(true);
        specGroup1.setMaxSelect(1);
        specGroup1.setStatus(1);
        specGroup1.setSortOrder(10);

        // 准备假数据：规格选项（一个启用，一个禁用）
        BcProductSpecOption option1 = new BcProductSpecOption();
        option1.setId(101L);
        option1.setTenantId(1000L);
        option1.setProductId(100L);
        option1.setSpecGroupId(1L);
        option1.setName("大杯");
        option1.setPriceDelta(new BigDecimal("2.00"));
        option1.setIsDefault(false);
        option1.setStatus(1); // 启用
        option1.setSortOrder(20);

        BcProductSpecOption option2 = new BcProductSpecOption();
        option2.setId(102L);
        option2.setTenantId(1000L);
        option2.setProductId(100L);
        option2.setSpecGroupId(1L);
        option2.setName("中杯");
        option2.setPriceDelta(BigDecimal.ZERO);
        option2.setIsDefault(true);
        option2.setStatus(0); // 禁用
        option2.setSortOrder(10);

        // 执行装配
        List<OptionGroupView> result = assembler.assembleSpecGroups(
                Arrays.asList(specGroup1),
                Arrays.asList(option1, option2)
        );

        // 验证结果：禁用的选项应被过滤（但规格组的选项不过滤，因为规格组直接属于商品）
        assertNotNull(result);
        assertEquals(1, result.size());

        OptionGroupView group = result.get(0);
        List<OptionItemView> items = group.getItems();
        assertNotNull(items);
        assertEquals(2, items.size()); // 规格组不过滤禁用选项，由业务层决定

        // 验证第二个选项被标记为禁用
        OptionItemView item2 = items.get(1);
        assertEquals(102L, item2.getItemId());
        assertFalse(item2.getEnabled()); // 禁用
    }
}

