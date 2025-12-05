package com.bluecone.app.product.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.product.dao.entity.StoreMenuSnapshotEntity;

/**
 * 门店菜单快照表 Mapper，对应表 {@code bc_store_menu_snapshot}，支撑高并发读取门店菜单快照。
 */
public interface StoreMenuSnapshotMapper extends BaseMapper<StoreMenuSnapshotEntity> {
}
