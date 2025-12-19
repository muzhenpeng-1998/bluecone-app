package com.bluecone.app.promo.domain.service;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.GrantSource;
import com.bluecone.app.promo.api.enums.GrantStatus;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券发放服务幂等性测试
 */
@ExtendWith(MockitoExtension.class)
class CouponGrantServiceIdempotencyTest {

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
    void testIdempotency_WhenGrantLogExists_ShouldReturnExistingCoupon() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";
        Long existingCouponId = 5000L;

        // 模拟已存在的成功发放日志
        CouponGrantLog existingLog = CouponGrantLog.builder()
                .id(1L)
                .tenantId(tenantId)
                .templateId(templateId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .couponId(existingCouponId)
                .grantStatus(GrantStatus.SUCCESS)
                .build();

        Coupon existingCoupon = Coupon.builder()
                .id(existingCouponId)
                .userId(userId)
                .status(CouponStatus.ISSUED)
                .build();

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.of(existingLog));
        when(couponRepository.findById(existingCouponId))
                .thenReturn(Optional.of(existingCoupon));

        // When
        Coupon result = grantService.grantCoupon(
                tenantId, templateId, userId, idempotencyKey,
                GrantSource.MANUAL_ADMIN, null, null, null
        );

        // Then
        assertNotNull(result);
        assertEquals(existingCouponId, result.getId());
        verify(metrics).recordIdempotentReplay(idempotencyKey);
        verify(grantLogRepository, never()).save(any());
        verify(templateRepository, never()).incrementIssuedCount(anyLong(), anyInt());
    }

    @Test
    void testIdempotency_WhenGrantLogFailed_ShouldThrowException() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";

        // 模拟已存在的失败发放日志
        CouponGrantLog failedLog = CouponGrantLog.builder()
                .id(1L)
                .tenantId(tenantId)
                .templateId(templateId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .grantStatus(GrantStatus.FAILED)
                .errorCode("QUOTA_EXCEEDED")
                .errorMessage("配额已用完")
                .build();

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.of(failedLog));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            grantService.grantCoupon(
                    tenantId, templateId, userId, idempotencyKey,
                    GrantSource.MANUAL_ADMIN, null, null, null
            );
        });

        assertEquals("QUOTA_EXCEEDED", exception.getCode());
        assertEquals("配额已用完", exception.getMessage());
    }

    @Test
    void testIdempotency_WhenGrantLogProcessing_ShouldThrowException() {
        // Given
        Long tenantId = 1L;
        Long templateId = 100L;
        Long userId = 1000L;
        String idempotencyKey = "test-idem-key";

        // 模拟处理中的发放日志（可能是并发请求）
        CouponGrantLog processingLog = CouponGrantLog.builder()
                .id(1L)
                .tenantId(tenantId)
                .templateId(templateId)
                .userId(userId)
                .idempotencyKey(idempotencyKey)
                .grantStatus(GrantStatus.PROCESSING)
                .build();

        when(grantLogRepository.findByIdempotencyKey(tenantId, idempotencyKey))
                .thenReturn(Optional.of(processingLog));

        // When & Then
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            grantService.grantCoupon(
                    tenantId, templateId, userId, idempotencyKey,
                    GrantSource.MANUAL_ADMIN, null, null, null
            );
        });

        assertEquals("GRANT_IN_PROGRESS", exception.getCode());
    }
}
