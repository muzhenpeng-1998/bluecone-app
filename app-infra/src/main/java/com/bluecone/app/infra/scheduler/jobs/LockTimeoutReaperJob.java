package com.bluecone.app.infra.scheduler.jobs;

import com.bluecone.app.infra.scheduler.annotation.BlueconeJob;
import com.bluecone.app.infra.scheduler.core.JobContext;
import com.bluecone.app.infra.scheduler.core.JobHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 锁定超时清理任务
 * 释放超时的优惠券锁定、钱包冻结、积分冻结
 * 
 * 补偿策略：
 * 1. 扫描超过 TTL 的锁定记录（默认 30 分钟）
 * 2. 将状态从 LOCKED/FROZEN 更新为 RELEASED/AVAILABLE
 * 3. 记录释放日志，便于审计
 * 
 * 注意：
 * - 该任务是兜底机制，正常情况下应该由订单取消事件触发释放
 * - 超时释放后，如果订单后续支付成功，会因为锁定不存在而失败（符合预期）
 */
@Slf4j
@Component
@BlueconeJob(
        code = "lock_timeout_reaper",
        name = "Lock Timeout Reaper",
        cron = "0 */5 * * * ?",  // 每 5 分钟执行一次
        timeoutSeconds = 60
)
@RequiredArgsConstructor
public class LockTimeoutReaperJob implements JobHandler {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 锁定超时时间（分钟）
     * 可通过配置文件覆盖，默认 30 分钟
     */
    @Value("${bluecone.asset.lock-timeout-minutes:30}")
    private int lockTimeoutMinutes;
    
    @Override
    public void handle(JobContext context) {
        String traceId = context.getTraceId();
        log.info("[LockTimeoutReaper] Starting lock timeout cleanup, traceId={}, timeoutMinutes={}", 
                traceId, lockTimeoutMinutes);
        
        LocalDateTime timeoutThreshold = LocalDateTime.now().minusMinutes(lockTimeoutMinutes);
        
        try {
            // 1. 释放超时的优惠券锁定
            int couponReleased = releaseCouponLocks(timeoutThreshold);
            
            // 2. 释放超时的钱包冻结
            int walletReleased = releaseWalletFreezes(timeoutThreshold);
            
            // 3. 释放超时的积分冻结
            int pointsReleased = releasePointsFreezes(timeoutThreshold);
            
            log.info("[LockTimeoutReaper] Lock timeout cleanup completed: " +
                    "coupon={}, wallet={}, points={}, traceId={}", 
                    couponReleased, walletReleased, pointsReleased, traceId);
            
        } catch (Exception e) {
            log.error("[LockTimeoutReaper] Lock timeout cleanup failed: traceId={}", traceId, e);
        }
    }
    
    /**
     * 释放超时的优惠券锁定
     * 
     * @param timeoutThreshold 超时阈值时间
     * @return 释放数量
     */
    private int releaseCouponLocks(LocalDateTime timeoutThreshold) {
        try {
            String sql = "UPDATE bc_coupon_lock " +
                    "SET status = 'RELEASED', " +
                    "    updated_at = NOW() " +
                    "WHERE status = 'LOCKED' " +
                    "AND created_at < ? " +
                    "AND deleted = 0";
            
            int count = jdbcTemplate.update(sql, timeoutThreshold);
            
            if (count > 0) {
                log.warn("[LockTimeoutReaper] Released {} timeout coupon locks, threshold={}", 
                        count, timeoutThreshold);
            }
            
            return count;
            
        } catch (Exception e) {
            log.error("[LockTimeoutReaper] Failed to release coupon locks", e);
            return 0;
        }
    }
    
