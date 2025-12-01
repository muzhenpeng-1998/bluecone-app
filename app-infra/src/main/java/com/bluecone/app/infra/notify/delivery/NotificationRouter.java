package com.bluecone.app.infra.notify.delivery;

import com.bluecone.app.infra.notify.policy.NotifyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 通道路由器：根据任务上的 channel 分发到对应插件。
 */
public class NotificationRouter {

    private static final Logger log = LoggerFactory.getLogger(NotificationRouter.class);

    private final Map<NotifyChannel, NotificationChannel> channelMap = new EnumMap<>(NotifyChannel.class);

    public NotificationRouter(List<NotificationChannel> channels) {
        Objects.requireNonNull(channels, "channels must not be null");
        for (NotificationChannel channel : channels) {
            channelMap.put(channel.channel(), channel);
            log.info("[NotifyRouter] registered channel {}", channel.channel());
        }
    }

    public DeliveryResult routeAndDeliver(NotificationEnvelope envelope) {
        NotificationChannel channel = channelMap.get(envelope.getTask().getChannel());
        if (channel == null) {
            log.error("[NotifyRouter] channel not found {}", envelope.getTask().getChannel());
            return DeliveryResult.failure("CHANNEL_NOT_FOUND", "Channel implementation missing");
        }
        return channel.deliver(envelope);
    }
}
