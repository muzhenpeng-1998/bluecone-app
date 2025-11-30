package com.bluecone.app.gateway.middleware;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.gateway.config.ApiGatewayProperties;

import lombok.RequiredArgsConstructor;

/**
 * Optional signature verification for open APIs.
 */
@Component
@RequiredArgsConstructor
public class SignatureMiddleware implements ApiMiddleware {

    private final ApiGatewayProperties properties;

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        if (!ctx.getContract().isSignatureRequired()) {
            chain.next(ctx);
            return;
        }
        String secret = properties.getSignatureSecret();
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("Signature secret must be configured");
        }
        String providedSignature = ctx.getRequest().getHeader("X-Signature");
        String timestampHeader = ctx.getRequest().getHeader("X-Signature-Ts");
        if (!StringUtils.hasText(providedSignature) || !StringUtils.hasText(timestampHeader)) {
            throw BusinessException.of(ErrorCode.SIGNATURE_INVALID.getCode(), "Missing signature headers");
        }
        long timestamp = Long.parseLong(timestampHeader);
        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > properties.getSignatureToleranceSeconds()) {
            throw BusinessException.of(ErrorCode.SIGNATURE_INVALID.getCode(), "Signature expired");
        }
        String payload = ctx.getRequest().getRequestURI() + "|" + ctx.getRequest().getMethod() + "|" + timestampHeader;
        String expected = hmacSha256(payload, secret);
        if (!expected.equalsIgnoreCase(providedSignature)) {
            throw BusinessException.of(ErrorCode.SIGNATURE_INVALID.getCode(), "Invalid signature");
        }
        chain.next(ctx);
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(raw.length * 2);
        for (byte b : raw) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }
}
