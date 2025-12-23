package com.bluecone.app.store.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.store.dao.entity.BcStoreReadModel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 门店只读快照表 Mapper，对应表 bc_store_read_model。
 */
@Mapper
public interface BcStoreReadModelMapper extends BaseMapper<BcStoreReadModel> {
}

