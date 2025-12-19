package com.bluecone.app.wallet.application.facade;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.wallet.api.dto.WalletAssetCommand;
import com.bluecone.app.wallet.api.dto.WalletAssetResult;
import com.bluecone.app.wallet.api.facade.WalletAssetFacade;
import com.bluecone.app.wallet.domain.model.WalletAccount;
import com.bluecone.app.wallet.domain.model.WalletFreeze;
import com.bluecone.app.wallet.domain.model.WalletLedger;
import com.bluecone.app.wallet.domain.service.WalletDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 钱包资产操作门面实现
 * 提供钱包资产的冻结、提交、释放、回退等操作能力
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAssetFacadeImpl implements WalletAssetFacade {
    
    private final WalletDomainService walletDomainService;
    
    /**
     * 冻结余额（下单锁定）
     */
    @Override
    public WalletAssetResult freeze(WalletAssetCommand command) {
        try {
            // 参数校验
            validateFreezeCommand(command);
            
            // 调用领域服务冻结余额
            WalletFreeze freeze = walletDomainService.freeze(
                    command.getTenantId(),
                    command.getUserId(),
                    command.getAmount(),
                    command.getBizType(),
                    command.getBizOrderId(),
                    command.getBizOrderNo(),
                    command.getIdempotencyKey(),
                    command.getOperatorId()
            );
            
            // 查询账户最新余额
            WalletAccount account = walletDomainService.getAccount(command.getTenantId(), command.getUserId());
            
            // 判断是否幂等重放
            boolean idempotent = freeze.getCreatedAt().isBefore(
                    java.time.LocalDateTime.now().minusSeconds(1)
            );
            
            return WalletAssetResult.success(
                    freeze.getAccountId(),
                    freeze.getFreezeNo(),
                    null,
                    account.getAvailableBalance(),
                    account.getFrozenBalance(),
                    idempotent
            );
        } catch (BizException e) {
            log.error("冻结余额失败：tenantId={}, userId={}, amount={}, error={}", 
                    command.getTenantId(), command.getUserId(), command.getAmount(), e.getMessage());
            return WalletAssetResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("冻结余额异常：tenantId={}, userId={}, amount={}", 
                    command.getTenantId(), command.getUserId(), command.getAmount(), e);
            return WalletAssetResult.failure("冻结余额失败：" + e.getMessage());
        }
    }
    
    /**
     * 提交余额变更（支付成功后提交扣减）
     */
    @Override
    public WalletAssetResult commit(WalletAssetCommand command) {
        try {
            // 参数校验
            validateCommitCommand(command);
            
            // 调用领域服务提交冻结
            WalletLedger ledger = walletDomainService.commit(
                    command.getTenantId(),
                    command.getUserId(),
                    command.getBizOrderId(),
                    command.getIdempotencyKey(),
                    command.getOperatorId()
            );
            
            // 查询账户最新余额
            WalletAccount account = walletDomainService.getAccount(command.getTenantId(), command.getUserId());
            
            // 判断是否幂等重放
            boolean idempotent = ledger.getCreatedAt().isBefore(
                    java.time.LocalDateTime.now().minusSeconds(1)
            );
            
            return WalletAssetResult.success(
                    ledger.getAccountId(),
                    null,
                    ledger.getLedgerNo(),
                    account.getAvailableBalance(),
                    account.getFrozenBalance(),
                    idempotent
            );
        } catch (BizException e) {
            log.error("提交冻结失败：tenantId={}, userId={}, bizOrderId={}, error={}", 
                    command.getTenantId(), command.getUserId(), command.getBizOrderId(), e.getMessage());
            return WalletAssetResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("提交冻结异常：tenantId={}, userId={}, bizOrderId={}", 
                    command.getTenantId(), command.getUserId(), command.getBizOrderId(), e);
            return WalletAssetResult.failure("提交冻结失败：" + e.getMessage());
        }
    }
    
    /**
     * 释放冻结余额（取消订单/超时）
     */
    @Override
    public WalletAssetResult release(WalletAssetCommand command) {
        try {
            // 参数校验
            validateReleaseCommand(command);
            
            // 调用领域服务释放冻结
            walletDomainService.release(
                    command.getTenantId(),
                    command.getUserId(),
                    command.getBizOrderId(),
                    command.getIdempotencyKey(),
                    command.getOperatorId()
            );
            
            // 查询账户最新余额
            WalletAccount account = walletDomainService.getAccount(command.getTenantId(), command.getUserId());
            
            return WalletAssetResult.success(
                    account.getId(),
                    null,
                    null,
                    account.getAvailableBalance(),
                    account.getFrozenBalance(),
                    false
            );
        } catch (BizException e) {
            log.error("释放冻结失败：tenantId={}, userId={}, bizOrderId={}, error={}", 
                    command.getTenantId(), command.getUserId(), command.getBizOrderId(), e.getMessage());
            return WalletAssetResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("释放冻结异常：tenantId={}, userId={}, bizOrderId={}", 
                    command.getTenantId(), command.getUserId(), command.getBizOrderId(), e);
            return WalletAssetResult.failure("释放冻结失败：" + e.getMessage());
        }
    }
    
    /**
     * 回退余额变更（退款返还）
     */
    @Override
    public WalletAssetResult revert(WalletAssetCommand command) {
        try {
            // 参数校验
            validateRevertCommand(command);
            
            // 调用领域服务回退余额
            WalletLedger ledger = walletDomainService.revert(
                    command.getTenantId(),
                    command.getUserId(),
                    command.getAmount(),
                    command.getBizType(),
                    command.getBizOrderId(),
                    command.getBizOrderNo(),
                    command.getIdempotencyKey(),
                    command.getOperatorId()
            );
            
            // 查询账户最新余额
            WalletAccount account = walletDomainService.getAccount(command.getTenantId(), command.getUserId());
            
            // 判断是否幂等重放
            boolean idempotent = ledger.getCreatedAt().isBefore(
                    java.time.LocalDateTime.now().minusSeconds(1)
            );
            
            return WalletAssetResult.success(
                    ledger.getAccountId(),
                    null,
                    ledger.getLedgerNo(),
                    account.getAvailableBalance(),
                    account.getFrozenBalance(),
                    idempotent
            );
        } catch (BizException e) {
            log.error("回退余额失败：tenantId={}, userId={}, amount={}, error={}", 
                    command.getTenantId(), command.getUserId(), command.getAmount(), e.getMessage());
            return WalletAssetResult.failure(e.getMessage());
        } catch (Exception e) {
            log.error("回退余额异常：tenantId={}, userId={}, amount={}", 
                    command.getTenantId(), command.getUserId(), command.getAmount(), e);
            return WalletAssetResult.failure("回退余额失败：" + e.getMessage());
        }
    }
    
    // ==================== 私有方法 ====================
    
    private void validateFreezeCommand(WalletAssetCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "冻结命令不能为空");
        }
        if (command.getTenantId() == null || command.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户ID和用户ID不能为空");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "冻结金额必须大于0");
        }
        if (command.getBizType() == null || command.getBizOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "业务类型和业务单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "幂等键不能为空");
        }
    }
    
    private void validateCommitCommand(WalletAssetCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "提交命令不能为空");
        }
        if (command.getTenantId() == null || command.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户ID和用户ID不能为空");
        }
        if (command.getBizOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "业务单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "幂等键不能为空");
        }
    }
    
    private void validateReleaseCommand(WalletAssetCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "释放命令不能为空");
        }
        if (command.getTenantId() == null || command.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户ID和用户ID不能为空");
        }
        if (command.getBizOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "业务单ID不能为空");
        }
    }
    
    private void validateRevertCommand(WalletAssetCommand command) {
        if (command == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "回退命令不能为空");
        }
        if (command.getTenantId() == null || command.getUserId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "租户ID和用户ID不能为空");
        }
        if (command.getAmount() == null || command.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "回退金额必须大于0");
        }
        if (command.getBizType() == null || command.getBizOrderId() == null) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "业务类型和业务单ID不能为空");
        }
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "幂等键不能为空");
        }
    }
}
