package com.bluecone.app.promo.domain.repository;

import com.bluecone.app.promo.domain.model.CouponTemplate;

import java.util.List;
import java.util.Optional;

/**
 * 优惠券模板仓储接口
 */
public interface CouponTemplateRepository {

    /**
     * 保存模板
     */
    CouponTemplate save(CouponTemplate template);

    /**
     * 更新模板
     */
    void update(CouponTemplate template);

    /**
     * 乐观锁更新（用于配额扣减）
     */
    boolean updateWithVersion(CouponTemplate template);

    /**
     * 根据ID查询
     */
    Optional<CouponTemplate> findById(Long id);

    /**
     * 根据模板编码查询
     */
    Optional<CouponTemplate> findByCode(Long tenantId, String templateCode);

    /**
     * 查询租户的所有模板
     */
    List<CouponTemplate> findByTenant(Long tenantId);

    /**
     * 查询在线模板
     */
    List<CouponTemplate> findOnlineTemplates(Long tenantId);

    /**
     * 原子增加已发放数量（用于配额控制）
     * 
     * @return 更新成功返回true，配额不足返回false
     */
    boolean incrementIssuedCount(Long templateId, int delta);
}
