package com.bluecone.app.core.user.domain.service;

import com.bluecone.app.core.user.domain.identity.RegisterChannel;
import com.bluecone.app.core.user.domain.identity.UserIdentity;
import com.bluecone.app.core.user.domain.identity.UserStatus;
import com.bluecone.app.core.user.domain.profile.UserProfile;
import com.bluecone.app.core.user.domain.repository.UserExternalIdentityRepository;
import com.bluecone.app.core.user.domain.repository.UserIdentityRepository;
import com.bluecone.app.core.user.domain.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 用户领域服务实现，聚合身份与画像初始化逻辑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDomainServiceImpl implements UserDomainService {

    private final UserIdentityRepository userIdentityRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserExternalIdentityRepository userExternalIdentityRepository;

    @Override
    public UserRegistrationResult registerOrLoadByWeChatUnionId(String unionId,
                                                                String phone,
                                                                String countryCode,
                                                                Long firstTenantId,
                                                                RegisterChannel registerChannel) {
        Optional<UserIdentity> existing = Optional.empty();
        if (StringUtils.hasText(unionId)) {
            existing = userIdentityRepository.findByUnionId(unionId);
        }
        if (existing.isEmpty() && StringUtils.hasText(phone)) {
            existing = userIdentityRepository.findByPhone(countryCode, phone);
        }
        if (existing.isPresent()) {
            return new UserRegistrationResult(existing.get(), false);
        }

        UserIdentity identity = new UserIdentity();
        identity.setUnionId(unionId);
        identity.setPhone(phone);
        identity.setCountryCode(countryCode);
        identity.setRegisterChannel(registerChannel != null ? registerChannel : RegisterChannel.WECHAT_MINI);
        identity.setStatus(UserStatus.ACTIVE);
        identity.setFirstTenantId(firstTenantId);
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());
        // TODO: 完善邮箱/风控字段等
        UserIdentity saved = userIdentityRepository.save(identity);
        return new UserRegistrationResult(saved, true);
    }

    @Override
    public UserRegistrationResult registerOrLoadByWeChatMiniApp(String unionId,
                                                                String appId,
                                                                String openId,
                                                                String phone,
                                                                String countryCode,
                                                                Long firstTenantId,
                                                                RegisterChannel registerChannel) {
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(openId)) {
            throw new IllegalArgumentException("appId 和 openId 不能为空");
        }

        // 1. 优先使用 unionId 识别用户
        Optional<UserIdentity> existing = Optional.empty();
        if (StringUtils.hasText(unionId)) {
            existing = userIdentityRepository.findByUnionId(unionId);
            if (existing.isPresent()) {
                log.debug("通过 unionId 找到用户: userId={}", existing.get().getId());
                // 确保 external_identity 绑定存在（幂等）
                userExternalIdentityRepository.bindWeChatOpenId(
                        existing.get().getId(), appId, openId, unionId);
                return new UserRegistrationResult(existing.get(), false);
            }
        }

        // 2. 次选：使用手机号识别用户
        if (existing.isEmpty() && StringUtils.hasText(phone)) {
            existing = userIdentityRepository.findByPhone(countryCode, phone);
            if (existing.isPresent()) {
                log.debug("通过手机号找到用户: userId={}", existing.get().getId());
                // 确保 external_identity 绑定存在（幂等）
                userExternalIdentityRepository.bindWeChatOpenId(
                        existing.get().getId(), appId, openId, unionId);
                return new UserRegistrationResult(existing.get(), false);
            }
        }

        // 3. 兜底：使用 external_identity (appId, openId) 识别用户
        Optional<Long> userIdByOpenId = userExternalIdentityRepository.findUserIdByWeChatOpenId(appId, openId);
        if (userIdByOpenId.isPresent()) {
            UserIdentity identity = userIdentityRepository.findById(userIdByOpenId.get())
                    .orElseThrow(() -> new IllegalStateException("用户身份不存在: userId=" + userIdByOpenId.get()));
            log.debug("通过 external_identity (appId, openId) 找到用户: userId={}", identity.getId());
            return new UserRegistrationResult(identity, false);
        }

        // 4. 创建新用户
        UserIdentity identity = new UserIdentity();
        identity.setUnionId(StringUtils.hasText(unionId) ? unionId : null);
        identity.setPhone(StringUtils.hasText(phone) ? phone : null);
        identity.setCountryCode(StringUtils.hasText(countryCode) ? countryCode : "+86");
        identity.setRegisterChannel(registerChannel != null ? registerChannel : RegisterChannel.WECHAT_MINI);
        identity.setStatus(UserStatus.ACTIVE);
        identity.setFirstTenantId(firstTenantId);
        identity.setCreatedAt(LocalDateTime.now());
        identity.setUpdatedAt(LocalDateTime.now());

        UserIdentity saved = userIdentityRepository.save(identity);
        log.info("创建新用户: userId={}, unionId={}, phone={}, appId={}", 
                saved.getId(), 
                unionId != null ? "***" : "null", 
                phone != null ? "***" : "null", 
                appId);

        // 5. 绑定 external_identity
        userExternalIdentityRepository.bindWeChatOpenId(saved.getId(), appId, openId, unionId);

        return new UserRegistrationResult(saved, true);
    }

    @Override
    public UserProfile initOrUpdateProfile(Long userId, UserProfile profileInput) {
        UserProfile profile = userProfileRepository.findByUserId(userId)
                .orElseGet(UserProfile::new);
        profile.setUserId(userId);
        if (profileInput != null) {
            profile.setNickname(profileInput.getNickname());
            profile.setAvatarUrl(profileInput.getAvatarUrl());
            profile.setGender(profileInput.getGender());
            profile.setBirthday(profileInput.getBirthday());
            profile.setCity(profileInput.getCity());
            profile.setProvince(profileInput.getProvince());
            profile.setCountry(profileInput.getCountry());
            profile.setLanguage(profileInput.getLanguage());
            profile.setTags(profileInput.getTags());
            profile.setLastLoginAt(profileInput.getLastLoginAt());
        }
        profile.setUpdatedAt(LocalDateTime.now());
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(LocalDateTime.now());
        }
        // TODO: 增加字段合法性校验与敏感词过滤
        return userProfileRepository.save(profile);
    }
}
