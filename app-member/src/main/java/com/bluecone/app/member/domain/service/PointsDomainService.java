package com.bluecone.app.member.domain.service;

import com.bluecone.app.member.domain.enums.PointsDirection;
import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.domain.model.PointsLedger;
import com.bluecone.app.member.domain.repository.PointsAccountRepository;
import com.bluecone.app.member.domain.repository.PointsLedgerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 积分领域服务
 * 负责积分账户的核心业务逻辑，保证账户变更与流水记录的一致性
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service("pointsLedgerDomainService")
public class PointsDomainService {
    
    private static final Logger log = LoggerFactory.getLogger(PointsDomainService.class);
    
    private final PointsAccountRepository accountRepository;
    private final PointsLedgerRepository ledgerRepository;
    
    // 最大重试次数（乐观锁冲突时）
    private static final int MAX_RETRY_TIMES = 3;
    
    public PointsDomainService(PointsAccountRepository accountRepository,
                               PointsLedgerRepository ledgerRepository) {
        this.accountRepository = accountRepository;
        this.ledgerRepository = ledgerRepository;
    }
    
    /**
     * 赚取积分（增加可用积分）
     * 例如：订单完成、签到奖励
     * 
     * @param ledgerId 流水ID
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录，如果是重复请求则返回已有流水
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsLedger earnPoints(Long ledgerId, Long tenantId, Long memberId, Long points,
                                   String bizType, String bizId, String idempotencyKey, String remark) {
        // 1. 幂等性检查
        Optional<PointsLedger> existing = ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("积分赚取操作已处理过，幂等键：{}", idempotencyKey);
            return existing.get();
        }
        
        // 2. 查询积分账户
        PointsAccount account = accountRepository.findByMemberId(tenantId, memberId)
                .orElseThrow(() -> new IllegalStateException("积分账户不存在，会员ID：" + memberId));
        
        // 3. 记录变动前余额
        Long beforeAvailable = account.getAvailablePoints();
        Long beforeFrozen = account.getFrozenPoints();
        
        // 4. 增加可用积分
        account.increaseAvailable(points);
        
        // 5. 创建流水记录
        PointsLedger ledger = PointsLedger.create(
                ledgerId, tenantId, memberId,
                PointsDirection.EARN, points,
                beforeAvailable, beforeFrozen,
                account.getAvailablePoints(), account.getFrozenPoints(),
                bizType, bizId, idempotencyKey, remark
        );
        ledger.validate();
        
        // 6. 保存流水（幂等键唯一约束）
        boolean saved = ledgerRepository.save(ledger);
        if (!saved) {
            // 并发情况下幂等键冲突，查询已有流水返回
            return ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("流水保存失败"));
        }
        
        // 7. 更新账户（乐观锁）
        boolean updated = updateAccountWithRetry(account);
        if (!updated) {
            throw new IllegalStateException("积分账户更新失败（乐观锁冲突）");
        }
        
        log.info("积分赚取成功，会员ID：{}，积分：{}，幂等键：{}", memberId, points, idempotencyKey);
        return ledger;
    }
    
    /**
     * 回退积分（退款返还）
     * 按照原流水做反向操作，恢复积分
     * 
     * @param ledgerId 流水ID
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsLedger revertPoints(Long ledgerId, Long tenantId, Long memberId, Long points,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        // 1. 幂等性检查
        Optional<PointsLedger> existing = ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("积分回退操作已处理过，幂等键：{}", idempotencyKey);
            return existing.get();
        }
        
        // 2. 查询积分账户
        PointsAccount account = accountRepository.findByMemberId(tenantId, memberId)
                .orElseThrow(() -> new IllegalStateException("积分账户不存在，会员ID：" + memberId));
        
        // 3. 记录变动前余额
        Long beforeAvailable = account.getAvailablePoints();
        Long beforeFrozen = account.getFrozenPoints();
        
        // 4. 回退积分（增加可用积分）
        account.increaseAvailable(points);
        
        // 5. 创建流水记录
        PointsLedger ledger = PointsLedger.create(
                ledgerId, tenantId, memberId,
                PointsDirection.REVERT, points,
                beforeAvailable, beforeFrozen,
                account.getAvailablePoints(), account.getFrozenPoints(),
                bizType, bizId, idempotencyKey, remark
        );
        ledger.validate();
        
        // 6. 保存流水
        boolean saved = ledgerRepository.save(ledger);
        if (!saved) {
            return ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("流水保存失败"));
        }
        
        // 7. 更新账户
        boolean updated = updateAccountWithRetry(account);
        if (!updated) {
            throw new IllegalStateException("积分账户更新失败（乐观锁冲突）");
        }
        
        log.info("积分回退成功，会员ID：{}，积分：{}，幂等键：{}", memberId, points, idempotencyKey);
        return ledger;
    }
    
    /**
     * 冻结积分（下单锁定）
     * 
     * @param ledgerId 流水ID
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsLedger freezePoints(Long ledgerId, Long tenantId, Long memberId, Long points,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        // 1. 幂等性检查
        Optional<PointsLedger> existing = ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("积分冻结操作已处理过，幂等键：{}", idempotencyKey);
            return existing.get();
        }
        
        // 2. 查询积分账户
        PointsAccount account = accountRepository.findByMemberId(tenantId, memberId)
                .orElseThrow(() -> new IllegalStateException("积分账户不存在，会员ID：" + memberId));
        
        // 3. 记录变动前余额
        Long beforeAvailable = account.getAvailablePoints();
        Long beforeFrozen = account.getFrozenPoints();
        
        // 4. 冻结积分
        account.freeze(points);
        
        // 5. 创建流水记录
        PointsLedger ledger = PointsLedger.create(
                ledgerId, tenantId, memberId,
                PointsDirection.FREEZE, points,
                beforeAvailable, beforeFrozen,
                account.getAvailablePoints(), account.getFrozenPoints(),
                bizType, bizId, idempotencyKey, remark
        );
        ledger.validate();
        
        // 6. 保存流水
        boolean saved = ledgerRepository.save(ledger);
        if (!saved) {
            return ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("流水保存失败"));
        }
        
        // 7. 更新账户
        boolean updated = updateAccountWithRetry(account);
        if (!updated) {
            throw new IllegalStateException("积分账户更新失败（乐观锁冲突）");
        }
        
        log.info("积分冻结成功，会员ID：{}，积分：{}，幂等键：{}", memberId, points, idempotencyKey);
        return ledger;
    }
    
    /**
     * 释放冻结积分（取消订单/超时）
     * 
     * @param ledgerId 流水ID
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsLedger releasePoints(Long ledgerId, Long tenantId, Long memberId, Long points,
                                      String bizType, String bizId, String idempotencyKey, String remark) {
        // 1. 幂等性检查
        Optional<PointsLedger> existing = ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("积分释放操作已处理过，幂等键：{}", idempotencyKey);
            return existing.get();
        }
        
        // 2. 查询积分账户
        PointsAccount account = accountRepository.findByMemberId(tenantId, memberId)
                .orElseThrow(() -> new IllegalStateException("积分账户不存在，会员ID：" + memberId));
        
        // 3. 记录变动前余额
        Long beforeAvailable = account.getAvailablePoints();
        Long beforeFrozen = account.getFrozenPoints();
        
        // 4. 释放积分
        account.release(points);
        
        // 5. 创建流水记录
        PointsLedger ledger = PointsLedger.create(
                ledgerId, tenantId, memberId,
                PointsDirection.RELEASE, points,
                beforeAvailable, beforeFrozen,
                account.getAvailablePoints(), account.getFrozenPoints(),
                bizType, bizId, idempotencyKey, remark
        );
        ledger.validate();
        
        // 6. 保存流水
        boolean saved = ledgerRepository.save(ledger);
        if (!saved) {
            return ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("流水保存失败"));
        }
        
        // 7. 更新账户
        boolean updated = updateAccountWithRetry(account);
        if (!updated) {
            throw new IllegalStateException("积分账户更新失败（乐观锁冲突）");
        }
        
        log.info("积分释放成功，会员ID：{}，积分：{}，幂等键：{}", memberId, points, idempotencyKey);
        return ledger;
    }
    
    /**
     * 调整积分（管理员手动调整）
     * 
     * @param ledgerId 流水ID
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param isIncrease true=增加，false=减少
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    @Transactional(rollbackFor = Exception.class)
    public PointsLedger adjustPoints(Long ledgerId, Long tenantId, Long memberId, Long points, boolean isIncrease,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        // 1. 幂等性检查
        Optional<PointsLedger> existing = ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey);
        if (existing.isPresent()) {
            log.info("积分调整操作已处理过，幂等键：{}", idempotencyKey);
            return existing.get();
        }
        
        // 2. 查询积分账户
        PointsAccount account = accountRepository.findByMemberId(tenantId, memberId)
                .orElseThrow(() -> new IllegalStateException("积分账户不存在，会员ID：" + memberId));
        
        // 3. 记录变动前余额
        Long beforeAvailable = account.getAvailablePoints();
        Long beforeFrozen = account.getFrozenPoints();
        
        // 4. 调整积分
        if (isIncrease) {
            account.increaseAvailable(points);
        } else {
            account.decreaseAvailable(points);
        }
        
        // 5. 创建流水记录
        PointsLedger ledger = PointsLedger.create(
                ledgerId, tenantId, memberId,
                PointsDirection.ADJUST, points,
                beforeAvailable, beforeFrozen,
                account.getAvailablePoints(), account.getFrozenPoints(),
                bizType, bizId, idempotencyKey, remark
        );
        ledger.validate();
        
        // 6. 保存流水
        boolean saved = ledgerRepository.save(ledger);
        if (!saved) {
            return ledgerRepository.findByIdempotencyKey(tenantId, idempotencyKey)
                    .orElseThrow(() -> new IllegalStateException("流水保存失败"));
        }
        
        // 7. 更新账户
        boolean updated = updateAccountWithRetry(account);
        if (!updated) {
            throw new IllegalStateException("积分账户更新失败（乐观锁冲突）");
        }
        
        log.info("积分调整成功，会员ID：{}，积分：{}，增加：{}，幂等键：{}", memberId, points, isIncrease, idempotencyKey);
        return ledger;
    }
    
    /**
     * 更新账户（带重试机制处理乐观锁冲突）
     */
    private boolean updateAccountWithRetry(PointsAccount account) {
        for (int i = 0; i < MAX_RETRY_TIMES; i++) {
            boolean updated = accountRepository.updateWithVersion(account);
            if (updated) {
                return true;
            }
            
            // 乐观锁冲突，重新查询并重试
            log.warn("积分账户更新乐观锁冲突，重试次数：{}/{}", i + 1, MAX_RETRY_TIMES);
            Optional<PointsAccount> latest = accountRepository.findByMemberId(account.getTenantId(), account.getMemberId());
            if (latest.isEmpty()) {
                return false;
            }
            account.setVersion(latest.get().getVersion());
        }
        return false;
    }
}
