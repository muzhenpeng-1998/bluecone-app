package com.bluecone.app.promo.application;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.enums.CouponStatus;
import com.bluecone.app.promo.api.enums.CouponType;
import com.bluecone.app.promo.domain.model.Coupon;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * 优惠券锁定门面并发测试
 * 
 * <p>测试场景：
 * <ul>
 *   <li>同一优惠券并发锁定，只有一个请求能成功</li>
 *   <li>不同订单使用不同幂等键，互不影响</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("优惠券锁定门面并发测试")
class CouponLockFacadeConcurrencyTest {

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
    private static final BigDecimal ORDER_AMOUNT = new BigDecimal("100.00");
    private static final BigDecimal DISCOUNT_AMOUNT = new BigDecimal("10.00");

    private Coupon validCoupon;

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
    }

    @Test
    @DisplayName("并发锁券测试：同一优惠券并发锁定，只有一个请求能成功")
    void testConcurrentLockSameCoupon() throws Exception {
        // Given
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 模拟：第一次更新成功，后续更新失败（并发控制）
        when(couponRepository.findById(TENANT_ID, COUPON_ID))
                .thenReturn(validCoupon);
        
        when(couponLockRepository.findByIdempotencyKey(anyString()))
                .thenReturn(null);  // 没有已存在的锁定记录
        
        when(idService.nextLong(IdScope.COUPON_LOCK))
                .thenAnswer(invocation -> System.nanoTime());  // 生成唯一ID
        
        // 模拟并发场景：只有第一次updateStatus成功
        AtomicInteger updateCallCount = new AtomicInteger(0);
        when(couponRepository.updateStatus(eq(COUPON_ID), eq(CouponStatus.ISSUED), 
                eq(CouponStatus.LOCKED), any(), any(), any()))
                .thenAnswer(invocation -> {
                    int callNumber = updateCallCount.incrementAndGet();
                    return callNumber == 1 ? 1 : 0;  // 只有第一次成功
                });

        // When - 并发执行
        for (int i = 0; i < threadCount; i++) {
            final long orderId = 6000L + i;
            final String idempotencyKey = String.format("ORDER:%d:COUPON:%d", orderId, COUPON_ID);
            
            executor.submit(() -> {
                try {
                    startLatch.await();  // 等待统一开始
                    
                    CouponLockCommand command = buildLockCommand(orderId, idempotencyKey);
                    CouponLockResult result = couponLockFacade.lock(command);
                    
                    if (result != null && result.getSuccess()) {
                        successCount.incrementAndGet();
                    } else {
                        failureCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 统一开始
        startLatch.countDown();
        
        // 等待所有线程完成
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(1);  // 只有一个请求成功
        assertThat(failureCount.get()).isEqualTo(threadCount - 1);  // 其他请求失败
        assertThat(successCount.get() + failureCount.get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("并发锁券测试：不同订单使用不同券，应互不影响")
    void testConcurrentLockDifferentCoupons() throws Exception {
        // Given
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        // 模拟每个线程使用不同的券
        when(couponRepository.findById(anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Long couponId = invocation.getArgument(1);
                    return Coupon.builder()
                            .id(couponId)
                            .tenantId(TENANT_ID)
                            .userId(USER_ID)
                            .couponType(CouponType.DISCOUNT_AMOUNT)
                            .discountAmount(DISCOUNT_AMOUNT)
                            .minOrderAmount(new BigDecimal("50.00"))
                            .status(CouponStatus.ISSUED)
                            .validStartTime(LocalDateTime.now().minusDays(1))
                            .validEndTime(LocalDateTime.now().plusDays(30))
                            .build();
                });
        
        when(couponLockRepository.findByIdempotencyKey(anyString()))
                .thenReturn(null);
        
        when(idService.nextLong(IdScope.COUPON_LOCK))
                .thenAnswer(invocation -> System.nanoTime());
        
        // 所有更新都成功（因为是不同的券）
        when(couponRepository.updateStatus(anyLong(), eq(CouponStatus.ISSUED), 
                eq(CouponStatus.LOCKED), any(), any(), any()))
                .thenReturn(1);

        // When - 并发执行（每个线程使用不同的券）
        for (int i = 0; i < threadCount; i++) {
            final long orderId = 6000L + i;
            final long couponId = 5000L + i;  // 不同的券ID
            final String idempotencyKey = String.format("ORDER:%d:COUPON:%d", orderId, couponId);
            
            executor.submit(() -> {
                try {
                    startLatch.await();
                    
                    CouponLockCommand command = new CouponLockCommand();
                    command.setTenantId(TENANT_ID);
                    command.setUserId(USER_ID);
                    command.setCouponId(couponId);
                    command.setOrderId(orderId);
                    command.setOrderAmount(ORDER_AMOUNT);
                    command.setIdempotencyKey(idempotencyKey);
                    command.setLockExpireMinutes(30);
                    
                    CouponLockResult result = couponLockFacade.lock(command);
                    
                    if (result != null && result.getSuccess()) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // 忽略异常
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 统一开始
        startLatch.countDown();
        
        // 等待所有线程完成
        boolean finished = endLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Then - 所有请求都应该成功（因为是不同的券）
        assertThat(finished).isTrue();
        assertThat(successCount.get()).isEqualTo(threadCount);
    }

    // ==================== Helper Methods ====================

    private CouponLockCommand buildLockCommand(Long orderId, String idempotencyKey) {
        CouponLockCommand command = new CouponLockCommand();
        command.setTenantId(TENANT_ID);
        command.setUserId(USER_ID);
        command.setCouponId(COUPON_ID);
        command.setOrderId(orderId);
        command.setOrderAmount(ORDER_AMOUNT);
        command.setIdempotencyKey(idempotencyKey);
        command.setLockExpireMinutes(30);
        return command;
    }
}
