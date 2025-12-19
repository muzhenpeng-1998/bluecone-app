package com.bluecone.app.member.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 会员模块自动配置
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
@MapperScan("com.bluecone.app.member.infra.persistence.mapper")
public class MemberAutoConfiguration {
    
}
