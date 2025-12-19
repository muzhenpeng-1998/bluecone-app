package com.bluecone.app.application.middleware;

import com.bluecone.app.config.UserContextProperties;
import com.bluecone.app.core.contextkit.CaffeineContextCache;
import com.bluecone.app.core.contextkit.ContextCache;
import com.bluecone.app.core.contextkit.ContextKitProperties;
import com.bluecone.app.core.contextkit.VersionChecker;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.user.runtime.api.UserSnapshot;
import com.bluecone.app.core.user.runtime.spi.UserPrincipalResolver;
import com.bluecone.app.core.user.runtime.spi.UserSnapshotRepository;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiContextHolder;
import com.bluecone.app.id.core.Ulid128;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserContextResolverAnonymousTest {

    @AfterEach
    void cleanup() {
        ApiContextHolder.clear();
    }

    @Test
    void anonymousPathShouldAllowNoUser() {
        UserPrincipalResolver principalResolver = mock(UserPrincipalResolver.class);
        UserSnapshotRepository snapshotRepository = mock(UserSnapshotRepository.class);
        when(principalResolver.resolve(any(HttpServletRequest.class))).thenReturn(Optional.empty());

        ContextCache cache = new CaffeineContextCache(100);
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(1), 1.0d);
        ContextKitProperties kitProps = new ContextKitProperties();

        UserContextProperties props = new UserContextProperties();
        props.setEnabled(true);
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());
        props.setAllowAnonymousPaths(java.util.List.of("/api/mini/public/**"));

        UserContextResolver resolver = new UserContextResolver(
                principalResolver,
                snapshotRepository,
                cache,
                versionChecker,
                kitProps,
                props,
                new ObjectMapper());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/public/ping");

        ApiContext ctx = ApiContext.builder()
                .traceId("trace-1")
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(null)
                .apiEndpoint(null)
                .apiVersion(null)
                .pathVariables(Collections.emptyMap())
                .queryParams(new java.util.HashMap<>())
                .build();
        ApiContextHolder.set(ctx);

        resolver.resolve(ctx);

        assertThat(ctx.getUserId()).isNull();
        assertThat(ctx.getAttribute("USER_SNAPSHOT")).isNull();
        verifyNoInteractions(snapshotRepository);
    }

    @Test
    void nonAnonymousPathWithoutUserShouldThrowUnauthorized() {
        UserPrincipalResolver principalResolver = mock(UserPrincipalResolver.class);
        UserSnapshotRepository snapshotRepository = mock(UserSnapshotRepository.class);
        when(principalResolver.resolve(any(HttpServletRequest.class))).thenReturn(Optional.empty());

        ContextCache cache = new CaffeineContextCache(100);
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(1), 1.0d);
        ContextKitProperties kitProps = new ContextKitProperties();

        UserContextProperties props = new UserContextProperties();
        props.setEnabled(true);
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());
        props.setAllowAnonymousPaths(java.util.List.of("/api/mini/public/**"));

        UserContextResolver resolver = new UserContextResolver(
                principalResolver,
                snapshotRepository,
                cache,
                versionChecker,
                kitProps,
                props,
                new ObjectMapper());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/secure/data");

        ApiContext ctx = ApiContext.builder()
                .traceId("trace-2")
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(null)
                .apiEndpoint(null)
                .apiVersion(null)
                .pathVariables(Collections.emptyMap())
                .queryParams(new java.util.HashMap<>())
                .build();
        ApiContextHolder.set(ctx);

        assertThatThrownBy(() -> resolver.resolve(ctx))
                .isInstanceOf(BizException.class)
                .satisfies(ex -> {
                    BizException biz = (BizException) ex;
                    assertThat(biz.getCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED.getCode());
                });

        verifyNoInteractions(snapshotRepository);
    }

    @Test
    void userNotFoundShouldWriteNegativeCacheAndThrow() {
        UserPrincipalResolver principalResolver = mock(UserPrincipalResolver.class);
        Ulid128 internalId = new Ulid128(1L, 2L);
        UserPrincipalResolver.UserPrincipal principal =
                new UserPrincipalResolver.UserPrincipal(1L, internalId, "TEST", "sub");
        when(principalResolver.resolve(any(HttpServletRequest.class))).thenReturn(Optional.of(principal));

        UserSnapshotRepository snapshotRepository = mock(UserSnapshotRepository.class);
        when(snapshotRepository.loadFull(any())).thenReturn(Optional.empty());

        ContextCache cache = new CaffeineContextCache(100);
        VersionChecker versionChecker = new VersionChecker(Duration.ofSeconds(1), 1.0d);
        ContextKitProperties kitProps = new ContextKitProperties();
        kitProps.setL1Ttl(Duration.ofMinutes(5));
        kitProps.setNegativeTtl(Duration.ofSeconds(30));

        UserContextProperties props = new UserContextProperties();
        props.setEnabled(true);
        props.setIncludePaths(java.util.List.of("/api/mini/**"));
        props.setExcludePaths(java.util.List.of());
        props.setAllowAnonymousPaths(java.util.List.of());

        UserContextResolver resolver = new UserContextResolver(
                principalResolver,
                snapshotRepository,
                cache,
                versionChecker,
                kitProps,
                props,
                new ObjectMapper());

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/mini/secure/data");

        ApiContext ctx = ApiContext.builder()
                .traceId("trace-3")
                .requestTime(LocalDateTime.now())
                .request(request)
                .contract(null)
                .apiEndpoint(null)
                .apiVersion(null)
                .pathVariables(Collections.emptyMap())
                .queryParams(new java.util.HashMap<>())
                .build();
        ApiContextHolder.set(ctx);

        assertThatThrownBy(() -> resolver.resolve(ctx))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("用户不存在");

        verify(snapshotRepository, times(1)).loadFull(any());
    }
}

