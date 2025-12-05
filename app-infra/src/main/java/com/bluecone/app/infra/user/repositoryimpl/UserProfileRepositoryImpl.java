package com.bluecone.app.infra.user.repositoryimpl;

import com.bluecone.app.core.user.domain.profile.Gender;
import com.bluecone.app.core.user.domain.profile.UserProfile;
import com.bluecone.app.core.user.domain.repository.UserProfileRepository;
import com.bluecone.app.infra.user.dataobject.UserProfileDO;
import com.bluecone.app.infra.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 用户画像仓储实现，基于 bc_user_profile。
 */
@Repository
@RequiredArgsConstructor
public class UserProfileRepositoryImpl implements UserProfileRepository {

    private final UserProfileMapper userProfileMapper;

    @Override
    public Optional<UserProfile> findByUserId(Long userId) {
        return Optional.ofNullable(toDomain(userProfileMapper.selectById(userId)));
    }

    @Override
    public UserProfile save(UserProfile profile) {
        UserProfileDO dataObject = toDO(profile);
        int updated = userProfileMapper.updateById(dataObject);
        if (updated == 0) {
            userProfileMapper.insert(dataObject);
        }
        return profile;
    }

    private UserProfile toDomain(UserProfileDO dataObject) {
        if (dataObject == null) {
            return null;
        }
        UserProfile profile = new UserProfile();
        profile.setUserId(dataObject.getUserId());
        profile.setNickname(dataObject.getNickname());
        profile.setAvatarUrl(dataObject.getAvatarUrl());
        profile.setGender(parseGender(dataObject.getGender()));
        profile.setBirthday(dataObject.getBirthday());
        profile.setCity(dataObject.getCity());
        profile.setProvince(dataObject.getProvince());
        profile.setCountry(dataObject.getCountry());
        profile.setLanguage(dataObject.getLanguage());
        profile.setTags(parseTags(dataObject.getTagsJson()));
        profile.setLastLoginAt(dataObject.getLastLoginAt());
        profile.setCreatedAt(dataObject.getCreatedAt());
        profile.setUpdatedAt(dataObject.getUpdatedAt());
        return profile;
    }

    private UserProfileDO toDO(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        UserProfileDO dataObject = new UserProfileDO();
        dataObject.setUserId(profile.getUserId());
        dataObject.setNickname(profile.getNickname());
        dataObject.setAvatarUrl(profile.getAvatarUrl());
        dataObject.setGender(toGenderValue(profile.getGender()));
        dataObject.setBirthday(profile.getBirthday());
        dataObject.setCity(profile.getCity());
        dataObject.setProvince(profile.getProvince());
        dataObject.setCountry(profile.getCountry());
        dataObject.setLanguage(profile.getLanguage());
        dataObject.setTagsJson(toTagsJson(profile.getTags()));
        dataObject.setLastLoginAt(profile.getLastLoginAt());
        dataObject.setCreatedAt(profile.getCreatedAt());
        dataObject.setUpdatedAt(profile.getUpdatedAt());
        return dataObject;
    }

    private Gender parseGender(Integer gender) {
        if (gender == null) {
            return Gender.UNKNOWN;
        }
        return switch (gender) {
            case 1 -> Gender.MALE;
            case 2 -> Gender.FEMALE;
            default -> Gender.UNKNOWN;
        };
    }

    private Integer toGenderValue(Gender gender) {
        if (gender == null) {
            return null;
        }
        return switch (gender) {
            case UNKNOWN -> 0;
            case MALE -> 1;
            case FEMALE -> 2;
        };
    }

    private Set<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isEmpty()) {
            return null;
        }
        // TODO: 改为标准 JSON 解析
        return new HashSet<>(Arrays.asList(tagsJson.split(",")));
    }

    private String toTagsJson(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        // TODO: 改为标准 JSON 序列化
        return String.join(",", tags);
    }
}
