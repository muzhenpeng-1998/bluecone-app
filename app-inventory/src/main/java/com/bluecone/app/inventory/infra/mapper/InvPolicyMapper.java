package com.bluecone.app.inventory.infra.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.inventory.infra.po.InvPolicyDO;

@Mapper
public interface InvPolicyMapper extends BaseMapper<InvPolicyDO> {
}
