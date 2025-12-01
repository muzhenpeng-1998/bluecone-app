package com.bluecone.app.infra.notify.policy;

/**
 * 单通道配置（Policy 层）。
 */
public class NotifyChannelConfig {

    private NotifyChannel channel;
    private String templateCode;
    private Integer maxPerMinute;
    private String channelConfigId;

    public NotifyChannel getChannel() {
        return channel;
    }

    public void setChannel(NotifyChannel channel) {
        this.channel = channel;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public Integer getMaxPerMinute() {
        return maxPerMinute;
    }

    public void setMaxPerMinute(Integer maxPerMinute) {
        this.maxPerMinute = maxPerMinute;
    }

    public String getChannelConfigId() {
        return channelConfigId;
    }

    public void setChannelConfigId(String channelConfigId) {
        this.channelConfigId = channelConfigId;
    }
}
