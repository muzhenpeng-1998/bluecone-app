package com.bluecone.app.core.user.domain.account.service;

import com.bluecone.app.core.user.domain.account.BalanceAccount;
import com.bluecone.app.core.user.domain.account.BalanceChangeResult;
import com.bluecone.app.core.user.domain.account.PointsAccount;
import com.bluecone.app.core.user.domain.account.PointsChangeResult;
import com.bluecone.app.core.user.domain.account.PointsLedger;
import com.bluecone.app.core.user.domain.account.service.impl.AccountDomainServiceImpl;
import com.bluecone.app.core.user.domain.member.service.GrowthDomainService;
import com.bluecone.app.core.user.domain.repository.BalanceAccountRepository;
import com.bluecone.app.core.user.domain.repository.BalanceLedgerRepository;
import com.bluecone.app.core.user.domain.repository.PointsAccountRepository;
import com.bluecone.app.core.user.domain.repository.PointsLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountDomainServiceImplTest {

    @Mock
    private PointsAccountRepository pointsAccountRepository;
    @Mock
    private PointsLedgerRepository pointsLedgerRepository;
    @Mock
    private BalanceAccountRepository balanceAccountRepository;
    @Mock
    private BalanceLedgerRepository balanceLedgerRepository;
    @Mock
    private GrowthDomainService growthDomainService;

    @InjectMocks
    private AccountDomainServiceImpl accountDomainService;

    private PointsAccount pointsAccount;

    @BeforeEach
    void setUp() {
        pointsAccount = PointsAccount.initFor(100L, 10L);
        pointsAccount.setId(1L);
    }

    @Test
    void changePointsShouldIncreaseBalanceAndCallGrowth() {
        when(pointsLedgerRepository.findByBiz(any(), any(), any())).thenReturn(Optional.empty());
        when(pointsAccountRepository.findByTenantAndMember(100L, 10L)).thenReturn(Optional.of(pointsAccount));
        when(pointsAccountRepository.saveWithVersion(any(), eq(0L))).thenReturn(true);

        PointsChangeResult result = accountDomainService.changePoints(100L, 10L, 20, "BIZ", "ID1", "remark");

        assertThat(result.isSuccess()).isTrue();
        verify(pointsLedgerRepository).save(any(PointsLedger.class));
        verify(growthDomainService).increaseGrowthAndCheckLevel(100L, 10L, 20, "BIZ", "ID1");
    }

    @Test
    void changePointsIdempotentShouldSkip() {
        PointsLedger ledger = new PointsLedger();
        ledger.setBalanceAfter(50);
        when(pointsLedgerRepository.findByBiz(any(), any(), any())).thenReturn(Optional.of(ledger));

        PointsChangeResult result = accountDomainService.changePoints(100L, 10L, 20, "BIZ", "ID1", "remark");

        assertThat(result.isSuccess()).isTrue();
        verify(pointsAccountRepository, never()).saveWithVersion(any(), anyLong());
        verify(growthDomainService, never()).increaseGrowthAndCheckLevel(any(), any(), anyInt(), any(), any());
    }

    @Test
    void changePointsShouldFailWhenInsufficient() {
        pointsAccount.setPointsBalance(5);
        when(pointsLedgerRepository.findByBiz(any(), any(), any())).thenReturn(Optional.empty());
        when(pointsAccountRepository.findByTenantAndMember(100L, 10L)).thenReturn(Optional.of(pointsAccount));

        PointsChangeResult result = accountDomainService.changePoints(100L, 10L, -10, "BIZ", "ID1", "remark");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailReason()).contains("积分余额不足");
        verify(pointsLedgerRepository, never()).save(any());
    }

    @Test
    void changeBalanceShouldHandlePositiveDelta() {
        BalanceAccount balance = BalanceAccount.initFor(100L, 10L);
        balance.setId(2L);
        when(balanceLedgerRepository.findByBiz(any(), any(), any())).thenReturn(Optional.empty());
        when(balanceAccountRepository.findByTenantAndMember(100L, 10L)).thenReturn(Optional.of(balance));
        when(balanceAccountRepository.saveWithVersion(any(), eq(0L))).thenReturn(true);

        BalanceChangeResult result = accountDomainService.changeBalance(100L, 10L, BigDecimal.TEN, "RECHARGE", "BIZ1", "remark");

        assertThat(result.isSuccess()).isTrue();
        verify(balanceLedgerRepository).save(any());
        verify(growthDomainService).increaseGrowthAndCheckLevel(100L, 10L, 10, "RECHARGE", "BIZ1");
    }

    @Test
    void changeBalanceShouldFailOnInsufficient() {
        BalanceAccount balance = BalanceAccount.initFor(100L, 10L);
        when(balanceLedgerRepository.findByBiz(any(), any(), any())).thenReturn(Optional.empty());
        when(balanceAccountRepository.findByTenantAndMember(100L, 10L)).thenReturn(Optional.of(balance));

        BalanceChangeResult result = accountDomainService.changeBalance(100L, 10L, BigDecimal.valueOf(-5), "CONSUME", "BIZ1", "remark");

        assertThat(result.isSuccess()).isFalse();
        verify(balanceLedgerRepository, never()).save(any());
    }
}
