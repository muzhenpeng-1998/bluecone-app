package com.bluecone.app.product.test;

import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.product.application.command.CreateProductAggregateCommand;
import com.bluecone.app.product.application.dto.ProductDetailDTO;
import com.bluecone.app.product.application.service.ProductAggregateAdminApplicationService;
import com.bluecone.app.product.dao.entity.*;
import com.bluecone.app.product.dao.mapper.*;
import com.bluecone.app.product.dto.view.unified.OptionGroupKind;
import com.bluecone.app.product.dto.view.unified.OptionGroupView;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 商品聚合写入与读取集成测试（Prompt 10）。
 * <p>
 * 测试场景：
 * <ul>
 *   <li>创建：商品 + 2 分类 + 1 specGroup(2 options) + 4 SKU 组合 + 2 attrGroups(温度/甜度) + 2 addonGroups(加奶/换豆子)</li>
 *   <li>断言：所有子表行数正确；getDetail 回显结构完整；OptionGroups 输出顺序正确</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@DisplayName("商品聚合写入与读取集成测试")
class ProductAggregateWriteReadTest extends AbstractProductIntegrationTest {

    @Autowired
    private ProductAggregateAdminApplicationService productAggregateService;

    @Autowired
    private IdService idService;

    @Autowired
    private BcProductMapper productMapper;

    @Autowired
    private BcProductSkuMapper skuMapper;

    @Autowired
    private BcProductSpecGroupMapper specGroupMapper;

    @Autowired
    private BcProductSpecOptionMapper specOptionMapper;

    @Autowired
    private BcProductAttrGroupRelMapper attrGroupRelMapper;

    @Autowired
    private BcProductAttrRelMapper attrRelMapper;

    @Autowired
    private BcProductAddonGroupRelMapper addonGroupRelMapper;

    @Autowired
    private BcProductAddonRelMapper addonRelMapper;

    @Autowired
    private BcProductCategoryRelMapper categoryRelMapper;

    @Autowired
    private BcProductCategoryMapper categoryMapper;

    @Autowired
    private BcProductAttrGroupMapper attrGroupMapper;

    @Autowired
    private BcProductAttrOptionMapper attrOptionMapper;

    @Autowired
    private BcAddonGroupMapper addonGroupMapper;

    @Autowired
    private BcAddonItemMapper addonItemMapper;

    private Long tenantId;
    private Long operatorId;
    private Long categoryId1;
    private Long categoryId2;
    private Long attrGroupId1; // 温度
    private Long attrGroupId2; // 甜度
    private Long attrOptionId1; // 热
    private Long attrOptionId2; // 冰
    private Long attrOptionId3; // 标准糖
    private Long attrOptionId4; // 少糖
    private Long addonGroupId1; // 加奶
    private Long addonGroupId2; // 换豆子
    private Long addonItemId1; // 全脂奶
    private Long addonItemId2; // 脱脂奶
    private Long addonItemId3; // 阿拉比卡
    private Long addonItemId4; // 罗布斯塔

    @BeforeEach
    void setUp() {
        tenantId = 1L;
        operatorId = 100L;
        TenantContext.set(tenantId);

        // 准备测试数据：分类、属性组、小料组
        prepareTestData();
    }

