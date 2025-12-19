package com.bluecone.app.promo.domain.repository;

import com.bluecone.app.promo.domain.model.CouponGrantLog;

import java.util.List;
import java.util.Optional;

/**
 * 优惠券发放日志仓储接口
 */
public interface CouponGrantLogRepository {

    /**
     * 保存发放日志（幂等键唯一约束）
     */
    CouponGrantLog save(CouponGrantLog grantLog);

    /**
     * 更新发放日志
     */
    void update(CouponGrantLog grantLog);

    /**
     * 根据幂等键查询
     */
    Optional<CouponGrantLog> findByIdempotencyKey(Long tenantId, String idempotencyKey);

    /**
     * 根据ID查询
     */
    Optional<CouponGrantLog> findById(Long id);

    /**
     * 统计用户从指定模板领取的券数量
     */
    int countUserGrantedByTemplate(Long tenantId, Long templateId, Long userId);

    /**
     * 查询用户的发放日志
     */
    List<CouponGrantLog> findByUser(Long tenantId, Long userId, int limit);

    /**
     * 查询模板的发放日志
     */
    List<CouponGrantLog> findByTemplate(Long tenantId, Long templateId, int limit);
}
