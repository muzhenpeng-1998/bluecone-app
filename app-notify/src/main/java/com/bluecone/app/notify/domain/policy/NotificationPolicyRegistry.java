package com.bluecone.app.notify.domain.policy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知策略注册中心
 * 管理所有业务类型的通知策略
 */
@Component
public class NotificationPolicyRegistry {
    
    private final Map<String, NotificationPolicy> policyMap;
    
    public NotificationPolicyRegistry(List<NotificationPolicy> policies) {
        this.policyMap = policies.stream()
                .collect(Collectors.toMap(
                        NotificationPolicy::getBizType,
                        Function.identity()
                ));
    }
    
    /**
     * 根据业务类型获取策略
     */
    public Optional<NotificationPolicy> getPolicy(String bizType) {
        return Optional.ofNullable(policyMap.get(bizType));
    }
    
    /**
     * 判断是否支持该业务类型
     */
    public boolean supports(String bizType) {
        return policyMap.containsKey(bizType);
    }
}