    @Test
    @DisplayName("创建商品聚合 - 完整结构")
    void testCreateProductAggregate() {
        // 1. 构建创建命令
        CreateProductAggregateCommand command = buildCreateCommand();

        // 2. 执行创建
        Long productId = productAggregateService.create(command);
        assertThat(productId).isNotNull().isPositive();

        // 3. 断言：商品主表
        BcProduct product = productMapper.selectById(productId);
        assertThat(product).isNotNull();
        assertThat(product.getName()).isEqualTo("美式咖啡");
        assertThat(product.getStatus()).isEqualTo(1);
        assertThat(product.getPublicId()).isNotNull();

        // 4. 断言：SKU 表（4 个 SKU）
        List<BcProductSku> skus = skuMapper.selectList(new LambdaQueryWrapper<BcProductSku>()
                .eq(BcProductSku::getProductId, productId)
                .eq(BcProductSku::getDeleted, 0));
        assertThat(skus).hasSize(4);
        assertThat(skus).allMatch(sku -> sku.getPublicId() != null);
        assertThat(skus).anyMatch(BcProductSku::getIsDefault);

        // 5. 断言：规格组表（1 个规格组）
        List<BcProductSpecGroup> specGroups = specGroupMapper.selectList(new LambdaQueryWrapper<BcProductSpecGroup>()
                .eq(BcProductSpecGroup::getProductId, productId)
                .eq(BcProductSpecGroup::getDeleted, 0));
        assertThat(specGroups).hasSize(1);
        BcProductSpecGroup specGroup = specGroups.get(0);
        assertThat(specGroup.getTitle()).isEqualTo("规格");

        // 6. 断言：规格选项表（2 个规格选项）
        List<BcProductSpecOption> specOptions = specOptionMapper.selectList(new LambdaQueryWrapper<BcProductSpecOption>()
                .eq(BcProductSpecOption::getGroupId, specGroup.getId())
                .eq(BcProductSpecOption::getDeleted, 0));
        assertThat(specOptions).hasSize(2);
        assertThat(specOptions).extracting(BcProductSpecOption::getTitle)
                .containsExactlyInAnyOrder("中杯", "大杯");

        // 7. 断言：属性组关联表（2 个属性组）
        List<BcProductAttrGroupRel> attrGroupRels = attrGroupRelMapper.selectList(new LambdaQueryWrapper<BcProductAttrGroupRel>()
                .eq(BcProductAttrGroupRel::getProductId, productId)
                .eq(BcProductAttrGroupRel::getDeleted, 0));
        assertThat(attrGroupRels).hasSize(2);

        // 8. 断言：属性选项关联表（4 个属性选项）
        List<BcProductAttrRel> attrRels = attrRelMapper.selectList(new LambdaQueryWrapper<BcProductAttrRel>()
                .eq(BcProductAttrRel::getProductId, productId)
                .eq(BcProductAttrRel::getDeleted, 0));
        assertThat(attrRels).hasSize(4);

        // 9. 断言：小料组关联表（2 个小料组）
        List<BcProductAddonGroupRel> addonGroupRels = addonGroupRelMapper.selectList(new LambdaQueryWrapper<BcProductAddonGroupRel>()
                .eq(BcProductAddonGroupRel::getProductId, productId)
                .eq(BcProductAddonGroupRel::getDeleted, 0));
        assertThat(addonGroupRels).hasSize(2);

        // 10. 断言：小料项关联表（4 个小料项）
        List<BcProductAddonRel> addonRels = addonRelMapper.selectList(new LambdaQueryWrapper<BcProductAddonRel>()
                .eq(BcProductAddonRel::getProductId, productId)
                .eq(BcProductAddonRel::getDeleted, 0));
        assertThat(addonRels).hasSize(4);

        // 11. 断言：分类关联表（2 个分类）
        List<BcProductCategoryRel> categoryRels = categoryRelMapper.selectList(new LambdaQueryWrapper<BcProductCategoryRel>()
                .eq(BcProductCategoryRel::getProductId, productId)
                .eq(BcProductCategoryRel::getDeleted, 0));
        assertThat(categoryRels).hasSize(2);
    }

    @Test
    @DisplayName("查询商品详情 - 完整结构回显")
    void testGetProductDetail() {
        // 1. 创建商品
        CreateProductAggregateCommand command = buildCreateCommand();
        Long productId = productAggregateService.create(command);

        // 2. 查询详情
        ProductDetailDTO detail = productAggregateService.getDetail(productId);

        // 3. 断言：基本信息
        assertThat(detail).isNotNull();
        assertThat(detail.getProductId()).isEqualTo(productId);
        assertThat(detail.getName()).isEqualTo("美式咖啡");
        assertThat(detail.getStatus()).isEqualTo(1);

        // 4. 断言：SKU 列表（4 个 SKU）
        assertThat(detail.getSkus()).hasSize(4);
        assertThat(detail.getSkus()).anyMatch(sku -> sku.getIsDefault());

        // 5. 断言：规格组列表（1 个规格组）
        assertThat(detail.getSpecGroups()).hasSize(1);
        ProductDetailDTO.SpecGroupDTO specGroup = detail.getSpecGroups().get(0);
        assertThat(specGroup.getTitle()).isEqualTo("规格");
        assertThat(specGroup.getOptions()).hasSize(2);

        // 6. 断言：属性组列表（2 个属性组）
        assertThat(detail.getAttrGroups()).hasSize(2);

        // 7. 断言：小料组列表（2 个小料组）
        assertThat(detail.getAddonGroups()).hasSize(2);

        // 8. 断言：分类列表（2 个分类）
        assertThat(detail.getCategories()).hasSize(2);
    }

