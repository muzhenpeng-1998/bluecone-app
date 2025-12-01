package com.bluecone.app.infra.notify.channel.wechat;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 微信机器人配置（通道层）。
 */
@ConfigurationProperties(prefix = "bluecone.notify.wechat-bot")
public class WeChatBotProperties {

    private Map<Long, WeChatBotTenantConfig> tenants = new HashMap<>();

    public Map<Long, WeChatBotTenantConfig> getTenants() {
        return tenants;
    }

    public void setTenants(Map<Long, WeChatBotTenantConfig> tenants) {
        this.tenants = tenants == null ? new HashMap<>() : new HashMap<>(tenants);
    }

    public static class WeChatBotTenantConfig {

        private List<WeChatBotWebhook> bots;

        public List<WeChatBotWebhook> getBots() {
            return bots;
        }

        public void setBots(List<WeChatBotWebhook> bots) {
            this.bots = bots;
        }
    }

    public static class WeChatBotWebhook {
        private String id;
        private String name;
        private String webhookUrl;
        private boolean mentionAll;
        private List<String> mentionMobiles;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getWebhookUrl() {
            return webhookUrl;
        }

        public void setWebhookUrl(String webhookUrl) {
            this.webhookUrl = webhookUrl;
        }

        public boolean isMentionAll() {
            return mentionAll;
        }

        public void setMentionAll(boolean mentionAll) {
            this.mentionAll = mentionAll;
        }

        public List<String> getMentionMobiles() {
            return mentionMobiles;
        }

        public void setMentionMobiles(List<String> mentionMobiles) {
            this.mentionMobiles = mentionMobiles;
        }
    }
}
