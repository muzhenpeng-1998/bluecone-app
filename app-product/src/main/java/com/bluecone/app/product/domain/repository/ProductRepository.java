package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.ProductSku;
import com.bluecone.app.product.domain.model.readmodel.StoreMenuSnapshot;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 商品聚合根的领域仓储接口，负责在多张表之间组装/拆分 Product 聚合（SPU、SKU、规格、属性、小料、标签等）。
 * <p>屏蔽 MyBatis/SQL 细节，领域层通过语义化方法完成商品的读写。</p>
 */
public interface ProductRepository {

    Optional<Product> findById(Long tenantId, Long productId);

    Optional<Product> findByCode(Long tenantId, String productCode);

    List<Product> findByIds(Long tenantId, Collection<Long> productIds);

    /**
     * 根据 SKU ID 精确读取单个 SKU（包含已下架 SKU）。
     */
    Optional<ProductSku> findSkuById(Long tenantId, Long skuId);

    /**
     * 新建商品聚合，返回生成的 productId。
     */
    Long save(Product product);

    /**
     * 更新商品及聚合内的子对象（SKU/规格/属性/小料/标签等）。
     */
    void update(Product product);

    /**
     * 修改商品状态（草稿/启用/禁用），可结合操作人做审计。
     */
    void changeStatus(Long tenantId, Long productId, ProductStatus newStatus, Long operatorId);

    /**
     * 更新 SKU 状态，用于上下架或逻辑删除。
     */
    void updateSkuStatus(Long tenantId, Long skuId, ProductStatus status, Long operatorId);

    /**
     * 更新 SKU 售价（基础价），支持运营改价等业务场景。
     *
     * @param newPrice 新售价（单位：元，对应数据库 DECIMAL）
     */
    void updateSkuPrice(Long tenantId, Long skuId, BigDecimal newPrice, Long operatorId);

    /**
     * 高并发读路径：加载单个商品在指定门店与渠道下的完整聚合（可直接用于缓存）。
     */
    Product loadProductAggregate(Long tenantId, Long productId, Long storeId, String channel);

    /**
     * 高并发读路径：按门店+渠道加载可售商品列表（可缓存的聚合视图）。
     */
    List<Product> loadAvailableProductsForStore(Long tenantId, Long storeId, String channel);

    /**
     * 读取门店菜单快照，用于高并发菜单拉取。
     */
    StoreMenuSnapshot loadStoreMenuSnapshot(Long tenantId, Long storeId, String channel, String orderScene);

    /**
     * 写入或覆盖门店菜单快照（简单 upsert，后续可加乐观锁与缓存刷新）。
     */
    void saveOrUpdateStoreMenuSnapshot(StoreMenuSnapshot snapshot);

    // ===== Prompt 05: 聚合写入与批量加载 =====

    /**
     * 保存商品聚合（包含所有子表）。
     * <p>使用批量插入优化性能。
     *
     * @param product 商品聚合根
     * @return 商品ID
     */
    Long saveAggregate(Product product);

    /**
     * 更新商品聚合（包含所有子表）。
     * <p>使用批量插入优化性能，采用 delete+insert 策略。
     *
     * @param product 商品聚合根
     */
    void updateAggregate(Product product);

    /**
     * 加载单个商品聚合（包含所有子表）。
     * <p>统一 deleted/status 过滤。
     *
     * @param tenantId 租户ID
     * @param productId 商品ID
     * @return 商品聚合根
     */
    Optional<Product> loadAggregate(Long tenantId, Long productId);

    /**
     * 批量加载商品聚合（包含所有子表）。
     * <p>使用 IN 查询一次性拉取所有相关数据，在内存中按 productId 聚合。
     * <p>禁止 N+1 循环查库，SQL 次数为常数级（十几条以内），不随产品数量线性增长。
     *
     * @param tenantId 租户ID
     * @param productIds 商品ID集合
     * @return 商品聚合根列表
     */
    List<Product> loadAggregatesBatch(Long tenantId, Set<Long> productIds);
}
