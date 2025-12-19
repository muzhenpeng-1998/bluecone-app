package com.bluecone.app.member.infra.converter;

import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.infra.persistence.po.PointsAccountPO;

/**
 * 积分账户实体与PO转换器
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class PointsAccountConverter {
    
    /**
     * PO 转 Domain
     */
    public static PointsAccount toDomain(PointsAccountPO po) {
        if (po == null) {
            return null;
        }
        
        PointsAccount account = new PointsAccount();
        account.setId(po.getId());
        account.setTenantId(po.getTenantId());
        account.setMemberId(po.getMemberId());
        account.setAvailablePoints(po.getAvailablePoints());
        account.setFrozenPoints(po.getFrozenPoints());
        account.setVersion(po.getVersion());
        account.setCreatedAt(po.getCreatedAt());
        account.setUpdatedAt(po.getUpdatedAt());
        return account;
    }
    
    /**
     * Domain 转 PO
     */
    public static PointsAccountPO toPO(PointsAccount account) {
        if (account == null) {
            return null;
        }
        
        PointsAccountPO po = new PointsAccountPO();
        po.setId(account.getId());
        po.setTenantId(account.getTenantId());
        po.setMemberId(account.getMemberId());
        po.setAvailablePoints(account.getAvailablePoints());
        po.setFrozenPoints(account.getFrozenPoints());
        po.setVersion(account.getVersion());
        po.setCreatedAt(account.getCreatedAt());
        po.setUpdatedAt(account.getUpdatedAt());
        return po;
    }
}
