package com.bluecone.app.user.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 管理端调节储值命令。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustBalanceCommand {

    private Long tenantId;

    private Long memberId;

    private BigDecimal delta;

    private String bizType;

    private String bizId;

    private String remark;
}