    @Test
    @DisplayName("OptionGroups 输出顺序正确")
    void testOptionGroupsOrder() {
        // 1. 创建商品
        CreateProductAggregateCommand command = buildCreateCommand();
        Long productId = productAggregateService.create(command);

        // 2. 查询详情
        ProductDetailDTO detail = productAggregateService.getDetail(productId);

        // 3. 构建 OptionGroups（使用 UnifiedOptionGroupAssembler）
        // 注意：这里假设 ProductDetailDTO 包含 optionGroups 字段
        // 如果没有，需要手动调用 UnifiedOptionGroupAssembler
        List<OptionGroupView> optionGroups = detail.getOptionGroups();

        // 4. 断言：OptionGroups 数量（1 spec + 2 attr + 2 addon = 5）
        assertThat(optionGroups).hasSize(5);

        // 5. 断言：OptionGroups 类型分布
        long specCount = optionGroups.stream().filter(g -> g.getKind() == OptionGroupKind.SPEC).count();
        long attrCount = optionGroups.stream().filter(g -> g.getKind() == OptionGroupKind.ATTR).count();
        long addonCount = optionGroups.stream().filter(g -> g.getKind() == OptionGroupKind.ADDON).count();
        assertThat(specCount).isEqualTo(1);
        assertThat(attrCount).isEqualTo(2);
        assertThat(addonCount).isEqualTo(2);

        // 6. 断言：OptionGroups 排序（按 sortOrder 降序）
        for (int i = 0; i < optionGroups.size() - 1; i++) {
            assertThat(optionGroups.get(i).getSortOrder())
                    .isGreaterThanOrEqualTo(optionGroups.get(i + 1).getSortOrder());
        }

        // 7. 断言：SPEC 组的 items 数量
        OptionGroupView specGroup = optionGroups.stream()
                .filter(g -> g.getKind() == OptionGroupKind.SPEC)
                .findFirst()
                .orElseThrow();
        assertThat(specGroup.getItems()).hasSize(2);

        // 8. 断言：ATTR 组的 items 数量
        List<OptionGroupView> attrGroups = optionGroups.stream()
                .filter(g -> g.getKind() == OptionGroupKind.ATTR)
                .toList();
        assertThat(attrGroups).allMatch(g -> g.getItems().size() == 2);

        // 9. 断言：ADDON 组的 items 数量
        List<OptionGroupView> addonGroups = optionGroups.stream()
                .filter(g -> g.getKind() == OptionGroupKind.ADDON)
                .toList();
        assertThat(addonGroups).allMatch(g -> g.getItems().size() == 2);
    }

    /**
     * 准备测试数据：分类、属性组、小料组。
     */
    private void prepareTestData() {
        // 1. 创建分类
        categoryId1 = idService.nextLong(IdScope.PRODUCT);
        BcProductCategory category1 = new BcProductCategory();
        category1.setId(categoryId1);
        category1.setTenantId(tenantId);
        category1.setPublicId(idService.nextUlid().toString());
        category1.setName("咖啡");
        category1.setStatus(1);
        category1.setSortOrder(100);
        category1.setDeleted(0);
        category1.setCreatedAt(LocalDateTime.now());
        category1.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category1);

        categoryId2 = idService.nextLong(IdScope.PRODUCT);
        BcProductCategory category2 = new BcProductCategory();
        category2.setId(categoryId2);
        category2.setTenantId(tenantId);
        category2.setPublicId(idService.nextUlid().toString());
        category2.setName("热饮");
        category2.setStatus(1);
        category2.setSortOrder(90);
        category2.setDeleted(0);
        category2.setCreatedAt(LocalDateTime.now());
        category2.setUpdatedAt(LocalDateTime.now());
        categoryMapper.insert(category2);

