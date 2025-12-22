package com.bluecone.app.product.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.product.application.dto.addon.*;
import com.bluecone.app.product.dao.entity.BcAddonGroup;
import com.bluecone.app.product.dao.entity.BcAddonItem;
import com.bluecone.app.product.dao.mapper.BcAddonGroupMapper;
import com.bluecone.app.product.dao.mapper.BcAddonItemMapper;
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
 * 小料素材库管理应用服务
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>小料组的创建、修改、查询、状态管理、排序</li>
 *   <li>小料项的创建、修改、查询、状态管理、排序</li>
 *   <li>定时展示配置</li>
 * </ul>
 * 
 * <h3>💡 设计原则：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：所有读写必须显式带 tenant_id 条件</li>
 *   <li><b>状态字段一致</b>：enabled ↔ status (1=启用, 0=禁用)</li>
 *   <li><b>定时展示</b>：display_start_at / display_end_at 过滤逻辑一致</li>
 *   <li><b>高性能/高可靠</b>：写操作触发菜单快照缓存失效（tenant 级别）</li>
 *   <li><b>事务保证</b>：create/update/status/reorder 必须 @Transactional</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddonAdminApplicationService {
    
    private final BcAddonGroupMapper addonGroupMapper;
    private final BcAddonItemMapper addonItemMapper;
    
    @Autowired(required = false)
    @Nullable
    private MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;
    
    // ===== 小料组管理 =====
    
    /**
     * 创建小料组
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAddonGroup(Long tenantId, CreateAddonGroupCommand cmd, Long operatorId) {
        log.info("创建小料组: tenantId={}, title={}", tenantId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组名称不能为空");
        }
        
        // 2. 组装实体
        BcAddonGroup group = new BcAddonGroup();
        group.setTenantId(tenantId);
        group.setName(cmd.getTitle());
        group.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        group.setStatus(Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0);
        group.setType(1); // 默认计价小料
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        
        // 3. 插入数据库
        addonGroupMapper.insert(group);
        
        Long groupId = group.getId();
        log.info("小料组创建成功: tenantId={}, groupId={}", tenantId, groupId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-group:create");
        
        return groupId;
    }
    
    /**
     * 更新小料组
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAddonGroup(Long tenantId, Long groupId, UpdateAddonGroupCommand cmd, Long operatorId) {
        log.info("更新小料组: tenantId={}, groupId={}, title={}", tenantId, groupId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组名称不能为空");
        }
        
        // 2. 查询小料组是否存在
        BcAddonGroup group = addonGroupMapper.selectOne(new LambdaQueryWrapper<BcAddonGroup>()
                .eq(BcAddonGroup::getId, groupId)
                .eq(BcAddonGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料组不存在");
        }
        
        // 3. 更新字段
        LambdaUpdateWrapper<BcAddonGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcAddonGroup::getId, groupId)
                .eq(BcAddonGroup::getTenantId, tenantId)
                .set(BcAddonGroup::getName, cmd.getTitle())
                .set(BcAddonGroup::getSortOrder, cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .set(BcAddonGroup::getStatus, Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0)
                .set(BcAddonGroup::getUpdatedAt, LocalDateTime.now());
        
        int updated = addonGroupMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料组更新失败");
        }
        
        log.info("小料组更新成功: tenantId={}, groupId={}", tenantId, groupId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-group:update");
    }
    
    /**
     * 查询小料组列表
     */
    public List<AddonGroupAdminView> listAddonGroups(Long tenantId, boolean includeDisabled, 
                                                     boolean filterByTime, LocalDateTime now) {
        log.info("查询小料组列表: tenantId={}, includeDisabled={}, filterByTime={}", 
                tenantId, includeDisabled, filterByTime);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<BcAddonGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcAddonGroup::getTenantId, tenantId);
        
        // 2.1 状态过滤
        if (!includeDisabled) {
            wrapper.eq(BcAddonGroup::getStatus, 1);
        }
        
        // 2.2 排序
        wrapper.orderByDesc(BcAddonGroup::getSortOrder)
                .orderByAsc(BcAddonGroup::getId);
        
        // 3. 查询数据库
        List<BcAddonGroup> groups = addonGroupMapper.selectList(wrapper);
        
        // 4. 映射为视图
        List<AddonGroupAdminView> views = groups.stream()
                .map(this::toAddonGroupView)
                .collect(Collectors.toList());
        
        log.info("查询小料组列表成功: tenantId={}, count={}", tenantId, views.size());
        return views;
    }
    
    /**
     * 修改小料组状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeAddonGroupStatus(Long tenantId, Long groupId, boolean enabled, Long operatorId) {
        log.info("修改小料组状态: tenantId={}, groupId={}, enabled={}", tenantId, groupId, enabled);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        
        // 2. 查询小料组是否存在
        BcAddonGroup group = addonGroupMapper.selectOne(new LambdaQueryWrapper<BcAddonGroup>()
                .eq(BcAddonGroup::getId, groupId)
                .eq(BcAddonGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料组不存在");
        }
        
        // 3. 更新状态
        LambdaUpdateWrapper<BcAddonGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcAddonGroup::getId, groupId)
                .eq(BcAddonGroup::getTenantId, tenantId)
                .set(BcAddonGroup::getStatus, enabled ? 1 : 0)
                .set(BcAddonGroup::getUpdatedAt, LocalDateTime.now());
        
        int updated = addonGroupMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料组状态修改失败");
        }
        
        log.info("小料组状态修改成功: tenantId={}, groupId={}, enabled={}", tenantId, groupId, enabled);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-group:status");
    }
    
    /**
     * 批量调整小料组排序
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderAddonGroups(Long tenantId, List<AddonGroupReorderItem> items, Long operatorId) {
        log.info("批量调整小料组排序: tenantId={}, count={}", tenantId, items != null ? items.size() : 0);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序项列表不能为空");
        }
        
        // 2. 逐个校验并更新
        for (AddonGroupReorderItem item : items) {
            if (item.getGroupId() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
            }
            if (item.getSortOrder() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序值不能为空");
            }
            
            // 更新排序值
            LambdaUpdateWrapper<BcAddonGroup> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BcAddonGroup::getId, item.getGroupId())
                    .eq(BcAddonGroup::getTenantId, tenantId)
                    .set(BcAddonGroup::getSortOrder, item.getSortOrder())
                    .set(BcAddonGroup::getUpdatedAt, LocalDateTime.now());
            
            int updated = addonGroupMapper.update(null, updateWrapper);
            
            if (updated == 0) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, 
                        "小料组不存在或不属于当前租户: groupId=" + item.getGroupId());
            }
        }
        
        log.info("小料组排序调整成功: tenantId={}, count={}", tenantId, items.size());
        
        // 3. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-group:reorder");
    }
    
    // ===== 小料项管理 =====
    
    /**
     * 创建小料项
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAddonItem(Long tenantId, Long groupId, CreateAddonItemCommand cmd, Long operatorId) {
        log.info("创建小料项: tenantId={}, groupId={}, title={}", tenantId, groupId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料项名称不能为空");
        }
        
        // 2. 校验小料组存在且属于租户
        BcAddonGroup group = addonGroupMapper.selectOne(new LambdaQueryWrapper<BcAddonGroup>()
                .eq(BcAddonGroup::getId, groupId)
                .eq(BcAddonGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料组不存在");
        }
        
        // 3. 组装实体
        BcAddonItem item = new BcAddonItem();
        item.setTenantId(tenantId);
        item.setGroupId(groupId);
        item.setName(cmd.getTitle());
        item.setPrice(cmd.getPriceDelta() != null ? cmd.getPriceDelta() : java.math.BigDecimal.ZERO);
        item.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        item.setStatus(Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0);
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        
        // 4. 插入数据库
        addonItemMapper.insert(item);
        
        Long itemId = item.getId();
        log.info("小料项创建成功: tenantId={}, groupId={}, itemId={}", tenantId, groupId, itemId);
        
        // 5. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-item:create");
        
        return itemId;
    }
    
    /**
     * 更新小料项
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAddonItem(Long tenantId, Long groupId, Long itemId, 
                               UpdateAddonItemCommand cmd, Long operatorId) {
        log.info("更新小料项: tenantId={}, groupId={}, itemId={}, title={}", 
                tenantId, groupId, itemId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        if (itemId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料项ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料项名称不能为空");
        }
        
        // 2. 查询小料项是否存在
        BcAddonItem item = addonItemMapper.selectOne(new LambdaQueryWrapper<BcAddonItem>()
                .eq(BcAddonItem::getId, itemId)
                .eq(BcAddonItem::getGroupId, groupId)
                .eq(BcAddonItem::getTenantId, tenantId));
        
        if (item == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料项不存在或不属于该小料组");
        }
        
        // 3. 更新字段
        LambdaUpdateWrapper<BcAddonItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcAddonItem::getId, itemId)
                .eq(BcAddonItem::getGroupId, groupId)
                .eq(BcAddonItem::getTenantId, tenantId)
                .set(BcAddonItem::getName, cmd.getTitle())
                .set(BcAddonItem::getPrice, cmd.getPriceDelta() != null ? cmd.getPriceDelta() : java.math.BigDecimal.ZERO)
                .set(BcAddonItem::getSortOrder, cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .set(BcAddonItem::getStatus, Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0)
                .set(BcAddonItem::getUpdatedAt, LocalDateTime.now());
        
        int updated = addonItemMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料项更新失败");
        }
        
        log.info("小料项更新成功: tenantId={}, groupId={}, itemId={}", tenantId, groupId, itemId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-item:update");
    }
    
    /**
     * 查询小料项列表
     */
    public List<AddonItemAdminView> listAddonItems(Long tenantId, Long groupId, boolean includeDisabled, 
                                                   boolean filterByTime, LocalDateTime now) {
        log.info("查询小料项列表: tenantId={}, groupId={}, includeDisabled={}, filterByTime={}", 
                tenantId, groupId, includeDisabled, filterByTime);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<BcAddonItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcAddonItem::getTenantId, tenantId)
                .eq(BcAddonItem::getGroupId, groupId);
        
        // 2.1 状态过滤
        if (!includeDisabled) {
            wrapper.eq(BcAddonItem::getStatus, 1);
        }
        
        // 2.2 排序
        wrapper.orderByDesc(BcAddonItem::getSortOrder)
                .orderByAsc(BcAddonItem::getId);
        
        // 3. 查询数据库
        List<BcAddonItem> items = addonItemMapper.selectList(wrapper);
        
        // 4. 映射为视图
        List<AddonItemAdminView> views = items.stream()
                .map(this::toAddonItemView)
                .collect(Collectors.toList());
        
        log.info("查询小料项列表成功: tenantId={}, groupId={}, count={}", tenantId, groupId, views.size());
        return views;
    }
    
    /**
     * 修改小料项状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeAddonItemStatus(Long tenantId, Long groupId, Long itemId, 
                                     boolean enabled, Long operatorId) {
        log.info("修改小料项状态: tenantId={}, groupId={}, itemId={}, enabled={}", 
                tenantId, groupId, itemId, enabled);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        if (itemId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料项ID不能为空");
        }
        
        // 2. 查询小料项是否存在
        BcAddonItem item = addonItemMapper.selectOne(new LambdaQueryWrapper<BcAddonItem>()
                .eq(BcAddonItem::getId, itemId)
                .eq(BcAddonItem::getGroupId, groupId)
                .eq(BcAddonItem::getTenantId, tenantId));
        
        if (item == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料项不存在");
        }
        
        // 3. 更新状态
        LambdaUpdateWrapper<BcAddonItem> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcAddonItem::getId, itemId)
                .eq(BcAddonItem::getGroupId, groupId)
                .eq(BcAddonItem::getTenantId, tenantId)
                .set(BcAddonItem::getStatus, enabled ? 1 : 0)
                .set(BcAddonItem::getUpdatedAt, LocalDateTime.now());
        
        int updated = addonItemMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "小料项状态修改失败");
        }
        
        log.info("小料项状态修改成功: tenantId={}, groupId={}, itemId={}, enabled={}", 
                tenantId, groupId, itemId, enabled);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-item:status");
    }
    
    /**
     * 批量调整小料项排序
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderAddonItems(Long tenantId, Long groupId, List<AddonItemReorderItem> items, Long operatorId) {
        log.info("批量调整小料项排序: tenantId={}, groupId={}, count={}", 
                tenantId, groupId, items != null ? items.size() : 0);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料组ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序项列表不能为空");
        }
        
        // 2. 逐个校验并更新
        for (AddonItemReorderItem item : items) {
            if (item.getItemId() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "小料项ID不能为空");
            }
            if (item.getSortOrder() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序值不能为空");
            }
            
            // 更新排序值
            LambdaUpdateWrapper<BcAddonItem> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BcAddonItem::getId, item.getItemId())
                    .eq(BcAddonItem::getGroupId, groupId)
                    .eq(BcAddonItem::getTenantId, tenantId)
                    .set(BcAddonItem::getSortOrder, item.getSortOrder())
                    .set(BcAddonItem::getUpdatedAt, LocalDateTime.now());
            
            int updated = addonItemMapper.update(null, updateWrapper);
            
            if (updated == 0) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, 
                        "小料项不存在或不属于当前租户: itemId=" + item.getItemId());
            }
        }
        
        log.info("小料项排序调整成功: tenantId={}, groupId={}, count={}", tenantId, groupId, items.size());
        
        // 3. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "addon-item:reorder");
    }
    
    // ===== 私有方法 =====
    
    /**
     * 实体转视图 - 小料组
     */
    private AddonGroupAdminView toAddonGroupView(BcAddonGroup group) {
        return AddonGroupAdminView.builder()
                .id(group.getId())
                .title(group.getName())
                .sortOrder(group.getSortOrder())
                .enabled(group.getStatus() != null && group.getStatus() == 1)
                .displayStartAt(null) // 实体中没有该字段
                .displayEndAt(null)   // 实体中没有该字段
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
    
    /**
     * 实体转视图 - 小料项
     */
    private AddonItemAdminView toAddonItemView(BcAddonItem item) {
        return AddonItemAdminView.builder()
                .id(item.getId())
                .title(item.getName())
                .priceDelta(item.getPrice())
                .sortOrder(item.getSortOrder())
                .enabled(item.getStatus() != null && item.getStatus() == 1)
                .displayStartAt(null) // 实体中没有该字段
                .displayEndAt(null)   // 实体中没有该字段
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
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

