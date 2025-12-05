package com.bluecone.app.infra.user.query;

import com.bluecone.app.core.user.domain.member.MemberTag;
import com.bluecone.app.core.user.domain.member.repository.read.MemberListView;
import com.bluecone.app.core.user.domain.member.repository.read.MemberReadRepository;
import com.bluecone.app.core.user.domain.member.repository.read.MemberSearchQuery;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
import com.bluecone.app.core.user.domain.repository.MemberTagRelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 会员读模型仓储实现。
 */
@Repository
@RequiredArgsConstructor
public class MemberReadRepositoryImpl implements MemberReadRepository {

    private final MemberReadMapper memberReadMapper;
    private final MemberTagRelationRepository memberTagRelationRepository;

    @Override
    public PageResult<MemberListView> searchMembers(MemberSearchQuery query) {
        int pageNo = query.getPageNo() <= 0 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() <= 0 ? 20 : query.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        List<MemberListViewDO> rows = memberReadMapper.selectMemberList(
                query.getTenantId(),
                query.getKeyword(),
                query.getLevelId(),
                query.getStatus(),
                query.getMinGrowth(),
                query.getMaxGrowth(),
                query.getJoinStart(),
                query.getJoinEnd(),
                offset,
                pageSize
        );
        long total = memberReadMapper.countMemberList(
                query.getTenantId(),
                query.getKeyword(),
                query.getLevelId(),
                query.getStatus(),
                query.getMinGrowth(),
                query.getMaxGrowth(),
                query.getJoinStart(),
                query.getJoinEnd()
        );

        List<Long> memberIds = rows.stream().map(MemberListViewDO::getMemberId).toList();
        Map<Long, List<MemberTag>> tagsMap = CollectionUtils.isEmpty(memberIds)
                ? Map.of()
                : memberTagRelationRepository.findTagsByMemberIds(query.getTenantId(), memberIds);

        List<MemberListView> list = new ArrayList<>();
        for (MemberListViewDO row : rows) {
            MemberListView view = new MemberListView();
            view.setMemberId(row.getMemberId());
            view.setTenantId(row.getTenantId());
            view.setUserId(row.getUserId());
            view.setMemberNo(row.getMemberNo());
            view.setStatus(row.getStatus());
            view.setStatusLabel(statusLabel(row.getStatus()));
            view.setNickname(row.getNickname());
            view.setAvatarUrl(row.getAvatarUrl());
            view.setPhoneMasked(maskPhone(row.getPhone()));
            view.setLevelId(row.getLevelId());
            view.setLevelName(row.getLevelName());
            view.setGrowthValue(row.getGrowthValue());
            List<String> tagNames = tagsMap.getOrDefault(row.getMemberId(), List.of()).stream()
                    .map(MemberTag::getTagName)
                    .collect(Collectors.toList());
            view.setTagNames(tagNames);
            view.setJoinAt(row.getJoinAt());
            view.setLastLoginAt(row.getLastLoginAt());
            list.add(view);
        }
        return new PageResult<>(list, total, pageNo, pageSize);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String statusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 1 -> "正常";
            case 0 -> "冻结";
            case -1 -> "注销";
            default -> "未知";
        };
    }
}
