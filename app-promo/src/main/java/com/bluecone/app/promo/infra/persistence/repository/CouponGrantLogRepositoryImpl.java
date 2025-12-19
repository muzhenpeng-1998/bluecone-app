package com.bluecone.app.promo.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.promo.domain.model.CouponGrantLog;
import com.bluecone.app.promo.domain.repository.CouponGrantLogRepository;
import com.bluecone.app.promo.infra.persistence.converter.CouponGrantLogConverter;
import com.bluecone.app.promo.infra.persistence.mapper.CouponGrantLogMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponGrantLogPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 优惠券发放日志仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CouponGrantLogRepositoryImpl implements CouponGrantLogRepository {

    private final CouponGrantLogMapper mapper;
    private final CouponGrantLogConverter converter;

    @Override
    public CouponGrantLog save(CouponGrantLog grantLog) {
        CouponGrantLogPO po = converter.toPO(grantLog);
        mapper.insert(po);
        return converter.toDomain(po);
    }

    @Override
    public void update(CouponGrantLog grantLog) {
        CouponGrantLogPO po = converter.toPO(grantLog);
        mapper.updateById(po);
    }

    @Override
    public Optional<CouponGrantLog> findByIdempotencyKey(Long tenantId, String idempotencyKey) {
        LambdaQueryWrapper<CouponGrantLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantLogPO::getTenantId, tenantId)
                .eq(CouponGrantLogPO::getIdempotencyKey, idempotencyKey);
        CouponGrantLogPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(converter::toDomain);
    }

    @Override
    public Optional<CouponGrantLog> findById(Long id) {
        CouponGrantLogPO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(converter::toDomain);
    }

    @Override
    public int countUserGrantedByTemplate(Long tenantId, Long templateId, Long userId) {
        return mapper.countUserGrantedByTemplate(tenantId, templateId, userId);
    }

    @Override
    public List<CouponGrantLog> findByUser(Long tenantId, Long userId, int limit) {
        LambdaQueryWrapper<CouponGrantLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantLogPO::getTenantId, tenantId)
                .eq(CouponGrantLogPO::getUserId, userId)
                .orderByDesc(CouponGrantLogPO::getCreatedAt)
                .last("LIMIT " + limit);
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponGrantLog> findByTemplate(Long tenantId, Long templateId, int limit) {
        LambdaQueryWrapper<CouponGrantLogPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponGrantLogPO::getTenantId, tenantId)
                .eq(CouponGrantLogPO::getTemplateId, templateId)
                .orderByDesc(CouponGrantLogPO::getCreatedAt)
                .last("LIMIT " + limit);
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }
}
