package com.bluecone.app.wallet.domain.service.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.wallet.domain.enums.AccountStatus;
import com.bluecone.app.wallet.domain.enums.BizType;
import com.bluecone.app.wallet.domain.enums.FreezeStatus;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.model.WalletFreeze;
import com.bluecone.app.wallet.domain.model.WalletLedger;
import com.bluecone.app.wallet.domain.repository.WalletAccountRepository;
import com.bluecone.app.wallet.domain.repository.WalletFreezeRepository;
import com.bluecone.app.wallet.domain.repository.WalletLedgerRepository;
import com.bluecone.app.wallet.domain.service.WalletDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 钱包领域服务实现
 * 实现钱包资产操作的核心业务逻辑
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletDomainServiceImpl implements WalletDomainService {
    
    private final WalletAccountRepository accountRepository;
    private final WalletFreezeRepository freezeRepository;
    private final WalletLedgerRepository ledgerRepository;
    private final IdService idService;
    
    /**
     * 冻结余额（下单锁定）
     * 幂等性：通过唯一约束（idem_key）保证，重复调用返回已冻结结果
     * 并发控制：通过乐观锁（version）保证账户并发安全
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletFreeze freeze(Long tenantId, Long userId, BigDecimal amount,
                              String bizType, Long bizOrderId, String bizOrderNo,
                              String idempotencyKey, Long operatorId) {
        // 参数校验
        validateFreezeParams(tenantId, userId, amount, bizType, bizOrderId, idempotencyKey);
        
        // 幂等性检查：如果已存在冻结记录，直接返回
        WalletFreeze existingFreeze = freezeRepository.findByIdemKey(tenantId, idempotencyKey);
        if (existingFreeze != null) {
            log.info("冻结操作幂等重放：tenantId={}, userId={}, bizOrderId={}, freezeNo={}", 
                    tenantId, userId, bizOrderId, existingFreeze.getFreezeNo());
            return existingFreeze;
        }
        
        // 获取或创建账户
        WalletAccount account = accountRepository.getOrCreate(tenantId, userId);
        if (!account.isActive()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "账户状态异常，无法冻结余额");
        }
        
        // 检查可用余额是否足够
        if (!account.hasEnoughBalance(amount)) {
            log.warn("可用余额不足：tenantId={}, userId={}, available={}, required={}", 
                    tenantId, userId, account.getAvailableBalance(), amount);
            throw new BizException(CommonErrorCode.BAD_REQUEST, "可用余额不足");
        }
        
        // 冻结余额（可用 -> 冻结）
        account.freeze(amount);
        
        // 乐观锁更新账户
        int updated = accountRepository.updateWithVersion(account);
        if (updated == 0) {
            log.warn("账户乐观锁冲突，冻结失败：tenantId={}, userId={}, version={}", 
                    tenantId, userId, account.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "账户余额变更冲突，请重试");
        }
        
        // 创建冻结记录
        LocalDateTime now = LocalDateTime.now();
        WalletFreeze freeze = WalletFreeze.builder()
                .id(idService.nextLong(IdScope.WALLET_FREEZE))
                .tenantId(tenantId)
                .userId(userId)
                .accountId(account.getId())
                .freezeNo(idService.nextPublicId(ResourceType.WALLET_FREEZE))
                .bizType(bizType)
                .bizOrderId(bizOrderId)
                .bizOrderNo(bizOrderNo)
                .frozenAmount(amount)
                .currency(account.getCurrency())
                .status(FreezeStatus.FROZEN)
                .idemKey(idempotencyKey)
                .frozenAt(now)
                .expiresAt(now.plusMinutes(30)) // 默认30分钟超时
                .version(0)
                .createdAt(now)
                .createdBy(operatorId)
                .updatedAt(now)
                .updatedBy(operatorId)
                .build();
        
        try {
            freezeRepository.insert(freeze);
            log.info("冻结余额成功：tenantId={}, userId={}, amount={}, freezeNo={}, bizOrderId={}", 
                    tenantId, userId, amount, freeze.getFreezeNo(), bizOrderId);
            return freeze;
        } catch (DuplicateKeyException e) {
            // 并发情况下，唯一约束冲突，重新查询返回
            log.info("冻结记录唯一约束冲突，重新查询：tenantId={}, idemKey={}", tenantId, idempotencyKey);
            return freezeRepository.findByIdemKey(tenantId, idempotencyKey);
        }
    }
    
    /**
     * 提交冻结余额（支付成功后扣减）
     * 幂等性：通过唯一约束（idem_key）保证，重复调用返回已提交结果
     * 账本化：必须写入 bc_wallet_ledger 流水表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletLedger commit(Long tenantId, Long userId, Long bizOrderId,
                              String idempotencyKey, Long operatorId) {
        // 参数校验
        if (tenantId == null || userId == null || bizOrderId == null || idempotencyKey == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "提交参数不能为空");
        }
        
        // 幂等性检查：如果已存在账本流水，直接返回
        WalletLedger existingLedger = ledgerRepository.findByIdemKey(tenantId, idempotencyKey).orElse(null);
        if (existingLedger != null) {
            log.info("提交操作幂等重放：tenantId={}, userId={}, bizOrderId={}, ledgerNo={}", 
                    tenantId, userId, bizOrderId, existingLedger.getLedgerNo());
            return existingLedger;
        }
        
        // 查询冻结记录
        WalletFreeze freeze = freezeRepository.findByBizOrderId(tenantId, BizType.ORDER_CHECKOUT.getCode(), bizOrderId);
        if (freeze == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "冻结记录不存在");
        }
        
        // 幂等性检查：如果冻结记录已提交，查询对应的账本流水返回
        if (FreezeStatus.COMMITTED.equals(freeze.getStatus())) {
            log.info("冻结记录已提交，查询账本流水：tenantId={}, freezeNo={}", tenantId, freeze.getFreezeNo());
            // 构造冻结时的幂等键，查询账本流水
            String freezeIdemKey = buildCommitIdemKey(tenantId, userId, bizOrderId);
            WalletLedger ledger = ledgerRepository.findByIdemKey(tenantId, freezeIdemKey).orElse(null);
            if (ledger != null) {
                return ledger;
            }
        }
        
        // 状态检查：只能提交 FROZEN 状态的冻结记录
        if (!freeze.canCommit()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, 
                    "冻结状态不允许提交：" + freeze.getStatus());
        }
        
        // 获取账户
        WalletAccount account = accountRepository.findById(tenantId, freeze.getAccountId());
        if (account == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "账户不存在");
        }
        
        // 提交冻结（冻结 -> 扣除）
        BigDecimal amount = freeze.getFrozenAmount();
        BigDecimal balanceBefore = account.getAvailableBalance();
        account.commitFreeze(amount);
        BigDecimal balanceAfter = account.getAvailableBalance();
        
        // 乐观锁更新账户
        int updated = accountRepository.updateWithVersion(account);
        if (updated == 0) {
            log.warn("账户乐观锁冲突，提交失败：tenantId={}, userId={}, version={}", 
                    tenantId, userId, account.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "账户余额变更冲突，请重试");
        }
        
        // 更新冻结记录状态
        freeze.markCommitted();
        freeze.setUpdatedAt(LocalDateTime.now());
        freeze.setUpdatedBy(operatorId);
        int freezeUpdated = freezeRepository.updateWithVersion(freeze);
        if (freezeUpdated == 0) {
            log.warn("冻结记录乐观锁冲突，提交失败：tenantId={}, freezeNo={}, version={}", 
                    tenantId, freeze.getFreezeNo(), freeze.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "冻结记录状态冲突，请重试");
        }
        
        // 写入账本流水（ORDER_PAY 出账）
        LocalDateTime now = LocalDateTime.now();
        WalletLedger ledger = WalletLedger.builder()
                .id(idService.nextLong(IdScope.WALLET_LEDGER))
                .tenantId(tenantId)
                .userId(userId)
                .accountId(account.getId())
                .ledgerNo(idService.nextPublicId(ResourceType.WALLET_LEDGER))
                .bizType(BizType.ORDER_PAY.getCode())
                .bizOrderId(bizOrderId)
                .bizOrderNo(freeze.getBizOrderNo())
                .amount(amount.negate()) // 出账：负数
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .remark("订单支付扣款")
                .idemKey(idempotencyKey)
                .createdAt(now)
                .createdBy(operatorId)
                .build();
        
        try {
            ledgerRepository.insert(ledger);
            log.info("提交冻结成功：tenantId={}, userId={}, amount={}, ledgerNo={}, bizOrderId={}", 
                    tenantId, userId, amount, ledger.getLedgerNo(), bizOrderId);
            return ledger;
        } catch (DuplicateKeyException e) {
            // 并发情况下，唯一约束冲突，重新查询返回
            log.info("账本流水唯一约束冲突，重新查询：tenantId={}, idemKey={}", tenantId, idempotencyKey);
            return ledgerRepository.findByIdemKey(tenantId, idempotencyKey).orElse(null);
        }
    }
    
    /**
     * 释放冻结余额（取消订单/超时）
     * 幂等性：重复调用不报错，直接返回
     * 不写入账本流水（只是状态恢复，非资金流水）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void release(Long tenantId, Long userId, Long bizOrderId,
                       String idempotencyKey, Long operatorId) {
        // 参数校验
        if (tenantId == null || userId == null || bizOrderId == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "释放参数不能为空");
        }
        
        // 查询冻结记录
        WalletFreeze freeze = freezeRepository.findByBizOrderId(tenantId, BizType.ORDER_CHECKOUT.getCode(), bizOrderId);
        if (freeze == null) {
            log.warn("冻结记录不存在，释放操作跳过：tenantId={}, userId={}, bizOrderId={}", 
                    tenantId, userId, bizOrderId);
            return;
        }
        
        // 幂等性检查：如果冻结记录已释放，直接返回
        if (FreezeStatus.RELEASED.equals(freeze.getStatus())) {
            log.info("冻结记录已释放，幂等返回：tenantId={}, freezeNo={}", tenantId, freeze.getFreezeNo());
            return;
        }
        
        // 状态检查：只能释放 FROZEN 状态的冻结记录
        if (!freeze.canRelease()) {
            log.warn("冻结状态不允许释放：tenantId={}, freezeNo={}, status={}", 
                    tenantId, freeze.getFreezeNo(), freeze.getStatus());
            return;
        }
        
        // 获取账户
        WalletAccount account = accountRepository.findById(tenantId, freeze.getAccountId());
        if (account == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "账户不存在");
        }
        
        // 释放冻结（冻结 -> 可用）
        BigDecimal amount = freeze.getFrozenAmount();
        account.releaseFreeze(amount);
        
        // 乐观锁更新账户
        int updated = accountRepository.updateWithVersion(account);
        if (updated == 0) {
            log.warn("账户乐观锁冲突，释放失败：tenantId={}, userId={}, version={}", 
                    tenantId, userId, account.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "账户余额变更冲突，请重试");
        }
        
        // 更新冻结记录状态
        freeze.markReleased();
        freeze.setUpdatedAt(LocalDateTime.now());
        freeze.setUpdatedBy(operatorId);
        int freezeUpdated = freezeRepository.updateWithVersion(freeze);
        if (freezeUpdated == 0) {
            log.warn("冻结记录乐观锁冲突，释放失败：tenantId={}, freezeNo={}, version={}", 
                    tenantId, freeze.getFreezeNo(), freeze.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "冻结记录状态冲突，请重试");
        }
        
        log.info("释放冻结成功：tenantId={}, userId={}, amount={}, freezeNo={}, bizOrderId={}", 
                tenantId, userId, amount, freeze.getFreezeNo(), bizOrderId);
    }
    
    /**
     * 回退余额变更（退款返还）
     * 幂等性：通过唯一约束（idem_key）保证，重复调用返回已回退结果
     * 账本化：必须写入 bc_wallet_ledger 流水表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WalletLedger revert(Long tenantId, Long userId, BigDecimal amount,
                              String bizType, Long bizOrderId, String bizOrderNo,
                              String idempotencyKey, Long operatorId) {
        // 参数校验
        if (tenantId == null || userId == null || amount == null || 
                bizType == null || bizOrderId == null || idempotencyKey == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "回退参数不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "回退金额必须大于0");
        }
        
        // 幂等性检查：如果已存在账本流水，直接返回
        WalletLedger existingLedger = ledgerRepository.findByIdemKey(tenantId, idempotencyKey).orElse(null);
        if (existingLedger != null) {
            log.info("回退操作幂等重放：tenantId={}, userId={}, bizOrderId={}, ledgerNo={}", 
                    tenantId, userId, bizOrderId, existingLedger.getLedgerNo());
            return existingLedger;
        }
        
        // 获取或创建账户
        WalletAccount account = accountRepository.getOrCreate(tenantId, userId);
        
        // 增加可用余额（退款入账）
        BigDecimal balanceBefore = account.getAvailableBalance();
        account.increaseAvailableBalance(amount);
        BigDecimal balanceAfter = account.getAvailableBalance();
        
        // 乐观锁更新账户
        int updated = accountRepository.updateWithVersion(account);
        if (updated == 0) {
            log.warn("账户乐观锁冲突，回退失败：tenantId={}, userId={}, version={}", 
                    tenantId, userId, account.getVersion());
            throw new BizException(CommonErrorCode.CONFLICT, "账户余额变更冲突，请重试");
        }
        
        // 写入账本流水（REFUND 入账）
        LocalDateTime now = LocalDateTime.now();
        WalletLedger ledger = WalletLedger.builder()
                .id(idService.nextLong(IdScope.WALLET_LEDGER))
                .tenantId(tenantId)
                .userId(userId)
                .accountId(account.getId())
                .ledgerNo(idService.nextPublicId(ResourceType.WALLET_LEDGER))
                .bizType(BizType.REFUND.getCode())
                .bizOrderId(bizOrderId)
                .bizOrderNo(bizOrderNo)
                .amount(amount) // 入账：正数
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .currency(account.getCurrency())
                .remark("订单退款返还")
                .idemKey(idempotencyKey)
                .createdAt(now)
                .createdBy(operatorId)
                .build();
        
        try {
            ledgerRepository.insert(ledger);
            log.info("回退余额成功：tenantId={}, userId={}, amount={}, ledgerNo={}, bizOrderId={}", 
                    tenantId, userId, amount, ledger.getLedgerNo(), bizOrderId);
            return ledger;
        } catch (DuplicateKeyException e) {
            // 并发情况下，唯一约束冲突，重新查询返回
            log.info("账本流水唯一约束冲突，重新查询：tenantId={}, idemKey={}", tenantId, idempotencyKey);
            return ledgerRepository.findByIdemKey(tenantId, idempotencyKey).orElse(null);
        }
    }
    
    /**
     * 查询或创建钱包账户
     */
    @Override
    public WalletAccount getOrCreateAccount(Long tenantId, Long userId) {
        return accountRepository.getOrCreate(tenantId, userId);
    }
    
    /**
     * 查询钱包账户
     */
    @Override
    public WalletAccount getAccount(Long tenantId, Long userId) {
        return accountRepository.findByUserId(tenantId, userId);
    }
    
    // ==================== 私有方法 ====================
    
    private void validateFreezeParams(Long tenantId, Long userId, BigDecimal amount,
                                     String bizType, Long bizOrderId, String idempotencyKey) {
        if (tenantId == null || userId == null || amount == null || 
                bizType == null || bizOrderId == null || idempotencyKey == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "冻结参数不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "冻结金额必须大于0");
        }
    }
    
    private String buildCommitIdemKey(Long tenantId, Long userId, Long bizOrderId) {
        return String.format("%d:%d:%d:commit", tenantId, userId, bizOrderId);
    }
}
