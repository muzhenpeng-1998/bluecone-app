package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.member.MemberTag;
import com.bluecone.app.core.user.domain.repository.MemberTagRepository;
import com.bluecone.app.infra.user.dataobject.MemberTagDO;
import com.bluecone.app.infra.user.mapper.MemberTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 会员标签仓储实现，基于表 bc_member_tag。
 */
@Repository
@RequiredArgsConstructor
public class MemberTagRepositoryImpl implements MemberTagRepository {

    private final MemberTagMapper memberTagMapper;

    @Override
    public Optional<MemberTag> findById(Long id) {
        return Optional.ofNullable(toDomain(memberTagMapper.selectById(id)));
    }

    @Override
    public Optional<MemberTag> findByTenantAndCode(Long tenantId, String tagCode) {
        MemberTagDO dataObject = memberTagMapper.selectOne(new LambdaQueryWrapper<MemberTagDO>()
                .eq(MemberTagDO::getTenantId, tenantId)
                .eq(MemberTagDO::getTagCode, tagCode));
        return Optional.ofNullable(toDomain(dataObject));
    }

    @Override
    public List<MemberTag> findByTenant(Long tenantId) {
        List<MemberTagDO> dataObjects = memberTagMapper.selectList(new LambdaQueryWrapper<MemberTagDO>()
                .eq(MemberTagDO::getTenantId, tenantId));
        return dataObjects.stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public MemberTag save(MemberTag tag) {
        MemberTagDO dataObject = toDO(tag);
        if (tag.getId() == null) {
            memberTagMapper.insert(dataObject);
            tag.setId(dataObject.getId());
        } else {
            memberTagMapper.updateById(dataObject);
        }
        return tag;
    }

    private MemberTag toDomain(MemberTagDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        MemberTag tag = new MemberTag();
        tag.setId(dataObject.getId());
        tag.setTenantId(dataObject.getTenantId());
        tag.setTagCode(dataObject.getTagCode());
        tag.setTagName(dataObject.getTagName());
        tag.setColor(dataObject.getColor());
        tag.setStatus(dataObject.getStatus() != null ? dataObject.getStatus() : 0);
        tag.setCreatedAt(dataObject.getCreatedAt());
        tag.setUpdatedAt(dataObject.getUpdatedAt());
        return tag;
    }

    private MemberTagDO toDO(MemberTag tag) {
        if (tag == null) {
            return null;
        }
        MemberTagDO dataObject = new MemberTagDO();
        dataObject.setId(tag.getId());
        dataObject.setTenantId(tag.getTenantId());
        dataObject.setTagCode(tag.getTagCode());
        dataObject.setTagName(tag.getTagName());
        dataObject.setColor(tag.getColor());
        dataObject.setStatus(tag.getStatus());
        dataObject.setCreatedAt(tag.getCreatedAt());
        dataObject.setUpdatedAt(tag.getUpdatedAt());
        return dataObject;
    }
}
