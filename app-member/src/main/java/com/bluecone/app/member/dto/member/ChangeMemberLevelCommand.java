package com.bluecone.app.user.dto.member;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会员等级调整命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeMemberLevelCommand {

    private Long tenantId;

    private Long storeId;

    private Long memberId;

    private String newLevelCode;

    private Long operatorId;
}
