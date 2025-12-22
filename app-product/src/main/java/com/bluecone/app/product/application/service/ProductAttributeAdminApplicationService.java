package com.bluecone.app.product.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.core.error.BizErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.product.application.dto.attr.*;
import com.bluecone.app.product.dao.entity.BcProductAttrGroup;
import com.bluecone.app.product.dao.entity.BcProductAttrOption;
import com.bluecone.app.product.dao.mapper.BcProductAttrGroupMapper;
import com.bluecone.app.product.dao.mapper.BcProductAttrOptionMapper;
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
 * 商品属性素材库管理应用服务
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>属性组的创建、修改、查询、状态管理、排序</li>
 *   <li>属性选项的创建、修改、查询、状态管理、排序</li>
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
public class ProductAttributeAdminApplicationService {
    
    private final BcProductAttrGroupMapper attrGroupMapper;
    private final BcProductAttrOptionMapper attrOptionMapper;
    
    @Autowired(required = false)
    @Nullable
    private MenuSnapshotInvalidationHelper menuSnapshotInvalidationHelper;
    
    // ===== 属性组管理 =====
    
    /**
     * 创建属性组
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAttrGroup(Long tenantId, CreateAttrGroupCommand cmd, Long operatorId) {
        log.info("创建属性组: tenantId={}, title={}", tenantId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组名称不能为空");
        }
        
        // 2. 组装实体
        BcProductAttrGroup group = new BcProductAttrGroup();
        group.setTenantId(tenantId);
        group.setName(cmd.getTitle());
        group.setScope(1); // 默认口味
        group.setSelectType(cmd.getSelectType() != null ? cmd.getSelectType() : 1); // 默认单选
        group.setRequired(cmd.getRequired() != null ? cmd.getRequired() : false);
        group.setMaxSelect(cmd.getMaxSelect() != null ? cmd.getMaxSelect() : 0);
        group.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        group.setStatus(Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0);
        group.setCreatedAt(LocalDateTime.now());
        group.setUpdatedAt(LocalDateTime.now());
        
        // 3. 插入数据库
        attrGroupMapper.insert(group);
        
        Long groupId = group.getId();
        log.info("属性组创建成功: tenantId={}, groupId={}", tenantId, groupId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-group:create");
        
        return groupId;
    }
    
    /**
     * 更新属性组
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAttrGroup(Long tenantId, Long groupId, UpdateAttrGroupCommand cmd, Long operatorId) {
        log.info("更新属性组: tenantId={}, groupId={}, title={}", tenantId, groupId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组名称不能为空");
        }
        
        // 2. 查询属性组是否存在
        BcProductAttrGroup group = attrGroupMapper.selectOne(new LambdaQueryWrapper<BcProductAttrGroup>()
                .eq(BcProductAttrGroup::getId, groupId)
                .eq(BcProductAttrGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性组不存在");
        }
        
        // 3. 更新字段
        LambdaUpdateWrapper<BcProductAttrGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductAttrGroup::getId, groupId)
                .eq(BcProductAttrGroup::getTenantId, tenantId)
                .set(BcProductAttrGroup::getName, cmd.getTitle())
                .set(BcProductAttrGroup::getSelectType, cmd.getSelectType() != null ? cmd.getSelectType() : 1)
                .set(BcProductAttrGroup::getRequired, cmd.getRequired() != null ? cmd.getRequired() : false)
                .set(BcProductAttrGroup::getMaxSelect, cmd.getMaxSelect() != null ? cmd.getMaxSelect() : 0)
                .set(BcProductAttrGroup::getSortOrder, cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .set(BcProductAttrGroup::getStatus, Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0)
                .set(BcProductAttrGroup::getUpdatedAt, LocalDateTime.now());
        
        int updated = attrGroupMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性组更新失败");
        }
        
        log.info("属性组更新成功: tenantId={}, groupId={}", tenantId, groupId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-group:update");
    }
    
    /**
     * 查询属性组列表
     */
    public List<AttrGroupAdminView> listAttrGroups(Long tenantId, boolean includeDisabled, 
                                                    boolean filterByTime, LocalDateTime now) {
        log.info("查询属性组列表: tenantId={}, includeDisabled={}, filterByTime={}", 
                tenantId, includeDisabled, filterByTime);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<BcProductAttrGroup> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcProductAttrGroup::getTenantId, tenantId);
        
