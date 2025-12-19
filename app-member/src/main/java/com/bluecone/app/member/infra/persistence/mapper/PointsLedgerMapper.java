package com.bluecone.app.member.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.member.infra.persistence.po.PointsLedgerPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 积分流水表 Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface PointsLedgerMapper extends BaseMapper<PointsLedgerPO> {
    
    /**
     * 根据幂等键查询流水记录
     */
    @Select("SELECT * FROM bc_points_ledger WHERE tenant_id = #{tenantId} AND idempotency_key = #{idempotencyKey} LIMIT 1")
    PointsLedgerPO selectByIdempotencyKey(@Param("tenantId") Long tenantId, @Param("idempotencyKey") String idempotencyKey);
}
