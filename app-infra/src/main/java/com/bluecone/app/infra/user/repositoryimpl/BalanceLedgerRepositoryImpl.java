package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.core.user.domain.account.BalanceLedger;
import com.bluecone.app.core.user.domain.repository.BalanceLedgerRepository;
import com.bluecone.app.infra.user.dataobject.MemberBalanceLedgerDO;
import com.bluecone.app.infra.user.mapper.MemberBalanceLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 储值流水仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class BalanceLedgerRepositoryImpl implements BalanceLedgerRepository {

    private final MemberBalanceLedgerMapper memberBalanceLedgerMapper;

    @Override
    public Optional<BalanceLedger> findByBiz(Long tenantId, String bizType, String bizId) {
        MemberBalanceLedgerDO dataObject = memberBalanceLedgerMapper.selectOne(new LambdaQueryWrapper<MemberBalanceLedgerDO>()
                .eq(MemberBalanceLedgerDO::getTenantId, tenantId)
                .eq(MemberBalanceLedgerDO::getBizType, bizType)
                .eq(MemberBalanceLedgerDO::getBizId, bizId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<BalanceLedger> listByTenantAndMember(Long tenantId, Long memberId, int pageNo, int pageSize) {
        Page<MemberBalanceLedgerDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<MemberBalanceLedgerDO> wrapper = new LambdaQueryWrapper<MemberBalanceLedgerDO>()
                .eq(MemberBalanceLedgerDO::getTenantId, tenantId)
                .eq(MemberBalanceLedgerDO::getMemberId, memberId)
                .orderByDesc(MemberBalanceLedgerDO::getOccurredAt);
        memberBalanceLedgerMapper.selectPage(page, wrapper);
        return page.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public BalanceLedger save(BalanceLedger ledger) {
        MemberBalanceLedgerDO dataObject = toDO(ledger);
        memberBalanceLedgerMapper.insert(dataObject);
        ledger.setId(dataObject.getId());
        return ledger;
    }

    private BalanceLedger toDomain(MemberBalanceLedgerDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        BalanceLedger ledger = new BalanceLedger();
        ledger.setId(dataObject.getId());
        ledger.setTenantId(dataObject.getTenantId());
        ledger.setMemberId(dataObject.getMemberId());
        ledger.setBizType(dataObject.getBizType());
        ledger.setBizId(dataObject.getBizId());
        ledger.setChangeAmount(dataObject.getChangeAmount());
        ledger.setBalanceAfter(dataObject.getBalanceAfter());
        ledger.setRemark(dataObject.getRemark());
        ledger.setOccurredAt(dataObject.getOccurredAt());
        ledger.setCreatedAt(dataObject.getCreatedAt());
        return ledger;
    }

    private MemberBalanceLedgerDO toDO(BalanceLedger ledger) {
        if (ledger == null) {
            return null;
        }
        MemberBalanceLedgerDO dataObject = new MemberBalanceLedgerDO();
        dataObject.setId(ledger.getId());
        dataObject.setTenantId(ledger.getTenantId());
        dataObject.setMemberId(ledger.getMemberId());
        dataObject.setBizType(ledger.getBizType());
        dataObject.setBizId(ledger.getBizId());
        dataObject.setChangeAmount(ledger.getChangeAmount());
        dataObject.setBalanceAfter(ledger.getBalanceAfter());
        dataObject.setRemark(ledger.getRemark());
        dataObject.setOccurredAt(ledger.getOccurredAt());
        dataObject.setCreatedAt(ledger.getCreatedAt());
        return dataObject;
    }
}
