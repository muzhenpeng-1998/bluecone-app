package com.bluecone.app.member.api.facade;

import com.bluecone.app.member.api.dto.PointsOperationCommand;
import com.bluecone.app.member.api.dto.PointsOperationResult;

/**
 * 积分资产操作门面接口
 * 提供积分的冻结、提交、释放、回退等资产操作能力
 * 所有操作都必须保证幂等性和原子性
 * 
 * @author bluecone
 * @since 2025-12-18
 */
public interface PointsAssetFacade {
    
    /**
     * 冻结积分（下单锁定）
     * 将可用积分转为冻结状态，等待后续提交或释放
     * 
     * @param command 积分操作命令（必须包含幂等键）
     * @return 操作结果
     */
    PointsOperationResult freezePoints(PointsOperationCommand command);
    
    /**
     * 提交积分变更（支付成功后提交扣减/入账）
     * - 如果之前有冻结，则将冻结积分扣除
     * - 如果没有冻结，则直接扣减可用积分
     * - 赚取积分时，直接增加可用积分
     * 
     * @param command 积分操作命令（必须包含幂等键）
     * @return 操作结果
     */
    PointsOperationResult commitPoints(PointsOperationCommand command);
    
    /**
     * 释放冻结积分（取消订单/超时）
     * 将冻结积分恢复为可用积分
     * 
     * @param command 积分操作命令（必须包含幂等键）
     * @return 操作结果
     */
    PointsOperationResult releasePoints(PointsOperationCommand command);
    
    /**
     * 回退积分变更（退款返还）
     * 按照原流水做反向操作，恢复积分
     * 
     * @param command 积分操作命令（必须包含幂等键）
     * @return 操作结果
     */
    PointsOperationResult revertPoints(PointsOperationCommand command);
    
    /**
     * 调整积分（管理员手动调整）
     * 直接增加或减少可用积分，用于补偿、修正等场景
     * 
     * @param command 积分操作命令（必须包含幂等键）
     * @param isIncrease true=增加，false=减少
     * @return 操作结果
     */
    PointsOperationResult adjustPoints(PointsOperationCommand command, boolean isIncrease);
}
