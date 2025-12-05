package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.repository.MemberLevelRepository;
import com.bluecone.app.infra.user.dataobject.MemberLevelDO;
import com.bluecone.app.infra.user.mapper.MemberLevelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会员等级仓储实现，基于表 bc_member_level。
 */
@Repository
@RequiredArgsConstructor
public class MemberLevelRepositoryImpl implements MemberLevelRepository {

    private final MemberLevelMapper memberLevelMapper;

    @Override
    public Optional<MemberLevel> findById(Long id) {
        return Optional.ofNullable(toDomain(memberLevelMapper.selectById(id)));
    }

    @Override
    public Optional<MemberLevel> findByTenantAndCode(Long tenantId, String levelCode) {
        MemberLevelDO dataObject = memberLevelMapper.selectOne(new LambdaQueryWrapper<MemberLevelDO>()
                .eq(MemberLevelDO::getTenantId, tenantId)
                .eq(MemberLevelDO::getLevelCode, levelCode));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<MemberLevel> findByTenant(Long tenantId) {
        List<MemberLevelDO> dataObjects = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevelDO>()
                .eq(MemberLevelDO::getTenantId, tenantId)
                .orderByAsc(MemberLevelDO::getSortOrder, MemberLevelDO::getMinGrowth));
        return dataObjects.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<MemberLevel> findEnabledByTenant(Long tenantId) {
        List<MemberLevelDO> dataObjects = memberLevelMapper.selectList(new LambdaQueryWrapper<MemberLevelDO>()
                .eq(MemberLevelDO::getTenantId, tenantId)
                .eq(MemberLevelDO::getStatus, 1)
                .orderByAsc(MemberLevelDO::getSortOrder, MemberLevelDO::getMinGrowth));
        return dataObjects.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public MemberLevel save(MemberLevel level) {
        MemberLevelDO dataObject = toDO(level);
        if (level.getId() == null) {
            memberLevelMapper.insert(dataObject);
            level.setId(dataObject.getId());
        } else {
            memberLevelMapper.updateById(dataObject);
        }
        return level;
    }

    private MemberLevel toDomain(MemberLevelDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        MemberLevel level = new MemberLevel();
        level.setId(dataObject.getId());
        level.setTenantId(dataObject.getTenantId());
        level.setLevelCode(dataObject.getLevelCode());
        level.setLevelName(dataObject.getLevelName());
        level.setMinGrowth(dataObject.getMinGrowth() != null ? dataObject.getMinGrowth() : 0);
        level.setMaxGrowth(dataObject.getMaxGrowth() != null ? dataObject.getMaxGrowth() : 0);
        level.setBenefitsJson(dataObject.getBenefitsJson());
        level.setSortOrder(dataObject.getSortOrder() != null ? dataObject.getSortOrder() : 0);
        level.setStatus(dataObject.getStatus() != null ? dataObject.getStatus() : 0);
        level.setCreatedAt(dataObject.getCreatedAt());
        level.setUpdatedAt(dataObject.getUpdatedAt());
        return level;
    }

    private MemberLevelDO toDO(MemberLevel level) {
        if (level == null) {
            return null;
        }
        MemberLevelDO dataObject = new MemberLevelDO();
        dataObject.setId(level.getId());
        dataObject.setTenantId(level.getTenantId());
        dataObject.setLevelCode(level.getLevelCode());
        dataObject.setLevelName(level.getLevelName());
        dataObject.setMinGrowth(level.getMinGrowth());
        dataObject.setMaxGrowth(level.getMaxGrowth());
        dataObject.setBenefitsJson(level.getBenefitsJson());
        dataObject.setSortOrder(level.getSortOrder());
        dataObject.setStatus(level.getStatus());
        dataObject.setCreatedAt(level.getCreatedAt());
        dataObject.setUpdatedAt(level.getUpdatedAt());
        return dataObject;
    }
}
