package com.bluecone.app.product.domain.repository;

import com.bluecone.app.product.domain.enums.ProductStatus;
import com.bluecone.app.product.domain.model.Product;
import com.bluecone.app.product.domain.model.readmodel.StoreMenuSnapshot;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 商品聚合根的领域仓储接口，负责在多张表之间组装/拆分 Product 聚合（SPU、SKU、规格、属性、小料、标签等）。
 * <p>屏蔽 MyBatis/SQL 细节，领域层通过语义化方法完成商品的读写。</p>
 */
public interface ProductRepository {

    Optional<Product> findById(Long tenantId, Long productId);

    Optional<Product> findByCode(Long tenantId, String productCode);

    List<Product> findByIds(Long tenantId, Collection<Long> productIds);

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
}
