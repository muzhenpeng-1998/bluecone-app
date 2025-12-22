package com.bluecone.app.api.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.api.auth.dto.LoginRequest;
import com.bluecone.app.api.auth.dto.LoginResponse;
import com.bluecone.app.api.auth.dto.LogoutAllRequest;
import com.bluecone.app.api.auth.dto.LogoutRequest;
import com.bluecone.app.api.auth.dto.RefreshTokenRequest;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.infra.security.session.AuthSessionEntity;
import com.bluecone.app.infra.security.session.AuthSessionService;
import com.bluecone.app.infra.security.token.TokenProperties;
import com.bluecone.app.infra.security.token.TokenProvider;
import com.bluecone.app.infra.security.token.TokenUserContext;
import com.bluecone.app.infra.security.token.blacklist.TokenBlacklistService;
import com.bluecone.app.security.core.SecurityConstants;
import com.bluecone.app.security.core.SecurityUserPrincipal;
import com.bluecone.app.core.user.infra.persistence.entity.UserEntity;
import com.bluecone.app.core.user.application.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 用户认证控制器
 * 
 * <p>提供完整的用户认证和会话管理功能，包括登录、登出、Token刷新等核心认证流程。
 * 
 * <p><b>核心功能：</b>
 * <ul>
 *   <li>用户登录认证：验证用户名密码，生成访问令牌</li>
 *   <li>Token刷新：使用刷新令牌获取新的访问令牌</li>
 *   <li>单点登出：注销当前会话</li>
 *   <li>全局登出：注销用户的所有会话</li>
 * </ul>
 * 
 * <p><b>Token机制：</b>
 * <ul>
 *   <li>AccessToken：短期令牌，用于API访问，默认有效期15分钟</li>
 *   <li>RefreshToken：长期令牌，用于刷新AccessToken，默认有效期7天</li>
 *   <li>SessionId：会话标识，用于会话管理和多端登录控制</li>
 * </ul>
 * 
 * <p><b>安全机制：</b>
 * <ul>
 *   <li>密码验证：使用BCrypt加密算法验证密码</li>
 *   <li>Token签名：使用JWT签名防止篡改</li>
 *   <li>Token黑名单：登出后的Token加入黑名单，防止重放攻击</li>
 *   <li>会话管理：支持多端登录控制和强制下线</li>
 * </ul>
 * 
 * <p><b>多租户支持：</b>
 * 用户登录后自动绑定租户上下文，后续所有操作都在租户隔离环境中执行。
 * 
 * @author BlueCone
 * @since 1.0.0
 */
