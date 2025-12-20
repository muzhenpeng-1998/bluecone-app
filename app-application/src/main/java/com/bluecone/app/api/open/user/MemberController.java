package com.bluecone.app.user.controller;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.user.application.member.MemberApplicationService;
import com.bluecone.app.user.dto.member.ChangeMemberLevelCommand;
import com.bluecone.app.user.dto.member.EnrollMemberCommand;
import com.bluecone.app.user.dto.member.MemberLevelDTO;
import com.bluecone.app.user.dto.member.MemberSummaryDTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员相关接口。
 */
@RestController
@RequestMapping("/api/member")
@Validated
public class MemberController {

    private final MemberApplicationService memberApplicationService;

    public MemberController(MemberApplicationService memberApplicationService) {
        this.memberApplicationService = memberApplicationService;
    }

    /**
     * 当前租户的会员概要。
     */
    @GetMapping("/me")
    public ApiResponse<MemberSummaryDTO> getMemberSummary() {
        MemberSummaryDTO summary = memberApplicationService.getCurrentMemberSummary();
        return ApiResponse.success(summary);
    }

    /**
     * 开通会员。
     */
    @PostMapping("/enroll")
    public ApiResponse<MemberSummaryDTO> enroll(@RequestBody EnrollMemberCommand command) {
        MemberSummaryDTO summary = memberApplicationService.enrollMember(command);
        return ApiResponse.success(summary);
    }

    /**
     * 会员等级列表。
     */
    @GetMapping("/levels")
    public ApiResponse<List<MemberLevelDTO>> listLevels() {
        List<MemberLevelDTO> levels = memberApplicationService.listMemberLevels(null);
        return ApiResponse.success(levels);
    }

    /**
     * 调整会员等级。
     */
    @PostMapping("/change-level")
    public ApiResponse<Void> changeLevel(@RequestBody ChangeMemberLevelCommand command) {
        memberApplicationService.changeMemberLevel(command);
        return ApiResponse.success(null);
    }
}
