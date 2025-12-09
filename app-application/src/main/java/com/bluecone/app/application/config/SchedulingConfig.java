package com.bluecone.app.application.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduling for the application, so outbox polling runs automatically.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
