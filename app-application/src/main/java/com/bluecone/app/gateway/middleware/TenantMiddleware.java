package com.bluecone.app.gateway.middleware;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.exception.ErrorCode;
import com.bluecone.app.gateway.ApiContext;
import com.bluecone.app.gateway.ApiMiddleware;
import com.bluecone.app.gateway.ApiMiddlewareChain;
import com.bluecone.app.infra.tenant.TenantContext;

/**
 * Binds tenant context for downstream persistence layers.
 */
@Component
public class TenantMiddleware implements ApiMiddleware {

    @Override
    public void apply(ApiContext ctx, ApiMiddlewareChain chain) throws Exception {
        boolean tenantRequired = ctx.getContract().isTenantRequired();
        try {
            if (StringUtils.hasText(ctx.getTenantId())) {
                TenantContext.setTenantId(ctx.getTenantId());
            } else if (tenantRequired) {
                throw BusinessException.of(ErrorCode.PERMISSION_DENIED.getCode(), "Tenant id missing");
            }
            chain.next(ctx);
        } finally {
            TenantContext.clear();
        }
    }
}
