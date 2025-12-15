package com.bluecone.app.core.publicid;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.exception.PublicIdForbiddenException;
import com.bluecone.app.core.publicid.guard.DefaultScopeGuard;
import com.bluecone.app.core.publicid.guard.ScopeGuard;
import com.bluecone.app.core.publicid.guard.ScopeGuardContext;
import com.bluecone.app.id.api.ResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ScopeGuard 单元测试。
 */
class ScopeGuardTest {

    private ScopeGuard scopeGuard;

    @BeforeEach
    void setUp() {
        scopeGuard = new DefaultScopeGuard();
    }

    @Test
    void check_shouldPass_whenTenantMatches() {
        // Given
        long tenantId = 1001L;
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.PRODUCT,
                "prd_01HN8X5K9G3QRST2VW4XYZ",
                tenantId,
                12345L
        );
        ScopeGuardContext context = new ScopeGuardContext(tenantId, null, ApiSide.USER);

        // When & Then (不抛异常即通过)
        assertDoesNotThrow(() -> scopeGuard.check(resolved, context));
    }

    @Test
    void check_shouldThrowForbidden_whenTenantMismatch() {
        // Given
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.PRODUCT,
                "prd_01HN8X5K9G3QRST2VW4XYZ",
                1001L,  // resolved tenantId
                12345L
        );
        ScopeGuardContext context = new ScopeGuardContext(1002L, null, ApiSide.USER);  // context tenantId

        // When & Then
        assertThrows(PublicIdForbiddenException.class, () ->
                scopeGuard.check(resolved, context)
        );
    }

    @Test
    void check_shouldPass_whenStoreMatches() {
        // Given
        long tenantId = 1001L;
        Long storePk = 12345L;
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.STORE,
                "sto_01HN8X5K9G3QRST2VW4XYZ",
                tenantId,
                storePk
        );
        ScopeGuardContext context = new ScopeGuardContext(tenantId, storePk, ApiSide.MERCHANT);

        // When & Then
        assertDoesNotThrow(() -> scopeGuard.check(resolved, context));
    }

    @Test
    void check_shouldThrowForbidden_whenStoreMismatch() {
        // Given
        long tenantId = 1001L;
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.STORE,
                "sto_01HN8X5K9G3QRST2VW4XYZ",
                tenantId,
                12345L  // resolved storePk
        );
        ScopeGuardContext context = new ScopeGuardContext(tenantId, 99999L, ApiSide.MERCHANT);  // context storePk

        // When & Then
        assertThrows(PublicIdForbiddenException.class, () ->
                scopeGuard.check(resolved, context)
        );
    }

    @Test
    void check_shouldSkip_whenPlatformSide() {
        // Given
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.PRODUCT,
                "prd_01HN8X5K9G3QRST2VW4XYZ",
                1001L,
                12345L
        );
        ScopeGuardContext context = new ScopeGuardContext(1002L, null, ApiSide.PLATFORM);  // 不同租户，但平台侧

        // When & Then (平台侧跳过校验，不抛异常)
        assertDoesNotThrow(() -> scopeGuard.check(resolved, context));
    }

    @Test
    void check_shouldPass_whenStoreContextNotPresent() {
        // Given
        long tenantId = 1001L;
        ResolvedPublicId resolved = new ResolvedPublicId(
                ResourceType.STORE,
                "sto_01HN8X5K9G3QRST2VW4XYZ",
                tenantId,
                12345L
        );
        ScopeGuardContext context = new ScopeGuardContext(tenantId, null, ApiSide.USER);  // 无 storePk

        // When & Then (无 store 上下文时，仅校验租户)
        assertDoesNotThrow(() -> scopeGuard.check(resolved, context));
    }
}

