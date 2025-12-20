package com.bluecone.app.user.application.account;

import com.bluecone.app.core.user.domain.account.BalanceChangeResult;
import com.bluecone.app.core.user.domain.account.BalanceLedger;
import com.bluecone.app.core.user.domain.account.PointsAccount;
import com.bluecone.app.core.user.domain.account.PointsChangeResult;
import com.bluecone.app.core.user.domain.account.PointsLedger;
import com.bluecone.app.core.user.domain.account.service.AccountDomainService;
import com.bluecone.app.core.user.domain.repository.BalanceAccountRepository;
import com.bluecone.app.core.user.domain.repository.BalanceLedgerRepository;
import com.bluecone.app.core.user.domain.repository.CouponRepository;
import com.bluecone.app.core.user.domain.repository.PointsAccountRepository;
import com.bluecone.app.core.user.domain.repository.PointsLedgerRepository;
import com.bluecone.app.core.context.CurrentUserContext;
import com.bluecone.app.user.dto.account.AccountSummaryDTO;
import com.bluecone.app.user.dto.account.AdjustBalanceCommand;
import com.bluecone.app.user.dto.account.AdjustPointsCommand;
import com.bluecone.app.user.dto.account.BalanceLedgerItemDTO;
import com.bluecone.app.user.dto.account.PointsLedgerItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 账户相关应用服务，负责积分、储值、优惠券的查询。
 */
@Service
@RequiredArgsConstructor
public class AccountApplicationService {

    private final AccountDomainService accountDomainService;
    private final PointsAccountRepository pointsAccountRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final PointsLedgerRepository pointsLedgerRepository;
    private final BalanceLedgerRepository balanceLedgerRepository;
    private final CouponRepository couponRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 查询当前会员的账户汇总。
     */
    public AccountSummaryDTO getCurrentAccountSummary() {
        Long tenantId = currentUserContext.getCurrentTenantId();
        Long memberId = currentUserContext.getCurrentMemberIdOrNull();
        PointsAccount pointsAccount = pointsAccountRepository.findByTenantAndMember(tenantId, memberId)
                .orElse(PointsAccount.initFor(tenantId, memberId));
        var balanceAccount = balanceAccountRepository.findByTenantAndMember(tenantId, memberId)
                .orElse(com.bluecone.app.core.user.domain.account.BalanceAccount.initFor(tenantId, memberId));
        int couponCount = couponRepository.findByMember(tenantId, memberId).size();
        return AccountSummaryDTO.builder()
                .tenantId(tenantId)
                .memberId(memberId)
                .pointsBalance(pointsAccount.getPointsBalance())
                .frozenPoints(pointsAccount.getFrozenPoints())
                .balanceAvailable(balanceAccount.getAvailableAmount())
                .balanceFrozen(balanceAccount.getFrozenAmount())
                .availableCouponCount(couponCount)
                .build();
    }

    /**
     * 积分流水列表（分页）。
     */
    public List<PointsLedgerItemDTO> listPointsLedger(Long tenantId, Long memberId, int pageNo, int pageSize) {
        Long resolvedTenantId = tenantId != null ? tenantId : currentUserContext.getCurrentTenantId();
        Long resolvedMemberId = memberId != null ? memberId : currentUserContext.getCurrentMemberIdOrNull();
        List<PointsLedger> ledgers = pointsLedgerRepository.listByTenantAndMember(resolvedTenantId, resolvedMemberId, pageNo, pageSize);
        return ledgers.stream().map(this::toPointsDTO).collect(Collectors.toList());
    }

    /**
     * 储值流水列表（分页）。
     */
    public List<BalanceLedgerItemDTO> listBalanceLedger(Long tenantId, Long memberId, int pageNo, int pageSize) {
        Long resolvedTenantId = tenantId != null ? tenantId : currentUserContext.getCurrentTenantId();
        Long resolvedMemberId = memberId != null ? memberId : currentUserContext.getCurrentMemberIdOrNull();
        List<BalanceLedger> ledgers = balanceLedgerRepository.listByTenantAndMember(resolvedTenantId, resolvedMemberId, pageNo, pageSize);
        return ledgers.stream().map(this::toBalanceDTO).collect(Collectors.toList());
    }

    /**
     * 管理端调节积分。
     */
    public PointsChangeResult adjustPoints(AdjustPointsCommand cmd) {
        return accountDomainService.changePoints(cmd.getTenantId(), cmd.getMemberId(), cmd.getDelta(), cmd.getBizType(), cmd.getBizId(), cmd.getRemark());
    }

    /**
     * 管理端调节储值。
     */
    public BalanceChangeResult adjustBalance(AdjustBalanceCommand cmd) {
        BigDecimal delta = cmd.getDelta() != null ? cmd.getDelta() : BigDecimal.ZERO;
        return accountDomainService.changeBalance(cmd.getTenantId(), cmd.getMemberId(), delta, cmd.getBizType(), cmd.getBizId(), cmd.getRemark());
    }

    private PointsLedgerItemDTO toPointsDTO(PointsLedger ledger) {
        PointsLedgerItemDTO dto = new PointsLedgerItemDTO();
        dto.setId(ledger.getId());
        dto.setBizType(ledger.getBizType());
        dto.setBizId(ledger.getBizId());
        dto.setChangePoints(ledger.getChangePoints());
        dto.setBalanceAfter(ledger.getBalanceAfter());
        dto.setRemark(ledger.getRemark());
        dto.setOccurredAt(ledger.getOccurredAt() != null ? ledger.getOccurredAt().toString() : null);
        return dto;
    }

    private BalanceLedgerItemDTO toBalanceDTO(BalanceLedger ledger) {
        BalanceLedgerItemDTO dto = new BalanceLedgerItemDTO();
        dto.setId(ledger.getId());
        dto.setBizType(ledger.getBizType());
        dto.setBizId(ledger.getBizId());
        dto.setChangeAmount(ledger.getChangeAmount());
        dto.setBalanceAfter(ledger.getBalanceAfter());
        dto.setRemark(ledger.getRemark());
        dto.setOccurredAt(ledger.getOccurredAt() != null ? ledger.getOccurredAt().toString() : null);
        return dto;
    }
}
