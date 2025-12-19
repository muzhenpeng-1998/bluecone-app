package com.bluecone.app.order.application;

import com.bluecone.app.member.api.dto.MemberDTO;
import com.bluecone.app.member.api.dto.PointsBalanceDTO;
import com.bluecone.app.member.api.facade.MemberQueryFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 会员积分集成示例
 * 展示如何在订单模块中调用会员服务查询积分余额
 * 
 * 注意：这是一个预留的集成点，仅用于演示跨模块调用
 * 当前不参与订单计价流程，待后续功能扩展时启用
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service
public class MemberPointsIntegrationExample {
    
    private static final Logger log = LoggerFactory.getLogger(MemberPointsIntegrationExample.class);
    
    @Autowired(required = false)
    private MemberQueryFacade memberQueryFacade;
    
    /**
     * 查询用户的会员积分余额（预留接口）
     * 
     * 使用场景举例：
     * 1. 订单预览时展示用户当前可用积分
     * 2. 下单时检查积分抵扣额度
     * 3. 订单完成后展示即将获得的积分奖励
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @return 积分余额，如果会员服务未启用则返回 null
     */
    public PointsBalanceDTO queryUserPointsBalance(Long tenantId, Long userId) {
        // 如果会员服务未启用，直接返回 null
        if (memberQueryFacade == null) {
            log.debug("会员服务未启用，跳过积分查询");
            return null;
        }
        
        try {
            // 1. 先获取或创建会员（幂等操作）
            MemberDTO member = memberQueryFacade.getOrCreateMember(tenantId, userId);
            if (member == null) {
                log.warn("会员创建失败，租户ID：{}，用户ID：{}", tenantId, userId);
                return null;
            }
            
            // 2. 查询积分余额
            PointsBalanceDTO balance = memberQueryFacade.getPointsBalance(tenantId, member.getMemberId());
            if (balance != null) {
                log.info("查询会员积分成功，租户ID：{}，会员ID：{}，可用积分：{}，冻结积分：{}", 
                        tenantId, member.getMemberId(), 
                        balance.getAvailablePoints(), balance.getFrozenPoints());
            }
            
            return balance;
            
        } catch (Exception e) {
            log.error("查询会员积分失败，租户ID：{}，用户ID：{}，错误：{}", tenantId, userId, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 检查是否已启用会员服务
     */
    public boolean isMemberServiceEnabled() {
        return memberQueryFacade != null;
    }
}
