package com.bluecone.app.promo.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.promo.api.enums.TemplateStatus;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 优惠券模板领域服务
 * 负责模板状态机管理：DRAFT -> ONLINE -> OFFLINE
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateDomainService {

    private final CouponTemplateRepository templateRepository;

    /**
     * 创建模板（草稿状态）
     */
    @Transactional
    public CouponTemplate createDraft(CouponTemplate template) {
        // 验证模板编码唯一性
        templateRepository.findByCode(template.getTenantId(), template.getTemplateCode())
                .ifPresent(existing -> {
                    throw new BusinessException("TEMPLATE_CODE_DUPLICATE", 
                            "模板编码已存在: " + template.getTemplateCode());
                });

        // 设置初始状态
        template.setStatus(TemplateStatus.DRAFT.name());
        template.setIssuedCount(0);
        template.setVersion(0);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        // 验证模板配置
        validateTemplateConfig(template);

        return templateRepository.save(template);
    }

    /**
     * 更新草稿模板
     */
    @Transactional
    public void updateDraft(CouponTemplate template) {
        CouponTemplate existing = templateRepository.findById(template.getId())
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

        // 只有草稿状态才能修改
        if (!existing.isDraft()) {
            throw new BusinessException("TEMPLATE_NOT_DRAFT", 
                    "只有草稿状态的模板才能修改，当前状态: " + existing.getStatus());
        }

        // 验证模板配置
        validateTemplateConfig(template);

        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.update(template);
    }

    /**
     * 上线模板（DRAFT -> ONLINE）
     */
    @Transactional
    public void publishTemplate(Long templateId) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

        // 状态机检查
        if (!template.isDraft()) {
            throw new BusinessException("INVALID_STATUS_TRANSITION", 
                    "只能从草稿状态上线，当前状态: " + template.getStatus());
        }

        // 上线前验证
        validateBeforePublish(template);

        template.setStatus(TemplateStatus.ONLINE.name());
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.update(template);

        log.info("优惠券模板已上线: templateId={}, templateCode={}", 
                templateId, template.getTemplateCode());
    }

    /**
     * 下线模板（ONLINE -> OFFLINE）
     */
    @Transactional
    public void offlineTemplate(Long templateId, String reason) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

        // 状态机检查
        if (!template.isOnline()) {
            throw new BusinessException("INVALID_STATUS_TRANSITION", 
                    "只能从在线状态下线，当前状态: " + template.getStatus());
        }

        template.setStatus(TemplateStatus.OFFLINE.name());
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.update(template);

        log.info("优惠券模板已下线: templateId={}, templateCode={}, reason={}", 
                templateId, template.getTemplateCode(), reason);
    }

    /**
     * 重新上线（OFFLINE -> ONLINE）
     */
    @Transactional
    public void republishTemplate(Long templateId) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

        // 状态机检查
        if (!template.isOffline()) {
            throw new BusinessException("INVALID_STATUS_TRANSITION", 
                    "只能从下线状态重新上线，当前状态: " + template.getStatus());
        }

        // 重新验证
        validateBeforePublish(template);

        template.setStatus(TemplateStatus.ONLINE.name());
        template.setUpdatedAt(LocalDateTime.now());
        templateRepository.update(template);

        log.info("优惠券模板已重新上线: templateId={}, templateCode={}", 
                templateId, template.getTemplateCode());
    }

    /**
     * 删除草稿模板
     */
    @Transactional
    public void deleteDraft(Long templateId) {
        CouponTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("TEMPLATE_NOT_FOUND", "模板不存在"));

        // 只能删除草稿
        if (!template.isDraft()) {
            throw new BusinessException("CANNOT_DELETE_PUBLISHED", 
                    "只能删除草稿状态的模板，当前状态: " + template.getStatus());
        }

        // 这里可以实现软删除或硬删除
        // 暂时不实现删除逻辑，保留数据
        throw new BusinessException("DELETE_NOT_SUPPORTED", "暂不支持删除模板");
    }

    /**
     * 验证模板配置
     */
    private void validateTemplateConfig(CouponTemplate template) {
        if (template.getCouponType() == null) {
            throw new BusinessException("INVALID_TEMPLATE", "券类型不能为空");
        }

        if (template.getApplicableScope() == null) {
            throw new BusinessException("INVALID_TEMPLATE", "适用范围不能为空");
        }

        // 验证有效期配置
        if (template.getValidDays() == null && 
            (template.getValidStartTime() == null || template.getValidEndTime() == null)) {
            throw new BusinessException("INVALID_TEMPLATE", 
                    "必须配置有效天数或固定有效期");
        }

        if (template.getValidDays() != null && template.getValidDays() <= 0) {
            throw new BusinessException("INVALID_TEMPLATE", "有效天数必须大于0");
        }

        if (template.getValidStartTime() != null && template.getValidEndTime() != null) {
            if (!template.getValidEndTime().isAfter(template.getValidStartTime())) {
                throw new BusinessException("INVALID_TEMPLATE", "结束时间必须晚于开始时间");
            }
        }

        // 验证配额配置
        if (template.getTotalQuantity() != null && template.getTotalQuantity() <= 0) {
            throw new BusinessException("INVALID_TEMPLATE", "总发行量必须大于0");
        }

        if (template.getPerUserLimit() != null && template.getPerUserLimit() <= 0) {
            throw new BusinessException("INVALID_TEMPLATE", "每人限领数量必须大于0");
        }
    }

    /**
     * 上线前验证
     */
    private void validateBeforePublish(CouponTemplate template) {
        // 如果使用固定有效期，检查是否已过期
        if (template.useFixedValidity()) {
            LocalDateTime now = LocalDateTime.now();
            if (template.getValidEndTime().isBefore(now)) {
                throw new BusinessException("TEMPLATE_EXPIRED", 
                        "模板有效期已过，无法上线");
            }
        }

        // 检查配额是否已用完
        if (template.getTotalQuantity() != null && 
            template.getIssuedCount() >= template.getTotalQuantity()) {
            throw new BusinessException("QUOTA_EXHAUSTED", 
                    "模板配额已用完，无法上线");
        }
    }
}
