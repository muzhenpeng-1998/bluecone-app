package com.bluecone.app.inventory.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.inventory.infra.po.InvStockDO;
import org.apache.ibatis.annotations.Param;

public interface InvStockMapper extends BaseMapper<InvStockDO> {

    /**
     * 基于乐观锁尝试增加锁定库存。
     *
     * @return 受影响行数，大于 0 表示更新成功。
     */
    int tryIncreaseLockedQty(@Param("tenantId") Long tenantId,
                             @Param("storeId") Long storeId,
                             @Param("itemId") Long itemId,
                             @Param("locationId") Long locationId,
                             @Param("lockQty") Long lockQty,
                             @Param("expectedVersion") Long expectedVersion);

    /**
     * 基于乐观锁尝试扣减库存（支付成功或确认后）。
     *
     * @return 受影响行数，大于 0 表示更新成功。
     */
    int tryDeductStock(@Param("tenantId") Long tenantId,
                       @Param("storeId") Long storeId,
                       @Param("itemId") Long itemId,
                       @Param("locationId") Long locationId,
                       @Param("deductQty") Long deductQty,
                       @Param("expectedVersion") Long expectedVersion);
}
