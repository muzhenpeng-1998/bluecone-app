package com.bluecone.app.payment.infrastructure.persistence.reconcile;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReconcileResultMapper extends BaseMapper<ReconcileResultDO> {
}
