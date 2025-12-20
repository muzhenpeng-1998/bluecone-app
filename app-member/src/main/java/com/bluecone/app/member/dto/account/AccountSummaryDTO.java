package com.bluecone.app.user.dto.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 账户汇总信息，包含积分、储值和券的概要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDTO {

    private Long tenantId;

    private Long memberId;

    private Integer pointsBalance;

    private Integer frozenPoints;

    private String pointsLevelName;

    private BigDecimal balanceAvailable;

    private BigDecimal balanceFrozen;

    private Integer availableCouponCount;
}
