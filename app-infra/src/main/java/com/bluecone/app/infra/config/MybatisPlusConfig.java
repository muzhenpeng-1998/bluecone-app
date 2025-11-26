package com.bluecone.app.infra.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * 功能：
 * 1. 配置 Mapper 扫描路径
 * 2. 注册分页插件
 * 3. 预留多租户拦截器位置（暂未启用）
 *
 * 注意：
 * - 当前仅配置分页功能，不依赖数据库
 * - 多租户功能已预留，待后续启用
 * - 使用 @ConditionalOnProperty 避免无数据源时启动失败
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.bluecone.app.**.mapper")
@ConditionalOnProperty(name = "mybatis-plus.enabled", havingValue = "true", matchIfMissing = false)
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器配置
     *
     * 当前已启用：
     * - 分页插件（PaginationInnerInterceptor）
     *
     * 待启用：
     * - 多租户插件（TenantLineInnerInterceptor）
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件配置
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置分页溢出处理：false 表示不处理溢出（超过最大页返回空）
        paginationInterceptor.setOverflow(false);
        // 设置单页最大查询数量限制
        paginationInterceptor.setMaxLimit(1000L);

        interceptor.addInnerInterceptor(paginationInterceptor);

        // TODO: 多租户：下一步加入 TenantLineInnerInterceptor
        // TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandler());
        // interceptor.addInnerInterceptor(tenantInterceptor);

        return interceptor;
    }
}
