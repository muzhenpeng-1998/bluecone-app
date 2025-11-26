package com.bluecone.app.core.log.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogMaskUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)(\\d{4})(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{6})(\\d{8})(\\d{4})");
    private static final Pattern JSON_FIELD_PATTERN = Pattern.compile(
            "\"(password|pwd|token|secret|accessToken|refreshToken|idCard|phone|mobile|bankCard)\"\\s*:\\s*\"([^\"]+)\""
    );

    public static String mask(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        String result = content;

        // 脱敏手机号
        result = maskPhone(result);

        // 脱敏身份证
        result = maskIdCard(result);

        // 脱敏 JSON 敏感字段
        result = maskJsonFields(result);

        // 截断过长内容
        if (result.length() > 5000) {
            result = result.substring(0, 5000) + "...[truncated]";
        }

        return result;
    }

    private static String maskPhone(String content) {
        Matcher matcher = PHONE_PATTERN.matcher(content);
        return matcher.replaceAll("$1****$3");
    }

    private static String maskIdCard(String content) {
        Matcher matcher = ID_CARD_PATTERN.matcher(content);
        return matcher.replaceAll("$1********$3");
    }

    private static String maskJsonFields(String content) {
        Matcher matcher = JSON_FIELD_PATTERN.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String fieldValue = matcher.group(2);
            String masked = maskValue(fieldValue);
            matcher.appendReplacement(sb, "\"" + fieldName + "\":\"" + masked + "\"");
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String maskValue(String value) {
        if (value == null || value.length() <= 4) {
            return "****";
        }
        if (value.length() <= 8) {
            return value.substring(0, 2) + "****";
        }
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }
}
