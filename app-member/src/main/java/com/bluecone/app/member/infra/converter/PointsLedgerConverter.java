package com.bluecone.app.member.infra.converter;

import com.bluecone.app.member.domain.enums.PointsDirection;
import com.bluecone.app.member.domain.model.PointsLedger;
import com.bluecone.app.member.infra.persistence.po.PointsLedgerPO;

/**
 * 积分流水实体与PO转换器
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public class PointsLedgerConverter {
    
    /**
     * PO 转 Domain
     */
    public static PointsLedger toDomain(PointsLedgerPO po) {
        if (po == null) {
            return null;
        }
        
        PointsLedger ledger = new PointsLedger();
        ledger.setId(po.getId());
        ledger.setTenantId(po.getTenantId());
        ledger.setMemberId(po.getMemberId());
        ledger.setDirection(PointsDirection.valueOf(po.getDirection()));
        ledger.setDeltaPoints(po.getDeltaPoints());
        ledger.setBeforeAvailable(po.getBeforeAvailable());
        ledger.setBeforeFrozen(po.getBeforeFrozen());
        ledger.setAfterAvailable(po.getAfterAvailable());
        ledger.setAfterFrozen(po.getAfterFrozen());
        ledger.setBizType(po.getBizType());
        ledger.setBizId(po.getBizId());
        ledger.setIdempotencyKey(po.getIdempotencyKey());
        ledger.setRemark(po.getRemark());
        ledger.setCreatedAt(po.getCreatedAt());
        ledger.setCreatedBy(po.getCreatedBy());
        return ledger;
    }
    
    /**
     * Domain 转 PO
     */
    public static PointsLedgerPO toPO(PointsLedger ledger) {
        if (ledger == null) {
            return null;
        }
        
        PointsLedgerPO po = new PointsLedgerPO();
        po.setId(ledger.getId());
        po.setTenantId(ledger.getTenantId());
        po.setMemberId(ledger.getMemberId());
        po.setDirection(ledger.getDirection().name());
        po.setDeltaPoints(ledger.getDeltaPoints());
        po.setBeforeAvailable(ledger.getBeforeAvailable());
        po.setBeforeFrozen(ledger.getBeforeFrozen());
        po.setAfterAvailable(ledger.getAfterAvailable());
        po.setAfterFrozen(ledger.getAfterFrozen());
        po.setBizType(ledger.getBizType());
        po.setBizId(ledger.getBizId());
        po.setIdempotencyKey(ledger.getIdempotencyKey());
        po.setRemark(ledger.getRemark());
        po.setCreatedAt(ledger.getCreatedAt());
        po.setCreatedBy(ledger.getCreatedBy());
        return po;
    }
}
