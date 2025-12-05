package com.bluecone.app.user.application.member;

import com.bluecone.app.core.user.domain.member.MemberLevel;
import com.bluecone.app.core.user.domain.member.TenantMember;
import com.bluecone.app.core.user.domain.member.repository.read.MemberListView;
import com.bluecone.app.core.user.domain.member.repository.read.MemberReadRepository;
import com.bluecone.app.core.user.domain.member.repository.read.MemberSearchQuery;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
import com.bluecone.app.core.user.domain.repository.BalanceAccountRepository;
import com.bluecone.app.core.user.domain.repository.CouponRepository;
import com.bluecone.app.core.user.domain.repository.MemberLevelRepository;
import com.bluecone.app.core.user.domain.repository.MemberTagRelationRepository;
import com.bluecone.app.core.user.domain.repository.MemberTagRepository;
import com.bluecone.app.core.user.domain.repository.PointsAccountRepository;
import com.bluecone.app.core.user.domain.repository.TenantMemberRepository;
import com.bluecone.app.core.user.domain.repository.UserIdentityRepository;
import com.bluecone.app.core.user.domain.repository.UserProfileRepository;
import com.bluecone.app.user.application.CurrentUserContext;
import com.bluecone.app.user.dto.member.MemberDetailDTO;
import com.bluecone.app.user.dto.member.MemberListItemDTO;
import com.bluecone.app.user.dto.member.EnrollMemberCommand;
import com.bluecone.app.user.dto.member.MemberLevelDTO;
import com.bluecone.app.user.dto.member.MemberSummaryDTO;
import com.bluecone.app.user.dto.member.MemberSearchQueryDTO;
import com.bluecone.app.user.dto.member.MemberTagCommandDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 会员相关应用服务，用于开卡与查询概要信息。
 */
@Service
@RequiredArgsConstructor
public class MemberApplicationService {

    private final TenantMemberRepository tenantMemberRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final MemberTagRepository memberTagRepository;
    private final PointsAccountRepository pointsAccountRepository;
    private final BalanceAccountRepository balanceAccountRepository;
    private final CouponRepository couponRepository;
    private final MemberTagRelationRepository memberTagRelationRepository;
    private final MemberReadRepository memberReadRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserProfileRepository userProfileRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 查询当前租户下的会员概要。
     */
    public MemberSummaryDTO getCurrentMemberSummary() {
        Long tenantId = currentUserContext.getCurrentTenantId();
        Long userId = currentUserContext.getCurrentUserId();
        Long memberId = currentUserContext.getCurrentMemberIdOrNull();
        TenantMember member = tenantMemberRepository.findByTenantAndUser(tenantId, userId).orElse(null);
        MemberLevel level = null;
        if (member != null && member.getLevelId() != null) {
            level = memberLevelRepository.findById(member.getLevelId()).orElse(null);
            memberId = member.getId();
        }
        return MemberSummaryDTO.builder()
                .tenantId(tenantId)
                .userId(userId)
                .memberId(memberId)
                .memberNo(member != null ? member.getMemberNo() : null)
                .status(member != null ? toStatusCode(member) : null)
                .levelId(member != null ? member.getLevelId() : null)
                .levelName(level != null ? level.getLevelName() : null)
                .growthValue(member != null ? member.getGrowthValue() : 0)
                .build();
    }

    /**
     * 为指定租户开通会员。
     */
    public MemberSummaryDTO enrollMember(EnrollMemberCommand cmd) {
        // TODO: 调用领域聚合完成开卡并返回结果
        return MemberSummaryDTO.builder()
                .tenantId(cmd.getTenantId())
                .userId(cmd.getUserId())
                .build();
    }

    /**
     * 查询租户的会员等级列表。
     */
    public List<MemberLevelDTO> listMemberLevels(Long tenantId) {
        Long resolvedTenantId = tenantId != null ? tenantId : currentUserContext.getCurrentTenantId();
        return memberLevelRepository.findByTenant(resolvedTenantId).stream()
                .map(this::toLevelDTO)
                .collect(Collectors.toList());
    }

    /**
     * 创建或更新会员等级配置。
     */
    public MemberLevelDTO createOrUpdateMemberLevel(MemberLevelDTO dto) {
        MemberLevel level = toDomain(dto);
        memberLevelRepository.save(level);
        return toLevelDTO(level);
    }

