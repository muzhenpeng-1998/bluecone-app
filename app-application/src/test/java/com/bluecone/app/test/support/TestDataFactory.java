package com.bluecone.app.test.support;

import com.bluecone.app.api.auth.dto.LoginRequest;
import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestDataFactory {

    private static final AtomicInteger USERNAME_SEQ = new AtomicInteger(1);

    private TestDataFactory() {
    }

    public static UserEntity user(Long tenantId, String passwordHash) {
        UserEntity entity = new UserEntity();
        entity.setTenantId(tenantId);
        entity.setUsername("user_" + USERNAME_SEQ.getAndIncrement());
        entity.setPasswordHash(passwordHash);
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public static LoginRequest loginRequest(String username, String password) {
        LoginRequest request = new LoginRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setClientType("test-client");
        request.setDeviceId("device-" + username);
        return request;
    }
}
