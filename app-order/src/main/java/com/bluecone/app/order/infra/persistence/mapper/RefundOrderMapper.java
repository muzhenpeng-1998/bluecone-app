package com.bluecone.app.order.infra.persistence.mapper;

import com.bluecone.app.order.infra.persistence.po.RefundOrderPO;
import org.apache.ibatis.annotations.*;

/**
 * 退款单 Mapper。
 */
@Mapper
public interface RefundOrderMapper {
    
    /**
     * 根据租户和退款单ID查询退款单。
     * 
     * @param tenantId 租户ID
     * @param refundOrderId 退款单ID
     * @return 退款单PO，不存在返回 null
     */
    @Select("SELECT * FROM bc_refund_order WHERE tenant_id = #{tenantId} AND id = #{refundOrderId}")
    RefundOrderPO findById(@Param("tenantId") Long tenantId, @Param("refundOrderId") Long refundOrderId);
    
    /**
     * 根据租户和幂等键查询退款单。
     * 
     * @param tenantId 租户ID
     * @param idemKey 幂等键
     * @return 退款单PO，不存在返回 null
     */
    @Select("SELECT * FROM bc_refund_order WHERE tenant_id = #{tenantId} AND idem_key = #{idemKey}")
    RefundOrderPO findByIdemKey(@Param("tenantId") Long tenantId, @Param("idemKey") String idemKey);
    
    /**
     * 根据租户和订单ID查询最近一笔退款单。
     * 
     * @param tenantId 租户ID
     * @param orderId 订单ID
     * @return 退款单PO，不存在返回 null
     */
    @Select("SELECT * FROM bc_refund_order WHERE tenant_id = #{tenantId} AND order_id = #{orderId} ORDER BY created_at DESC LIMIT 1")
    RefundOrderPO findLatestByOrderId(@Param("tenantId") Long tenantId, @Param("orderId") Long orderId);
    
    /**
     * 新建退款单。
     * 
     * @param po 退款单PO
     * @return 插入行数
     */
    @Insert("INSERT INTO bc_refund_order (id, tenant_id, store_id, order_id, public_order_no, refund_id, channel, " +
            "refund_amount, currency, status, refund_no, reason_code, reason_desc, idem_key, pay_order_id, pay_no, " +
            "refund_requested_at, refund_completed_at, ext_json, version, created_at, created_by, updated_at, updated_by) " +
            "VALUES (#{id}, #{tenantId}, #{storeId}, #{orderId}, #{publicOrderNo}, #{refundId}, #{channel}, " +
            "#{refundAmount}, #{currency}, #{status}, #{refundNo}, #{reasonCode}, #{reasonDesc}, #{idemKey}, " +
            "#{payOrderId}, #{payNo}, #{refundRequestedAt}, #{refundCompletedAt}, #{extJson}, #{version}, " +
            "#{createdAt}, #{createdBy}, #{updatedAt}, #{updatedBy})")
    int insert(RefundOrderPO po);
    
    /**
     * 更新退款单（使用乐观锁）。
     * 
     * @param po 退款单PO
     * @return 更新行数（用于判断乐观锁是否成功）
     */
    @Update("UPDATE bc_refund_order SET " +
            "status = #{status}, refund_no = #{refundNo}, refund_completed_at = #{refundCompletedAt}, " +
            "ext_json = #{extJson}, version = #{version} + 1, updated_at = #{updatedAt}, updated_by = #{updatedBy} " +
            "WHERE tenant_id = #{tenantId} AND id = #{id} AND version = #{version}")
    int updateWithVersion(RefundOrderPO po);
}
