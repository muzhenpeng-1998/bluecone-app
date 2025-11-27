package com.bluecone.app.core.log.pipeline;

import com.bluecone.app.core.log.ApiEvent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * 敏感字段脱敏：密码、Token、手机号、身份证，并限制体积。
 */
@Component
public class EventSanitizer {

    private static final int MAX_DIGEST_LENGTH = 2048;
    private static final Pattern PASSWORD_FIELD = Pattern.compile("(?i)(\"?(password|token|authorization)\"?\\s*[:=]\\s*\")([^\"]*)\"");
    private static final Pattern PASSWORD_PARAM = Pattern.compile("(?i)(password=)([^&\\s]+)");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(1\\d{2})\\d{4}(\\d{4})\\b");
    private static final Pattern ID_PATTERN = Pattern.compile("\\b(\\d{6})\\d{8}(\\w{4})\\b");

    public ApiEvent sanitize(ApiEvent event) {
        event.setRequestBodyDigest(limit(maskSensitive(event.getRequestBodyDigest())));

        if (event.getPayload() instanceof String strPayload) {
            event.setPayload(limit(maskSensitive(strPayload)));
        }
        return event;
    }

    private String maskSensitive(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        String masked = PASSWORD_FIELD.matcher(source).replaceAll("$1***\"");
        masked = PASSWORD_PARAM.matcher(masked).replaceAll("$1***");
        masked = PHONE_PATTERN.matcher(masked).replaceAll("$1****$2");
        masked = ID_PATTERN.matcher(masked).replaceAll("$1********$2");
        return masked;
    }

    private String limit(String source) {
        if (!StringUtils.hasText(source)) {
            return source;
        }
        if (source.length() > MAX_DIGEST_LENGTH) {
            return source.substring(0, MAX_DIGEST_LENGTH) + "...";
        }
        return source;
    }
}
