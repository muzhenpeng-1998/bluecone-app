package com.bluecone.app.infra.redis.idempotent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.bluecone.app.infra.redis.support.MethodInvocationContext;
import com.bluecone.app.infra.redis.support.SpelExpressionEvaluator;

/**
 * 幂等业务键解析器，复用 SpEL 并统一校验。
 */
@Component
public class IdempotentKeyResolver {

    private final SpelExpressionEvaluator spelExpressionEvaluator;

    public IdempotentKeyResolver(SpelExpressionEvaluator spelExpressionEvaluator) {
        this.spelExpressionEvaluator = spelExpressionEvaluator;
    }

    /**
     * 解析幂等业务键。
     *
     * @param expression SpEL 表达式
     * @param context    方法上下文
     * @return 业务键字符串
     */
    public String resolve(String expression, MethodInvocationContext context) {
        String result = spelExpressionEvaluator.evaluateToString(expression, context);
        if (!StringUtils.hasText(result)) {
            throw new IllegalArgumentException("Idempotent key SpEL evaluated to blank for method " + context.method());
        }
        return result;
    }
}