    /**
     * 释放超时的钱包冻结
     * 
     * @param timeoutThreshold 超时阈值时间
     * @return 释放数量
     */
    private int releaseWalletFreezes(LocalDateTime timeoutThreshold) {
        try {
            // 1. 查询超时的冻结记录
            String selectSql = "SELECT id, user_id, freeze_amount " +
                    "FROM bc_wallet_freeze " +
                    "WHERE status = 'FROZEN' " +
                    "AND created_at < ? " +
                    "AND deleted = 0";
            
            var freezeRecords = jdbcTemplate.queryForList(selectSql, timeoutThreshold);
            
            if (freezeRecords.isEmpty()) {
                return 0;
            }
            
            // 2. 逐个释放（需要更新账户余额）
            int count = 0;
            for (var record : freezeRecords) {
                Long freezeId = ((Number) record.get("id")).longValue();
                Long userId = ((Number) record.get("user_id")).longValue();
                java.math.BigDecimal freezeAmount = (java.math.BigDecimal) record.get("freeze_amount");
                
                try {
                    // 更新账户：减少冻结金额，增加可用金额
                    String updateAccountSql = "UPDATE bc_wallet_account " +
                            "SET frozen_balance = frozen_balance - ?, " +
                            "    available_balance = available_balance + ?, " +
                            "    updated_at = NOW(), " +
                            "    version = version + 1 " +
                            "WHERE user_id = ? " +
                            "AND frozen_balance >= ? " +
                            "AND deleted = 0";
                    
                    int updated = jdbcTemplate.update(updateAccountSql, 
                            freezeAmount, freezeAmount, userId, freezeAmount);
                    
                    if (updated > 0) {
                        // 标记冻结记录为已释放
                        String updateFreezeSql = "UPDATE bc_wallet_freeze " +
                                "SET status = 'RELEASED', " +
                                "    updated_at = NOW() " +
                                "WHERE id = ?";
                        
                        jdbcTemplate.update(updateFreezeSql, freezeId);
                        count++;
                    }
                    
                } catch (Exception e) {
                    log.error("[LockTimeoutReaper] Failed to release wallet freeze: freezeId={}, userId={}", 
                            freezeId, userId, e);
                }
            }
            
            if (count > 0) {
                log.warn("[LockTimeoutReaper] Released {} timeout wallet freezes, threshold={}", 
                        count, timeoutThreshold);
            }
            
            return count;
            
        } catch (Exception e) {
            log.error("[LockTimeoutReaper] Failed to release wallet freezes", e);
            return 0;
        }
    }
    
    /**
     * 释放超时的积分冻结
     * 
     * @param timeoutThreshold 超时阈值时间
     * @return 释放数量
     */
    private int releasePointsFreezes(LocalDateTime timeoutThreshold) {
        try {
            // 1. 查询超时的冻结记录
            String selectSql = "SELECT id, user_id, frozen_points " +
                    "FROM bc_points_freeze " +
                    "WHERE status = 'FROZEN' " +
                    "AND created_at < ? " +
                    "AND deleted = 0";
            
            var freezeRecords = jdbcTemplate.queryForList(selectSql, timeoutThreshold);
            
            if (freezeRecords.isEmpty()) {
                return 0;
            }
            
            // 2. 逐个释放（需要更新账户积分）
            int count = 0;
            for (var record : freezeRecords) {
                Long freezeId = ((Number) record.get("id")).longValue();
                Long userId = ((Number) record.get("user_id")).longValue();
                Integer frozenPoints = ((Number) record.get("frozen_points")).intValue();
                
                try {
                    // 更新账户：减少冻结积分，增加可用积分
                    String updateAccountSql = "UPDATE bc_points_account " +
                            "SET frozen_points = frozen_points - ?, " +
                            "    available_points = available_points + ?, " +
                            "    updated_at = NOW(), " +
                            "    version = version + 1 " +
                            "WHERE user_id = ? " +
                            "AND frozen_points >= ? " +
                            "AND deleted = 0";
                    
                    int updated = jdbcTemplate.update(updateAccountSql, 
                            frozenPoints, frozenPoints, userId, frozenPoints);
                    
                    if (updated > 0) {
                        // 标记冻结记录为已释放
                        String updateFreezeSql = "UPDATE bc_points_freeze " +
                                "SET status = 'RELEASED', " +
                                "    updated_at = NOW() " +
                                "WHERE id = ?";
                        
                        jdbcTemplate.update(updateFreezeSql, freezeId);
                        count++;
                    }
                    
                } catch (Exception e) {
                    log.error("[LockTimeoutReaper] Failed to release points freeze: freezeId={}, userId={}", 
                            freezeId, userId, e);
                }
            }
            
            if (count > 0) {
                log.warn("[LockTimeoutReaper] Released {} timeout points freezes, threshold={}", 
                        count, timeoutThreshold);
            }
            
            return count;
            
        } catch (Exception e) {
            log.error("[LockTimeoutReaper] Failed to release points freezes", e);
            return 0;
        }
    }
}
