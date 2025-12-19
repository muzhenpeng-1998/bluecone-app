package com.bluecone.app.pricing.domain.model;

import com.bluecone.app.pricing.api.dto.PricingItem;
import com.bluecone.app.pricing.api.dto.PricingLine;
import com.bluecone.app.pricing.api.dto.PricingRequest;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 计价上下文
 * 在计价流水线中传递，每个 Stage 只操作此上下文
 * 确保可测试性和可扩展性
 */
@Data
public class PricingContext {
    
    /**
     * 原始请求
     */
    private PricingRequest request;
    
    /**
     * 当前金额（流水线中不断更新）
     */
    private BigDecimal currentAmount;
    
    /**
     * 商品原价（基价+规格加价）
     */
    private BigDecimal originalAmount;
    
    /**
     * 会员优惠金额
     */
    private BigDecimal memberDiscountAmount;
    
    /**
     * 活动优惠金额
     */
    private BigDecimal promoDiscountAmount;
    
    /**
     * 优惠券抵扣金额
     */
    private BigDecimal couponDiscountAmount;
    
    /**
     * 积分抵扣金额
     */
    private BigDecimal pointsDiscountAmount;
    
    /**
     * 配送费
     */
    private BigDecimal deliveryFee;
    
    /**
     * 打包费
     */
    private BigDecimal packingFee;
    
    /**
     * 抹零金额
     */
    private BigDecimal roundingAmount;
    
    /**
     * 计价明细行列表
     */
    private List<PricingLine> breakdownLines;
    
    /**
     * 使用的优惠券ID
     */
    private Long appliedCouponId;
    
    /**
     * 使用的积分数量
     */
    private Integer appliedPoints;
    
    /**
     * 上下文扩展数据（用于 Stage 之间传递数据）
     */
    private Map<String, Object> contextData;
    
    /**
     * 不可用原因（如果计价失败）
     */
    private String unavailableReason;
    
    public PricingContext(PricingRequest request) {
        this.request = request;
        this.currentAmount = BigDecimal.ZERO;
        this.originalAmount = BigDecimal.ZERO;
        this.memberDiscountAmount = BigDecimal.ZERO;
        this.promoDiscountAmount = BigDecimal.ZERO;
        this.couponDiscountAmount = BigDecimal.ZERO;
        this.pointsDiscountAmount = BigDecimal.ZERO;
        this.deliveryFee = BigDecimal.ZERO;
        this.packingFee = BigDecimal.ZERO;
        this.roundingAmount = BigDecimal.ZERO;
        this.breakdownLines = new ArrayList<>();
        this.contextData = new HashMap<>();
    }
    
    /**
     * 添加计价明细行
     */
    public void addBreakdownLine(PricingLine line) {
        this.breakdownLines.add(line);
    }
    
    /**
     * 增加当前金额
     */
    public void addAmount(BigDecimal amount) {
        this.currentAmount = this.currentAmount.add(amount);
    }
    
    /**
     * 减少当前金额
     */
    public void subtractAmount(BigDecimal amount) {
        this.currentAmount = this.currentAmount.subtract(amount);
    }
    
    /**
     * 获取上下文数据
     */
    @SuppressWarnings("unchecked")
    public <T> T getContextData(String key) {
        return (T) this.contextData.get(key);
    }
    
    /**
     * 设置上下文数据
     */
    public void putContextData(String key, Object value) {
        this.contextData.put(key, value);
    }
}
