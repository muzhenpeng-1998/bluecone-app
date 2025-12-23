package com.bluecone.app.wechat;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 微信模块测试配置类。
 * <p>
 * 用于集成测试的最小 Spring Boot 配置。
 * </p>
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.bluecone.app.wechat")
public class WechatTestConfiguration {
}