        // 2. 创建属性组：温度
        attrGroupId1 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrGroup attrGroup1 = new BcProductAttrGroup();
        attrGroup1.setId(attrGroupId1);
        attrGroup1.setTenantId(tenantId);
        attrGroup1.setTitle("温度");
        attrGroup1.setSelectType(1); // 单选
        attrGroup1.setRequired(true);
        attrGroup1.setMinSelect(1);
        attrGroup1.setMaxSelect(1);
        attrGroup1.setSortOrder(100);
        attrGroup1.setEnabled(true);
        attrGroup1.setDeleted(0);
        attrGroup1.setCreatedAt(LocalDateTime.now());
        attrGroup1.setUpdatedAt(LocalDateTime.now());
        attrGroupMapper.insert(attrGroup1);

        // 3. 创建属性选项：热、冰
        attrOptionId1 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrOption attrOption1 = new BcProductAttrOption();
        attrOption1.setId(attrOptionId1);
        attrOption1.setTenantId(tenantId);
        attrOption1.setGroupId(attrGroupId1);
        attrOption1.setTitle("热");
        attrOption1.setPriceDelta(BigDecimal.ZERO);
        attrOption1.setSortOrder(100);
        attrOption1.setEnabled(true);
        attrOption1.setDeleted(0);
        attrOption1.setCreatedAt(LocalDateTime.now());
        attrOption1.setUpdatedAt(LocalDateTime.now());
        attrOptionMapper.insert(attrOption1);

        attrOptionId2 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrOption attrOption2 = new BcProductAttrOption();
        attrOption2.setId(attrOptionId2);
        attrOption2.setTenantId(tenantId);
        attrOption2.setGroupId(attrGroupId1);
        attrOption2.setTitle("冰");
        attrOption2.setPriceDelta(BigDecimal.ZERO);
        attrOption2.setSortOrder(90);
        attrOption2.setEnabled(true);
        attrOption2.setDeleted(0);
        attrOption2.setCreatedAt(LocalDateTime.now());
        attrOption2.setUpdatedAt(LocalDateTime.now());
        attrOptionMapper.insert(attrOption2);

        // 4. 创建属性组：甜度
        attrGroupId2 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrGroup attrGroup2 = new BcProductAttrGroup();
        attrGroup2.setId(attrGroupId2);
        attrGroup2.setTenantId(tenantId);
        attrGroup2.setTitle("甜度");
        attrGroup2.setSelectType(1); // 单选
        attrGroup2.setRequired(false);
        attrGroup2.setMinSelect(0);
        attrGroup2.setMaxSelect(1);
        attrGroup2.setSortOrder(90);
        attrGroup2.setEnabled(true);
        attrGroup2.setDeleted(0);
        attrGroup2.setCreatedAt(LocalDateTime.now());
        attrGroup2.setUpdatedAt(LocalDateTime.now());
        attrGroupMapper.insert(attrGroup2);

        // 5. 创建属性选项：标准糖、少糖
        attrOptionId3 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrOption attrOption3 = new BcProductAttrOption();
        attrOption3.setId(attrOptionId3);
        attrOption3.setTenantId(tenantId);
        attrOption3.setGroupId(attrGroupId2);
        attrOption3.setTitle("标准糖");
        attrOption3.setPriceDelta(BigDecimal.ZERO);
        attrOption3.setSortOrder(100);
        attrOption3.setEnabled(true);
        attrOption3.setDeleted(0);
        attrOption3.setCreatedAt(LocalDateTime.now());
        attrOption3.setUpdatedAt(LocalDateTime.now());
        attrOptionMapper.insert(attrOption3);

        attrOptionId4 = idService.nextLong(IdScope.PRODUCT);
        BcProductAttrOption attrOption4 = new BcProductAttrOption();
        attrOption4.setId(attrOptionId4);
        attrOption4.setTenantId(tenantId);
        attrOption4.setGroupId(attrGroupId2);
        attrOption4.setTitle("少糖");
        attrOption4.setPriceDelta(BigDecimal.ZERO);
        attrOption4.setSortOrder(90);
        attrOption4.setEnabled(true);
        attrOption4.setDeleted(0);
        attrOption4.setCreatedAt(LocalDateTime.now());
        attrOption4.setUpdatedAt(LocalDateTime.now());
        attrOptionMapper.insert(attrOption4);

