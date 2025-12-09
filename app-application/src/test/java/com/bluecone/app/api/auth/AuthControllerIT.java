package com.bluecone.app.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.api.auth.dto.LogoutRequest;
import com.bluecone.app.api.auth.dto.RefreshTokenRequest;
import com.bluecone.app.core.exception.ApiErrorResponse;
import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;
import com.bluecone.app.core.user.infra.persistence.mapper.UserMapper;
import com.bluecone.app.test.AbstractWebIntegrationTest;
import com.bluecone.app.test.support.TestDataFactory;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends AbstractWebIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerIT.class);

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        flushRedis();
        log.info("清理完成：数据库和 Redis 状态已重置");
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        UserEntity user = TestDataFactory.user(2001L, "secret");
        userMapper.insert(user);
        log.info("准备用户用于登录测试, userId={}, username={}", user.getId(), user.getUsername());

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "secret"),
                LoginResponse.class);
        LoginResponse loginBody = response.getBody();
        log.info("登录响应: status={}, bodyExists={}", response.getStatusCode(), loginBody != null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginBody).as("登录返回体").isNotNull();
        loginBody = Objects.requireNonNull(loginBody);
        assertThat(loginBody.getAccessToken()).isNotBlank();
        assertThat(loginBody.getRefreshToken()).isNotBlank();
        assertThat(loginBody.getSessionId()).isNotBlank();
        log.info("登录成功断言通过，返回 token 与 sessionId 均非空");
    }

    @Test
    void loginFailsForInvalidCredentials() {
        UserEntity user = TestDataFactory.user(2001L, "correct");
        userMapper.insert(user);
        log.info("准备用户用于错误密码测试, userId={}, username={}", user.getId(), user.getUsername());

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "wrong"),
                ApiErrorResponse.class);
        ApiErrorResponse errorBody = response.getBody();
        log.info("错误密码登录响应: status={}, code={}", response.getStatusCode(), errorBody != null ? errorBody.getCode() : "null");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(errorBody).as("错误密码返回体").isNotNull();
        errorBody = Objects.requireNonNull(errorBody);
        assertThat(errorBody.getCode()).isNotNull();
        assertThat(errorBody.getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
        log.info("错误密码断言通过，返回业务码 AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void refreshIssuesNewAccessTokenAndBlacklistIsHonored() {
        UserEntity user = TestDataFactory.user(3001L, "refresh-pass");
        userMapper.insert(user);
        log.info("准备用户用于刷新 token 测试, userId={}, tenantId={}", user.getId(), user.getTenantId());

        LoginResponse login = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "refresh-pass"),
                LoginResponse.class).getBody();
        assertThat(login).as("初次登录结果不能为空").isNotNull();
        login = Objects.requireNonNull(login);
        log.info("初次登录成功，accessTokenLen={}, refreshTokenLen={}",
                login.getAccessToken() != null ? login.getAccessToken().length() : 0,
                login.getRefreshToken() != null ? login.getRefreshToken().length() : 0);

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(login.getRefreshToken());

        LoginResponse refreshed = restTemplate.postForEntity(
                url("/api/auth/refresh"),
                refreshRequest,
                LoginResponse.class).getBody();
        assertThat(refreshed).as("刷新后返回体不能为空").isNotNull();
        refreshed = Objects.requireNonNull(refreshed);
        log.info("刷新后 accessToken 长度={}, 与初次 access 是否相同={}",
                refreshed.getAccessToken() != null ? refreshed.getAccessToken().length() : 0,
                refreshed.getAccessToken() != null && refreshed.getAccessToken().equals(login.getAccessToken()));

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setSessionId(login.getSessionId());
        String loginAccessToken = Objects.requireNonNull(login.getAccessToken(), "login access token");
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(loginAccessToken);
        logoutHeaders.add("X-Tenant-Id", String.valueOf(user.getTenantId()));

        restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest, logoutHeaders),
                Void.class);
        log.info("已登出会话 sessionId={}", logoutRequest.getSessionId());

        HttpHeaders staleHeaders = new HttpHeaders();
        staleHeaders.setBearerAuth(loginAccessToken);
        staleHeaders.add("X-Tenant-Id", String.valueOf(user.getTenantId()));

        ResponseEntity<ApiErrorResponse> forbidden = restTemplate.exchange(
                url("/health/db"),
                HttpMethod.GET,
                new HttpEntity<>(staleHeaders),
                ApiErrorResponse.class);
        ApiErrorResponse forbiddenBody = forbidden.getBody();
        log.info("使用旧 accessToken 访问结果: status={}, code={}",
                forbidden.getStatusCode(),
                forbiddenBody != null ? forbiddenBody.getCode() : "null");

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(forbiddenBody).as("旧 token 返回体").isNotNull();
        forbiddenBody = Objects.requireNonNull(forbiddenBody);
        String forbiddenCode = Objects.toString(forbiddenBody.getCode(), "null");
        log.info("旧 accessToken 被拒绝访问断言通过，业务码={}", forbiddenCode);
        log.info("旧 accessToken 被拒绝访问断言通过");
    }
}
