package com.bluecone.app.user.dto.member;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 会员详情 DTO，面向管理后台。
 */
@Data
public class MemberDetailDTO {

    private Long memberId;
    private Long tenantId;
    private Long userId;
    private String memberNo;

    private Integer status;
    private String statusLabel;

    private String nickname;
    private String avatarUrl;
    private String phoneMasked;
    private String city;
    private String province;
    private String country;
    private String language;
    private String birthday;

    private Long levelId;
    private String levelCode;
    private String levelName;
    private Integer growthValue;
    private List<String> tagNames;

    private Integer pointsBalance;
    private BigDecimal balanceAvailable;
    private Integer availableCouponCount;

    private String joinAt;
    private String lastLoginAt;
    private String remark;
}
