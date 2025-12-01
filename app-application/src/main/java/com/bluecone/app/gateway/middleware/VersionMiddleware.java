package com.bluecone.app.gateway.middleware;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;

/**
 * Validates requested API version against contract.
 */
@Component
public class VersionMiddleware implements ApiMiddleware {

    private static final Pattern VERSION_IN_PATH = Pattern.compile("/api/v(\\d+)/");

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        String requested = resolveVersion(ctx);
        if (!StringUtils.hasText(requested)) {
            requested = ctx.getContract().getVersion();
        }
        if (!ctx.getContract().getVersion().equalsIgnoreCase(requested)) {
            throw BusinessException.of(ErrorCode.UNSUPPORTED_VERSION.getCode(),
                    "Unsupported API version: " + requested);
        }
        chain.next(ctx);
    }

    private String resolveVersion(ApiContext ctx) {
        if (ctx.getRequest() == null) {
            return ctx.getApiVersion();
        }
        String headerVersion = ctx.getRequest().getHeader("X-Api-Version");
        if (StringUtils.hasText(headerVersion)) {
            return normalize(headerVersion);
        }
        String queryVersion = ctx.getRequest().getParameter("version");
        if (StringUtils.hasText(queryVersion)) {
            return normalize(queryVersion);
        }
        Matcher matcher = VERSION_IN_PATH.matcher(ctx.getRequest().getRequestURI() + "/");
        if (matcher.find()) {
            return "v" + matcher.group(1);
        }
        return ctx.getApiVersion();
    }

    private String normalize(String version) {
        return version.toLowerCase().startsWith("v") ? version.toLowerCase() : "v" + version.toLowerCase();
    }
}