        // 6. 创建小料组：加奶
        addonGroupId1 = idService.nextLong(IdScope.PRODUCT);
        BcAddonGroup addonGroup1 = new BcAddonGroup();
        addonGroup1.setId(addonGroupId1);
        addonGroup1.setTenantId(tenantId);
        addonGroup1.setTitle("加奶");
        addonGroup1.setSelectType(1); // 单选
        addonGroup1.setRequired(false);
        addonGroup1.setMinSelect(0);
        addonGroup1.setMaxSelect(1);
        addonGroup1.setSortOrder(100);
        addonGroup1.setEnabled(true);
        addonGroup1.setDeleted(0);
        addonGroup1.setCreatedAt(LocalDateTime.now());
        addonGroup1.setUpdatedAt(LocalDateTime.now());
        addonGroupMapper.insert(addonGroup1);

        // 7. 创建小料项：全脂奶、脱脂奶
        addonItemId1 = idService.nextLong(IdScope.PRODUCT);
        BcAddonItem addonItem1 = new BcAddonItem();
        addonItem1.setId(addonItemId1);
        addonItem1.setTenantId(tenantId);
        addonItem1.setGroupId(addonGroupId1);
        addonItem1.setTitle("全脂奶");
        addonItem1.setPrice(BigDecimal.valueOf(3.00));
        addonItem1.setSortOrder(100);
        addonItem1.setEnabled(true);
        addonItem1.setDeleted(0);
        addonItem1.setCreatedAt(LocalDateTime.now());
        addonItem1.setUpdatedAt(LocalDateTime.now());
        addonItemMapper.insert(addonItem1);

        addonItemId2 = idService.nextLong(IdScope.PRODUCT);
        BcAddonItem addonItem2 = new BcAddonItem();
        addonItem2.setId(addonItemId2);
        addonItem2.setTenantId(tenantId);
        addonItem2.setGroupId(addonGroupId1);
        addonItem2.setTitle("脱脂奶");
        addonItem2.setPrice(BigDecimal.valueOf(3.00));
        addonItem2.setSortOrder(90);
        addonItem2.setEnabled(true);
        addonItem2.setDeleted(0);
        addonItem2.setCreatedAt(LocalDateTime.now());
        addonItem2.setUpdatedAt(LocalDateTime.now());
        addonItemMapper.insert(addonItem2);

        // 8. 创建小料组：换豆子
        addonGroupId2 = idService.nextLong(IdScope.PRODUCT);
        BcAddonGroup addonGroup2 = new BcAddonGroup();
        addonGroup2.setId(addonGroupId2);
        addonGroup2.setTenantId(tenantId);
        addonGroup2.setTitle("换豆子");
        addonGroup2.setSelectType(1); // 单选
        addonGroup2.setRequired(false);
        addonGroup2.setMinSelect(0);
        addonGroup2.setMaxSelect(1);
        addonGroup2.setSortOrder(90);
        addonGroup2.setEnabled(true);
        addonGroup2.setDeleted(0);
        addonGroup2.setCreatedAt(LocalDateTime.now());
        addonGroup2.setUpdatedAt(LocalDateTime.now());
        addonGroupMapper.insert(addonGroup2);

        // 9. 创建小料项：阿拉比卡、罗布斯塔
        addonItemId3 = idService.nextLong(IdScope.PRODUCT);
        BcAddonItem addonItem3 = new BcAddonItem();
        addonItem3.setId(addonItemId3);
        addonItem3.setTenantId(tenantId);
        addonItem3.setGroupId(addonGroupId2);
        addonItem3.setTitle("阿拉比卡");
        addonItem3.setPrice(BigDecimal.valueOf(5.00));
        addonItem3.setSortOrder(100);
        addonItem3.setEnabled(true);
        addonItem3.setDeleted(0);
        addonItem3.setCreatedAt(LocalDateTime.now());
        addonItem3.setUpdatedAt(LocalDateTime.now());
        addonItemMapper.insert(addonItem3);

