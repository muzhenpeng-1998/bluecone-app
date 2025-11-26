package com.bluecone.app.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.entity.TestEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 测试 Mapper
 *
 * 用途：
 * - 验证 MyBatis-Plus BaseMapper 功能
 * - 验证多租户拦截器自动添加 WHERE tenant_id = ? 条件
 * - 验证数据库连接和 SQL 执行
 *
 * 继承 BaseMapper 后自动拥有以下方法：
 * - insert: 插入一条记录
 * - deleteById: 根据 ID 删除
 * - updateById: 根据 ID 更新
 * - selectById: 根据 ID 查询
 * - selectList: 查询列表
 * - selectCount: 查询总数
 * - 等等...
 *
 * 注意：
 * - 所有查询操作都会自动添加租户条件（由 TenantLineInnerInterceptor 处理）
 * - 插入操作会自动填充 tenant_id 字段
 *
 * @author BlueCone Architecture Team
 * @since 1.0.0
 */
@Mapper
public interface TestMapper extends BaseMapper<TestEntity> {
    // BaseMapper 已提供所有基础 CRUD 方法
    // 如需自定义 SQL，可在此添加方法并配合 XML 或 @Select 注解使用
}
