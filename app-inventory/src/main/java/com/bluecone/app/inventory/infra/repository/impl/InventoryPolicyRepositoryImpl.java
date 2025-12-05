package com.bluecone.app.inventory.infra.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.inventory.domain.model.InventoryPolicy;
import com.bluecone.app.inventory.domain.repository.InventoryPolicyRepository;
import com.bluecone.app.inventory.domain.type.InventoryDeductMode;
import com.bluecone.app.inventory.infra.mapper.InvPolicyMapper;
import com.bluecone.app.inventory.infra.po.InvPolicyDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class InventoryPolicyRepositoryImpl implements InventoryPolicyRepository {

    private final InvPolicyMapper invPolicyMapper;

    @Override
    public InventoryPolicy findByItem(Long tenantId, Long storeId, Long itemId) {
        InvPolicyDO data = invPolicyMapper.selectOne(new LambdaQueryWrapper<InvPolicyDO>()
                .eq(InvPolicyDO::getTenantId, tenantId)
                .eq(InvPolicyDO::getStoreId, storeId)
                .eq(InvPolicyDO::getItemId, itemId));
        return toDomain(data);
    }

    @Override
    public void save(InventoryPolicy policy) {
        if (policy == null) {
            return;
        }
        InvPolicyDO data = toDO(policy);
        invPolicyMapper.insert(data);
        policy.setId(data.getId());
    }

    @Override
    public void update(InventoryPolicy policy) {
        if (policy == null || policy.getId() == null) {
            return;
        }
        invPolicyMapper.updateById(toDO(policy));
    }

    private InventoryPolicy toDomain(InvPolicyDO data) {
        if (data == null) {
            return null;
        }
        return InventoryPolicy.builder()
                .id(data.getId())
                .tenantId(data.getTenantId())
                .storeId(data.getStoreId())
                .itemId(data.getItemId())
                .deductMode(InventoryDeductMode.fromCode(data.getDeductMode()))
                .oversellAllowed(data.getOversellAllowed() != null ? data.getOversellAllowed() == 1 : null)
                .oversellLimit(data.getOversellLimit())
                .maxDailySold(data.getMaxDailySold())
                .status(data.getStatus())
                .remark(data.getRemark())
                .createdAt(data.getCreatedAt())
                .updatedAt(data.getUpdatedAt())
                .build();
    }

    private InvPolicyDO toDO(InventoryPolicy policy) {
        if (policy == null) {
            return null;
        }
        InvPolicyDO data = new InvPolicyDO();
        data.setId(policy.getId());
        data.setTenantId(policy.getTenantId());
        data.setStoreId(policy.getStoreId());
        data.setItemId(policy.getItemId());
        data.setDeductMode(policy.getDeductMode() != null ? policy.getDeductMode().getCode() : null);
        data.setOversellAllowed(policy.getOversellAllowed() == null ? null : (policy.getOversellAllowed() ? 1 : 0));
        data.setOversellLimit(policy.getOversellLimit());
        data.setMaxDailySold(policy.getMaxDailySold());
        data.setStatus(policy.getStatus());
        data.setRemark(policy.getRemark());
        data.setCreatedAt(policy.getCreatedAt());
        data.setUpdatedAt(policy.getUpdatedAt());
        return data;
    }
}
