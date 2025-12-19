package com.bluecone.app.growth.domain.service;

import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * 邀请码生成器
 * 生成简短、唯一、可读的邀请码
 */
@Component
public class InviteCodeGenerator {
    
    private static final String CHARSET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去除易混淆字符
    private static final int CODE_LENGTH = 8;
    
    /**
     * 生成邀请码
     * 基于租户ID + 活动编码 + 用户ID 生成确定性的邀请码
     * 
     * @param tenantId 租户ID
     * @param campaignCode 活动编码
     * @param userId 用户ID
     * @return 邀请码
     */
    public String generate(Long tenantId, String campaignCode, Long userId) {
        try {
            // 生成唯一标识字符串
            String input = String.format("%d:%s:%d:%d", 
                    tenantId, campaignCode, userId, System.currentTimeMillis());
            
            // 使用MD5哈希
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            
            // 转换为Base64并映射到自定义字符集
            String base64 = Base64.getEncoder().encodeToString(hash);
            StringBuilder code = new StringBuilder();
            
            for (int i = 0; i < CODE_LENGTH && i < base64.length(); i++) {
                int index = Math.abs(base64.charAt(i)) % CHARSET.length();
                code.append(CHARSET.charAt(index));
            }
            
            return code.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // Fallback: 使用简单的组合方式
            return String.format("INV%d%d", userId % 100000, System.currentTimeMillis() % 1000);
        }
    }
}
