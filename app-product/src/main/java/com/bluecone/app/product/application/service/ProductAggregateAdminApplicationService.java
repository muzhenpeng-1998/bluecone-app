package com.bluecone.app.product.application.service;

import com.bluecone.app.core.cacheinval.api.CacheInvalidationEvent;
import com.bluecone.app.core.cacheinval.api.CacheInvalidationPublisher;
import com.bluecone.app.core.cacheinval.api.InvalidationScope;
import com.bluecone.app.core.contextkit.CacheNamespaces;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.product.application.command.CreateProductAggregateCommand;
import com.bluecone.app.product.application.command.UpdateProductAggregateCommand;
import com.bluecone.app.product.application.dto.ProductDetailDTO;
import com.bluecone.app.product.dao.entity.*;
import com.bluecone.app.product.dao.mapper.*;
import com.bluecone.app.product.domain.enums.ProductStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 商品聚合管理应用服务
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商品聚合的创建（Product + SKU + Spec + Attr + Addon + Category）</li>
 *   <li>商品聚合的更新（子表全量覆盖 delete+insert 策略）</li>
 *   <li>商品详情查询（完整聚合结构回显）</li>
 *   <li>商品状态修改（草稿/启用/禁用）</li>
 * </ul>
 * 
 * <h3>💡 设计原则：</h3>
 * <ul>
 *   <li><b>高可靠写入</b>：@Transactional 保证原子性</li>
 *   <li><b>子表全量覆盖</b>：update 采用 delete+insert 策略，优先保证正确性</li>
 *   <li><b>强校验</b>：引用存在性、租户归属、规则合法性、SKU 组合一致性</li>
 *   <li><b>Public ID 生成</b>：对 product/sku 生成 public_id</li>
 *   <li><b>缓存失效</b>：afterCommit 发布 CacheInvalidationEvent</li>
 * </ul>
 * 
 * <h3>🔄 写入时序图（注释形式）：</h3>
 * <pre>
 * 【创建商品聚合】
 * 1. 开启事务（@Transactional）
 * 2. 强校验：引用存在性、租户归属、规则合法性、SKU 组合一致性
 * 3. 生成 ID：product.id（IdService.nextLong）、product.public_id（IdService.nextPublicId）
 * 4. 插入 bc_product
 * 5. 插入 bc_product_sku（生成 sku.id、sku.public_id）
 * 6. 插入 bc_product_spec_group + bc_product_spec_option
 * 7. 插入 bc_product_attr_group_rel + bc_product_attr_rel
 * 8. 插入 bc_product_addon_group_rel + bc_product_addon_rel
 * 9. 插入 bc_product_category_rel
 * 10. 提交事务
 * 11. afterCommit：发布 CacheInvalidationEvent（失效菜单快照）
 * 
 * 【更新商品聚合】
 * 1. 开启事务（@Transactional）
 * 2. 强校验：引用存在性、租户归属、规则合法性、SKU 组合一致性
 * 3. 更新 bc_product
 * 4. 子表全量覆盖（delete+insert）：
 *    - 删除旧的 bc_product_sku，插入新的 bc_product_sku
 *    - 删除旧的 bc_product_spec_group + bc_product_spec_option，插入新的
 *    - 删除旧的 bc_product_attr_group_rel + bc_product_attr_rel，插入新的
 *    - 删除旧的 bc_product_addon_group_rel + bc_product_addon_rel，插入新的
 *    - 删除旧的 bc_product_category_rel，插入新的
 * 5. 提交事务
 * 6. afterCommit：发布 CacheInvalidationEvent（失效菜单快照）
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductAggregateAdminApplicationService {
    
    // ===== Mappers =====
    private final BcProductMapper productMapper;
    private final BcProductSkuMapper skuMapper;
    private final BcProductSpecGroupMapper specGroupMapper;
    private final BcProductSpecOptionMapper specOptionMapper;
    private final BcProductAttrGroupMapper attrGroupMapper;
    private final BcProductAttrGroupRelMapper attrGroupRelMapper;
    private final BcProductAttrOptionMapper attrOptionMapper;
    private final BcProductAttrRelMapper attrRelMapper;
    private final BcAddonGroupMapper addonGroupMapper;
    private final BcAddonItemMapper addonItemMapper;
    private final BcProductAddonGroupRelMapper addonGroupRelMapper;
    private final BcProductAddonRelMapper addonRelMapper;
    private final BcProductCategoryMapper categoryMapper;
    private final BcProductCategoryRelMapper categoryRelMapper;
    private final BcProductStoreConfigMapper storeConfigMapper;
    
    // ===== Services =====
    private final IdService idService;
    
    @Autowired(required = false)
    @Nullable
    private CacheInvalidationPublisher cacheInvalidationPublisher;
    
    @Autowired(required = false)
    @Nullable
    private com.bluecone.app.product.infrastructure.cache.MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;
    
    /**
     * 创建商品聚合
     * 
     * <p>创建完整的商品聚合，包括商品基本信息、SKU、规格、属性、小料、分类绑定。
     * 
     * @param command 创建命令
     * @return 创建的商品ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateProductAggregateCommand command) {
        Long tenantId = command.getTenantId();
        Long operatorId = command.getOperatorId();
        
        log.info("创建商品聚合: tenantId={}, name={}", tenantId, command.getName());
        
        // ===== 1. 强校验 =====
        validateCreateCommand(command);
        
        // ===== 2. 生成 public_id（对外 ID）=====
        String productPublicId = idService.nextPublicId(ResourceType.PRODUCT);
        
        // ===== 3. 插入 bc_product（DB 自增生成 id）=====
        BcProduct product = new BcProduct();
        // 不设置 id，让 DB AUTO 生成
        product.setTenantId(tenantId);
        product.setPublicId(productPublicId);
        product.setProductCode(command.getProductCode());
        product.setName(command.getName());
        product.setSubtitle(command.getSubtitle());
        product.setProductType(command.getProductType());
        product.setDescription(command.getDescription());
        product.setMainImage(command.getMainImage());
        product.setMediaGallery(command.getMediaGallery() != null ? 
                String.join(",", command.getMediaGallery()) : null);
        product.setUnit(command.getUnit());
        // 根据 publishNow 设置状态
        product.setStatus(Boolean.TRUE.equals(command.getPublishNow()) ? 1 : 0);
        product.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        product.setCreatedBy(operatorId);
        product.setUpdatedBy(operatorId);
        product.setDeleted(0);
        
        productMapper.insert(product);
        
        // insert 后获取 DB 自增的 id
        Long productId = product.getId();
        log.info("商品基本信息已插入: productId={}, publicId={}, status={}", productId, productPublicId, product.getStatus());
        
        // ===== 4. 插入 bc_product_sku =====
        if (command.getSkus() != null && !command.getSkus().isEmpty()) {
            insertSkus(tenantId, productId, command.getSkus(), operatorId);
        }
        
        // ===== 5. 插入 bc_product_spec_group + bc_product_spec_option =====
        if (command.getSpecGroups() != null && !command.getSpecGroups().isEmpty()) {
            insertSpecGroups(tenantId, productId, command.getSpecGroups(), operatorId);
        }
        
        // ===== 6. 插入 bc_product_attr_group_rel + bc_product_attr_rel =====
        if (command.getAttrGroups() != null && !command.getAttrGroups().isEmpty()) {
            insertAttrGroupBindings(tenantId, productId, command.getAttrGroups(), operatorId);
        }
        
        // ===== 7. 插入 bc_product_addon_group_rel + bc_product_addon_rel =====
        if (command.getAddonGroups() != null && !command.getAddonGroups().isEmpty()) {
            insertAddonGroupBindings(tenantId, productId, command.getAddonGroups(), operatorId);
        }
        
        // ===== 8. 插入 bc_product_category_rel =====
        if (command.getCategoryIds() != null && !command.getCategoryIds().isEmpty()) {
            insertCategoryBindings(tenantId, productId, command.getCategoryIds(), operatorId);
        }
        
        // ===== 9. Prompt 06: 创建后立即上架（如果传了 storeId）=====
        if (command.getStoreId() != null) {
            insertStoreConfig(tenantId, command.getStoreId(), productId, command.getChannel(), operatorId);
            log.info("商品已自动上架到门店: tenantId={}, storeId={}, productId={}, channel={}", 
                    tenantId, command.getStoreId(), productId, command.getChannel());
        }
        
        // ===== 10. afterCommit：发布缓存失效事件 =====
        publishMenuSnapshotInvalidation(tenantId);
        
        log.info("商品聚合创建成功: tenantId={}, productId={}, publicId={}", 
                tenantId, productId, productPublicId);
        return productId;
    }
    
    /**
     * 更新商品聚合
     * 
     * <p>更新商品聚合，采用子表全量覆盖策略（delete+insert）。
     * 
     * @param productId 商品ID
     * @param command 更新命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long productId, UpdateProductAggregateCommand command) {
        Long tenantId = command.getTenantId();
        Long operatorId = command.getOperatorId();
        
        log.info("更新商品聚合: tenantId={}, productId={}", tenantId, productId);
        
        // ===== 1. 查询商品是否存在 =====
        BcProduct product = productMapper.selectOne(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getId, productId)
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getDeleted, 0));
        
        if (product == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商品不存在或无权访问");
        }
        
        // ===== 2. 强校验 =====
        validateUpdateCommand(command);
        
        // ===== 3. 更新 bc_product =====
        product.setName(command.getName());
        product.setSubtitle(command.getSubtitle());
        product.setDescription(command.getDescription());
        product.setMainImage(command.getMainImage());
        product.setMediaGallery(command.getMediaGallery() != null ? 
                String.join(",", command.getMediaGallery()) : null);
        product.setUnit(command.getUnit());
        product.setSortOrder(command.getSortOrder() != null ? command.getSortOrder() : 0);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(operatorId);
        
        productMapper.updateById(product);
        log.info("商品基本信息已更新: productId={}", productId);
        
        // ===== 4. 子表全量覆盖（delete+insert）=====
        
        // 4.1 删除并重新插入 SKU
        skuMapper.delete(new LambdaQueryWrapper<BcProductSku>()
                .eq(BcProductSku::getTenantId, tenantId)
                .eq(BcProductSku::getProductId, productId));
        if (command.getSkus() != null && !command.getSkus().isEmpty()) {
            insertSkus(tenantId, productId, command.getSkus(), operatorId);
        }
        
        // 4.2 删除并重新插入规格组和规格选项
        specOptionMapper.delete(new LambdaQueryWrapper<BcProductSpecOption>()
                .eq(BcProductSpecOption::getTenantId, tenantId)
                .eq(BcProductSpecOption::getProductId, productId));
        specGroupMapper.delete(new LambdaQueryWrapper<BcProductSpecGroup>()
                .eq(BcProductSpecGroup::getTenantId, tenantId)
                .eq(BcProductSpecGroup::getProductId, productId));
        if (command.getSpecGroups() != null && !command.getSpecGroups().isEmpty()) {
            insertSpecGroups(tenantId, productId, command.getSpecGroups(), operatorId);
        }
        
        // 4.3 删除并重新插入属性组绑定和属性选项覆盖
        attrRelMapper.delete(new LambdaQueryWrapper<BcProductAttrRel>()
                .eq(BcProductAttrRel::getTenantId, tenantId)
                .eq(BcProductAttrRel::getProductId, productId));
        attrGroupRelMapper.delete(new LambdaQueryWrapper<BcProductAttrGroupRel>()
                .eq(BcProductAttrGroupRel::getTenantId, tenantId)
                .eq(BcProductAttrGroupRel::getProductId, productId));
        if (command.getAttrGroups() != null && !command.getAttrGroups().isEmpty()) {
            insertAttrGroupBindings(tenantId, productId, command.getAttrGroups(), operatorId);
        }
        
        // 4.4 删除并重新插入小料组绑定和小料项覆盖
        addonRelMapper.delete(new LambdaQueryWrapper<BcProductAddonRel>()
                .eq(BcProductAddonRel::getTenantId, tenantId)
                .eq(BcProductAddonRel::getProductId, productId));
        addonGroupRelMapper.delete(new LambdaQueryWrapper<BcProductAddonGroupRel>()
                .eq(BcProductAddonGroupRel::getTenantId, tenantId)
                .eq(BcProductAddonGroupRel::getProductId, productId));
        if (command.getAddonGroups() != null && !command.getAddonGroups().isEmpty()) {
            insertAddonGroupBindings(tenantId, productId, command.getAddonGroups(), operatorId);
        }
        
        // 4.5 删除并重新插入分类绑定
        categoryRelMapper.delete(new LambdaQueryWrapper<BcProductCategoryRel>()
                .eq(BcProductCategoryRel::getTenantId, tenantId)
                .eq(BcProductCategoryRel::getProductId, productId));
        if (command.getCategoryIds() != null && !command.getCategoryIds().isEmpty()) {
            insertCategoryBindings(tenantId, productId, command.getCategoryIds(), operatorId);
        }
        
        // ===== 5. afterCommit：发布缓存失效事件 =====
        publishMenuSnapshotInvalidation(tenantId);
        
        log.info("商品聚合更新成功: tenantId={}, productId={}", tenantId, productId);
    }
    
    /**
     * 查询商品详情
     * 
     * <p>查询完整的商品聚合结构，用于回显编辑。
     * 
     * @param tenantId 租户ID
     * @param productId 商品ID
     * @return 商品详情DTO
     */
    public ProductDetailDTO getDetail(Long tenantId, Long productId) {
        log.info("查询商品详情: tenantId={}, productId={}", tenantId, productId);
        
        // TODO: 实现详情查询逻辑
        // 1. 查询 bc_product
        // 2. 查询 bc_product_sku
        // 3. 查询 bc_product_spec_group + bc_product_spec_option
        // 4. 查询 bc_product_attr_group_rel + bc_product_attr_rel + bc_product_attr_group + bc_product_attr_option
        // 5. 查询 bc_product_addon_group_rel + bc_product_addon_rel + bc_addon_group + bc_addon_item
        // 6. 查询 bc_product_category_rel + bc_product_category
        // 7. 组装为 ProductDetailDTO
        
        throw new UnsupportedOperationException("详情查询功能待实现");
    }
    
    /**
     * 修改商品状态
     * 
     * <p>修改商品状态（草稿/启用/禁用）。
     * 
     * @param tenantId 租户ID
     * @param productId 商品ID
     * @param status 新状态（0=草稿，1=启用，-1=禁用）
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long tenantId, Long productId, Integer status, Long operatorId) {
        log.info("修改商品状态: tenantId={}, productId={}, status={}", tenantId, productId, status);
        
        // 查询商品是否存在
        BcProduct product = productMapper.selectOne(new LambdaQueryWrapper<BcProduct>()
                .eq(BcProduct::getId, productId)
                .eq(BcProduct::getTenantId, tenantId)
                .eq(BcProduct::getDeleted, 0));
        
        if (product == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商品不存在或无权访问");
        }
        
        // 更新状态
        product.setStatus(status);
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(operatorId);
        productMapper.updateById(product);
        
        // afterCommit：发布缓存失效事件
        publishMenuSnapshotInvalidation(tenantId);
        
        log.info("商品状态修改成功: tenantId={}, productId={}, status={}", tenantId, productId, status);
    }
    
    // ===== 私有方法：插入子表 =====
    
    /**
     * 插入 SKU 列表（修复：DB AUTO 生成 id + 根据 publishNow 设置 status + 序列化 specCombination）
     */
    private void insertSkus(Long tenantId, Long productId, 
                           List<CreateProductAggregateCommand.SkuRequest> skuRequests, 
                           Long operatorId) {
        // 获取 product 的 status 来决定 SKU 的 status
        BcProduct product = productMapper.selectById(productId);
        int skuStatus = (product != null && product.getStatus() != null && product.getStatus() == 1) ? 1 : 0;
        
        for (CreateProductAggregateCommand.SkuRequest skuReq : skuRequests) {
            String skuPublicId = idService.nextPublicId(ResourceType.SKU);
            
            BcProductSku sku = new BcProductSku();
            // 不设置 id，让 DB AUTO 生成
            sku.setTenantId(tenantId);
            sku.setPublicId(skuPublicId);
            sku.setProductId(productId);
            sku.setSkuCode(skuReq.getSkuCode());
            sku.setName(skuReq.getName());
            sku.setBasePrice(skuReq.getBasePrice());
            sku.setMarketPrice(skuReq.getMarketPrice());
            sku.setCostPrice(skuReq.getCostPrice());
            sku.setBarcode(skuReq.getBarcode());
            sku.setIsDefault(skuReq.isDefaultSku());
            sku.setStatus(skuStatus); // 跟随 product 的 status
            sku.setSortOrder(skuReq.getSortOrder() != null ? skuReq.getSortOrder() : 0);
            
            // 序列化 specCombination 到 JSON
            if (skuReq.getSpecCombination() != null && !skuReq.getSpecCombination().isEmpty()) {
                try {
                    String specCombinationJson = new com.fasterxml.jackson.databind.ObjectMapper()
                            .writeValueAsString(skuReq.getSpecCombination());
                    sku.setSpecCombination(specCombinationJson);
                } catch (Exception e) {
                    log.error("序列化 specCombination 失败: skuName={}", skuReq.getName(), e);
                    sku.setSpecCombination(null);
                }
            }
            
            sku.setCreatedAt(LocalDateTime.now());
            sku.setUpdatedAt(LocalDateTime.now());
            sku.setDeleted(0);
            
            skuMapper.insert(sku);
        }
        log.info("SKU已插入: productId={}, count={}, status={}", productId, skuRequests.size(), skuStatus);
    }
    
    /**
     * 插入规格组和规格选项（修复：DB AUTO 生成 id）
     */
    private void insertSpecGroups(Long tenantId, Long productId,
                                  List<CreateProductAggregateCommand.SpecGroupRequest> specGroupRequests,
                                  Long operatorId) {
        for (CreateProductAggregateCommand.SpecGroupRequest groupReq : specGroupRequests) {
            BcProductSpecGroup group = new BcProductSpecGroup();
            // 不设置 id，让 DB AUTO 生成
            group.setTenantId(tenantId);
            group.setProductId(productId);
            group.setName(groupReq.getName());
            group.setSelectType(groupReq.getSelectType());
            group.setRequired(groupReq.getRequired());
            group.setMaxSelect(groupReq.getMaxSelect());
            group.setStatus(1);
            group.setSortOrder(groupReq.getSortOrder() != null ? groupReq.getSortOrder() : 0);
            group.setCreatedAt(LocalDateTime.now());
            group.setUpdatedAt(LocalDateTime.now());
            
            specGroupMapper.insert(group);
            
            // insert 后获取 DB 自增的 id
            Long groupId = group.getId();
            
            // 插入规格选项
            if (groupReq.getOptions() != null && !groupReq.getOptions().isEmpty()) {
                for (CreateProductAggregateCommand.SpecOptionRequest optionReq : groupReq.getOptions()) {
                    BcProductSpecOption option = new BcProductSpecOption();
                    // 不设置 id，让 DB AUTO 生成
                    option.setTenantId(tenantId);
                    option.setProductId(productId);
                    option.setSpecGroupId(groupId);
                    option.setName(optionReq.getName());
                    option.setPriceDelta(optionReq.getPriceDelta());
                    option.setIsDefault(optionReq.getIsDefault() != null ? optionReq.getIsDefault() : false);
                    option.setStatus(1);
                    option.setSortOrder(optionReq.getSortOrder() != null ? optionReq.getSortOrder() : 0);
                    option.setCreatedAt(LocalDateTime.now());
                    option.setUpdatedAt(LocalDateTime.now());
                    
                    specOptionMapper.insert(option);
                }
            }
        }
        log.info("规格组已插入: productId={}, count={}", productId, specGroupRequests.size());
    }
    
    /**
     * 插入属性组绑定和属性选项覆盖（修复：DB AUTO 生成 id）
     */
    private void insertAttrGroupBindings(Long tenantId, Long productId,
                                        List<CreateProductAggregateCommand.AttrGroupBinding> attrGroupBindings,
                                        Long operatorId) {
        for (CreateProductAggregateCommand.AttrGroupBinding binding : attrGroupBindings) {
            BcProductAttrGroupRel groupRel = new BcProductAttrGroupRel();
            // 不设置 id，让 DB AUTO 生成
            groupRel.setTenantId(tenantId);
            groupRel.setProductId(productId);
            groupRel.setAttrGroupId(binding.getGroupId());
            groupRel.setRequired(binding.getRequired());
            groupRel.setMinSelect(binding.getMinSelect());
            groupRel.setMaxSelect(binding.getMaxSelect());
            groupRel.setStatus(binding.getEnabled() ? 1 : 0);
            groupRel.setSortOrder(binding.getSortOrder() != null ? binding.getSortOrder() : 0);
            groupRel.setDisplayStartAt(binding.getDisplayStartAt());
            groupRel.setDisplayEndAt(binding.getDisplayEndAt());
            groupRel.setCreatedAt(LocalDateTime.now());
            groupRel.setUpdatedAt(LocalDateTime.now());
            groupRel.setDeleted(0);
            
            attrGroupRelMapper.insert(groupRel);
            
            // 插入属性选项覆盖
            if (binding.getOptionOverrides() != null && !binding.getOptionOverrides().isEmpty()) {
                for (CreateProductAggregateCommand.AttrOptionOverride override : binding.getOptionOverrides()) {
                    BcProductAttrRel attrRel = new BcProductAttrRel();
                    // 不设置 id，让 DB AUTO 生成
                    attrRel.setTenantId(tenantId);
                    attrRel.setProductId(productId);
                    attrRel.setAttrGroupId(binding.getGroupId());
                    attrRel.setAttrOptionId(override.getOptionId());
                    attrRel.setPriceDeltaOverride(override.getPriceDeltaOverride());
                    attrRel.setStatus(override.getEnabled() ? 1 : 0);
                    attrRel.setSortOrder(override.getSortOrder() != null ? override.getSortOrder() : 0);
                    attrRel.setCreatedAt(LocalDateTime.now());
                    attrRel.setUpdatedAt(LocalDateTime.now());
                    attrRel.setDeleted(0);
                    
                    attrRelMapper.insert(attrRel);
                }
            }
        }
        log.info("属性组绑定已插入: productId={}, count={}", productId, attrGroupBindings.size());
    }
    
    /**
     * 插入小料组绑定和小料项覆盖（修复：DB AUTO 生成 id）
     */
    private void insertAddonGroupBindings(Long tenantId, Long productId,
                                         List<CreateProductAggregateCommand.AddonGroupBinding> addonGroupBindings,
                                         Long operatorId) {
        for (CreateProductAggregateCommand.AddonGroupBinding binding : addonGroupBindings) {
            BcProductAddonGroupRel groupRel = new BcProductAddonGroupRel();
            // 不设置 id，让 DB AUTO 生成
            groupRel.setTenantId(tenantId);
            groupRel.setProductId(productId);
            groupRel.setAddonGroupId(binding.getGroupId());
            groupRel.setRequired(binding.getRequired());
            groupRel.setMinSelect(binding.getMinSelect());
            groupRel.setMaxSelect(binding.getMaxSelect());
            groupRel.setMaxTotalQuantity(binding.getMaxTotal());
            groupRel.setStatus(binding.getEnabled() ? 1 : 0);
            groupRel.setSortOrder(binding.getSortOrder() != null ? binding.getSortOrder() : 0);
            groupRel.setDisplayStartAt(binding.getDisplayStartAt());
            groupRel.setDisplayEndAt(binding.getDisplayEndAt());
            groupRel.setCreatedAt(LocalDateTime.now());
            groupRel.setUpdatedAt(LocalDateTime.now());
            groupRel.setDeleted(0);
            
            addonGroupRelMapper.insert(groupRel);
            
            // 插入小料项覆盖
            if (binding.getItemOverrides() != null && !binding.getItemOverrides().isEmpty()) {
                for (CreateProductAggregateCommand.AddonItemOverride override : binding.getItemOverrides()) {
                    BcProductAddonRel addonRel = new BcProductAddonRel();
                    // 不设置 id，让 DB AUTO 生成
                    addonRel.setTenantId(tenantId);
                    addonRel.setProductId(productId);
                    addonRel.setAddonGroupId(binding.getGroupId());
                    addonRel.setAddonItemId(override.getItemId());
                    addonRel.setPriceOverride(override.getPriceOverride());
                    addonRel.setMaxQuantityOverride(override.getMaxQuantityOverride());
                    addonRel.setStatus(override.getEnabled() ? 1 : 0);
                    addonRel.setSortOrder(override.getSortOrder() != null ? override.getSortOrder() : 0);
                    addonRel.setCreatedAt(LocalDateTime.now());
                    addonRel.setUpdatedAt(LocalDateTime.now());
                    addonRel.setDeleted(0);
                    
                    addonRelMapper.insert(addonRel);
                }
            }
        }
        log.info("小料组绑定已插入: productId={}, count={}", productId, addonGroupBindings.size());
    }
    
    /**
     * 插入分类绑定（修复：DB AUTO 生成 id）
     */
    private void insertCategoryBindings(Long tenantId, Long productId, 
                                       List<Long> categoryIds, 
                                       Long operatorId) {
        for (Long categoryId : categoryIds) {
            BcProductCategoryRel categoryRel = new BcProductCategoryRel();
            // 不设置 id，让 DB AUTO 生成
            categoryRel.setTenantId(tenantId);
            categoryRel.setCategoryId(categoryId);
            categoryRel.setProductId(productId);
            categoryRel.setSortOrder(0);
            categoryRel.setStatus(1);
            categoryRel.setCreatedAt(LocalDateTime.now());
            categoryRel.setUpdatedAt(LocalDateTime.now());
            categoryRel.setDeleted(0);
            
            categoryRelMapper.insert(categoryRel);
        }
        log.info("分类绑定已插入: productId={}, count={}", productId, categoryIds.size());
    }
    
    /**
     * 插入门店配置（自动上架）（修复：DB AUTO 生成 id）
     * 
     * <p>Prompt 06: 创建后立即上架
     */
    private void insertStoreConfig(Long tenantId, Long storeId, Long productId, 
                                   String channel, Long operatorId) {
        String channelCode = channel != null ? channel.toUpperCase() : "ALL";
        
        BcProductStoreConfig config = new BcProductStoreConfig();
        // 不设置 id，让 DB AUTO 生成
        config.setTenantId(tenantId);
        config.setStoreId(storeId);
        config.setProductId(productId);
        config.setSkuId(null); // SPU 级别配置
        config.setChannel(channelCode);
        config.setVisible(true); // 自动上架
        config.setStatus(1); // 启用
        config.setSortOrder(0);
        config.setDeleted(0);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        config.setCreatedBy(operatorId);
        config.setUpdatedBy(operatorId);
        
        storeConfigMapper.insert(config);
    }
    
    // ===== 私有方法：强校验 =====
    
    /**
     * 校验创建命令
     */
    private void validateCreateCommand(CreateProductAggregateCommand command) {
        Long tenantId = command.getTenantId();
        
        // 1. 校验 SKU 必须 >=1
        if (command.getSkus() == null || command.getSkus().isEmpty()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商品必须至少有一个SKU");
        }
        
        // 2. 校验默认 SKU 只有一个
        long defaultSkuCount = command.getSkus().stream()
                .filter(CreateProductAggregateCommand.SkuRequest::isDefaultSku)
                .count();
        if (defaultSkuCount != 1) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "商品必须有且仅有一个默认SKU");
        }
        
        // 3. 校验 SKU 价格 >=0
        for (CreateProductAggregateCommand.SkuRequest sku : command.getSkus()) {
            if (sku.getBasePrice() != null && sku.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, "SKU价格不能小于0");
            }
        }
        
        // 4. 校验分类引用存在且归属租户
        if (command.getCategoryIds() != null && !command.getCategoryIds().isEmpty()) {
            for (Long categoryId : command.getCategoryIds()) {
                BcProductCategory category = categoryMapper.selectOne(new LambdaQueryWrapper<BcProductCategory>()
                        .eq(BcProductCategory::getId, categoryId)
                        .eq(BcProductCategory::getTenantId, tenantId)
                        .eq(BcProductCategory::getDeleted, 0));
                if (category == null) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "分类不存在或无权访问: categoryId=" + categoryId);
                }
            }
        }
        
        // 5. 校验属性组引用存在且归属租户
        if (command.getAttrGroups() != null && !command.getAttrGroups().isEmpty()) {
            for (CreateProductAggregateCommand.AttrGroupBinding binding : command.getAttrGroups()) {
                BcProductAttrGroup attrGroup = attrGroupMapper.selectOne(new LambdaQueryWrapper<BcProductAttrGroup>()
                        .eq(BcProductAttrGroup::getId, binding.getGroupId())
                        .eq(BcProductAttrGroup::getTenantId, tenantId));
                if (attrGroup == null) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "属性组不存在或无权访问: attrGroupId=" + binding.getGroupId());
                }
                
                // 校验规则合法性：required => min >= 1
                if (Boolean.TRUE.equals(binding.getRequired()) && 
                    (binding.getMinSelect() == null || binding.getMinSelect() < 1)) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "必选属性组的最小选择数量必须 >= 1");
                }
                
                // 校验规则合法性：max >= min
                if (binding.getMaxSelect() != null && binding.getMinSelect() != null &&
                    binding.getMaxSelect() < binding.getMinSelect()) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "属性组的最大选择数量必须 >= 最小选择数量");
                }
                
                // 校验属性选项引用存在且归属租户
                if (binding.getOptionOverrides() != null && !binding.getOptionOverrides().isEmpty()) {
                    for (CreateProductAggregateCommand.AttrOptionOverride override : binding.getOptionOverrides()) {
                        BcProductAttrOption option = attrOptionMapper.selectOne(new LambdaQueryWrapper<BcProductAttrOption>()
                                .eq(BcProductAttrOption::getId, override.getOptionId())
                                .eq(BcProductAttrOption::getTenantId, tenantId)
                                .eq(BcProductAttrOption::getAttrGroupId, binding.getGroupId()));
                        if (option == null) {
                            throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                                    "属性选项不存在或不属于该属性组: optionId=" + override.getOptionId());
                        }
                    }
                }
            }
        }
        
        // 6. 校验小料组引用存在且归属租户
        if (command.getAddonGroups() != null && !command.getAddonGroups().isEmpty()) {
            for (CreateProductAggregateCommand.AddonGroupBinding binding : command.getAddonGroups()) {
                BcAddonGroup addonGroup = addonGroupMapper.selectOne(new LambdaQueryWrapper<BcAddonGroup>()
                        .eq(BcAddonGroup::getId, binding.getGroupId())
                        .eq(BcAddonGroup::getTenantId, tenantId));
                if (addonGroup == null) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "小料组不存在或无权访问: addonGroupId=" + binding.getGroupId());
                }
                
                // 校验规则合法性：required => min >= 1
                if (Boolean.TRUE.equals(binding.getRequired()) && 
                    (binding.getMinSelect() == null || binding.getMinSelect() < 1)) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "必选小料组的最小选择数量必须 >= 1");
                }
                
                // 校验规则合法性：max >= min
                if (binding.getMaxSelect() != null && binding.getMinSelect() != null &&
                    binding.getMaxSelect() < binding.getMinSelect()) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "小料组的最大选择数量必须 >= 最小选择数量");
                }
                
                // 校验规则合法性：maxTotal >= maxSelect（若同时存在）
                if (binding.getMaxTotal() != null && binding.getMaxSelect() != null &&
                    binding.getMaxTotal().compareTo(BigDecimal.valueOf(binding.getMaxSelect())) < 0) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                            "小料组的总可选上限必须 >= 最大选择数量");
                }
                
                // 校验小料项引用存在且归属租户
                if (binding.getItemOverrides() != null && !binding.getItemOverrides().isEmpty()) {
                    for (CreateProductAggregateCommand.AddonItemOverride override : binding.getItemOverrides()) {
                        BcAddonItem item = addonItemMapper.selectOne(new LambdaQueryWrapper<BcAddonItem>()
                                .eq(BcAddonItem::getId, override.getItemId())
                                .eq(BcAddonItem::getTenantId, tenantId)
                                .eq(BcAddonItem::getGroupId, binding.getGroupId()));
                        if (item == null) {
                            throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                                    "小料项不存在或不属于该小料组: itemId=" + override.getItemId());
                        }
                    }
                }
            }
        }
        
        // TODO: 7. 校验 SKU specCombination 必须能映射到当前请求的 specOptions
    }
    
    /**
     * 校验更新命令
     */
    private void validateUpdateCommand(UpdateProductAggregateCommand command) {
        // 更新命令的校验逻辑与创建命令类似
        validateCreateCommand(command);
    }
    
    // ===== 私有方法：缓存失效 =====
    
    /**
     * 发布菜单快照失效事件（粗粒度：按 tenant 失效）
     */
    private void publishMenuSnapshotInvalidation(Long tenantId) {
        if (menuSnapshotInvalidationHelper == null || tenantId == null) {
            log.warn("MenuSnapshotInvalidationHelper 未注入或 tenantId 为空，跳过菜单快照失效");
            return;
        }
        
        // Prompt 09: 商品变更时，失效租户下所有门店的菜单快照（粗粒度）
        menuSnapshotInvalidationHelper.invalidateTenantMenus(tenantId, "商品聚合变更");
    }
    
    /**
     * 发布菜单快照失效事件（指定门店）- Prompt 09。
     */
    private void publishMenuSnapshotInvalidation(Long tenantId, Long storeId, String reason) {
        if (menuSnapshotInvalidationHelper == null || tenantId == null || storeId == null) {
            log.warn("MenuSnapshotInvalidationHelper 未注入或参数为空，跳过菜单快照失效");
            return;
        }
        
        // Prompt 09: 按门店失效（细粒度）
        menuSnapshotInvalidationHelper.invalidateStoreMenu(tenantId, storeId, reason);
    }
}

