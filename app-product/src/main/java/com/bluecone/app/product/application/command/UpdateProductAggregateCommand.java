package com.bluecone.app.product.application.command;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 更新商品聚合命令
 * 
 * <p>继承自 {@link CreateProductAggregateCommand}，结构完全相同。
 * <p>更新策略：子表全量覆盖（delete+insert）。
 * 
 * @author BlueCone Team
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UpdateProductAggregateCommand extends CreateProductAggregateCommand {
    
    // 与 CreateProductAggregateCommand 结构完全相同
    // 仅在语义上区分创建和更新
}

