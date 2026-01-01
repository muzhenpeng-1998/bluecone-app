package com.bluecone.app.member.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 会员模块自动配置
 * 
 * 注意：Mapper 扫描由全局 MybatisPlusConfig 统一处理，无需在此重复配置
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Configuration
@ComponentScan(basePackages = {
        "com.bluecone.app.member.application",
        "com.bluecone.app.member.domain",
        "com.bluecone.app.member.infra",
        "com.bluecone.app.member.api.impl"
})
public class MemberAutoConfiguration {
    
}
