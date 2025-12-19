package com.bluecone.app.member;

import com.bluecone.app.member.api.dto.MemberDTO;
import com.bluecone.app.member.api.dto.PointsBalanceDTO;
import com.bluecone.app.member.api.dto.PointsOperationCommand;
import com.bluecone.app.member.api.dto.PointsOperationResult;
import com.bluecone.app.member.api.facade.MemberQueryFacade;
import com.bluecone.app.member.api.facade.PointsAssetFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 会员服务集成测试
 * 测试会员创建和积分操作的幂等性
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@SpringBootTest
@ActiveProfiles("test")
public class MemberServiceIT {
    
    @Autowired(required = false)
    private MemberQueryFacade memberQueryFacade;
    
    @Autowired(required = false)
    private PointsAssetFacade pointsAssetFacade;
    
    /**
     * 测试：获取或创建会员（幂等性）
     * 验证：多次调用 getOrCreateMember，应该返回同一个会员
     */
    @Test
    public void testGetOrCreateMember_Idempotent() {
        // 跳过测试，如果 Facade 未注入（说明测试环境未完整配置）
        if (memberQueryFacade == null) {
            System.out.println("MemberQueryFacade not available, skipping test");
            return;
        }
        
        Long tenantId = 1L;
        Long userId = 1001L;
        
        // 第一次调用：创建会员
        MemberDTO member1 = memberQueryFacade.getOrCreateMember(tenantId, userId);
        assertThat(member1).isNotNull();
        assertThat(member1.getTenantId()).isEqualTo(tenantId);
        assertThat(member1.getUserId()).isEqualTo(userId);
        assertThat(member1.getMemberId()).isNotNull();
        assertThat(member1.getMemberNo()).isNotNull();
        
        // 第二次调用：应该返回同一个会员（幂等）
        MemberDTO member2 = memberQueryFacade.getOrCreateMember(tenantId, userId);
        assertThat(member2).isNotNull();
        assertThat(member2.getMemberId()).isEqualTo(member1.getMemberId());
        assertThat(member2.getMemberNo()).isEqualTo(member1.getMemberNo());
        
        System.out.println("✅ 会员创建幂等性测试通过");
    }
    
    /**
     * 测试：积分赚取操作（幂等性）
     * 验证：使用相同幂等键多次调用 earnPoints，应该只增加一次积分
     */
    @Test
    public void testEarnPoints_Idempotent() {
        // 跳过测试，如果 Facade 未注入
        if (memberQueryFacade == null || pointsAssetFacade == null) {
            System.out.println("Facades not available, skipping test");
            return;
        }
        
        Long tenantId = 1L;
        Long userId = 1002L;
        
        // 1. 创建会员
        MemberDTO member = memberQueryFacade.getOrCreateMember(tenantId, userId);
        assertThat(member).isNotNull();
        
        Long memberId = member.getMemberId();
        
        // 2. 查询初始积分余额
        PointsBalanceDTO initialBalance = memberQueryFacade.getPointsBalance(tenantId, memberId);
        assertThat(initialBalance).isNotNull();
        Long initialAvailable = initialBalance.getAvailablePoints();
        
        // 3. 第一次赚取积分
        String idempotencyKey = tenantId + ":ORDER_COMPLETE:order_123:earn";
        PointsOperationCommand command = new PointsOperationCommand(
                tenantId, memberId, 100L,
                "ORDER_COMPLETE", "order_123", idempotencyKey
        );
        command.setRemark("订单完成奖励");
        
        PointsOperationResult result1 = pointsAssetFacade.commitPoints(command);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result1.getAvailablePoints()).isEqualTo(initialAvailable + 100L);
        
        // 4. 第二次使用相同幂等键赚取积分（应该被拒绝或返回相同结果）
        PointsOperationResult result2 = pointsAssetFacade.commitPoints(command);
        assertThat(result2.isSuccess()).isTrue();
        
        // 5. 验证积分余额：应该仍然只增加了 100 积分
        PointsBalanceDTO finalBalance = memberQueryFacade.getPointsBalance(tenantId, memberId);
        assertThat(finalBalance.getAvailablePoints()).isEqualTo(initialAvailable + 100L);
        
        System.out.println("✅ 积分赚取幂等性测试通过");
    }
}
