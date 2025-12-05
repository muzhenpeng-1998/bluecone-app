package com.bluecone.app.inventory.infra.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.inventory.domain.model.InventoryItem;
import com.bluecone.app.inventory.domain.repository.InventoryItemRepository;
import com.bluecone.app.inventory.domain.type.InventoryItemType;
import com.bluecone.app.inventory.infra.mapper.InvItemMapper;
import com.bluecone.app.inventory.infra.po.InvItemDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryItemRepositoryImpl implements InventoryItemRepository {

    private final InvItemMapper invItemMapper;

    @Override
    public InventoryItem findById(Long id) {
        InvItemDO data = invItemMapper.selectById(id);
        return toDomain(data);
    }

    @Override
    public InventoryItem findByRef(Long tenantId, InventoryItemType itemType, Long refId) {
        InvItemDO data = invItemMapper.selectOne(new LambdaQueryWrapper<InvItemDO>()
                .eq(InvItemDO::getTenantId, tenantId)
                .eq(InvItemDO::getItemType, itemType != null ? itemType.getCode() : null)
                .eq(InvItemDO::getRefId, refId));
        return toDomain(data);
    }

    @Override
    public void save(InventoryItem item) {
        if (item == null) {
            return;
        }
        InvItemDO data = toDO(item);
        invItemMapper.insert(data);
        item.setId(data.getId());
    }

    @Override
    public void update(InventoryItem item) {
        if (item == null || item.getId() == null) {
            return;
        }
        invItemMapper.updateById(toDO(item));
    }

    private InventoryItem toDomain(InvItemDO data) {
        if (data == null) {
            return null;
        }
        return InventoryItem.builder()
                .id(data.getId())
                .tenantId(data.getTenantId())
                .itemType(InventoryItemType.fromCode(data.getItemType()))
                .refId(data.getRefId())
                .externalCode(data.getExternalCode())
                .name(data.getName())
                .unit(data.getUnit())
                .status(data.getStatus())
                .remark(data.getRemark())
                .ext(data.getExt())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }

    private InvItemDO toDO(InventoryItem item) {
        if (item == null) {
            return null;
        }
        InvItemDO data = new InvItemDO();
        data.setId(item.getId());
        data.setTenantId(item.getTenantId());
        data.setItemType(item.getItemType() != null ? item.getItemType().getCode() : null);
        data.setRefId(item.getRefId());
        data.setExternalCode(item.getExternalCode());
        data.setName(item.getName());
        data.setUnit(item.getUnit());
        data.setStatus(item.getStatus());
        data.setRemark(item.getRemark());
        data.setExt(item.getExt());
        data.setCreatedAt(item.getCreatedAt());
        data.setUpdatedAt(item.getUpdatedAt());
        return data;
    }
}
