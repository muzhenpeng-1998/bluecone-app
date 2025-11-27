package com.bluecone.app.infra.cache.aop;

import com.bluecone.app.infra.cache.annotation.CacheEvict;
import com.bluecone.app.infra.cache.annotation.Cached;
import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfile;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;
import com.bluecone.app.infra.tenant.TenantContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * AOP 门面：用 SpEL 解析业务 key，委托给 CacheClient。
 */
@Aspect
@Component
public class CacheAnnotationAspect {

    private final CacheClient cacheClient;
    private final CacheProfileRegistry profileRegistry;
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public CacheAnnotationAspect(CacheClient cacheClient, CacheProfileRegistry profileRegistry) {
        this.cacheClient = cacheClient;
        this.profileRegistry = profileRegistry;
    }

    @Around(value = "@annotation(cached)", argNames = "pjp,cached")
    public Object aroundCached(ProceedingJoinPoint pjp, Cached cached) throws Throwable {
        CacheProfile profile = profileRegistry.getProfile(cached.profile());
        Object bizId = evalSpel(pjp, cached.key());
        CacheKey key = CacheKey.generic(resolveTenantId(), profile.domain(), bizId);
        Class<?> returnType = ((MethodSignature) pjp.getSignature()).getReturnType();

        @SuppressWarnings("unchecked")
        Class<Object> type = (Class<Object>) returnType;

        return cacheClient.get(cached.profile(),
                ignored -> key,
                bizId,
                type,
                () -> proceed(pjp));
    }

    @AfterReturning(value = "@annotation(cacheEvict)", argNames = "joinPoint,cacheEvict")
    public void afterEvict(org.aspectj.lang.JoinPoint joinPoint, CacheEvict cacheEvict) {
        CacheProfile profile = profileRegistry.getProfile(cacheEvict.profile());
        Object bizId = evalSpel(joinPoint, cacheEvict.key());
        CacheKey key = CacheKey.generic(resolveTenantId(), profile.domain(), bizId);
        cacheClient.evict(cacheEvict.profile(), key);
    }

    private Object proceed(ProceedingJoinPoint pjp) {
        try {
            return pjp.proceed();
        } catch (RuntimeException | Error ex) {
            throw ex;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private Object evalSpel(org.aspectj.lang.JoinPoint joinPoint, String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        EvaluationContext context = new MethodBasedEvaluationContext(null, method, joinPoint.getArgs(), parameterNameDiscoverer);
        return parser.parseExpression(expression).getValue(context);
    }

    private String resolveTenantId() {
        return Objects.requireNonNullElse(TenantContext.getTenantId(), "default");
    }
}
