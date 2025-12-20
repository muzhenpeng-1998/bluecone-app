package com.bluecone.app.user.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开通会员命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollMemberCommand {

    private Long tenantId;

    private Long userId;

    /** JOIN_WECHAT_AUTO, ADMIN_ADD, IMPORT 等 */
    private String joinChannel;

    private String remark;
}
