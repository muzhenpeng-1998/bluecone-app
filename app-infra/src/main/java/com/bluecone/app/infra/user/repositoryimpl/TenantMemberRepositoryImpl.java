package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.member.MemberStatus;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.repository.TenantMemberRepository;
import com.bluecone.app.infra.user.dataobject.TenantMemberDO;
import com.bluecone.app.infra.user.mapper.TenantMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会员关系仓储实现，访问表 bc_tenant_member。
 */
@Repository
@RequiredArgsConstructor
public class TenantMemberRepositoryImpl implements TenantMemberRepository {

    private final TenantMemberMapper tenantMemberMapper;

    @Override
    public Optional<TenantMember> findById(Long id) {
        return Optional.ofNullable(toDomain(tenantMemberMapper.selectById(id)));
    }

    @Override
    public Optional<TenantMember> findByTenantAndUser(Long tenantId, Long userId) {
        TenantMemberDO dataObject = tenantMemberMapper.selectOne(new LambdaQueryWrapper<TenantMemberDO>()
                .eq(TenantMemberDO::getTenantId, tenantId)
                .eq(TenantMemberDO::getUserId, userId));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public TenantMember save(TenantMember member) {
        TenantMemberDO dataObject = toDO(member);
        if (member.getId() == null) {
            tenantMemberMapper.insert(dataObject);
            member.setId(dataObject.getId());
        } else {
            tenantMemberMapper.updateById(dataObject);
        }
        return member;
    }

    private TenantMember toDomain(TenantMemberDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        TenantMember member = new TenantMember();
        member.setId(dataObject.getId());
        member.setTenantId(dataObject.getTenantId());
        member.setUserId(dataObject.getUserId());
        member.setMemberNo(dataObject.getMemberNo());
        member.setStatus(parseStatus(dataObject.getStatus()));
        member.setJoinChannel(dataObject.getJoinChannel());
        member.setJoinAt(dataObject.getJoinAt());
        member.setLevelId(dataObject.getLevelId());
        member.setGrowthValue(dataObject.getGrowthValue() != null ? dataObject.getGrowthValue() : 0);
        member.setRemark(dataObject.getRemark());
        member.setCreatedAt(dataObject.getCreatedAt());
        member.setUpdatedAt(dataObject.getUpdatedAt());
        return member;
    }

    private TenantMemberDO toDO(TenantMember member) {
        if (member == null) {
            return null;
        }
        TenantMemberDO dataObject = new TenantMemberDO();
        dataObject.setId(member.getId());
        dataObject.setTenantId(member.getTenantId());
        dataObject.setUserId(member.getUserId());
        dataObject.setMemberNo(member.getMemberNo());
        dataObject.setStatus(toStatusValue(member.getStatus()));
        dataObject.setJoinChannel(member.getJoinChannel());
        dataObject.setJoinAt(member.getJoinAt());
        dataObject.setLevelId(member.getLevelId());
        dataObject.setGrowthValue(member.getGrowthValue());
        dataObject.setRemark(member.getRemark());
        dataObject.setCreatedAt(member.getCreatedAt());
        dataObject.setUpdatedAt(member.getUpdatedAt());
        return dataObject;
    }

    private MemberStatus parseStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> MemberStatus.ACTIVE;
            case 0 -> MemberStatus.FROZEN;
            case -1 -> MemberStatus.QUIT;
            default -> null;
        };
    }

    private Integer toStatusValue(MemberStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> 1;
            case FROZEN -> 0;
            case QUIT -> -1;
        };
    }
}
