package com.bluecone.app.promo.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.api.enums.TemplateStatus;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 优惠券模板领域服务测试
 */
@ExtendWith(MockitoExtension.class)
class CouponTemplateDomainServiceTest {

    @Mock
    private CouponTemplateRepository templateRepository;

    private CouponTemplateDomainService templateService;

    @BeforeEach
    void setUp() {
        templateService = new CouponTemplateDomainService(templateRepository);
    }

    @Test
    void testCreateDraft_ShouldSetInitialStatus() {
        // Given
        CouponTemplate template = createBasicTemplate();
        when(templateRepository.findByCode(template.getTenantId(), template.getTemplateCode()))
                .thenReturn(Optional.empty());
        when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        CouponTemplate result = templateService.createDraft(template);

        // Then
        assertNotNull(result);
        assertEquals(TemplateStatus.DRAFT.name(), result.getStatus());
        assertEquals(0, result.getIssuedCount());
        assertEquals(0, result.getVersion());
        assertNotNull(result.getCreatedAt());
        verify(templateRepository).save(any());
    }

    @Test
    void testCreateDraft_WhenCodeDuplicate_ShouldThrowException() {
        // Given
        CouponTemplate template = createBasicTemplate();
        when(templateRepository.findByCode(template.getTenantId(), template.getTemplateCode()))
                .thenReturn(Optional.of(template));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateService.createDraft(template);
        });

        assertEquals("TEMPLATE_CODE_DUPLICATE", exception.getCode());
        verify(templateRepository, never()).save(any());
    }

    @Test
    void testPublishTemplate_WhenDraft_ShouldChangeToOnline() {
        // Given
        Long templateId = 100L;
        CouponTemplate template = createBasicTemplate();
        template.setId(templateId);
        template.setStatus(TemplateStatus.DRAFT.name());
        template.setValidStartTime(LocalDateTime.now());
        template.setValidEndTime(LocalDateTime.now().plusDays(30));

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        templateService.publishTemplate(templateId);

        // Then
        verify(templateRepository).update(argThat(t -> 
                TemplateStatus.ONLINE.name().equals(t.getStatus())
        ));
    }

    @Test
    void testPublishTemplate_WhenNotDraft_ShouldThrowException() {
        // Given
        Long templateId = 100L;
        CouponTemplate template = createBasicTemplate();
        template.setId(templateId);
        template.setStatus(TemplateStatus.ONLINE.name());

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateService.publishTemplate(templateId);
        });

        assertEquals("INVALID_STATUS_TRANSITION", exception.getCode());
        verify(templateRepository, never()).update(any());
    }

    @Test
    void testOfflineTemplate_WhenOnline_ShouldChangeToOffline() {
        // Given
        Long templateId = 100L;
        CouponTemplate template = createBasicTemplate();
        template.setId(templateId);
        template.setStatus(TemplateStatus.ONLINE.name());

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        templateService.offlineTemplate(templateId, "测试下线");

        // Then
        verify(templateRepository).update(argThat(t -> 
                TemplateStatus.OFFLINE.name().equals(t.getStatus())
        ));
    }

    @Test
    void testRepublishTemplate_WhenOffline_ShouldChangeToOnline() {
        // Given
        Long templateId = 100L;
        CouponTemplate template = createBasicTemplate();
        template.setId(templateId);
        template.setStatus(TemplateStatus.OFFLINE.name());
        template.setValidStartTime(LocalDateTime.now());
        template.setValidEndTime(LocalDateTime.now().plusDays(30));

        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));

        // When
        templateService.republishTemplate(templateId);

        // Then
        verify(templateRepository).update(argThat(t -> 
                TemplateStatus.ONLINE.name().equals(t.getStatus())
        ));
    }

    @Test
    void testUpdateDraft_WhenNotDraft_ShouldThrowException() {
        // Given
        CouponTemplate template = createBasicTemplate();
        template.setId(100L);
        template.setStatus(TemplateStatus.ONLINE.name());

        when(templateRepository.findById(template.getId())).thenReturn(Optional.of(template));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            templateService.updateDraft(template);
        });

        assertEquals("TEMPLATE_NOT_DRAFT", exception.getCode());
        verify(templateRepository, never()).update(any());
    }

    private CouponTemplate createBasicTemplate() {
        return CouponTemplate.builder()
                .tenantId(1L)
                .templateCode("TEST-TEMPLATE")
                .templateName("测试模板")
                .couponType(CouponType.DISCOUNT_AMOUNT)
                .discountAmount(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.ZERO)
                .applicableScope(ApplicableScope.ALL)
                .validDays(30)
                .totalQuantity(100)
                .perUserLimit(5)
                .build();
    }
}
