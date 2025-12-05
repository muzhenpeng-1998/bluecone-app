package com.bluecone.app.core.user.domain.profile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import lombok.Data;

/**
 * 用户画像，承载基础偏好与标签，对应表 bc_user_profile。
 */
@Data
public class UserProfile {

    /** 对应 bc_user_identity.id */
    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Gender gender;

    private LocalDate birthday;

    private String city;

    private String province;

    private String country;

    private String language;

    /** 领域视角下的标签集合，由 JSON 列反序列化而来 */
    private Set<String> tags;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /**
     * 更新画像信息，具体策略与合法性校验由后续实现补充。
     */
    public void updateProfile(String nickname,
                              String avatarUrl,
                              Gender gender,
                              LocalDate birthday,
                              String city,
                              String province,
                              String country,
                              String language,
                              Set<String> tags) {
        throw new UnsupportedOperationException("TODO implement updateProfile");
    }
}
