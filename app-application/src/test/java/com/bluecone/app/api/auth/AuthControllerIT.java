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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthControllerIT extends AbstractWebIntegrationTest {

    @Autowired
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        flushRedis();
    }

    @Test
    void loginReturnsAccessAndRefreshTokens() {
        UserEntity user = TestDataFactory.user(2001L, "secret");
        userMapper.insert(user);

        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "secret"),
                LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAccessToken()).isNotBlank();
        assertThat(response.getBody().getRefreshToken()).isNotBlank();
        assertThat(response.getBody().getSessionId()).isNotBlank();
    }

    @Test
    void loginFailsForInvalidCredentials() {
        UserEntity user = TestDataFactory.user(2001L, "correct");
        userMapper.insert(user);

        ResponseEntity<ApiErrorResponse> response = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "wrong"),
                ApiErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo("AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void refreshIssuesNewAccessTokenAndBlacklistIsHonored() {
        UserEntity user = TestDataFactory.user(3001L, "refresh-pass");
        userMapper.insert(user);

        LoginResponse login = restTemplate.postForEntity(
                url("/api/auth/login"),
                TestDataFactory.loginRequest(user.getUsername(), "refresh-pass"),
                LoginResponse.class).getBody();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(login.getRefreshToken());

        LoginResponse refreshed = restTemplate.postForEntity(
                url("/api/auth/refresh"),
                refreshRequest,
                LoginResponse.class).getBody();

        assertThat(refreshed.getAccessToken()).isNotBlank();
        assertThat(refreshed.getAccessToken()).isNotEqualTo(login.getAccessToken());

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setSessionId(login.getSessionId());
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(login.getAccessToken());
        logoutHeaders.add("X-Tenant-Id", String.valueOf(user.getTenantId()));

        restTemplate.exchange(
                url("/api/auth/logout"),
                HttpMethod.POST,
                new HttpEntity<>(logoutRequest, logoutHeaders),
                Void.class);

        HttpHeaders staleHeaders = new HttpHeaders();
        staleHeaders.setBearerAuth(login.getAccessToken());
        staleHeaders.add("X-Tenant-Id", String.valueOf(user.getTenantId()));

        ResponseEntity<ApiErrorResponse> forbidden = restTemplate.exchange(
                url("/health/db"),
                HttpMethod.GET,
                new HttpEntity<>(staleHeaders),
                ApiErrorResponse.class);

        assertThat(forbidden.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
