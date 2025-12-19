package com.bluecone.app.member.api.impl;

import com.bluecone.app.member.api.dto.PointsOperationCommand;
import com.bluecone.app.member.api.dto.PointsOperationResult;
import com.bluecone.app.member.api.facade.PointsAssetFacade;
import com.bluecone.app.member.application.service.PointsApplicationService;
import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.domain.model.PointsLedger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 积分资产操作门面实现
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service
public class PointsAssetFacadeImpl implements PointsAssetFacade {
    
    private static final Logger log = LoggerFactory.getLogger(PointsAssetFacadeImpl.class);
    
    private final PointsApplicationService pointsApplicationService;
    
    public PointsAssetFacadeImpl(PointsApplicationService pointsApplicationService) {
        this.pointsApplicationService = pointsApplicationService;
    }
    
    @Override
    public PointsOperationResult freezePoints(PointsOperationCommand command) {
        try {
            // 1. 验证命令参数
            command.validate();
            
            // 2. 执行冻结操作
            PointsLedger ledger = pointsApplicationService.freezePoints(
                    command.getTenantId(),
                    command.getMemberId(),
                    command.getPoints(),
                    command.getBizType(),
                    command.getBizId(),
                    command.getIdempotencyKey(),
                    command.getRemark()
            );
            
            // 3. 查询最新余额
            PointsAccount account = pointsApplicationService.getPointsBalance(
                    command.getTenantId(), command.getMemberId());
            
            return PointsOperationResult.success(
                    ledger.getId(),
                    account.getAvailablePoints(),
                    account.getFrozenPoints()
            );
            
        } catch (Exception e) {
            log.error("积分冻结失败，幂等键：{}，错误：{}", command.getIdempotencyKey(), e.getMessage(), e);
            return PointsOperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public PointsOperationResult commitPoints(PointsOperationCommand command) {
        try {
            // 1. 验证命令参数
            command.validate();
            
            // 2. 执行提交操作（简化版，暂时只支持赚取积分）
            // TODO: 后续支持扣减冻结积分的场景
            PointsLedger ledger = pointsApplicationService.earnPoints(
                    command.getTenantId(),
                    command.getMemberId(),
                    command.getPoints(),
                    command.getBizType(),
                    command.getBizId(),
                    command.getIdempotencyKey(),
                    command.getRemark()
            );
            
            // 3. 查询最新余额
            PointsAccount account = pointsApplicationService.getPointsBalance(
                    command.getTenantId(), command.getMemberId());
            
            return PointsOperationResult.success(
                    ledger.getId(),
                    account.getAvailablePoints(),
                    account.getFrozenPoints()
            );
            
        } catch (Exception e) {
            log.error("积分提交失败，幂等键：{}，错误：{}", command.getIdempotencyKey(), e.getMessage(), e);
            return PointsOperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public PointsOperationResult releasePoints(PointsOperationCommand command) {
        try {
            // 1. 验证命令参数
            command.validate();
            
            // 2. 执行释放操作
            PointsLedger ledger = pointsApplicationService.releasePoints(
                    command.getTenantId(),
                    command.getMemberId(),
                    command.getPoints(),
                    command.getBizType(),
                    command.getBizId(),
                    command.getIdempotencyKey(),
                    command.getRemark()
            );
            
            // 3. 查询最新余额
            PointsAccount account = pointsApplicationService.getPointsBalance(
                    command.getTenantId(), command.getMemberId());
            
            return PointsOperationResult.success(
                    ledger.getId(),
                    account.getAvailablePoints(),
                    account.getFrozenPoints()
            );
            
        } catch (Exception e) {
            log.error("积分释放失败，幂等键：{}，错误：{}", command.getIdempotencyKey(), e.getMessage(), e);
            return PointsOperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public PointsOperationResult revertPoints(PointsOperationCommand command) {
        try {
            // 1. 验证命令参数
            command.validate();
            
            // 2. 执行回退操作
            PointsLedger ledger = pointsApplicationService.revertPoints(
                    command.getTenantId(),
                    command.getMemberId(),
                    command.getPoints(),
                    command.getBizType(),
                    command.getBizId(),
                    command.getIdempotencyKey(),
                    command.getRemark()
            );
            
            // 3. 查询最新余额
            PointsAccount account = pointsApplicationService.getPointsBalance(
                    command.getTenantId(), command.getMemberId());
            
            return PointsOperationResult.success(
                    ledger.getId(),
                    account.getAvailablePoints(),
                    account.getFrozenPoints()
            );
            
        } catch (Exception e) {
            log.error("积分回退失败，幂等键：{}，错误：{}", command.getIdempotencyKey(), e.getMessage(), e);
            return PointsOperationResult.failure(e.getMessage());
        }
    }
    
    @Override
    public PointsOperationResult adjustPoints(PointsOperationCommand command, boolean isIncrease) {
        try {
            // 1. 验证命令参数
            command.validate();
            
            // 2. 执行调整操作
            PointsLedger ledger = pointsApplicationService.adjustPoints(
                    command.getTenantId(),
                    command.getMemberId(),
                    command.getPoints(),
                    isIncrease,
                    command.getBizType(),
                    command.getBizId(),
                    command.getIdempotencyKey(),
                    command.getRemark()
            );
            
            // 3. 查询最新余额
            PointsAccount account = pointsApplicationService.getPointsBalance(
                    command.getTenantId(), command.getMemberId());
            
            return PointsOperationResult.success(
                    ledger.getId(),
                    account.getAvailablePoints(),
                    account.getFrozenPoints()
            );
            
        } catch (Exception e) {
            log.error("积分调整失败，幂等键：{}，错误：{}", command.getIdempotencyKey(), e.getMessage(), e);
            return PointsOperationResult.failure(e.getMessage());
        }
    }
}
