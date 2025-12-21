package com.bluecone.app.product.test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 商品模块测试应用（Prompt 10）。
 * <p>
 * 用于集成测试，扫描 app-product 和 app-core 的组件。
 *
 * @author System
 * @since 2025-12-21
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.bluecone.app.product",
        "com.bluecone.app.core",
        "com.bluecone.app.id",
        "com.bluecone.app.infra"
})
public class ProductTestApplication {
}

