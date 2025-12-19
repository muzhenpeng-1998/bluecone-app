package com.bluecone.app.wallet.infra.persistence.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值单持久化对象
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("bc_wallet_recharge_order")
public class RechargeOrderPO {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    private Long tenantId;
    
    private Long userId;
    
    private Long accountId;
    
    /**
     * 充值单号（PublicId，对应数据库字段 recharge_id）
     */
    @TableField("recharge_id")
    private String rechargeNo;
    
    /**
     * 充值金额（单位：元）
     */
    private BigDecimal rechargeAmount;
    
    /**
     * 赠送金额（单位：元）
     */
    private BigDecimal bonusAmount;
    
    /**
     * 总到账金额（单位：元）
     */
    private BigDecimal totalAmount;
    
    private String currency;
    
    /**
     * 充值状态
     */
    private String status;
    
    /**
     * 支付单ID
     */
    private Long payOrderId;
    
    /**
     * 支付渠道
     */
    private String payChannel;
    
    /**
     * 第三方支付单号（渠道交易号，对应数据库字段 pay_no）
     */
    private String payNo;
    
    /**
     * 充值发起时间
     */
    private LocalDateTime rechargeRequestedAt;
    
    /**
     * 充值完成时间
     */
    private LocalDateTime rechargeCompletedAt;
    
    /**
     * 幂等键
     */
    private String idemKey;
    
    /**
     * 扩展信息JSON
     */
    private String extJson;
    
    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
    
    private LocalDateTime createdAt;
    
    private Long createdBy;
    
    private LocalDateTime updatedAt;
    
    private Long updatedBy;
}
