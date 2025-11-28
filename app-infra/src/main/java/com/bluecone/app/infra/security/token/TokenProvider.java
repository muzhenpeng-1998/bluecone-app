package com.bluecone.app.infra.security.token;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * JWT 生成与解析器。
 */
@Component
public class TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(TokenProvider.class);
    private static final String DEFAULT_DEV_SECRET = "bluecone-dev-secret-please-override-32ch";

    private final TokenProperties properties;
    private final SecretKey signingKey;
    private final ZoneId zoneId = ZoneId.systemDefault();

    public TokenProvider(TokenProperties properties) {
        this.properties = properties;
        String configuredSecret = properties.getSecret();
        String secretToUse = StringUtils.hasText(configuredSecret) ? configuredSecret : DEFAULT_DEV_SECRET;
        if (!StringUtils.hasText(configuredSecret)) {
            log.warn("JWT secret not configured, falling back to default dev secret. Configure bluecone.security.token.secret in application.yml for production.");
        }
        Assert.hasText(secretToUse, "JWT secret must be configured");
        this.signingKey = Keys.hmacShaKeyFor(secretToUse.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(TokenUserContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusMinutes(properties.getAccessTokenTtlMinutes());
        return buildToken(ctx, now, expireAt);
    }

    public String generateRefreshToken(TokenUserContext ctx) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireAt = now.plusDays(properties.getRefreshTokenTtlDays());
        return buildToken(ctx, now, expireAt);
    }

    public TokenUserContext parseAccessToken(String token) {
        return parseToken(token);
    }

    public TokenUserContext parseRefreshToken(String token) {
        return parseToken(token);
    }

    private String buildToken(TokenUserContext ctx, LocalDateTime issuedAt, LocalDateTime expireAt) {
        String jti = UUID.randomUUID().toString();
        return Jwts.builder()
                .id(jti)
                .issuer(properties.getIssuer())
                .subject(String.valueOf(ctx.getUserId()))
                .issuedAt(toDate(issuedAt))
                .expiration(toDate(expireAt))
                .claim("tenantId", ctx.getTenantId())
                .claim("sessionId", ctx.getSessionId())
                .claim("clientType", ctx.getClientType())
                .claim("deviceId", ctx.getDeviceId())
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private TokenUserContext parseToken(String token) {
        try {
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            Claims claims = jws.getPayload();
            LocalDateTime issuedAt = toLocalDateTime(claims.getIssuedAt());
            LocalDateTime expireAt = toLocalDateTime(claims.getExpiration());
            return TokenUserContext.builder()
                    .tokenId(claims.getId())
                    .userId(toLong(claims.getSubject()))
                    .tenantId(toLong(claims.get("tenantId")))
                    .sessionId(claims.get("sessionId", String.class))
                    .clientType(claims.get("clientType", String.class))
                    .deviceId(claims.get("deviceId", String.class))
                    .issuedAt(issuedAt)
                    .expireAt(expireAt)
                    .build();
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Token parse failed: {}", ex.getMessage());
            throw BusinessException.of(ErrorCode.AUTH_TOKEN_INVALID.getCode(),
                    ErrorCode.AUTH_TOKEN_INVALID.getMessage());
        }
    }

    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(zoneId).toInstant());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), zoneId);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
