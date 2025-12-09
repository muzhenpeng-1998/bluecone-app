package com.bluecone.app.user.application.profile;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.user.domain.event.UserProfileUpdatedEvent;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.profile.Gender;
import com.bluecone.app.core.user.domain.profile.UserProfile;
import com.bluecone.app.core.user.domain.repository.UserIdentityRepository;
import com.bluecone.app.core.user.domain.repository.UserProfileRepository;
import com.bluecone.app.core.user.domain.service.UserDomainService;
import com.bluecone.app.user.application.CurrentUserContext;
import com.bluecone.app.user.dto.profile.UpdateUserProfileCommand;
import com.bluecone.app.user.dto.profile.UserProfileDTO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户画像应用服务，负责查询与更新画像。
 */
@Service
@RequiredArgsConstructor
public class UserProfileApplicationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final UserProfileRepository userProfileRepository;
    private final UserIdentityRepository userIdentityRepository;
    private final UserDomainService userDomainService;
    private final DomainEventPublisher domainEventPublisher;
    private final CurrentUserContext currentUserContext;

    /**
     * 获取当前用户画像。
     */
    public UserProfileDTO getCurrentProfile() {
        Long userId = currentUserContext.getCurrentUserId();
        UserProfile profile = userProfileRepository.findByUserId(userId).orElse(null);
        UserIdentity identity = userIdentityRepository.findById(userId).orElse(null);
        return toDTO(userId, profile, identity);
    }

    /**
     * 更新当前用户画像。
     */
    public UserProfileDTO updateCurrentProfile(UpdateUserProfileCommand cmd) {
        Long userId = currentUserContext.getCurrentUserId();
        UserProfile input = new UserProfile();
        input.setNickname(cmd.getNickname());
        input.setAvatarUrl(cmd.getAvatarUrl());
        input.setGender(parseGender(cmd.getGender()));
        input.setBirthday(parseBirthday(cmd.getBirthday()));
        input.setCity(cmd.getCity());
        input.setProvince(cmd.getProvince());
        input.setCountry(cmd.getCountry());
        input.setLanguage(cmd.getLanguage());

        UserProfile profile = userDomainService.initOrUpdateProfile(userId, input);
        UserIdentity identity = userIdentityRepository.findById(userId).orElse(null);

        UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(
                userId,
                profile.getNickname(),
                profile.getAvatarUrl(),
                identity != null ? identity.getPhone() : null,
                resolveSource(cmd.getSource())
        );
        domainEventPublisher.publish(event);
        return toDTO(userId, profile, identity);
    }

    private UserProfileDTO toDTO(Long userId, UserProfile profile, UserIdentity identity) {
        return UserProfileDTO.builder()
                .userId(userId)
                .nickname(profile != null ? profile.getNickname() : null)
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .gender(profile != null && profile.getGender() != null ? profile.getGender().ordinal() : null)
                .birthday(profile != null && profile.getBirthday() != null ? profile.getBirthday().format(DATE_FORMAT) : null)
                .city(profile != null ? profile.getCity() : null)
                .province(profile != null ? profile.getProvince() : null)
                .country(profile != null ? profile.getCountry() : null)
                .language(profile != null ? profile.getLanguage() : null)
                .tags(profile != null ? toTagList(profile.getTags()) : Collections.emptyList())
                .phoneMasked(identity != null ? maskPhone(identity.getPhone()) : null)
                .build();
    }

    private List<String> toTagList(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        return tags.stream().toList();
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 4) {
            return phone;
        }
        if (phone.length() <= 7) {
            return phone.charAt(0) + "****" + phone.substring(phone.length() - 1);
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private Gender parseGender(Integer genderCode) {
        if (genderCode == null) {
            return null;
        }
        return switch (genderCode) {
            case 1 -> Gender.MALE;
            case 2 -> Gender.FEMALE;
            default -> Gender.UNKNOWN;
        };
    }

    private LocalDate parseBirthday(String birthday) {
        if (!StringUtils.hasText(birthday)) {
            return null;
        }
        return LocalDate.parse(birthday, DATE_FORMAT);
    }

    private String resolveSource(String source) {
        return StringUtils.hasText(source) ? source : "USER_SELF";
    }
}
