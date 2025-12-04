package com.bluecone.app.core.user.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;
import com.bluecone.app.core.user.infra.persistence.mapper.UserMapper;

import lombok.RequiredArgsConstructor;

/**
 * 用户基础服务，提供查询与基础认证能力。
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserMapper userMapper;

    /**
     * 根据用户名查找用户。
     *
     * @param username 用户名
     * @return 用户实体或 null
     */
    public UserEntity findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getUsername, username));
    }

    /**
     * 校验用户名密码并返回用户信息。
     *
     * @param username    用户名
     * @param rawPassword 明文密码
     * @return 通过校验的用户
     */
    public UserEntity validateAndGetUser(String username, String rawPassword) {
        UserEntity user = findByUsername(username);
        if (user == null) {
            throw BusinessException.of(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode(),
                    ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
        }
        if (!passwordMatches(rawPassword, user.getPasswordHash())) {
            log.warn("Login failed for user={}, reason=bad credentials", username);
            throw BusinessException.of(ErrorCode.AUTH_INVALID_CREDENTIALS.getCode(),
                    ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage());
        }
        return user;
    }

    private boolean passwordMatches(String rawPassword, String storedHash) {
        if (!StringUtils.hasText(storedHash)) {
            return false;
        }
        // 同时兼容明文和 SHA-256 哈希，便于后续平滑迁移到更安全方案。
        return Objects.equals(rawPassword, storedHash) || Objects.equals(sha256(rawPassword), storedHash);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
