package com.bluecone.app.infra.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 测试实体类
 *
 * 用途：
 * - 验证 MyBatis-Plus 基础功能
 * - 验证多租户拦截器是否正常工作
 * - 验证数据源连接是否正常
 *
 * 对应表：bc_test
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@TableName("bc_test")
public class TestEntity {

    /**
     * 主键 ID
     * 使用数据库自增策略
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID
     * 用于多租户数据隔离
     * MyBatis-Plus 的 TenantLineInnerInterceptor 会自动处理此字段
     */
    private String tenantId;

    /**
     * 名称
     * 测试用字段
     */
    private String name;

    /**
     * 创建时间
     * 数据库默认值：CURRENT_TIMESTAMP
     */
    private LocalDateTime createdAt;

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "TestEntity{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
