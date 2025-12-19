package com.bluecone.app.notify.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 通知渠道注册中心
 * 管理所有渠道适配器
 */
@Component
public class NotificationChannelRegistry {
    
    private final Map<com.bluecone.app.notify.api.enums.NotificationChannel, NotificationChannel> channelMap;
    
    public NotificationChannelRegistry(List<NotificationChannel> channels) {
        this.channelMap = channels.stream()
                .collect(Collectors.toMap(
                        NotificationChannel::getChannelType,
                        Function.identity()
                ));
    }
    
    /**
     * 根据渠道类型获取适配器
     */
    public Optional<NotificationChannel> getChannel(com.bluecone.app.notify.api.enums.NotificationChannel channelType) {
        return Optional.ofNullable(channelMap.get(channelType));
    }
}
