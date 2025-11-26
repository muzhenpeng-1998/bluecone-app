package com.bluecone.app.core.api;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API 版本号解析器
 * 按优先级从请求中提取版本号：Header > URL Path > Query Parameter
 */
@Component
public class VersionExtractor {

    private static final String HEADER_API_VERSION = "X-Api-Version";
    private static final String QUERY_PARAM_VERSION = "version";
    private static final Pattern URL_VERSION_PATTERN = Pattern.compile("/api/v(\\d+)/");
    private static final int DEFAULT_VERSION = 1;

    /**
     * 从 HTTP 请求中提取 API 版本号
     *
     * @param request HTTP 请求对象
     * @return 版本号（默认 1）
     * @throws BusinessException 版本号非法时抛出
     */
    public int extract(HttpServletRequest request) {
        // 优先级 1: Header X-Api-Version
        String headerVersion = request.getHeader(HEADER_API_VERSION);
        if (headerVersion != null && !headerVersion.isBlank()) {
            return parseVersion(headerVersion, "Header");
        }

        // 优先级 2: URL Path /api/v{N}/...
        String path = request.getRequestURI();
        Matcher matcher = URL_VERSION_PATTERN.matcher(path);
        if (matcher.find()) {
            return parseVersion(matcher.group(1), "URL Path");
        }

        // 优先级 3: Query Parameter ?version=N
        String queryVersion = request.getParameter(QUERY_PARAM_VERSION);
        if (queryVersion != null && !queryVersion.isBlank()) {
            return parseVersion(queryVersion, "Query Parameter");
        }

        // 默认版本
        return DEFAULT_VERSION;
    }

    /**
     * 解析版本号字符串为整数
     */
    private int parseVersion(String versionStr, String source) {
        try {
            int version = Integer.parseInt(versionStr.trim());
            if (version <= 0) {
                throw new BusinessException(
                    ErrorCode.INVALID_VERSION.getCode(),
                    String.format("Invalid API version in %s: %s (must be positive integer)", source, versionStr)
                );
            }
            return version;
        } catch (NumberFormatException e) {
            throw new BusinessException(
                ErrorCode.INVALID_VERSION.getCode(),
                String.format("Invalid API version format in %s: %s", source, versionStr)
            );
        }
    }
}
