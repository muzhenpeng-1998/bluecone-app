package com.bluecone.app.infra.user.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户画像表映射，表名：bc_user_profile。
 */
@Data
@TableName("bc_user_profile")
public class UserProfileDO {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String nickname;

    private String avatarUrl;

    private Integer gender;

    private LocalDate birthday;

    private String city;

    private String province;

    private String country;

    private String language;

    private String tagsJson;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