@Tag(name = "🔐 认证授权 (Auth)", description = "认证接口（登录/登出/刷新Token）")
@RestController
@RequestMapping("/api/auth")
@Validated
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    /** 用户服务，负责用户验证和查询 */
    private final UserService userService;
    
    /** Token提供者，负责生成和解析JWT令牌 */
    private final TokenProvider tokenProvider;
    
    /** Token配置属性，包含过期时间等配置 */
    private final TokenProperties tokenProperties;
    
    /** 认证会话服务，负责会话的创建、查询和注销 */
    private final AuthSessionService authSessionService;
    
    /** Token黑名单服务，负责管理已注销的Token */
    private final TokenBlacklistService tokenBlacklistService;

    /**
     * 用户登录
     * 
     * <p>验证用户名和密码，成功后创建会话并返回访问令牌和刷新令牌。
     * 
     * <p><b>登录流程：</b>
     * <ol>
     *   <li>验证用户名和密码是否正确</li>
     *   <li>检查用户状态是否正常（未禁用、未锁定）</li>
     *   <li>设置租户上下文（如果用户属于某个租户）</li>
     *   <li>生成唯一的会话ID</li>
     *   <li>创建Token上下文，包含用户ID、租户ID、会话ID、客户端类型等</li>
     *   <li>生成RefreshToken并保存会话信息到数据库</li>
     *   <li>生成AccessToken用于API访问</li>
     *   <li>返回Token和过期时间</li>
     * </ol>
     * 
     * <p><b>多端登录控制：</b>
     * <ul>
     *   <li>通过clientType和deviceId识别不同的登录端</li>
     *   <li>同一用户可以在多个设备上同时登录</li>
     *   <li>每个登录会话都有独立的sessionId</li>
     * </ul>
     * 
     * <p><b>安全性说明：</b>
     * <ul>
     *   <li>密码使用BCrypt加密验证，不会明文传输或存储</li>
     *   <li>RefreshToken的哈希值存储在数据库，防止泄露</li>
     *   <li>登录失败会记录日志，支持异常登录检测</li>
     * </ul>
     * 
     * @param request 登录请求，包含用户名、密码、客户端类型、设备ID
     * @return 登录响应，包含AccessToken、RefreshToken、过期时间、会话ID
     * @throws BusinessException 当用户名或密码错误、用户被禁用等情况
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        // 1. 验证用户名和密码，获取用户实体
        UserEntity user = userService.validateAndGetUser(request.getUsername(), request.getPassword());
        
        // 2. 设置租户上下文（如果用户属于某个租户）
        if (user.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(user.getTenantId()));
        }
        
        // 3. 生成唯一的会话ID
        String sessionId = UUID.randomUUID().toString();
        
        // 4. 构建Token上下文，包含用户身份和会话信息
        TokenUserContext context = TokenUserContext.builder()
                .userId(user.getId())
                .tenantId(user.getTenantId())
                .sessionId(sessionId)
                .clientType(request.getClientType())
                .deviceId(request.getDeviceId())
                .build();

        // 5. 生成RefreshToken（长期令牌）
        String refreshToken = tokenProvider.generateRefreshToken(context);
        
        // 6. 创建会话记录，保存到数据库
        authSessionService.createSessionOnLogin(sessionId, user.getId(), user.getTenantId(),
                request.getClientType(), request.getDeviceId(), refreshToken,
                Duration.ofDays(tokenProperties.getRefreshTokenTtlDays()));

        // 7. 生成AccessToken（短期令牌）
        String accessToken = tokenProvider.generateAccessToken(context);
        
        // 8. 计算Token过期时间
        LocalDateTime accessExpireAt = LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes());
        LocalDateTime refreshExpireAt = LocalDateTime.now().plusDays(tokenProperties.getRefreshTokenTtlDays());

        // 9. 记录登录成功日志
        log.info("User login success, userId={}, tenantId={}, clientType={}, deviceId={}, sessionId={}",
                user.getId(), user.getTenantId(), request.getClientType(), request.getDeviceId(), sessionId);

        // 10. 返回登录响应
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpireAt(accessExpireAt)
                .refreshTokenExpireAt(refreshExpireAt)
                .sessionId(sessionId)
                .build();
    }

    /**
     * 刷新访问令牌
     * 
     * <p>使用RefreshToken获取新的AccessToken，避免用户频繁登录。
     * 
     * <p><b>刷新流程：</b>
     * <ol>
     *   <li>解析RefreshToken，提取用户上下文信息</li>
     *   <li>设置租户上下文</li>
     *   <li>从数据库查询会话信息，验证会话是否有效</li>
     *   <li>验证RefreshToken的哈希值是否匹配（防止Token被篡改）</li>
     *   <li>检查RefreshToken是否过期</li>
     *   <li>生成新的AccessToken</li>
     *   <li>更新会话的最后活跃时间</li>
     *   <li>返回新的AccessToken和原RefreshToken</li>
     * </ol>
     * 
     * <p><b>安全验证：</b>
     * <ul>
     *   <li>验证RefreshToken的签名和有效期</li>
     *   <li>验证会话是否存在且未被注销</li>
     *   <li>验证RefreshToken的哈希值，防止Token被替换</li>
     *   <li>验证RefreshToken是否过期</li>
     * </ul>
     * 
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>RefreshToken不会被刷新，只有AccessToken会更新</li>
     *   <li>RefreshToken过期后需要重新登录</li>
     *   <li>会话被注销后，RefreshToken立即失效</li>
     * </ul>
     * 
     * @param request 刷新请求，包含RefreshToken
     * @return 登录响应，包含新的AccessToken和原RefreshToken
     * @throws BusinessException 当Token无效、会话不存在或已过期时
     */
    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        // 1. 解析RefreshToken，提取用户上下文
        TokenUserContext ctx = tokenProvider.parseRefreshToken(request.getRefreshToken());
        
        // 2. 设置租户上下文
        if (ctx.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(ctx.getTenantId()));
        }
        
        // 3. 查询会话信息，验证会话是否有效
        AuthSessionEntity session = authSessionService.getActiveSession(ctx.getSessionId());
        
        // 4. 验证RefreshToken的哈希值是否匹配（防止Token被篡改）
        if (!hash(request.getRefreshToken()).equals(session.getRefreshTokenHash())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
        
        // 5. 检查RefreshToken是否过期
        if (session.getRefreshTokenExpireAt() != null && session.getRefreshTokenExpireAt().isBefore(LocalDateTime.now())) {
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }

        // 6. 构建新的Token上下文
        TokenUserContext accessCtx = TokenUserContext.builder()
                .userId(ctx.getUserId())
                .tenantId(ctx.getTenantId())
                .sessionId(ctx.getSessionId())
                .clientType(ctx.getClientType())
                .deviceId(ctx.getDeviceId())
                .build();
        
        // 7. 生成新的AccessToken
        String newAccessToken = tokenProvider.generateAccessToken(accessCtx);
        
        // 8. 更新会话的最后活跃时间
        authSessionService.refreshLastActive(ctx.getSessionId());

        // 9. 返回新的AccessToken和原RefreshToken
        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .accessTokenExpireAt(LocalDateTime.now().plusMinutes(tokenProperties.getAccessTokenTtlMinutes()))
                .refreshTokenExpireAt(session.getRefreshTokenExpireAt())
                .sessionId(ctx.getSessionId())
                .build();
    }

    /**
     * 用户登出（单点登出）
     * 
     * <p>注销当前会话，使AccessToken和RefreshToken立即失效。
     * 
     * <p><b>登出流程：</b>
     * <ol>
     *   <li>从HTTP请求头中提取AccessToken</li>
     *   <li>解析AccessToken，获取用户上下文和会话ID</li>
     *   <li>设置租户上下文</li>
     *   <li>确定要注销的会话ID（优先使用请求参数，其次使用Token中的）</li>
     *   <li>将AccessToken加入黑名单（如果未过期）</li>
     *   <li>标记会话为已注销状态</li>
     *   <li>记录登出日志</li>
     * </ol>
     * 
     * <p><b>Token黑名单机制：</b>
     * <ul>
     *   <li>登出后的AccessToken会加入Redis黑名单</li>
     *   <li>黑名单的过期时间等于Token的剩余有效期</li>
     *   <li>后续使用该Token的请求会被拦截器拒绝</li>
     * </ul>
     * 
     * <p><b>会话管理：</b>
     * <ul>
     *   <li>会话被标记为已注销（revoked）状态</li>
     *   <li>该会话的RefreshToken立即失效</li>
     *   <li>只注销当前会话，不影响用户的其他登录会话</li>
     * </ul>
     * 
     * <p><b>安全性说明：</b>
     * <ul>
     *   <li>即使Token被盗用，登出后也无法继续使用</li>
     *   <li>支持无Token登出（通过sessionId参数）</li>
     *   <li>登出操作不可撤销</li>
     * </ul>
     * 
     * @param request 登出请求（可选），可指定要注销的会话ID
     * @param servletRequest HTTP请求对象，用于提取AccessToken
     * @throws BusinessException 当会话ID无效时
     */
    @PostMapping("/logout")
    public void logout(@RequestBody(required = false) LogoutRequest request, HttpServletRequest servletRequest) {
        // 1. 从HTTP请求头中提取AccessToken
        String accessToken = resolveBearer(servletRequest);
        
        // 2. 解析AccessToken，获取用户上下文（如果Token存在且有效）
        TokenUserContext ctx = StringUtils.hasText(accessToken) ? tokenProvider.parseAccessToken(accessToken) : null;
        
        // 3. 设置租户上下文
        if (ctx != null && ctx.getTenantId() != null) {
            com.bluecone.app.infra.tenant.TenantContext.setTenantId(String.valueOf(ctx.getTenantId()));
        }
        
        // 4. 确定要注销的会话ID（优先使用请求参数，其次使用Token中的）
        String sessionId = request != null && StringUtils.hasText(request.getSessionId())
                ? request.getSessionId()
                : (ctx != null ? ctx.getSessionId() : null);
        
        // 5. 验证会话ID是否有效
        if (!StringUtils.hasText(sessionId)) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        
        // 6. 将AccessToken加入黑名单（如果Token未过期）
        if (ctx != null && ctx.getExpireAt() != null) {
            Duration ttl = Duration.between(LocalDateTime.now(), ctx.getExpireAt());
            if (!ttl.isNegative() && !ttl.isZero()) {
                tokenBlacklistService.blacklistAccessToken(ctx.getTokenId(), ttl);
            }
        }
        
        // 7. 标记会话为已注销状态
        authSessionService.markSessionRevoked(sessionId);
        
        // 8. 记录登出成功日志
        log.info("Logout success, sessionId={}, userId={}, tenantId={}", sessionId,
                ctx != null ? ctx.getUserId() : null,
                ctx != null ? ctx.getTenantId() : null);
    }

    /**
     * 全局登出
     * 
     * <p>注销用户的所有会话，可选择性地只注销特定客户端类型的会话。
     * 
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>用户修改密码后，强制所有设备重新登录</li>
     *   <li>用户发现账号异常，需要清除所有登录状态</li>
     *   <li>管理员强制用户下线</li>
     *   <li>只注销特定客户端（如只注销所有Web端登录）</li>
     * </ul>
     * 
     * <p><b>登出流程：</b>
     * <ol>
     *   <li>从Spring Security上下文中获取当前用户信息</li>
     *   <li>验证用户是否已登录</li>
     *   <li>根据用户ID和租户ID查询所有活跃会话</li>
     *   <li>如果指定了客户端类型，只注销该类型的会话</li>
     *   <li>批量标记会话为已注销状态</li>
     *   <li>记录全局登出日志</li>
     * </ol>
     * 
     * <p><b>客户端类型筛选：</b>
     * <ul>
     *   <li>不指定clientType：注销所有客户端的会话</li>
     *   <li>指定clientType：只注销该类型客户端的会话（如WEB、MOBILE、MINI_PROGRAM）</li>
     * </ul>
     * 
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>全局登出后，所有RefreshToken立即失效</li>
     *   <li>当前请求的AccessToken不会加入黑名单（因为数量可能很多）</li>
     *   <li>用户需要重新登录才能继续使用系统</li>
     * </ul>
     * 
     * @param request 全局登出请求（可选），可指定只注销特定客户端类型
     * @throws BusinessException 当用户未登录或会话无效时
     */
    @PostMapping("/logout-all")
    public void logoutAll(@RequestBody(required = false) LogoutAllRequest request) {
        // 1. 获取当前登录用户信息
        SecurityUserPrincipal principal = currentPrincipal();
        
        // 2. 验证用户是否已登录
        if (principal == null) {
            throw BusinessException.of(ErrorCode.AUTH_SESSION_INVALID.getCode(),
                    ErrorCode.AUTH_SESSION_INVALID.getMessage());
        }
        
        // 3. 注销用户的所有会话（可选择性地只注销特定客户端类型）
        authSessionService.revokeAllByUser(principal.getUserId(), principal.getTenantId(),
                request != null ? request.getClientType() : null);
        
        // 4. 记录全局登出日志
        log.info("Logout all, userId={}, tenantId={}, clientType={}", principal.getUserId(),
                principal.getTenantId(), request != null ? request.getClientType() : null);
    }

    /**
     * 获取当前登录用户的安全主体
     * 
     * <p>从Spring Security上下文中提取当前用户的身份信息。
     * 
     * @return 当前用户的安全主体，如果未登录则返回null
     */
    private SecurityUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUserPrincipal)) {
            return null;
        }
        return (SecurityUserPrincipal) authentication.getPrincipal();
    }

    /**
     * 从HTTP请求头中提取Bearer Token
     * 
     * <p>解析Authorization请求头，提取JWT令牌。
     * 
     * <p><b>请求头格式：</b>
     * <pre>
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * </pre>
     * 
     * @param request HTTP请求对象
     * @return JWT令牌字符串，如果请求头不存在或格式错误则返回null
     */
    private String resolveBearer(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (!StringUtils.hasText(header) || !header.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return null;
        }
        return header.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
    }

    /**
     * 计算Token的SHA-256哈希值
     * 
     * <p>使用SHA-256算法对Token进行哈希，用于安全存储和验证。
     * 
     * <p><b>安全性说明：</b>
     * <ul>
     *   <li>数据库中只存储Token的哈希值，不存储明文</li>
     *   <li>即使数据库泄露，攻击者也无法还原原始Token</li>
     *   <li>使用SHA-256算法，具有良好的抗碰撞性</li>
     * </ul>
     * 
     * @param token 原始Token字符串
     * @return 十六进制格式的哈希值
     * @throws IllegalStateException 当SHA-256算法不可用时（理论上不会发生）
     */
    private String hash(String token) {
        try {
            // 使用SHA-256算法计算哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // 将字节数组转换为十六进制字符串
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
