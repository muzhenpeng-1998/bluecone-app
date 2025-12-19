package com.bluecone.app.promo.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.enums.ApplicableScope;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponGrantLog;
import com.bluecone.app.promo.domain.model.CouponTemplate;
import com.bluecone.app.promo.domain.repository.CouponGrantLogRepository;
import com.bluecone.app.promo.domain.repository.CouponRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券发放服务配额测试
 */
@ExtendWith(MockitoExtension.class)
class CouponGrantServiceQuotaTest {

    @Mock
    private CouponTemplateRepository templateRepository;

    @Mock
    private CouponGrantLogRepository grantLogRepository;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private IdService idService;

    @Mock
    private CouponGrantMetrics metrics;

    private CouponGrantService grantService;

    @BeforeEach
    void setUp() {
        grantService = new CouponGrantService(
                templateRepository,
                grantLogRepository,
                couponRepository,
                idService,
                metrics
        );
    }

    @Test
    void testQuota_WhenTotalQuotaExceeded_ShouldThrowException() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";

        CouponTemplate template = createTemplate(templateId, 100, 5, 100, 0);

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idService.nextLong(IdScope.COUPON_GRANT_LOG)).thenReturn(1L);
        when(grantLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId))
                .thenReturn(0);
        // 模拟配额已用完
        when(templateRepository.incrementIssuedCount(templateId, 1)).thenReturn(false);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            grantService.grantCoupon(
                    tenantId, templateId, userId, idempotencyKey,
                    GrantSource.MANUAL_ADMIN, null, null, null
            );
        });

        assertEquals("TOTAL_QUOTA_EXCEEDED", exception.getCode());
        verify(metrics).recordQuotaExceeded(templateId);
        verify(grantLogRepository).update(argThat(log -> 
                log.getErrorCode().equals("TOTAL_QUOTA_EXCEEDED")
        ));
    }

    @Test
    void testQuota_WhenUserQuotaExceeded_ShouldThrowException() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";

        CouponTemplate template = createTemplate(templateId, 100, 5, 50, 0);

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idService.nextLong(IdScope.COUPON_GRANT_LOG)).thenReturn(1L);
        when(grantLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        // 用户已领取5张，达到上限
        when(grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId))
                .thenReturn(5);

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            grantService.grantCoupon(
                    tenantId, templateId, userId, idempotencyKey,
                    GrantSource.MANUAL_ADMIN, null, null, null
            );
        });

        assertEquals("USER_QUOTA_EXCEEDED", exception.getCode());
        verify(metrics).recordUserQuotaExceeded(userId, templateId);
        verify(templateRepository, never()).incrementIssuedCount(anyLong(), anyInt());
    }

    @Test
    void testQuota_WhenQuotaSufficient_ShouldGrantSuccessfully() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";
        Long couponId = 5000L;

        CouponTemplate template = createTemplate(templateId, 100, 5, 50, 0);

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idService.nextLong(IdScope.COUPON_GRANT_LOG)).thenReturn(1L);
        when(idService.nextLong(IdScope.COUPON)).thenReturn(couponId);
        when(grantLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId))
                .thenReturn(2); // 已领取2张，还可以领取
        when(templateRepository.incrementIssuedCount(templateId, 1)).thenReturn(true);
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Coupon result = grantService.grantCoupon(
                tenantId, templateId, userId, idempotencyKey,
                GrantSource.MANUAL_ADMIN, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(couponId, result.getId());
        assertEquals(userId, result.getUserId());
        verify(metrics).recordSuccess();
        verify(templateRepository).incrementIssuedCount(templateId, 1);
        verify(couponRepository).save(any());
        verify(grantLogRepository).update(argThat(log -> 
                log.getGrantStatus().name().equals("SUCCESS") && 
                log.getCouponId().equals(couponId)
        ));
    }

    @Test
    void testQuota_WhenNoLimit_ShouldGrantSuccessfully() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";
        Long couponId = 5000L;

        // 不限量模板
        CouponTemplate template = createTemplate(templateId, null, null, 0, 0);

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.empty());
        when(idService.nextLong(IdScope.COUPON_GRANT_LOG)).thenReturn(1L);
        when(idService.nextLong(IdScope.COUPON)).thenReturn(couponId);
        when(grantLogRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
        when(grantLogRepository.countUserGrantedByTemplate(tenantId, templateId, userId))
                .thenReturn(100); // 即使已领取很多，也可以继续领取
        when(templateRepository.incrementIssuedCount(templateId, 1)).thenReturn(true);
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        Coupon result = grantService.grantCoupon(
                tenantId, templateId, userId, idempotencyKey,
                GrantSource.MANUAL_ADMIN, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(couponId, result.getId());
        verify(metrics).recordSuccess();
    }

    private CouponTemplate createTemplate(Long id, Integer totalQuantity, Integer perUserLimit,
                                         Integer issuedCount, Integer version) {
        return CouponTemplate.builder()
                .id(id)
                .tenantId(1L)
                .templateCode("TEST-TEMPLATE")
                .templateName("测试模板")
                .couponType(CouponType.DISCOUNT_AMOUNT)
                .discountAmount(BigDecimal.valueOf(10))
                .minOrderAmount(BigDecimal.ZERO)
                .applicableScope(ApplicableScope.ALL)
                .validDays(30)
                .totalQuantity(totalQuantity)
                .perUserLimit(perUserLimit)
                .issuedCount(issuedCount)
                .version(version)
                .status("ONLINE")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
