package com.bluecone.app.infra.redis.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.redis.support.MethodInvocationContext;
import com.bluecone.app.infra.redis.support.SpelExpressionEvaluator;

/**
 * 解析限流注解中的业务键 SpEL，封装统一校验逻辑。
 */
@Component
public class RateLimitKeyResolver {

    private final SpelExpressionEvaluator spelExpressionEvaluator;

    public RateLimitKeyResolver(SpelExpressionEvaluator spelExpressionEvaluator) {
        this.spelExpressionEvaluator = spelExpressionEvaluator;
    }

    /**
     * 解析限流键。
     *
     * @param expression SpEL 表达式
     * @param context    方法调用上下文
     * @return 业务键字符串
     */
    public String resolve(String expression, MethodInvocationContext context) {
        String result = spelExpressionEvaluator.evaluateToString(expression, context);
        if (!StringUtils.hasText(result)) {
            throw new IllegalArgumentException("RateLimit key SpEL evaluated to blank for method " + context.method());
        }
        return result;
    }
}