        // 2.1 状态过滤
        if (!includeDisabled) {
            wrapper.eq(BcProductAttrGroup::getStatus, 1);
        }
        
        // 2.2 排序
        wrapper.orderByDesc(BcProductAttrGroup::getSortOrder)
                .orderByAsc(BcProductAttrGroup::getId);
        
        // 3. 查询数据库
        List<BcProductAttrGroup> groups = attrGroupMapper.selectList(wrapper);
        
        // 4. 映射为视图
        List<AttrGroupAdminView> views = groups.stream()
                .map(this::toAttrGroupView)
                .collect(Collectors.toList());
        
        log.info("查询属性组列表成功: tenantId={}, count={}", tenantId, views.size());
        return views;
    }
    
    /**
     * 修改属性组状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeAttrGroupStatus(Long tenantId, Long groupId, boolean enabled, Long operatorId) {
        log.info("修改属性组状态: tenantId={}, groupId={}, enabled={}", tenantId, groupId, enabled);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        
        // 2. 查询属性组是否存在
        BcProductAttrGroup group = attrGroupMapper.selectOne(new LambdaQueryWrapper<BcProductAttrGroup>()
                .eq(BcProductAttrGroup::getId, groupId)
                .eq(BcProductAttrGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性组不存在");
        }
        
        // 3. 更新状态
        LambdaUpdateWrapper<BcProductAttrGroup> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductAttrGroup::getId, groupId)
                .eq(BcProductAttrGroup::getTenantId, tenantId)
                .set(BcProductAttrGroup::getStatus, enabled ? 1 : 0)
                .set(BcProductAttrGroup::getUpdatedAt, LocalDateTime.now());
        
        int updated = attrGroupMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性组状态修改失败");
        }
        
        log.info("属性组状态修改成功: tenantId={}, groupId={}, enabled={}", tenantId, groupId, enabled);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-group:status");
    }
    
    /**
     * 批量调整属性组排序
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderAttrGroups(Long tenantId, List<AttrGroupReorderItem> items, Long operatorId) {
        log.info("批量调整属性组排序: tenantId={}, count={}", tenantId, items != null ? items.size() : 0);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序项列表不能为空");
        }
        
        // 2. 逐个校验并更新
        for (AttrGroupReorderItem item : items) {
            if (item.getGroupId() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
            }
            if (item.getSortOrder() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序值不能为空");
            }
            
            // 更新排序值
            LambdaUpdateWrapper<BcProductAttrGroup> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BcProductAttrGroup::getId, item.getGroupId())
                    .eq(BcProductAttrGroup::getTenantId, tenantId)
                    .set(BcProductAttrGroup::getSortOrder, item.getSortOrder())
                    .set(BcProductAttrGroup::getUpdatedAt, LocalDateTime.now());
            
            int updated = attrGroupMapper.update(null, updateWrapper);
            
            if (updated == 0) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, 
                        "属性组不存在或不属于当前租户: groupId=" + item.getGroupId());
            }
        }
        
        log.info("属性组排序调整成功: tenantId={}, count={}", tenantId, items.size());
        
        // 3. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-group:reorder");
    }
    
    // ===== 属性选项管理 =====
    
    /**
     * 创建属性选项
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createAttrOption(Long tenantId, Long groupId, CreateAttrOptionCommand cmd, Long operatorId) {
        log.info("创建属性选项: tenantId={}, groupId={}, title={}", tenantId, groupId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性选项名称不能为空");
        }
        
        // 2. 校验属性组存在且属于租户
        BcProductAttrGroup group = attrGroupMapper.selectOne(new LambdaQueryWrapper<BcProductAttrGroup>()
                .eq(BcProductAttrGroup::getId, groupId)
                .eq(BcProductAttrGroup::getTenantId, tenantId));
        
        if (group == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性组不存在");
        }
        
        // 3. 组装实体
        BcProductAttrOption option = new BcProductAttrOption();
        option.setTenantId(tenantId);
        option.setAttrGroupId(groupId);
        option.setName(cmd.getTitle());
        option.setPriceDelta(cmd.getPriceDelta() != null ? cmd.getPriceDelta() : java.math.BigDecimal.ZERO);
        option.setSortOrder(cmd.getSortOrder() != null ? cmd.getSortOrder() : 0);
        option.setStatus(Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0);
        option.setCreatedAt(LocalDateTime.now());
        option.setUpdatedAt(LocalDateTime.now());
        
        // 4. 插入数据库
        attrOptionMapper.insert(option);
        
        Long optionId = option.getId();
        log.info("属性选项创建成功: tenantId={}, groupId={}, optionId={}", tenantId, groupId, optionId);
        
        // 5. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-option:create");
        
        return optionId;
    }
    
    /**
     * 更新属性选项
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAttrOption(Long tenantId, Long groupId, Long optionId, 
                                UpdateAttrOptionCommand cmd, Long operatorId) {
        log.info("更新属性选项: tenantId={}, groupId={}, optionId={}, title={}", 
                tenantId, groupId, optionId, cmd.getTitle());
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        if (optionId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性选项ID不能为空");
        }
        if (cmd.getTitle() == null || cmd.getTitle().isBlank()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性选项名称不能为空");
        }
        
        // 2. 查询属性选项是否存在
        BcProductAttrOption option = attrOptionMapper.selectOne(new LambdaQueryWrapper<BcProductAttrOption>()
                .eq(BcProductAttrOption::getId, optionId)
                .eq(BcProductAttrOption::getAttrGroupId, groupId)
                .eq(BcProductAttrOption::getTenantId, tenantId));
        
        if (option == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性选项不存在或不属于该属性组");
        }
        
        // 3. 更新字段
        LambdaUpdateWrapper<BcProductAttrOption> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductAttrOption::getId, optionId)
                .eq(BcProductAttrOption::getAttrGroupId, groupId)
                .eq(BcProductAttrOption::getTenantId, tenantId)
                .set(BcProductAttrOption::getName, cmd.getTitle())
                .set(BcProductAttrOption::getPriceDelta, cmd.getPriceDelta() != null ? cmd.getPriceDelta() : java.math.BigDecimal.ZERO)
                .set(BcProductAttrOption::getSortOrder, cmd.getSortOrder() != null ? cmd.getSortOrder() : 0)
                .set(BcProductAttrOption::getStatus, Boolean.TRUE.equals(cmd.getEnabled()) ? 1 : 0)
                .set(BcProductAttrOption::getUpdatedAt, LocalDateTime.now());
        
        int updated = attrOptionMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性选项更新失败");
        }
        
        log.info("属性选项更新成功: tenantId={}, groupId={}, optionId={}", tenantId, groupId, optionId);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-option:update");
    }
    
    /**
     * 查询属性选项列表
     */
    public List<AttrOptionAdminView> listAttrOptions(Long tenantId, Long groupId, boolean includeDisabled, 
                                                     boolean filterByTime, LocalDateTime now) {
        log.info("查询属性选项列表: tenantId={}, groupId={}, includeDisabled={}, filterByTime={}", 
                tenantId, groupId, includeDisabled, filterByTime);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        
        // 2. 构建查询条件
        LambdaQueryWrapper<BcProductAttrOption> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BcProductAttrOption::getTenantId, tenantId)
                .eq(BcProductAttrOption::getAttrGroupId, groupId);
        
        // 2.1 状态过滤
        if (!includeDisabled) {
            wrapper.eq(BcProductAttrOption::getStatus, 1);
        }
        
        // 2.2 排序
        wrapper.orderByDesc(BcProductAttrOption::getSortOrder)
                .orderByAsc(BcProductAttrOption::getId);
        
        // 3. 查询数据库
        List<BcProductAttrOption> options = attrOptionMapper.selectList(wrapper);
        
        // 4. 映射为视图
        List<AttrOptionAdminView> views = options.stream()
                .map(this::toAttrOptionView)
                .collect(Collectors.toList());
        
        log.info("查询属性选项列表成功: tenantId={}, groupId={}, count={}", tenantId, groupId, views.size());
        return views;
    }
    
    /**
     * 修改属性选项状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeAttrOptionStatus(Long tenantId, Long groupId, Long optionId, 
                                      boolean enabled, Long operatorId) {
        log.info("修改属性选项状态: tenantId={}, groupId={}, optionId={}, enabled={}", 
                tenantId, groupId, optionId, enabled);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        if (optionId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性选项ID不能为空");
        }
        
        // 2. 查询属性选项是否存在
        BcProductAttrOption option = attrOptionMapper.selectOne(new LambdaQueryWrapper<BcProductAttrOption>()
                .eq(BcProductAttrOption::getId, optionId)
                .eq(BcProductAttrOption::getAttrGroupId, groupId)
                .eq(BcProductAttrOption::getTenantId, tenantId));
        
        if (option == null) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性选项不存在");
        }
        
        // 3. 更新状态
        LambdaUpdateWrapper<BcProductAttrOption> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(BcProductAttrOption::getId, optionId)
                .eq(BcProductAttrOption::getAttrGroupId, groupId)
                .eq(BcProductAttrOption::getTenantId, tenantId)
                .set(BcProductAttrOption::getStatus, enabled ? 1 : 0)
                .set(BcProductAttrOption::getUpdatedAt, LocalDateTime.now());
        
        int updated = attrOptionMapper.update(null, updateWrapper);
        
        if (updated == 0) {
            throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, "属性选项状态修改失败");
        }
        
        log.info("属性选项状态修改成功: tenantId={}, groupId={}, optionId={}, enabled={}", 
                tenantId, groupId, optionId, enabled);
        
        // 4. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-option:status");
    }
    
    /**
     * 批量调整属性选项排序
     */
    @Transactional(rollbackFor = Exception.class)
    public void reorderAttrOptions(Long tenantId, Long groupId, List<AttrOptionReorderItem> items, Long operatorId) {
        log.info("批量调整属性选项排序: tenantId={}, groupId={}, count={}", 
                tenantId, groupId, items != null ? items.size() : 0);
        
        // 1. 校验参数
        if (tenantId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "租户ID不能为空");
        }
        if (groupId == null) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性组ID不能为空");
        }
        if (items == null || items.isEmpty()) {
            throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序项列表不能为空");
        }
        
        // 2. 逐个校验并更新
        for (AttrOptionReorderItem item : items) {
            if (item.getOptionId() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "属性选项ID不能为空");
            }
            if (item.getSortOrder() == null) {
                throw new BusinessException(BizErrorCode.INVALID_PARAM, "排序值不能为空");
            }
            
            // 更新排序值
            LambdaUpdateWrapper<BcProductAttrOption> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(BcProductAttrOption::getId, item.getOptionId())
                    .eq(BcProductAttrOption::getAttrGroupId, groupId)
                    .eq(BcProductAttrOption::getTenantId, tenantId)
                    .set(BcProductAttrOption::getSortOrder, item.getSortOrder())
                    .set(BcProductAttrOption::getUpdatedAt, LocalDateTime.now());
            
            int updated = attrOptionMapper.update(null, updateWrapper);
            
            if (updated == 0) {
                throw new BusinessException(BizErrorCode.RESOURCE_NOT_FOUND, 
                        "属性选项不存在或不属于当前租户: optionId=" + item.getOptionId());
            }
        }
        
        log.info("属性选项排序调整成功: tenantId={}, groupId={}, count={}", tenantId, groupId, items.size());
        
        // 3. 失效菜单快照缓存
        invalidateTenantMenus(tenantId, "attr-option:reorder");
    }
    
    // ===== 私有方法 =====
    
    /**
     * 实体转视图 - 属性组
     */
    private AttrGroupAdminView toAttrGroupView(BcProductAttrGroup group) {
        return AttrGroupAdminView.builder()
                .id(group.getId())
                .title(group.getName())
                .selectType(group.getSelectType())
                .required(group.getRequired())
                .maxSelect(group.getMaxSelect())
                .sortOrder(group.getSortOrder())
                .enabled(group.getStatus() != null && group.getStatus() == 1)
                .displayStartAt(null) // 实体中没有该字段
                .displayEndAt(null)   // 实体中没有该字段
                .createdAt(group.getCreatedAt())
                .updatedAt(group.getUpdatedAt())
                .build();
    }
    
    /**
     * 实体转视图 - 属性选项
     */
    private AttrOptionAdminView toAttrOptionView(BcProductAttrOption option) {
        return AttrOptionAdminView.builder()
                .id(option.getId())
                .title(option.getName())
                .priceDelta(option.getPriceDelta())
                .sortOrder(option.getSortOrder())
                .enabled(option.getStatus() != null && option.getStatus() == 1)
                .displayStartAt(null) // 实体中没有该字段
                .displayEndAt(null)   // 实体中没有该字段
                .createdAt(option.getCreatedAt())
                .updatedAt(option.getUpdatedAt())
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

