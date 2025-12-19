package com.bluecone.app.promo.infra.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import com.bluecone.app.promo.infra.persistence.converter.CouponTemplateConverter;
import com.bluecone.app.promo.infra.persistence.mapper.CouponTemplateMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponTemplatePO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 优惠券模板仓储实现
 */
@Repository
@RequiredArgsConstructor
public class CouponTemplateRepositoryImpl implements CouponTemplateRepository {

    private final CouponTemplateMapper mapper;
    private final CouponTemplateConverter converter;

    @Override
    public CouponTemplate save(CouponTemplate template) {
        CouponTemplatePO po = converter.toPO(template);
        mapper.insert(po);
        return converter.toDomain(po);
    }

    @Override
    public void update(CouponTemplate template) {
        CouponTemplatePO po = converter.toPO(template);
        mapper.updateById(po);
    }

    @Override
    public boolean updateWithVersion(CouponTemplate template) {
        CouponTemplatePO po = converter.toPO(template);
        LambdaUpdateWrapper<CouponTemplatePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplatePO::getId, po.getId())
                .eq(CouponTemplatePO::getVersion, template.getVersion());
        
        // 更新时版本号+1
        po.setVersion(template.getVersion() + 1);
        
        int updated = mapper.update(po, wrapper);
        return updated > 0;
    }

    @Override
    public Optional<CouponTemplate> findById(Long id) {
        CouponTemplatePO po = mapper.selectById(id);
        return Optional.ofNullable(po).map(converter::toDomain);
    }

    @Override
    public Optional<CouponTemplate> findByCode(Long tenantId, String templateCode) {
        LambdaQueryWrapper<CouponTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplatePO::getTenantId, tenantId)
                .eq(CouponTemplatePO::getTemplateCode, templateCode);
        CouponTemplatePO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(converter::toDomain);
    }

    @Override
    public List<CouponTemplate> findByTenant(Long tenantId) {
        LambdaQueryWrapper<CouponTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplatePO::getTenantId, tenantId)
                .orderByDesc(CouponTemplatePO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<CouponTemplate> findOnlineTemplates(Long tenantId) {
        LambdaQueryWrapper<CouponTemplatePO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CouponTemplatePO::getTenantId, tenantId)
                .eq(CouponTemplatePO::getStatus, "ONLINE")
                .orderByDesc(CouponTemplatePO::getCreatedAt);
        return mapper.selectList(wrapper).stream()
                .map(converter::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean incrementIssuedCount(Long templateId, int delta) {
        // 使用原子SQL更新，确保配额不超发
        LambdaUpdateWrapper<CouponTemplatePO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(CouponTemplatePO::getId, templateId)
                .setSql("issued_count = issued_count + " + delta)
                .setSql("version = version + 1")
                // 关键：只有在配额充足时才更新
                .apply("(total_quantity IS NULL OR issued_count + {0} <= total_quantity)", delta);
        
        int updated = mapper.update(null, wrapper);
        return updated > 0;
    }
}
