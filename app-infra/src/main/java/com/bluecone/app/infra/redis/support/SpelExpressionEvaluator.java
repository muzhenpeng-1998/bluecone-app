package com.bluecone.app.infra.redis.support;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * 统一的 SpEL 解析工具，基于方法入参暴露变量，供锁/限流/幂等三套能力复用。
 * <p>设计目标：避免各处重复拼装 SpEL 上下文，确保表达式解析行为一致且易于排查。</p>
 */
@Component
public class SpelExpressionEvaluator {

    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * 解析表达式并转换为字符串。
     *
     * @param expression SpEL 表达式，不允许为空
     * @param context    方法上下文
     * @return 解析后的字符串结果
     */
    public String evaluateToString(String expression, MethodInvocationContext context) {
        Assert.notNull(context, "context must not be null");
        if (!StringUtils.hasText(expression)) {
            throw new IllegalArgumentException("SpEL expression must not be blank");
        }
        try {
            StandardEvaluationContext evalContext = new StandardEvaluationContext(context.target());
            String[] parameterNames = parameterNameDiscoverer.getParameterNames(context.method());
            Object[] args = context.args();
            if (parameterNames != null && args != null) {
                for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                    evalContext.setVariable(parameterNames[i], args[i]);
                }
            }
            Expression parsed = expressionParser.parseExpression(expression);
            Object value = parsed.getValue(evalContext);
            return value == null ? null : String.valueOf(value);
        } catch (Exception ex) {
            String message = String.format("Failed to evaluate SpEL expression: [%s] on method %s", expression,
                    context.method().toGenericString());
            throw new IllegalArgumentException(message, ex);
        }
    }
}
