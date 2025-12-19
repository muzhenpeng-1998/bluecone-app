package com.bluecone.app.promo.api.facade;

import com.bluecone.app.promo.api.dto.CouponCommitCommand;
import com.bluecone.app.promo.api.dto.CouponLockCommand;
import com.bluecone.app.promo.api.dto.CouponLockResult;
import com.bluecone.app.promo.api.dto.CouponReleaseCommand;

/**
 * 优惠券锁定门面
 */
public interface CouponLockFacade {
    
    /**
     * 锁定优惠券（订单下单时调用）
     * 支持幂等：相同idempotencyKey重复调用返回相同结果
     * 
     * @param command 锁定命令
     * @return 锁定结果
     */
    CouponLockResult lock(CouponLockCommand command);
    
    /**
     * 释放优惠券（订单取消/超时时调用）
     * 支持幂等：重复调用不报错
     * 
     * @param command 释放命令
     */
    void release(CouponReleaseCommand command);
    
    /**
     * 提交核销优惠券（订单支付成功时调用）
     * 支持幂等：相同idempotencyKey重复调用不会重复扣减
     * 
     * @param command 核销命令
     */
    void commit(CouponCommitCommand command);
}