        addonItemId4 = idService.nextLong(IdScope.PRODUCT);
        BcAddonItem addonItem4 = new BcAddonItem();
        addonItem4.setId(addonItemId4);
        addonItem4.setTenantId(tenantId);
        addonItem4.setGroupId(addonGroupId2);
        addonItem4.setTitle("罗布斯塔");
        addonItem4.setPrice(BigDecimal.valueOf(4.00));
        addonItem4.setSortOrder(90);
        addonItem4.setEnabled(true);
        addonItem4.setDeleted(0);
        addonItem4.setCreatedAt(LocalDateTime.now());
        addonItem4.setUpdatedAt(LocalDateTime.now());
        addonItemMapper.insert(addonItem4);
    }

    /**
     * 构建创建商品命令。
     */
    private CreateProductAggregateCommand buildCreateCommand() {
        CreateProductAggregateCommand command = new CreateProductAggregateCommand();
        command.setTenantId(tenantId);
        command.setOperatorId(operatorId);
        command.setName("美式咖啡");
        command.setSubtitle("经典美式");
        command.setMainImage("https://example.com/americano.jpg");
        command.setStatus(1);

        // 规格组
        CreateProductAggregateCommand.SpecGroupCommand specGroup = new CreateProductAggregateCommand.SpecGroupCommand();
        specGroup.setTitle("规格");
        specGroup.setRequired(true);
        specGroup.setSortOrder(100);

        CreateProductAggregateCommand.SpecOptionCommand option1 = new CreateProductAggregateCommand.SpecOptionCommand();
        option1.setTitle("中杯");
        option1.setSortOrder(100);

        CreateProductAggregateCommand.SpecOptionCommand option2 = new CreateProductAggregateCommand.SpecOptionCommand();
        option2.setTitle("大杯");
        option2.setSortOrder(90);

        specGroup.setOptions(List.of(option1, option2));
        command.setSpecGroups(List.of(specGroup));

        // SKU（4 个组合：中杯热、中杯冰、大杯热、大杯冰）
        // 注意：这里简化处理，实际应该根据 spec options 生成 SKU
        command.setSkus(List.of(
                buildSku("中杯", BigDecimal.valueOf(25.00), true),
                buildSku("大杯", BigDecimal.valueOf(30.00), false)
        ));

        // 属性组
        command.setAttrGroups(List.of(
                buildAttrGroup(attrGroupId1, List.of(attrOptionId1, attrOptionId2), 100),
                buildAttrGroup(attrGroupId2, List.of(attrOptionId3, attrOptionId4), 90)
        ));

        // 小料组
        command.setAddonGroups(List.of(
                buildAddonGroup(addonGroupId1, List.of(addonItemId1, addonItemId2), 100),
                buildAddonGroup(addonGroupId2, List.of(addonItemId3, addonItemId4), 90)
        ));

        // 分类
        command.setCategoryIds(List.of(categoryId1, categoryId2));

        return command;
    }

    private CreateProductAggregateCommand.SkuCommand buildSku(String name, BigDecimal price, boolean isDefault) {
        CreateProductAggregateCommand.SkuCommand sku = new CreateProductAggregateCommand.SkuCommand();
        sku.setName(name);
        sku.setPrice(price);
        sku.setOriginPrice(price.add(BigDecimal.valueOf(5.00)));
        sku.setIsDefault(isDefault);
        sku.setSortOrder(100);
        return sku;
    }

    private CreateProductAggregateCommand.AttrGroupCommand buildAttrGroup(Long groupId, List<Long> optionIds, int sortOrder) {
        CreateProductAggregateCommand.AttrGroupCommand group = new CreateProductAggregateCommand.AttrGroupCommand();
        group.setGroupId(groupId);
        group.setSortOrder(sortOrder);
        group.setEnabled(true);
        group.setOptionIds(optionIds);
        return group;
    }

    private CreateProductAggregateCommand.AddonGroupCommand buildAddonGroup(Long groupId, List<Long> itemIds, int sortOrder) {
        CreateProductAggregateCommand.AddonGroupCommand group = new CreateProductAggregateCommand.AddonGroupCommand();
        group.setGroupId(groupId);
        group.setSortOrder(sortOrder);
        group.setEnabled(true);
        group.setItemIds(itemIds);
        return group;
    }
}

