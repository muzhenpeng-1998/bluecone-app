package com.bluecone.app.wechat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 微信集成测试（最小集成测试）。
 * <p>
 * 测试目标：
 * 1. 验证 app-wechat 模块可以正常加载
 * 2. 验证配置类可以正常初始化
 * 3. 验证 Bean 可以正常注入
 * </p>
 * <p>
 * 注意：此测试需要完整的 Spring 上下文，因此需要依赖 app-application 模块。
 * 如果只想测试 app-wechat 模块本身，可以创建一个最小的测试配置类。
 * </p>
 */
@SpringBootTest(
    classes = WechatTestConfiguration.class,
    properties = {
        "wechat.open-platform.enabled=false",  // 使用 Stub 实现，不需要真实配置
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false"  // 测试环境禁用 Flyway
    }
)
@ActiveProfiles("test")
public class WechatIntegrationTest {

    /**
     * 测试 Spring 上下文加载。
     */
    @Test
    public void contextLoads() {
        // 如果上下文加载成功，测试通过
    }
}

