package com.bluecone.app.infra.user.repositoryimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.user.domain.repository.UserExternalIdentityRepository;
import com.bluecone.app.infra.user.dataobject.UserExternalIdentityDO;
import com.bluecone.app.infra.user.mapper.UserExternalIdentityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户外部身份绑定仓储实现，基于 MyBatis-Plus 访问表 bc_user_external_identity。
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserExternalIdentityRepositoryImpl implements UserExternalIdentityRepository {

    private final UserExternalIdentityMapper mapper;

    private static final String PROVIDER_WECHAT_MINI = "WECHAT_MINI";

    @Override
    public Optional<Long> findUserIdByWeChatOpenId(String appId, String openId) {
        return findUserIdByExternalIdentity(PROVIDER_WECHAT_MINI, appId, openId);
    }

    @Override
    public void bindWeChatOpenId(long userId, String appId, String openId, String unionId) {
        bindExternalIdentity(userId, PROVIDER_WECHAT_MINI, appId, openId, unionId);
    }

    @Override
    public Optional<Long> findUserIdByExternalIdentity(String provider, String appId, String openId) {
        UserExternalIdentityDO dataObject = mapper.selectOne(new LambdaQueryWrapper<UserExternalIdentityDO>()
                .eq(UserExternalIdentityDO::getProvider, provider)
                .eq(UserExternalIdentityDO::getAppId, appId)
                .eq(UserExternalIdentityDO::getOpenId, openId)
                .last("LIMIT 1"));
        
        if (dataObject != null) {
            log.debug("找到外部身份绑定: provider={}, appId={}, openId前3后3={}, userId={}", 
                    provider, appId, maskOpenId(openId), dataObject.getUserId());
            return Optional.of(dataObject.getUserId());
        }
        
        log.debug("未找到外部身份绑定: provider={}, appId={}, openId前3后3={}", 
                provider, appId, maskOpenId(openId));
        return Optional.empty();
    }

    @Override
    public void bindExternalIdentity(long userId, String provider, String appId, String openId, String unionId) {
        // 先查询是否已绑定
        Optional<Long> existingUserId = findUserIdByExternalIdentity(provider, appId, openId);
        if (existingUserId.isPresent()) {
            if (existingUserId.get().equals(userId)) {
                log.debug("外部身份已绑定到同一用户，跳过: provider={}, appId={}, openId前3后3={}, userId={}", 
                        provider, appId, maskOpenId(openId), userId);
                return;
            } else {
                log.warn("外部身份已绑定到不同用户: provider={}, appId={}, openId前3后3={}, existingUserId={}, newUserId={}", 
                        provider, appId, maskOpenId(openId), existingUserId.get(), userId);
                throw new IllegalStateException("外部身份已绑定到其他用户");
            }
        }

        // 插入新绑定
        UserExternalIdentityDO dataObject = new UserExternalIdentityDO();
        dataObject.setProvider(provider);
        dataObject.setAppId(appId);
        dataObject.setOpenId(openId);
        dataObject.setUnionId(unionId);
        dataObject.setUserId(userId);

        try {
            mapper.insert(dataObject);
            log.info("绑定外部身份成功: provider={}, appId={}, openId前3后3={}, userId={}", 
                    provider, appId, maskOpenId(openId), userId);
        } catch (DuplicateKeyException e) {
            // 并发场景下可能出现唯一键冲突，再次查询确认
            Optional<Long> recheck = findUserIdByExternalIdentity(provider, appId, openId);
            if (recheck.isPresent() && recheck.get().equals(userId)) {
                log.debug("并发场景下外部身份已绑定到同一用户，忽略: provider={}, appId={}, openId前3后3={}, userId={}", 
                        provider, appId, maskOpenId(openId), userId);
            } else {
                throw new IllegalStateException("绑定外部身份失败（唯一键冲突）", e);
            }
        }
    }

    /**
     * 脱敏 openId：只显示前3后3字符。
     */
    private String maskOpenId(String openId) {
        if (openId == null || openId.length() <= 6) {
            return "***";
        }
        return openId.substring(0, 3) + "***" + openId.substring(openId.length() - 3);
    }
}

