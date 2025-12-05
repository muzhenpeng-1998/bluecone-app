package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.core.user.domain.account.PointsLedger;
import com.bluecone.app.core.user.domain.repository.PointsLedgerRepository;
import com.bluecone.app.infra.user.dataobject.MemberPointsLedgerDO;
import com.bluecone.app.infra.user.mapper.MemberPointsLedgerMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 积分流水仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class PointsLedgerRepositoryImpl implements PointsLedgerRepository {

    private final MemberPointsLedgerMapper memberPointsLedgerMapper;

    @Override
    public Optional<PointsLedger> findByBiz(Long tenantId, String bizType, String bizId) {
        MemberPointsLedgerDO dataObject = memberPointsLedgerMapper.selectOne(new LambdaQueryWrapper<MemberPointsLedgerDO>()
                .eq(MemberPointsLedgerDO::getTenantId, tenantId)
                .eq(MemberPointsLedgerDO::getBizType, bizType)
                .eq(MemberPointsLedgerDO::getBizId, bizId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<PointsLedger> listByTenantAndMember(Long tenantId, Long memberId, int pageNo, int pageSize) {
        Page<MemberPointsLedgerDO> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<MemberPointsLedgerDO> wrapper = new LambdaQueryWrapper<MemberPointsLedgerDO>()
                .eq(MemberPointsLedgerDO::getTenantId, tenantId)
                .eq(MemberPointsLedgerDO::getMemberId, memberId)
                .orderByDesc(MemberPointsLedgerDO::getOccurredAt);
        memberPointsLedgerMapper.selectPage(page, wrapper);
        return page.getRecords().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public PointsLedger save(PointsLedger ledger) {
        MemberPointsLedgerDO dataObject = toDO(ledger);
        memberPointsLedgerMapper.insert(dataObject);
        ledger.setId(dataObject.getId());
        return ledger;
    }

    private PointsLedger toDomain(MemberPointsLedgerDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        PointsLedger ledger = new PointsLedger();
        ledger.setId(dataObject.getId());
        ledger.setTenantId(dataObject.getTenantId());
        ledger.setMemberId(dataObject.getMemberId());
        ledger.setBizType(dataObject.getBizType());
        ledger.setBizId(dataObject.getBizId());
        ledger.setChangePoints(dataObject.getChangePoints());
        ledger.setBalanceAfter(dataObject.getBalanceAfter());
        ledger.setRemark(dataObject.getRemark());
        ledger.setOccurredAt(dataObject.getOccurredAt());
        ledger.setCreatedAt(dataObject.getCreatedAt());
        return ledger;
    }

    private MemberPointsLedgerDO toDO(PointsLedger ledger) {
        if (ledger == null) {
            return null;
        }
        MemberPointsLedgerDO dataObject = new MemberPointsLedgerDO();
        dataObject.setId(ledger.getId());
        dataObject.setTenantId(ledger.getTenantId());
        dataObject.setMemberId(ledger.getMemberId());
        dataObject.setBizType(ledger.getBizType());
        dataObject.setBizId(ledger.getBizId());
        dataObject.setChangePoints(ledger.getChangePoints());
        dataObject.setBalanceAfter(ledger.getBalanceAfter());
        dataObject.setRemark(ledger.getRemark());
        dataObject.setOccurredAt(ledger.getOccurredAt());
        dataObject.setCreatedAt(ledger.getCreatedAt());
        return dataObject;
    }
}
