package com.bluecone.app.pricing.api.facade;

import com.bluecone.app.pricing.api.dto.PricingQuote;
import com.bluecone.app.pricing.api.dto.PricingRequest;

/**
 * 统一计价引擎门面接口
 * 
 * <p>职责：
 * <ul>
 *   <li>接收计价请求，返回计价报价单</li>
 *   <li>计算商品基价、会员价、活动折扣、优惠券、积分抵扣、配送费、打包费、抹零</li>
 *   <li>输出可落库的 PricingQuote 快照，包含完整的明细行（breakdownLines）</li>
 *   <li>确保计价过程是"纯计算"，不锁券/冻结积分/冻结余额</li>
 * </ul>
 * 
 * <p>约束：
 * <ul>
 *   <li>计价阶段只做查询和可用性判断，不做资源锁定</li>
 *   <li>同样的输入必须产生同样的输出（确定性）</li>
 *   <li>输出的 PricingQuote 必须包含 pricingVersion 和 breakdownLines</li>
 * </ul>
 * 
 * @author bluecone
 * @since 2025-12-19
 */
public interface PricingFacade {
    
    /**
     * 计算订单价格
     * 
     * <p>计价流程：
     * <ol>
     *   <li>Stage1: 计算商品基价（basePrice + specSurcharge）</li>
     *   <li>Stage2: 应用会员价/时段价（预留接口，暂不改价）</li>
     *   <li>Stage3: 应用活动折扣（预留接口，暂不改价）</li>
     *   <li>Stage4: 应用优惠券抵扣（调用 CouponQueryFacade 校验与预估）</li>
     *   <li>Stage5: 应用积分抵扣（调用 PointsQuery 校验与折算）</li>
     *   <li>Stage6: 计算配送费和打包费</li>
     *   <li>Stage7: 应用抹零规则</li>
     * </ol>
     * 
     * @param request 计价请求
     * @return 计价报价单（包含完整明细和版本号）
     */
    PricingQuote quote(PricingRequest request);
}
