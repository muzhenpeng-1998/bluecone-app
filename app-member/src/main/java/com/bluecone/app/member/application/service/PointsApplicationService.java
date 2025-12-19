package com.bluecone.app.member.application.service;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.member.domain.model.PointsAccount;
import com.bluecone.app.member.domain.model.PointsLedger;
import com.bluecone.app.member.domain.repository.PointsAccountRepository;
import com.bluecone.app.member.domain.service.PointsDomainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 积分应用服务
 * 负责积分查询和操作的应用层编排
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Service("pointsLedgerApplicationService")
public class PointsApplicationService {
    
    private static final Logger log = LoggerFactory.getLogger(PointsApplicationService.class);
    
    private final PointsAccountRepository accountRepository;
    private final PointsDomainService pointsDomainService;
    private final IdService idService;
    
    public PointsApplicationService(PointsAccountRepository accountRepository,
                                   PointsDomainService pointsDomainService,
                                   IdService idService) {
        this.accountRepository = accountRepository;
        this.pointsDomainService = pointsDomainService;
        this.idService = idService;
    }
    
    /**
     * 查询积分余额
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @return 积分账户（包含可用积分和冻结积分）
     */
    public PointsAccount getPointsBalance(Long tenantId, Long memberId) {
        Optional<PointsAccount> account = accountRepository.findByMemberId(tenantId, memberId);
        if (account.isEmpty()) {
            log.warn("积分账户不存在，租户ID：{}，会员ID：{}", tenantId, memberId);
            return null;
        }
        return account.get();
    }
    
    /**
     * 赚取积分
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    public PointsLedger earnPoints(Long tenantId, Long memberId, Long points,
                                   String bizType, String bizId, String idempotencyKey, String remark) {
        Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
        return pointsDomainService.earnPoints(ledgerId, tenantId, memberId, points, 
                bizType, bizId, idempotencyKey, remark);
    }
    
    /**
     * 回退积分（退款返还）
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    public PointsLedger revertPoints(Long tenantId, Long memberId, Long points,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
        return pointsDomainService.revertPoints(ledgerId, tenantId, memberId, points, 
                bizType, bizId, idempotencyKey, remark);
    }
    
    /**
     * 冻结积分
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    public PointsLedger freezePoints(Long tenantId, Long memberId, Long points,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
        return pointsDomainService.freezePoints(ledgerId, tenantId, memberId, points, 
                bizType, bizId, idempotencyKey, remark);
    }
    
    /**
     * 释放冻结积分
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    public PointsLedger releasePoints(Long tenantId, Long memberId, Long points,
                                      String bizType, String bizId, String idempotencyKey, String remark) {
        Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
        return pointsDomainService.releasePoints(ledgerId, tenantId, memberId, points, 
                bizType, bizId, idempotencyKey, remark);
    }
    
    /**
     * 调整积分
     * 
     * @param tenantId 租户ID
     * @param memberId 会员ID
     * @param points 积分值
     * @param isIncrease true=增加，false=减少
     * @param bizType 业务类型
     * @param bizId 业务ID
     * @param idempotencyKey 幂等键
     * @param remark 备注
     * @return 流水记录
     */
    public PointsLedger adjustPoints(Long tenantId, Long memberId, Long points, boolean isIncrease,
                                     String bizType, String bizId, String idempotencyKey, String remark) {
        Long ledgerId = idService.nextLong(IdScope.POINTS_LEDGER);
        return pointsDomainService.adjustPoints(ledgerId, tenantId, memberId, points, isIncrease, 
                bizType, bizId, idempotencyKey, remark);
    }
}
