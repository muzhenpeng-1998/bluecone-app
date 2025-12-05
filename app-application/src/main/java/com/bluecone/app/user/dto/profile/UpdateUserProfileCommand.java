package com.bluecone.app.user.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新用户画像的命令对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserProfileCommand {

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private String birthday;

    private String city;

    private String province;

    private String country;

    private String language;
}
