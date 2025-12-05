package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.member.MemberTag;
import com.bluecone.app.core.user.domain.repository.MemberTagRelationRepository;
import com.bluecone.app.infra.user.dataobject.MemberTagDO;
import com.bluecone.app.infra.user.dataobject.MemberTagRelationDO;
import com.bluecone.app.infra.user.mapper.MemberTagMapper;
import com.bluecone.app.infra.user.mapper.MemberTagRelationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员标签关联仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class MemberTagRelationRepositoryImpl implements MemberTagRelationRepository {

    private final MemberTagRelationMapper memberTagRelationMapper;
    private final MemberTagMapper memberTagMapper;

    @Override
    public void addTagToMember(Long tenantId, Long memberId, Long tagId) {
        MemberTagRelationDO existing = memberTagRelationMapper.selectOne(new LambdaQueryWrapper<MemberTagRelationDO>()
                .eq(MemberTagRelationDO::getTenantId, tenantId)
                .eq(MemberTagRelationDO::getMemberId, memberId)
                .eq(MemberTagRelationDO::getTagId, tagId));
        if (existing != null) {
            return;
        }
        MemberTagRelationDO relation = new MemberTagRelationDO();
        relation.setTenantId(tenantId);
        relation.setMemberId(memberId);
        relation.setTagId(tagId);
        relation.setCreatedAt(LocalDateTime.now());
        memberTagRelationMapper.insert(relation);
    }

    @Override
    public void removeTagFromMember(Long tenantId, Long memberId, Long tagId) {
        memberTagRelationMapper.delete(new LambdaQueryWrapper<MemberTagRelationDO>()
                .eq(MemberTagRelationDO::getTenantId, tenantId)
                .eq(MemberTagRelationDO::getMemberId, memberId)
                .eq(MemberTagRelationDO::getTagId, tagId));
    }

    @Override
    public List<MemberTag> findTagsByMember(Long tenantId, Long memberId) {
        List<MemberTagRelationDO> relations = memberTagRelationMapper.selectList(new LambdaQueryWrapper<MemberTagRelationDO>()
                .eq(MemberTagRelationDO::getTenantId, tenantId)
                .eq(MemberTagRelationDO::getMemberId, memberId));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> tagIds = relations.stream().map(MemberTagRelationDO::getTagId).toList();
        List<MemberTagDO> tagDOs = memberTagMapper.selectBatchIds(tagIds);
        return tagDOs.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<MemberTag>> findTagsByMemberIds(Long tenantId, List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            return Map.of();
        }
        List<MemberTagRelationDO> relations = memberTagRelationMapper.selectList(new LambdaQueryWrapper<MemberTagRelationDO>()
                .eq(MemberTagRelationDO::getTenantId, tenantId)
                .in(MemberTagRelationDO::getMemberId, memberIds));
        if (relations.isEmpty()) {
            return Map.of();
        }
        List<Long> tagIds = relations.stream().map(MemberTagRelationDO::getTagId).distinct().toList();
        Map<Long, MemberTag> tagMap = memberTagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(MemberTagDO::getId, this::toDomain));

        Map<Long, List<MemberTag>> result = new HashMap<>();
        for (MemberTagRelationDO relation : relations) {
            MemberTag tag = tagMap.get(relation.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(relation.getMemberId(), k -> new java.util.ArrayList<>()).add(tag);
        }
        return result;
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
}
