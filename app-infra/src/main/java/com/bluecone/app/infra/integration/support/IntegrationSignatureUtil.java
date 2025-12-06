package com.bluecone.app.infra.integration.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Webhook 签名工具：HMAC-SHA256(body + timestamp, secret)。
 */
public final class IntegrationSignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private IntegrationSignatureUtil() {
    }

    public static String sign(final String body, final String timestamp, final String secret) {
        if (secret == null || secret.isBlank()) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            String payload = body + "." + timestamp;
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate integration signature", ex);
        }
    }

    public static boolean verify(final String body, final String timestamp, final String secret, final String signature) {
        if (signature == null) {
            return false;
        }
        return signature.equals(sign(body, timestamp, secret));
    }
}

