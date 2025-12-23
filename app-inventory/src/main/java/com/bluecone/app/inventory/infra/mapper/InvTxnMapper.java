package com.bluecone.app.inventory.infra.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.inventory.infra.po.InvTxnDO;

@Mapper
public interface InvTxnMapper extends BaseMapper<InvTxnDO> {
}
