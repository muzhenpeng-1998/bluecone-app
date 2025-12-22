package com.bluecone.app.api.admin.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.order.infra.persistence.po.OrderPO;
import com.bluecone.app.order.infra.persistence.mapper.OrderMapper;
import com.bluecone.app.promo.infra.persistence.po.CouponRedemptionPO;
import com.bluecone.app.promo.infra.persistence.po.CouponGrantLogPO;
import com.bluecone.app.promo.infra.persistence.mapper.CouponRedemptionMapper;
import com.bluecone.app.promo.infra.persistence.mapper.CouponGrantLogMapper;
import com.bluecone.app.security.admin.RequireAdminPermission;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletLedgerMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 仪表盘后台接口
 * 
 * 提供经营数据概览：
 * - 今日订单数/GMV
 * - 优惠券核销统计
 * - 余额支付统计
 * - 其他关键KPI
 * 
 * 权限要求：
 * - 查看：dashboard:view
 */
@Tag(name = "🎛️ 平台管理后台 > 系统管理 > 仪表盘", description = "平台后台仪表盘接口（数据统计）")
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class DashboardAdminController {
    
    private final OrderMapper orderMapper;
    private final CouponRedemptionMapper couponRedemptionMapper;
    private final CouponGrantLogMapper couponGrantLogMapper;
    private final WalletLedgerMapper ledgerMapper;
    
    /**
     * 查询经营数据概览
     * 
     * @param tenantId 租户ID
     * @param date 日期（可选，默认今天）
     * @return 经营数据概览
     */
    @GetMapping("/summary")
    @RequireAdminPermission("dashboard:view")
    public DashboardSummary getSummary(@RequestHeader("X-Tenant-Id") Long tenantId,
                                      @RequestParam(required = false) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        log.info("查询经营数据概览: tenantId={}, date={}", tenantId, date);
        
        LocalDateTime startTime = date.atStartOfDay();
        LocalDateTime endTime = date.atTime(LocalTime.MAX);
        
        DashboardSummary summary = new DashboardSummary();
        summary.setDate(date);
        
        // 1. 订单统计
        OrderStats orderStats = calculateOrderStats(tenantId, startTime, endTime);
        summary.setOrderStats(orderStats);
        
        // 2. 优惠券统计
        CouponStats couponStats = calculateCouponStats(tenantId, startTime, endTime);
        summary.setCouponStats(couponStats);
        
        // 3. 钱包统计
        WalletStats walletStats = calculateWalletStats(tenantId, startTime, endTime);
        summary.setWalletStats(walletStats);
        
        return summary;
    }
    
    /**
     * 计算订单统计
     */
    private OrderStats calculateOrderStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        OrderStats stats = new OrderStats();
        
        // 查询今日订单
        List<OrderPO> todayOrders = orderMapper.selectList(new LambdaQueryWrapper<OrderPO>()
                .eq(OrderPO::getTenantId, tenantId)
                .ge(OrderPO::getCreatedAt, startTime)
                .le(OrderPO::getCreatedAt, endTime));
        
        stats.setTotalCount(todayOrders.size());
        
        // 计算GMV（已支付订单的应付金额总和）
        BigDecimal gmv = todayOrders.stream()
                .filter(order -> "PAID".equals(order.getPayStatus()))
                .map(OrderPO::getPayableAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setGmv(gmv);
        
        // 按状态统计
        long paidCount = todayOrders.stream()
                .filter(order -> "PAID".equals(order.getPayStatus()))
                .count();
        stats.setPaidCount((int) paidCount);
        
        long canceledCount = todayOrders.stream()
                .filter(order -> "CANCELED".equals(order.getStatus()))
                .count();
        stats.setCanceledCount((int) canceledCount);
        
        // 计算平均订单金额
        if (paidCount > 0) {
            stats.setAvgOrderAmount(gmv.divide(BigDecimal.valueOf(paidCount), 2, BigDecimal.ROUND_HALF_UP));
        } else {
            stats.setAvgOrderAmount(BigDecimal.ZERO);
        }
        
        return stats;
    }
    
    /**
     * 计算优惠券统计
     */
    private CouponStats calculateCouponStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        CouponStats stats = new CouponStats();
        
        // 查询今日核销的优惠券
        List<CouponRedemptionPO> usedCoupons = couponRedemptionMapper.selectList(
                new LambdaQueryWrapper<CouponRedemptionPO>()
                        .eq(CouponRedemptionPO::getTenantId, tenantId)
                        .ge(CouponRedemptionPO::getCreatedAt, startTime)
                        .le(CouponRedemptionPO::getCreatedAt, endTime));
        
        stats.setUsedCount(usedCoupons.size());
        
        // 计算优惠金额总和
        BigDecimal totalDiscount = usedCoupons.stream()
                .map(CouponRedemptionPO::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setTotalDiscountAmount(totalDiscount);
        
        // 查询今日发放的优惠券（成功发放）
        List<CouponGrantLogPO> grantedCoupons = couponGrantLogMapper.selectList(
                new LambdaQueryWrapper<CouponGrantLogPO>()
                        .eq(CouponGrantLogPO::getTenantId, tenantId)
                        .eq(CouponGrantLogPO::getGrantStatus, "SUCCESS")
                        .ge(CouponGrantLogPO::getCreatedAt, startTime)
                        .le(CouponGrantLogPO::getCreatedAt, endTime));
        stats.setGrantedCount(grantedCoupons.size());
        
        return stats;
    }
    
    /**
     * 计算钱包统计
     */
    private WalletStats calculateWalletStats(Long tenantId, LocalDateTime startTime, LocalDateTime endTime) {
        WalletStats stats = new WalletStats();
        
        // 查询今日充值流水
        List<WalletLedgerPO> rechargeLedgers = ledgerMapper.selectList(
                new LambdaQueryWrapper<WalletLedgerPO>()
                        .eq(WalletLedgerPO::getTenantId, tenantId)
                        .eq(WalletLedgerPO::getBizType, "RECHARGE")
                        .ge(WalletLedgerPO::getCreatedAt, startTime)
                        .le(WalletLedgerPO::getCreatedAt, endTime));
        
        BigDecimal rechargeAmount = rechargeLedgers.stream()
                .map(WalletLedgerPO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setRechargeAmount(rechargeAmount);
        stats.setRechargeCount(rechargeLedgers.size());
        
        // 查询今日消费流水
        List<WalletLedgerPO> consumeLedgers = ledgerMapper.selectList(
                new LambdaQueryWrapper<WalletLedgerPO>()
                        .eq(WalletLedgerPO::getTenantId, tenantId)
                        .eq(WalletLedgerPO::getBizType, "ORDER_PAY")
                        .ge(WalletLedgerPO::getCreatedAt, startTime)
                        .le(WalletLedgerPO::getCreatedAt, endTime));
        
        BigDecimal consumeAmount = consumeLedgers.stream()
                .map(WalletLedgerPO::getAmount)
                .map(BigDecimal::abs) // 消费金额为负数，取绝对值
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.setConsumeAmount(consumeAmount);
        stats.setConsumeCount(consumeLedgers.size());
        
        return stats;
    }
    
    // ===== DTO类 =====
    
    @Data
    public static class DashboardSummary {
        private LocalDate date;
        private OrderStats orderStats;
        private CouponStats couponStats;
        private WalletStats walletStats;
    }
    
    @Data
    public static class OrderStats {
        private Integer totalCount;      // 总订单数
        private Integer paidCount;       // 已支付订单数
        private Integer canceledCount;   // 已取消订单数
        private BigDecimal gmv;          // GMV（已支付订单金额总和）
        private BigDecimal avgOrderAmount; // 平均订单金额
    }
    
    @Data
    public static class CouponStats {
        private Integer grantedCount;    // 发放数量
        private Integer usedCount;       // 核销数量
        private BigDecimal totalDiscountAmount; // 优惠金额总和
    }
    
    @Data
    public static class WalletStats {
        private Integer rechargeCount;   // 充值笔数
        private BigDecimal rechargeAmount; // 充值金额
        private Integer consumeCount;    // 消费笔数
        private BigDecimal consumeAmount;  // 消费金额
    }
}
