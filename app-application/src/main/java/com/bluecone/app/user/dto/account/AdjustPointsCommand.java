package com.bluecone.app.user.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端调节积分命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustPointsCommand {

    private Long tenantId;

    private Long memberId;

    private Integer delta;

    private String bizType;

    private String bizId;

    private String remark;
}
