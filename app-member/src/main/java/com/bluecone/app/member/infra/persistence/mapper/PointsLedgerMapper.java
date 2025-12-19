package com.bluecone.app.member.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.member.infra.persistence.po.PointsLedgerPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
    
    /**
     * 查询指定业务ID的所有积分流水记录（用于运维诊断）
     * 
     * @param tenantId 租户ID
     * @param bizId 业务ID（通常是订单ID字符串）
     * @param limit 限制数量
     * @return 流水记录列表
     */
    @Select("SELECT * FROM bc_points_ledger " +
            "WHERE tenant_id = #{tenantId} " +
            "AND biz_id = #{bizId} " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<PointsLedgerPO> selectByBizId(@Param("tenantId") Long tenantId,
                                       @Param("bizId") String bizId,
                                       @Param("limit") int limit);
}
