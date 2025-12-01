package com.bluecone.app.infra.notify.config;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.notify.NotificationFacade;
import com.bluecone.app.infra.notify.NotificationFacadeImpl;
import com.bluecone.app.infra.notify.channel.wechat.WeChatBotChannel;
import com.bluecone.app.infra.notify.channel.wechat.WeChatBotClient;
import com.bluecone.app.infra.notify.channel.wechat.WeChatBotMessageBuilder;
import com.bluecone.app.infra.notify.channel.wechat.WeChatBotProperties;
import com.bluecone.app.infra.notify.delivery.NotificationChannel;
import com.bluecone.app.infra.notify.delivery.NotificationRouter;
import com.bluecone.app.infra.notify.outbox.NotifyOutboxPublisher;
import com.bluecone.app.infra.notify.policy.DefaultNotificationPolicyEngine;
import com.bluecone.app.infra.notify.policy.InMemoryNotifyConfigRepository;
import com.bluecone.app.infra.notify.policy.NotificationPolicyEngine;
import com.bluecone.app.infra.notify.policy.NotifyConfigRepository;
import com.bluecone.app.infra.notify.timeline.DefaultNotificationTimeline;
import com.bluecone.app.infra.notify.timeline.NotificationDigestService;
import com.bluecone.app.infra.notify.timeline.NotificationTimeline;
import com.bluecone.app.infra.notify.timeline.NotifyDedupService;
import com.bluecone.app.infra.notify.timeline.NotifyRateLimiter;
import com.bluecone.app.infra.notify.support.NotificationContextBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisOps;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * 通知平台自动装配（Config 层）。
 */
@Configuration
@EnableConfigurationProperties({NotifyProperties.class, WeChatBotProperties.class})
public class NotifyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public NotifyConfigRepository notifyConfigRepository() {
        return new InMemoryNotifyConfigRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationPolicyEngine notificationPolicyEngine(NotifyConfigRepository notifyConfigRepository) {
        return new DefaultNotificationPolicyEngine(notifyConfigRepository);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyRateLimiter notifyRateLimiter(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        return new NotifyRateLimiter(redisOps, redisKeyBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyDedupService notifyDedupService(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        return new NotifyDedupService(redisOps, redisKeyBuilder);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationDigestService notificationDigestService() {
        return new NotificationDigestService();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationTimeline notificationTimeline(NotifyRateLimiter rateLimiter,
                                                     NotifyDedupService dedupService,
                                                     NotificationDigestService digestService,
                                                     NotifyProperties notifyProperties) {
        return new DefaultNotificationTimeline(rateLimiter, dedupService, digestService, notifyProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationContextBuilder notificationContextBuilder() {
        return new NotificationContextBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public NotifyOutboxPublisher notifyOutboxPublisher(NotificationPolicyEngine policyEngine,
                                                       NotificationTimeline notificationTimeline,
                                                       DomainEventPublisher domainEventPublisher) {
        return new NotifyOutboxPublisher(policyEngine, notificationTimeline, domainEventPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationRouter notificationRouter(List<NotificationChannel> channels) {
        return new NotificationRouter(channels);
    }

    @Bean
    @ConditionalOnMissingBean
    public NotificationFacade notificationFacade(NotifyProperties notifyProperties,
                                                 NotificationContextBuilder contextBuilder,
                                                 NotifyOutboxPublisher notifyOutboxPublisher) {
        return new NotificationFacadeImpl(notifyProperties, contextBuilder, notifyOutboxPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate notifyRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnMissingBean
    public WeChatBotClient weChatBotClient(RestTemplate notifyRestTemplate) {
        return new WeChatBotClient(notifyRestTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public WeChatBotMessageBuilder weChatBotMessageBuilder() {
        return new WeChatBotMessageBuilder();
    }

    @Bean
    @ConditionalOnMissingBean
    public WeChatBotChannel weChatBotChannel(WeChatBotProperties weChatBotProperties,
                                             WeChatBotClient weChatBotClient,
                                             WeChatBotMessageBuilder weChatBotMessageBuilder,
                                             NotifyProperties notifyProperties) {
        return new WeChatBotChannel(weChatBotProperties, weChatBotClient, weChatBotMessageBuilder, notifyProperties);
    }
}