    private MemberLevelDTO toLevelDTO(MemberLevel level) {
        MemberLevelDTO dto = new MemberLevelDTO();
        dto.setId(level.getId());
        dto.setTenantId(level.getTenantId());
        dto.setLevelCode(level.getLevelCode());
        dto.setLevelName(level.getLevelName());
        dto.setMinGrowth(level.getMinGrowth());
        dto.setMaxGrowth(level.getMaxGrowth());
        dto.setBenefitsJson(level.getBenefitsJson());
        dto.setSortOrder(level.getSortOrder());
        dto.setStatus(level.getStatus());
        return dto;
    }

    private MemberLevel toDomain(MemberLevelDTO dto) {
        MemberLevel level = new MemberLevel();
        level.setId(dto.getId());
        level.setTenantId(dto.getTenantId() != null ? dto.getTenantId() : currentUserContext.getCurrentTenantId());
        level.setLevelCode(dto.getLevelCode());
        level.setLevelName(dto.getLevelName());
        level.setMinGrowth(dto.getMinGrowth() != null ? dto.getMinGrowth() : 0);
        level.setMaxGrowth(dto.getMaxGrowth() != null ? dto.getMaxGrowth() : Integer.MAX_VALUE);
        level.setBenefitsJson(dto.getBenefitsJson());
        level.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        level.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        return level;
    }

    private Integer toStatusCode(TenantMember member) {
        if (member.getStatus() == null) {
            return null;
        }
        return switch (member.getStatus()) {
            case ACTIVE -> 1;
            case FROZEN -> 0;
            case QUIT -> -1;
        };
    }

    /**
     * 会员列表搜索。
     */
    public PageResult<MemberListItemDTO> searchMembers(MemberSearchQueryDTO queryDTO) {
        MemberSearchQuery query = toSearchQuery(queryDTO);
        PageResult<MemberListView> result = memberReadRepository.searchMembers(query);
        List<MemberListItemDTO> dtoList = result.getList().stream().map(this::toListDTO).collect(Collectors.toList());
        return new PageResult<>(dtoList, result.getTotal(), result.getPageNo(), result.getPageSize());
    }

    /**
     * 会员详情。
     */
    public MemberDetailDTO getMemberDetail(Long memberId) {
        Long tenantId = currentUserContext.getCurrentTenantId();
        TenantMember member = tenantMemberRepository.findById(memberId).orElse(null);
        if (member == null || (tenantId != null && !tenantId.equals(member.getTenantId()))) {
            return null;
        }
        MemberLevel level = member.getLevelId() != null ? memberLevelRepository.findById(member.getLevelId()).orElse(null) : null;
        var identity = userIdentityRepository.findById(member.getUserId()).orElse(null);
        var profile = userProfileRepository.findByUserId(member.getUserId()).orElse(null);
        List<String> tagNames = memberTagRelationRepository.findTagsByMember(member.getTenantId(), member.getId()).stream()
                .map(com.bluecone.app.core.user.domain.member.MemberTag::getTagName)
                .collect(Collectors.toList());
        var points = pointsAccountRepository.findByTenantAndMember(member.getTenantId(), member.getId()).orElse(null);
        var balance = balanceAccountRepository.findByTenantAndMember(member.getTenantId(), member.getId()).orElse(null);
        int couponCount = couponRepository.findByMember(member.getTenantId(), member.getId()).size();

        MemberDetailDTO dto = new MemberDetailDTO();
        dto.setMemberId(member.getId());
        dto.setTenantId(member.getTenantId());
        dto.setUserId(member.getUserId());
        dto.setMemberNo(member.getMemberNo());
        dto.setStatus(toStatusCode(member));
        dto.setStatusLabel(statusLabel(member.getStatus()));
        if (profile != null) {
            dto.setNickname(profile.getNickname());
            dto.setAvatarUrl(profile.getAvatarUrl());
            dto.setCity(profile.getCity());
            dto.setProvince(profile.getProvince());
            dto.setCountry(profile.getCountry());
            dto.setLanguage(profile.getLanguage());
            dto.setBirthday(profile.getBirthday() != null ? profile.getBirthday().toString() : null);
            dto.setLastLoginAt(profile.getLastLoginAt() != null ? profile.getLastLoginAt().toString() : null);
        }
        if (identity != null) {
            dto.setPhoneMasked(maskPhone(identity.getPhone()));
        }
        dto.setLevelId(member.getLevelId());
        if (level != null) {
            dto.setLevelCode(level.getLevelCode());
            dto.setLevelName(level.getLevelName());
        }
        dto.setGrowthValue(member.getGrowthValue());
        dto.setTagNames(tagNames);
        dto.setPointsBalance(points != null ? points.getPointsBalance() : 0);
        dto.setBalanceAvailable(balance != null ? balance.getAvailableAmount() : null);
        dto.setAvailableCouponCount(couponCount);
        dto.setJoinAt(member.getJoinAt() != null ? member.getJoinAt().toString() : null);
        dto.setRemark(member.getRemark());
        return dto;
    }

