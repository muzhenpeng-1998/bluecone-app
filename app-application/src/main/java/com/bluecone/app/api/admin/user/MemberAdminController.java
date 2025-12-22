package com.bluecone.app.api.admin.user;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.user.application.member.MemberApplicationService;
import com.bluecone.app.user.dto.member.MemberDetailDTO;
import com.bluecone.app.user.dto.member.MemberLevelDTO;
import com.bluecone.app.user.dto.member.MemberListItemDTO;
import com.bluecone.app.user.dto.member.MemberSearchQueryDTO;
import com.bluecone.app.user.dto.member.MemberTagCommandDTO;
import com.bluecone.app.core.user.domain.member.repository.read.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端会员相关接口。
 */
@Tag(name = "🎛️ 平台管理后台 > 用户管理", description = "平台后台会员管理接口")
@RestController
@RequestMapping("/api/admin")
@Validated
public class MemberAdminController {

    private final MemberApplicationService memberApplicationService;

    public MemberAdminController(MemberApplicationService memberApplicationService) {
        this.memberApplicationService = memberApplicationService;
    }

    /**
     * 会员等级列表。
     */
    @GetMapping("/member-levels")
    public ApiResponse<List<MemberLevelDTO>> listLevels() {
        List<MemberLevelDTO> levels = memberApplicationService.listMemberLevels(null);
        return ApiResponse.success(levels);
    }

    /**
     * 创建或更新会员等级。
     */
    @PostMapping("/member-levels")
    public ApiResponse<MemberLevelDTO> saveLevel(@RequestBody MemberLevelDTO dto) {
        MemberLevelDTO saved = memberApplicationService.createOrUpdateMemberLevel(dto);
        return ApiResponse.success(saved);
    }

    /**
     * 会员列表查询。
     */
    @GetMapping("/members")
    public ApiResponse<PageResult<MemberListItemDTO>> searchMembers(MemberSearchQueryDTO query) {
        PageResult<MemberListItemDTO> result = memberApplicationService.searchMembers(query);
        return ApiResponse.success(result);
    }

    /**
     * 会员详情。
     */
    @GetMapping("/members/{memberId}")
    public ApiResponse<MemberDetailDTO> memberDetail(@PathVariable Long memberId) {
        MemberDetailDTO detail = memberApplicationService.getMemberDetail(memberId);
        return ApiResponse.success(detail);
    }

    /**
     * 批量打标签。
     */
    @PostMapping("/members/tags:add")
    public ApiResponse<Void> addTags(@RequestBody MemberTagCommandDTO cmd) {
        memberApplicationService.addTagsToMembers(cmd);
        return ApiResponse.success();
    }

    /**
     * 批量去标签。
     */
    @PostMapping("/members/tags:remove")
    public ApiResponse<Void> removeTags(@RequestBody MemberTagCommandDTO cmd) {
        memberApplicationService.removeTagsFromMembers(cmd);
        return ApiResponse.success();
    }
}
