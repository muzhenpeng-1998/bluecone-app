package com.bluecone.app.infra.notify.channel.wechat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 微信机器人 HTTP 客户端（通道层）。
 */
public class WeChatBotClient {

    private static final Logger log = LoggerFactory.getLogger(WeChatBotClient.class);

    private final RestTemplate restTemplate;

    public WeChatBotClient(RestTemplate restTemplate) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate must not be null");
    }

    public void sendMarkdown(String webhookUrl, String content, List<String> mobiles, boolean mentionAll) {
        Map<String, Object> body = new HashMap<>();
        body.put("msgtype", "markdown");
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("content", content);
        if (mobiles != null && !mobiles.isEmpty()) {
            markdown.put("mentioned_mobile_list", mobiles);
        }
        markdown.put("mentioned_list", mentionAll ? List.of("@all") : List.of());
        body.put("markdown", markdown);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl, entity, String.class);
        log.info("[WeChatBot] push result status={} body={}", response.getStatusCode(), response.getBody());
    }
}
