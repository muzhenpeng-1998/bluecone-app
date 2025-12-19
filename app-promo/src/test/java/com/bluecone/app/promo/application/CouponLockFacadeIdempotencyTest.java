package com.bluecone.app.promo.application;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.dto.CouponCommitCommand;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.dto.CouponReleaseCommand;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.api.enums.LockStatus;
import com.bluecone.app.promo.domain.model.Coupon;
import com.bluecone.app.promo.domain.model.CouponLock;
import com.bluecone.app.promo.domain.model.CouponRedemption;
import com.bluecone.app.promo.domain.repository.CouponLockRepository;
import com.bluecone.app.promo.domain.repository.CouponRedemptionRepository;
import com.bluecone.app.promo.domain.repository.CouponRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 优惠券锁定门面幂等性测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("优惠券锁定门面幂等性测试")
class CouponLockFacadeIdempotencyTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponLockRepository couponLockRepository;

    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;

    @Mock
    private IdService idService;

    @InjectMocks
    private CouponLockFacadeImpl couponLockFacade;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 1001L;
    private static final Long COUPON_ID = 5001L;
    private static final Long ORDER_ID = 6001L;
    private static final String IDEMPOTENCY_KEY = "ORDER:6001:COUPON:5001";
    private static final BigDecimal ORDER_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal DISCOUNT_AMOUNT = new BigDecimal("10.00");

    private Coupon validCoupon;
    private CouponLock existingLock;

    @BeforeEach
    void setUp() {
        // 准备有效的优惠券
        validCoupon = Coupon.builder()
                .id(COUPON_ID)
                .tenantId(TENANT_ID)
                .userId(USER_ID)
                .couponType(CouponType.DISCOUNT_AMOUNT)
                .discountAmount(DISCOUNT_AMOUNT)
                .minOrderAmount(new BigDecimal("50.00"))
                .status(CouponStatus.ISSUED)
                .validStartTime(LocalDateTime.now().minusDays(1))
                .validEndTime(LocalDateTime.now().plusDays(30))
                .build();

        // 准备已存在的锁定记录
        existingLock = CouponLock.builder()
                .id(7001L)
                .tenantId(TENANT_ID)
                .couponId(COUPON_ID)
                .userId(USER_ID)
                .orderId(ORDER_ID)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .lockStatus(LockStatus.LOCKED)
                .lockTime(LocalDateTime.now())
                .expireTime(LocalDateTime.now().plusMinutes(30))
                .build();
    }

    @Test
    @DisplayName("lock - 幂等测试：相同幂等键重复调用应返回相同结果")
    void testLockIdempotency() {
        // Given
        CouponLockCommand command = buildLockCommand();
        
        // 模拟已存在锁定记录
        when(couponLockRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(existingLock);

        // When - 第一次调用
        CouponLockResult result1 = couponLockFacade.lock(command);

        // Then - 应返回已有结果
        assertThat(result1).isNotNull();
        assertThat(result1.getSuccess()).isTrue();
        assertThat(result1.getLockId()).isEqualTo(existingLock.getId());
        assertThat(result1.getMessage()).contains("幂等");

        // When - 第二次调用（相同幂等键）
        CouponLockResult result2 = couponLockFacade.lock(command);

        // Then - 应返回相同结果
        assertThat(result2).isNotNull();
        assertThat(result2.getSuccess()).isTrue();
        assertThat(result2.getLockId()).isEqualTo(result1.getLockId());

        // 验证只查询了一次，没有重复插入
        verify(couponLockRepository, times(2)).findByIdempotencyKey(IDEMPOTENCY_KEY);
        verify(couponLockRepository, never()).save(any());
        verify(couponRepository, never()).updateStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("lock - 首次锁定成功后，重复调用应返回幂等结果")
    void testLockFirstTimeThenIdempotent() {
        // Given
        CouponLockCommand command = buildLockCommand();
        
        // 第一次调用时不存在锁定记录
        when(couponLockRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(null)  // 第一次
                .thenReturn(existingLock);  // 第二次

        when(couponRepository.findById(TENANT_ID, COUPON_ID))
                .thenReturn(validCoupon);
        
        when(idService.nextLong(IdScope.COUPON_LOCK))
                .thenReturn(7001L);
        
        when(couponRepository.updateStatus(eq(COUPON_ID), eq(CouponStatus.ISSUED), 
                eq(CouponStatus.LOCKED), any(), any(), any()))
                .thenReturn(1);

        // When - 第一次锁定
        CouponLockResult result1 = couponLockFacade.lock(command);

        // Then
        assertThat(result1.getSuccess()).isTrue();
        verify(couponLockRepository, times(1)).save(any());

        // When - 第二次调用（幂等）
        CouponLockResult result2 = couponLockFacade.lock(command);

        // Then
        assertThat(result2.getSuccess()).isTrue();
        assertThat(result2.getLockId()).isEqualTo(result1.getLockId());
        // 第二次不应再插入
        verify(couponLockRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("release - 幂等测试：重复释放应不报错")
    void testReleaseIdempotency() {
        // Given
        CouponReleaseCommand command = buildReleaseCommand();
        
        // 模拟已释放的锁定记录
        CouponLock releasedLock = CouponLock.builder()
                .id(7001L)
                .lockStatus(LockStatus.RELEASED)
                .build();
        
        when(couponLockRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(releasedLock);

        // When - 重复释放
        couponLockFacade.release(command);
        couponLockFacade.release(command);

        // Then - 不应抛异常，且不应更新状态
        verify(couponLockRepository, times(2)).findByIdempotencyKey(IDEMPOTENCY_KEY);
        verify(couponLockRepository, never()).update(any());
        verify(couponRepository, never()).updateStatus(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("release - 锁定记录不存在时，应幂等返回")
    void testReleaseWhenLockNotExists() {
        // Given
        CouponReleaseCommand command = buildReleaseCommand();
        
        when(couponLockRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(null);

        // When
        couponLockFacade.release(command);

        // Then - 不应抛异常
        verify(couponLockRepository, times(1)).findByIdempotencyKey(IDEMPOTENCY_KEY);
        verify(couponLockRepository, never()).update(any());
    }

    @Test
    @DisplayName("commit - 幂等测试：相同幂等键重复调用应不重复核销")
    void testCommitIdempotency() {
        // Given
        CouponCommitCommand command = buildCommitCommand();
        
        // 模拟已存在核销记录
        CouponRedemption existingRedemption = CouponRedemption.builder()
                .id(8001L)
                .couponId(COUPON_ID)
                .orderId(ORDER_ID)
                .idempotencyKey(IDEMPOTENCY_KEY)
                .build();
        
        when(couponRedemptionRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(existingRedemption);

        // When - 重复核销
        couponLockFacade.commit(command);
        couponLockFacade.commit(command);

        // Then - 不应重复插入
        verify(couponRedemptionRepository, times(2)).findByIdempotencyKey(IDEMPOTENCY_KEY);
        verify(couponRedemptionRepository, never()).save(any());
        verify(couponLockRepository, never()).update(any());
    }

    @Test
    @DisplayName("commit - 首次核销成功后，重复调用应返回幂等结果")
    void testCommitFirstTimeThenIdempotent() {
        // Given
        CouponCommitCommand command = buildCommitCommand();
        
        // 第一次调用时不存在核销记录
        when(couponRedemptionRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(null)  // 第一次
                .thenReturn(CouponRedemption.builder().id(8001L).build());  // 第二次
        
        when(couponLockRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(existingLock);
        
        when(couponRepository.findById(TENANT_ID, COUPON_ID))
                .thenReturn(validCoupon);
        
        when(idService.nextLong(IdScope.COUPON_REDEMPTION))
                .thenReturn(8001L);
        
        when(couponRepository.updateStatus(eq(COUPON_ID), eq(CouponStatus.LOCKED), 
                eq(CouponStatus.USED), any(), any(), any()))
                .thenReturn(1);

        // When - 第一次核销
        couponLockFacade.commit(command);

        // Then
        verify(couponRedemptionRepository, times(1)).save(any());

        // When - 第二次调用（幂等）
        couponLockFacade.commit(command);

        // Then - 不应再插入
        verify(couponRedemptionRepository, times(1)).save(any());
    }

    // ==================== Helper Methods ====================

    private CouponLockCommand buildLockCommand() {
        CouponLockCommand command = new CouponLockCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(USER_ID);
        command.setCouponId(COUPON_ID);
        command.setOrderId(ORDER_ID);
        command.setOrderAmount(ORDER_AMOUNT);
        command.setIdempotencyKey(IDEMPOTENCY_KEY);
        command.setLockExpireMinutes(30);
        return command;
    }

    private CouponReleaseCommand buildReleaseCommand() {
        CouponReleaseCommand command = new CouponReleaseCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(USER_ID);
        command.setCouponId(COUPON_ID);
        command.setOrderId(ORDER_ID);
        command.setIdempotencyKey(IDEMPOTENCY_KEY);
        return command;
    }

    private CouponCommitCommand buildCommitCommand() {
        CouponCommitCommand command = new CouponCommitCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(USER_ID);
        command.setCouponId(COUPON_ID);
        command.setOrderId(ORDER_ID);
        command.setOriginalAmount(ORDER_AMOUNT);
        command.setDiscountAmount(DISCOUNT_AMOUNT);
        command.setFinalAmount(ORDER_AMOUNT.subtract(DISCOUNT_AMOUNT));
        command.setIdempotencyKey(IDEMPOTENCY_KEY);
        return command;
    }
}
