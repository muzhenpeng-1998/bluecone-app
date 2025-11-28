package com.bluecone.app.id.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.bluecone.app.id.IdModuleMarker;
import com.bluecone.app.id.core.UlidIdGenerator;

@Configuration
@ComponentScan(basePackageClasses = IdModuleMarker.class)
public class IdConfiguration {

    @Bean
    public UlidIdGenerator ulidIdGenerator() {
        return new UlidIdGenerator();
    }
}
