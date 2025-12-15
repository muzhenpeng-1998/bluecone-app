package com.bluecone.app.infra.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.bluecone.app.infra.tenant.TenantLineHandlerImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * 功能：
 * 1. 配置 Mapper 扫描路径
 * 2. 注册多租户拦截器（TenantLineInnerInterceptor）
 * 3. 注册分页插件（PaginationInnerInterceptor）
 *
 * 拦截器执行顺序：
 * 1. 多租户拦截器：自动在 SQL 中添加 tenant_id 条件
 * 2. 分页拦截器：处理分页查询
 *
 * 注意：
 * - 使用 @ConditionalOnProperty 控制启用/禁用
 * - 在 application.yml 中配置 mybatis-plus.enabled=true 启用
 * - 多租户功能依赖 TenantContext（ThreadLocal）存储租户 ID
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Configuration
@MapperScan({
        "com.bluecone.app.**.mapper",
        "com.bluecone.app.infra.security.session",
        "com.bluecone.app.infra.user.query",
        "com.bluecone.app.infra.idempotency",
        "com.bluecone.app.infra.event.consume",
        "com.bluecone.app.infra.idresolve",
        "com.bluecone.app.infra.cacheinval",
        // 支付模块 Mapper（位于 infrastructure.persistence 包下）
        "com.bluecone.app.payment.infrastructure.persistence"
})
@ConditionalOnClass(MybatisPlusInterceptor.class)
@ConditionalOnProperty(name = "mybatis-plus.enabled", havingValue = "true", matchIfMissing = false)
public class MybatisPlusConfig {

    /**
     * MyBatis-Plus 拦截器配置
     *
     * 拦截器链（按顺序执行）：
     * 1. TenantLineInnerInterceptor - 多租户行级数据隔离
     *    - 自动在 SELECT/UPDATE/DELETE 语句中添加 WHERE tenant_id = ?
     *    - 自动在 INSERT 语句中填充 tenant_id 字段
     *
     * 2. PaginationInnerInterceptor - 分页插件
     *    - 自动处理分页查询
     *    - 支持多种数据库方言
     *    - 限制单页最大查询数量
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1. 多租户拦截器（必须放在分页拦截器之前）
        TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor(new TenantLineHandlerImpl());
        interceptor.addInnerInterceptor(tenantInterceptor);

        // 2. 分页拦截器
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        // 设置分页溢出处理：false 表示不处理溢出（超过最大页返回空）
        paginationInterceptor.setOverflow(false);
        // 设置单页最大查询数量限制
        paginationInterceptor.setMaxLimit(1000L);
        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
