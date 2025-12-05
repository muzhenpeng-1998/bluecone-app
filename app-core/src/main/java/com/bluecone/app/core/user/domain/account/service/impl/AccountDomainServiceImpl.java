package com.bluecone.app.core.user.domain.account.service.impl;

import com.bluecone.app.core.user.domain.account.BalanceAccount;
import com.bluecone.app.core.user.domain.account.BalanceChangeResult;
import com.bluecone.app.core.user.domain.account.BalanceLedger;
import com.bluecone.app.core.user.domain.account.PointsAccount;
import com.bluecone.app.core.user.domain.account.PointsChangeResult;
import com.bluecone.app.core.user.domain.account.PointsLedger;
import com.bluecone.app.core.user.domain.account.service.AccountDomainService;
import com.bluecone.app.core.user.domain.member.service.GrowthDomainService;
import com.bluecone.app.core.user.domain.repository.BalanceAccountRepository;
import com.bluecone.app.core.user.domain.repository.BalanceLedgerRepository;
import com.bluecone.app.core.user.domain.repository.PointsAccountRepository;
import com.bluecone.app.core.user.domain.repository.PointsLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 账户领域服务实现，处理积分与储值的幂等变更。
 */
@Service
@RequiredArgsConstructor
public class AccountDomainServiceImpl implements AccountDomainService {

    private final PointsAccountRepository pointsAccountRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final BalanceLedgerRepository balanceLedgerRepository;
    private final GrowthDomainService growthDomainService;

    @Override
    public PointsChangeResult changePoints(Long tenantId,
                                           Long memberId,
                                           int delta,
                                           String bizType,
                                           String bizId,
                                           String remark) {
        if (tenantId == null || memberId == null || !StringUtils.hasText(bizType) || !StringUtils.hasText(bizId)) {
            return PointsChangeResult.failed("参数不完整");
        }
        Optional<PointsLedger> existing = pointsLedgerRepository.findByBiz(tenantId, bizType, bizId);
        if (existing.isPresent()) {
            PointsLedger ledger = existing.get();
            return PointsChangeResult.idempotent(delta, ledger.getBalanceAfter());
        }

        PointsAccount account = pointsAccountRepository.findByTenantAndMember(tenantId, memberId)
                .orElseGet(() -> PointsAccount.initFor(tenantId, memberId));
        int newBalance = account.getPointsBalance() + delta;
        if (newBalance < 0) {
            // TODO: 考虑是否允许负数或冻结抵扣
            return PointsChangeResult.failed("积分余额不足");
        }

        boolean updated = applyPointsWithOptimisticLock(account, newBalance);
        if (!updated) {
            // TODO: 可加入重试策略
            return PointsChangeResult.failed("积分账户并发更新失败");
        }

        PointsLedger ledger = new PointsLedger();
        ledger.setTenantId(tenantId);
        ledger.setMemberId(memberId);
        ledger.setBizType(bizType);
        ledger.setBizId(bizId);
        ledger.setChangePoints(delta);
        ledger.setBalanceAfter(newBalance);
        ledger.setRemark(remark);
        ledger.setOccurredAt(LocalDateTime.now());
        ledger.setCreatedAt(LocalDateTime.now());
        pointsLedgerRepository.save(ledger);

        // TODO: 成长值与积分换算规则可配置
        if (delta > 0) {
            growthDomainService.increaseGrowthAndCheckLevel(tenantId, memberId, delta, bizType, bizId);
        }

        return PointsChangeResult.success(delta, newBalance);
    }

    @Override
    public BalanceChangeResult changeBalance(Long tenantId,
                                             Long memberId,
                                             BigDecimal delta,
                                             String bizType,
                                             String bizId,
                                             String remark) {
        if (tenantId == null || memberId == null || delta == null || !StringUtils.hasText(bizType) || !StringUtils.hasText(bizId)) {
            return BalanceChangeResult.failed("参数不完整");
        }
        Optional<BalanceLedger> existing = balanceLedgerRepository.findByBiz(tenantId, bizType, bizId);
        if (existing.isPresent()) {
            BalanceLedger ledger = existing.get();
            return BalanceChangeResult.idempotent(delta, ledger.getBalanceAfter());
        }

        BalanceAccount account = balanceAccountRepository.findByTenantAndMember(tenantId, memberId)
                .orElseGet(() -> BalanceAccount.initFor(tenantId, memberId));
        BigDecimal newBalance = account.getAvailableAmount().add(delta);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            // TODO: 考虑冻结金额、透支策略
            return BalanceChangeResult.failed("储值余额不足");
        }

        boolean updated = applyBalanceWithOptimisticLock(account, newBalance);
        if (!updated) {
            // TODO: 可加入重试策略
            return BalanceChangeResult.failed("储值账户并发更新失败");
        }

        BalanceLedger ledger = new BalanceLedger();
        ledger.setTenantId(tenantId);
        ledger.setMemberId(memberId);
        ledger.setBizType(bizType);
        ledger.setBizId(bizId);
        ledger.setChangeAmount(delta);
        ledger.setBalanceAfter(newBalance);
        ledger.setRemark(remark);
        ledger.setOccurredAt(LocalDateTime.now());
        ledger.setCreatedAt(LocalDateTime.now());
        balanceLedgerRepository.save(ledger);

        // TODO: 成长值规则配置化，目前按积分变化同步成长值（仅正向）
        if (delta != null && delta.compareTo(BigDecimal.ZERO) > 0) {
            growthDomainService.increaseGrowthAndCheckLevel(tenantId, memberId, delta.intValue(), bizType, bizId);
        }

        return BalanceChangeResult.success(delta, newBalance);
    }

    private boolean applyPointsWithOptimisticLock(PointsAccount account, int newBalance) {
        if (account.getId() == null) {
            account.setPointsBalance(newBalance);
            account.setVersion(0);
            account.applyChange(0);
            pointsAccountRepository.save(account);
            return true;
        }
        long expectedVersion = account.getVersion();
        account.setPointsBalance(newBalance);
        account.applyChange(0); // 内部递增版本
        return pointsAccountRepository.saveWithVersion(account, expectedVersion);
    }

    private boolean applyBalanceWithOptimisticLock(BalanceAccount account, BigDecimal newBalance) {
        if (account.getId() == null) {
            account.setAvailableAmount(newBalance);
            account.setVersion(0);
            account.applyChange(BigDecimal.ZERO);
            balanceAccountRepository.save(account);
            return true;
        }
        long expectedVersion = account.getVersion();
        account.setAvailableAmount(newBalance);
        account.applyChange(BigDecimal.ZERO);
        return balanceAccountRepository.saveWithVersion(account, expectedVersion);
    }
}
