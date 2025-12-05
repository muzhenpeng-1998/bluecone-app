package com.bluecone.app.product.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 商品模块 MyBatis 配置，占位。
 * 负责扫描本模块下的 Mapper 接口。
 */
@Configuration
@MapperScan("com.bluecone.app.product.dao.mapper")
public class ProductMybatisConfig {
}
