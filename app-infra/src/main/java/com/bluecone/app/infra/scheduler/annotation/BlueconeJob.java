package com.bluecone.app.infra.scheduler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式任务定义，所有任务以插件形式接入调度中心。
 *
 * <p>注意：同一 code 唯一标识一个任务，生产环境默认以数据库配置为准。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BlueconeJob {

    /**
     * 任务唯一编码，作为调度与幂等标识。
     */
    String code();

    /**
     * 人类可读名称，用于管理端展示。
     */
    String name();

    /**
     * Quartz 风格 Cron 表达式。
     */
    String cron();

    /**
     * 最大执行时长（秒）。
     */
    int timeoutSeconds() default 60;

    /**
     * 初始启用状态，数据库存在时以数据库为准。
     */
    boolean enabled() default true;
}
