package com.bluecone.app.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户画像对外视图，组合基础身份与画像信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private String birthday;

    private String city;

    private String province;

    private String country;

    private String language;

    private List<String> tags;

    /** 脱敏后的手机号 */
    private String phoneMasked;
}
