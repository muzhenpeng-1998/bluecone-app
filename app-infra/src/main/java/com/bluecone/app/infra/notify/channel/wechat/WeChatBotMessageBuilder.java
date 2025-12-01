package com.bluecone.app.infra.notify.channel.wechat;

import com.bluecone.app.infra.notify.delivery.NotificationEnvelope;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Objects;

/**
 * 微信机器人 Markdown 构建器。
 *
 * <p>当前实现以属性键值拼装，未来可接入模板中心。</p>
 */
public class WeChatBotMessageBuilder {

    public String buildMarkdown(NotificationEnvelope envelope, String templateCode) {
        Objects.requireNonNull(envelope, "envelope must not be null");
        Map<String, Object> vars = envelope.getTask().getVariables();
        String custom = (String) vars.get("content");
        if (StringUtils.hasText(custom)) {
            return replacePlaceholders(custom, vars);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【通知】").append(templateCode).append("\n");
        sb.append("> 场景：").append(envelope.getTask().getScenarioCode()).append("\n");
        sb.append("> 租户：").append(envelope.getTask().getTenantId()).append("\n");
        vars.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
        return sb.toString();
    }

    private String replacePlaceholders(String template, Map<String, Object> vars) {
        String result = template;
        for (Map.Entry<String, Object> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            result = result.replace(placeholder, entry.getValue() == null ? "" : String.valueOf(entry.getValue()));
        }
        return result;
    }
}