    /**
     * 打标签。
     */
    public void addTagsToMembers(MemberTagCommandDTO cmd) {
        Long tenantId = resolveTenantId(cmd.getTenantId());
        if (CollectionUtils.isEmpty(cmd.getMemberIds()) || CollectionUtils.isEmpty(cmd.getTagIds())) {
            return;
        }
        for (Long tagId : cmd.getTagIds()) {
            memberTagRepository.findById(tagId)
                    .filter(tag -> tenantId.equals(tag.getTenantId()))
                    .ifPresent(tag -> {
                        for (Long memberId : cmd.getMemberIds()) {
                            memberTagRelationRepository.addTagToMember(tenantId, memberId, tagId);
                        }
                    });
        }
        // TODO: 记录操作日志
    }

    /**
     * 去标签。
     */
    public void removeTagsFromMembers(MemberTagCommandDTO cmd) {
        Long tenantId = resolveTenantId(cmd.getTenantId());
        if (CollectionUtils.isEmpty(cmd.getMemberIds()) || CollectionUtils.isEmpty(cmd.getTagIds())) {
            return;
        }
        for (Long memberId : cmd.getMemberIds()) {
            for (Long tagId : cmd.getTagIds()) {
                memberTagRelationRepository.removeTagFromMember(tenantId, memberId, tagId);
            }
        }
        // TODO: 记录操作日志
    }

    private MemberSearchQuery toSearchQuery(MemberSearchQueryDTO dto) {
        MemberSearchQuery query = new MemberSearchQuery();
        query.setTenantId(resolveTenantId(dto.getTenantId()));
        query.setKeyword(dto.getKeyword());
        query.setLevelId(dto.getLevelId());
        query.setStatus(dto.getStatus());
        query.setTagIds(dto.getTagIds());
        query.setMinGrowth(dto.getMinGrowth());
        query.setMaxGrowth(dto.getMaxGrowth());
        query.setJoinStart(parseDate(dto.getJoinStartDate()));
        query.setJoinEnd(parseDate(dto.getJoinEndDate()));
        query.setPageNo(dto.getPageNo() != null ? dto.getPageNo() : 1);
        query.setPageSize(dto.getPageSize() != null ? dto.getPageSize() : 20);
        return query;
    }

    private MemberListItemDTO toListDTO(MemberListView view) {
        MemberListItemDTO dto = new MemberListItemDTO();
        dto.setMemberId(view.getMemberId());
        dto.setTenantId(view.getTenantId());
        dto.setUserId(view.getUserId());
        dto.setMemberNo(view.getMemberNo());
        dto.setStatus(view.getStatus());
        dto.setStatusLabel(view.getStatusLabel());
        dto.setNickname(view.getNickname());
        dto.setAvatarUrl(view.getAvatarUrl());
        dto.setPhoneMasked(view.getPhoneMasked());
        dto.setLevelId(view.getLevelId());
        dto.setLevelName(view.getLevelName());
        dto.setGrowthValue(view.getGrowthValue());
        dto.setTagNames(view.getTagNames());
        dto.setJoinAt(view.getJoinAt() != null ? view.getJoinAt().toString() : null);
        dto.setLastLoginAt(view.getLastLoginAt() != null ? view.getLastLoginAt().toString() : null);
        return dto;
    }

    private Long resolveTenantId(Long input) {
        return input != null ? input : currentUserContext.getCurrentTenantId();
    }

    private LocalDateTime parseDate(String date) {
        if (date == null || date.isEmpty()) {
            return null;
        }
        try {
            LocalDate d = LocalDate.parse(date);
            return d.atStartOfDay();
        } catch (Exception e) {
            return null;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String statusLabel(com.bluecone.app.core.user.domain.member.MemberStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> "正常";
            case FROZEN -> "冻结";
            case QUIT -> "注销";
        };
    }
}
