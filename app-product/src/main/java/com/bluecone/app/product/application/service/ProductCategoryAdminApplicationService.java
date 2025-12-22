package com.bluecone.app.product.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.product.application.dto.category.CategoryReorderItem;
import com.bluecone.app.product.application.dto.category.CreateProductCategoryCommand;
import com.bluecone.app.product.application.dto.category.ProductCategoryAdminView;
import com.bluecone.app.product.application.dto.category.UpdateProductCategoryCommand;
import com.bluecone.app.product.dao.entity.BcProductCategory;
import com.bluecone.app.product.dao.mapper.BcProductCategoryMapper;
import com.bluecone.app.product.infrastructure.cache.MenuSnapshotInvalidationHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 商品分类管理应用服务
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商品分类的创建、修改、查询</li>
 *   <li>分类的显示/隐藏状态管理</li>
 *   <li>分类的排序管理（批量调整排序）</li>
 *   <li>分类的定时展示配置</li>
 * </ul>
 * 
 * <h3>💡 设计原则：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：所有读写必须显式带 tenant_id 条件，严禁跨租户更新</li>
 *   <li><b>状态字段一致</b>：bc_product_category.status 约定 1=启用(显示)，0=禁用(隐藏)</li>
 *   <li><b>定时展示</b>：display_start_at / display_end_at 的过滤逻辑与 StoreMenuSnapshotBuilderService 一致</li>
 *   <li><b>高性能/高可靠</b>：写操作触发菜单快照缓存失效（tenant 级别通配）</li>
 *   <li><b>事务保证</b>：create/update/status/reorder 必须 @Transactional</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductCategoryAdminApplicationService {
    
    private final BcProductCategoryMapper productCategoryMapper;
    
    @Autowired(required = false)
    @Nullable
    private MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;
    
    /**
     * 最大分类层级限制
     */
    private static final int MAX_CATEGORY_LEVEL = 3;
    
    /**
     * 创建商品分类
     * 
     * <p>创建新的商品分类，支持设置图标、排序、启用状态、定时展示等配置。
     * 
     * @param tenantId 租户ID
     * @param cmd 创建命令
     * @param operatorId 操作人ID
     * @return 创建的分类ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createCategory(Long tenantId, CreateProductCategoryCommand cmd, Long operatorId) {
        log.info("创建商品分类: tenantId={}, title={}, parentId={}", tenantId, cmd.getTitle(), cmd.getParentId());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类名称不能为空");
        }
        if (cmd.getParentId() == null || cmd.getParentId() < 0) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "父分类ID不能为空且不能小于0");
        }
        
        // 2. 计算层级
        int level;
        if (cmd.getParentId() == 0) {
            // 顶级分类
            level = 1;
        } else {
            // 查询父分类
            BcProductCategory parent = productCategoryMapper.selectOne(new LambdaQueryWrapper<BcProductCategory>()
                    .eq(BcProductCategory::getId, cmd.getParentId())
                    .eq(BcProductCategory::getTenantId, tenantId)
                    .eq(BcProductCategory::getDeleted, 0));
            
            if (parent == null) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "父分类不存在");
            }
            
            level = parent.getLevel() + 1;
            
            // 限制最大层级
            if (level > MAX_CATEGORY_LEVEL) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类层级过深，最多支持" + MAX_CATEGORY_LEVEL + "级");
            }
        }
        
        // 3. 组装实体
        BcProductCategory category = new BcProductCategory();
        category.setTenantId(tenantId);
        category.setParentId(cmd.getParentId());
        category.setName(cmd.getTitle());
        category.setIcon(cmd.getImageUrl());
        category.setType(1); // 默认商品菜单
        category.setLevel(level);
        category.setStatus(Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0);
        category.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        category.setDisplayStartAt(cmd.getDisplayStartAt());
        category.setDisplayEndAt(cmd.getDisplayEndAt());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        category.setCreatedBy(operatorId);
        category.setUpdatedBy(operatorId);
        category.setDeleted(0);
        
        // 4. 插入数据库
        productCategoryMapper.insert(category);
        
        Long categoryId = category.getId();
        log.info("商品分类创建成功: tenantId={}, categoryId={}, level={}", tenantId, categoryId, level);
        
        // 5. 失效菜单快照缓存（best-effort）
        invalidateTenantMenus(tenantId, "product-category:create");
        
        return categoryId;
    }
    
    /**
     * 更新商品分类
     * 
     * <p>更新商品分类的基本信息、图标、排序、启用状态、定时展示等配置。
     * 
     * @param tenantId 租户ID
     * @param categoryId 分类ID
     * @param cmd 更新命令
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(Long tenantId, Long categoryId, UpdateProductCategoryCommand cmd, Long operatorId) {
        log.info("更新商品分类: tenantId={}, categoryId={}, title={}", tenantId, categoryId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (categoryId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类名称不能为空");
        }
        
        // 2. 查询分类是否存在
        BcProductCategory category = productCategoryMapper.selectOne(new LambdaQueryWrapper<BcProductCategory>()
                .eq(BcProductCategory::getId, categoryId)
                .eq(BcProductCategory::getTenantId, tenantId)
                .eq(BcProductCategory::getDeleted, 0));
        
        if (category == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
        
        // 3. 更新字段
        LambdaUpdateWrapper<BcProductCategory> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductCategory::getId, categoryId)
                .eq(BcProductCategory::getTenantId, tenantId)
                .eq(BcProductCategory::getDeleted, 0)
                .set(BcProductCategory::getName, cmd.getTitle())
                .set(BcProductCategory::getIcon, cmd.getImageUrl())
                .set(BcProductCategory::getSortOrder, cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .set(BcProductCategory::getStatus, Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0)
                .set(BcProductCategory::getDisplayStartAt, cmd.getDisplayStartAt())
                .set(BcProductCategory::getDisplayEndAt, cmd.getDisplayEndAt())
                .set(BcProductCategory::getUpdatedAt, LocalDateTime.now())
                .set(BcProductCategory::getUpdatedBy, operatorId);
        
        int updated = productCategoryMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "分类更新失败，分类不存在或不属于当前租户");
        }
        
        log.info("商品分类更新成功: tenantId={}, categoryId={}", tenantId, categoryId);
        
        // 4. 失效菜单快照缓存（best-effort）
        invalidateTenantMenus(tenantId, "product-category:update");
    }
    
    /**
     * 查询商品分类列表
     * 
     * <p>查询商品分类列表，支持按启用状态筛选、按定时展示时间过滤。
     * 
     * @param tenantId 租户ID
     * @param includeDisabled 是否包含禁用的分类（默认false，仅返回启用的）
     * @param filterByTime 是否按当前时间过滤定时展示（默认false，返回全部）
     * @param now 当前时间（用于定时展示判断）
     * @return 分类列表
     */
    public List<ProductCategoryAdminView> listCategories(Long tenantId, boolean includeDisabled, 
                                                         boolean filterByTime, LocalDateTime now) {
        log.info("查询商品分类列表: tenantId={}, includeDisabled={}, filterByTime={}", 
                tenantId, includeDisabled, filterByTime);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<BcProductCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcProductCategory::getTenantId, tenantId)
                .eq(BcProductCategory::getDeleted, 0);
        
        // 2.1 状态过滤
        if (!includeDisabled) {
            wrapper.eq(BcProductCategory::getStatus, 1);
        } else {
            wrapper.in(BcProductCategory::getStatus, 0, 1);
        }
        
        // 2.2 定时展示窗口过滤（在 SQL 层过滤）
        if (filterByTime && now != null) {
            // (displayStartAt is null OR displayStartAt <= now)
            wrapper.and(w -> w.isNull(BcProductCategory::getDisplayStartAt)
                    .or()
                    .le(BcProductCategory::getDisplayStartAt, now));
            
            // (displayEndAt is null OR displayEndAt >= now)
            wrapper.and(w -> w.isNull(BcProductCategory::getDisplayEndAt)
                    .or()
                    .ge(BcProductCategory::getDisplayEndAt, now));
        }
        
        // 2.3 排序：sortOrder desc, id asc
        wrapper.orderByDesc(BcProductCategory::getSortOrder)
                .orderByAsc(BcProductCategory::getId);
        
        // 3. 查询数据库
        List<BcProductCategory> categories = productCategoryMapper.selectList(wrapper);
        
        // 4. 映射为视图
        List<ProductCategoryAdminView> views = categories.stream()
                .map(this::toAdminView)
                .collect(Collectors.toList());
        
        log.info("查询商品分类列表成功: tenantId={}, count={}", tenantId, views.size());
        return views;
    }
    
    /**
     * 修改分类状态（显示/隐藏）
     * 
     * <p>修改商品分类的启用状态，用于控制分类在C端的显示/隐藏。
     * 
     * @param tenantId 租户ID
     * @param categoryId 分类ID
     * @param enabled 是否启用（true=显示，false=隐藏）
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeCategoryStatus(Long tenantId, Long categoryId, boolean enabled, Long operatorId) {
        log.info("修改分类状态: tenantId={}, categoryId={}, enabled={}", tenantId, categoryId, enabled);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (categoryId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类ID不能为空");
        }
        
        // 2. 查询分类是否存在
        BcProductCategory category = productCategoryMapper.selectOne(new LambdaQueryWrapper<BcProductCategory>()
                .eq(BcProductCategory::getId, categoryId)
                .eq(BcProductCategory::getTenantId, tenantId)
                .eq(BcProductCategory::getDeleted, 0));
        
        if (category == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "分类不存在");
        }
        
        // 3. 更新状态
        LambdaUpdateWrapper<BcProductCategory> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductCategory::getId, categoryId)
                .eq(BcProductCategory::getTenantId, tenantId)
                .eq(BcProductCategory::getDeleted, 0)
                .set(BcProductCategory::getStatus, enabled ? 1 : 0)
                .set(BcProductCategory::getUpdatedAt, LocalDateTime.now())
                .set(BcProductCategory::getUpdatedBy, operatorId);
        
        int updated = productCategoryMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "分类状态修改失败，分类不存在或不属于当前租户");
        }
        
        log.info("分类状态修改成功: tenantId={}, categoryId={}, enabled={}", tenantId, categoryId, enabled);
        
        // 4. 失效菜单快照缓存（best-effort）
        invalidateTenantMenus(tenantId, "product-category:status");
    }
    
    /**
     * 批量调整分类排序
     * 
     * <p>批量调整商品分类的排序值，用于调整分类在C端的展示顺序。
     * 
     * @param tenantId 租户ID
     * @param items 排序项列表
     * @param operatorId 操作人ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderCategories(Long tenantId, List<CategoryReorderItem> items, Long operatorId) {
        log.info("批量调整分类排序: tenantId={}, count={}", tenantId, items != null ? items.size() : 0);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序项列表不能为空");
        }
        
        // 2. 逐个校验并更新
        for (CategoryReorderItem item : items) {
            if (item.getCategoryId() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "分类ID不能为空");
            }
            if (item.getSortOrder() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序值不能为空");
            }
            
            // 更新排序值
            LambdaUpdateWrapper<BcProductCategory> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BcProductCategory::getId, item.getCategoryId())
                    .eq(BcProductCategory::getTenantId, tenantId)
                    .eq(BcProductCategory::getDeleted, 0)
                    .set(BcProductCategory::getSortOrder, item.getSortOrder())
                    .set(BcProductCategory::getUpdatedAt, LocalDateTime.now())
                    .set(BcProductCategory::getUpdatedBy, operatorId);
            
            int updated = productCategoryMapper.update(null, updateWrapper);
            
            if (updated == 0) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, 
                        "分类不存在或不属于当前租户: categoryId=" + item.getCategoryId());
            }
        }
        
        log.info("分类排序调整成功: tenantId={}, count={}", tenantId, items.size());
        
        // 3. 失效菜单快照缓存（best-effort）
        invalidateTenantMenus(tenantId, "product-category:reorder");
    }
    
    // ===== 私有方法 =====
    
    /**
     * 实体转视图
     */
    private ProductCategoryAdminView toAdminView(BcProductCategory category) {
        return ProductCategoryAdminView.builder()
                .id(category.getId())
                .parentId(category.getParentId())
                .title(category.getName())
                .imageUrl(category.getIcon())
                .sortOrder(category.getSortOrder())
                .enabled(category.getStatus() != null && category.getStatus() == 1)
                .displayStartAt(category.getDisplayStartAt())
                .displayEndAt(category.getDisplayEndAt())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .level(category.getLevel())
                .build();
    }
    
    /**
     * 失效租户菜单快照缓存（best-effort）
     */
    private void invalidateTenantMenus(Long tenantId, String reason) {
        if (menuSnapshotInvalidationHelper != null && tenantId != null) {
            try {
                menuSnapshotInvalidationHelper.invalidateTenantMenus(tenantId, reason);
            } catch (Exception ex) {
                // best-effort: 不影响主流程
                log.error("菜单快照缓存失效失败: tenantId={}, reason={}", tenantId, reason, ex);
            }
        }
    }
}

