package com.bluecone.app.wallet.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 钱包账本流水Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface WalletLedgerMapper extends BaseMapper<WalletLedgerPO> {
    
    /**
     * 根据幂等键查询流水
     */
    @Select("SELECT * FROM bc_wallet_ledger WHERE tenant_id = #{tenantId} AND idem_key = #{idemKey}")
    WalletLedgerPO selectByIdemKey(@Param("tenantId") Long tenantId, @Param("idemKey") String idemKey);
    
    /**
     * 根据用户ID分页查询流水
     */
    @Select("SELECT * FROM bc_wallet_ledger WHERE tenant_id = #{tenantId} AND user_id = #{userId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<WalletLedgerPO> selectByUserId(@Param("tenantId") Long tenantId, 
                                        @Param("userId") Long userId,
                                        @Param("offset") int offset, 
                                        @Param("limit") int limit);
    
    /**
     * 根据账户ID分页查询流水
     */
    @Select("SELECT * FROM bc_wallet_ledger WHERE tenant_id = #{tenantId} AND account_id = #{accountId} ORDER BY created_at DESC LIMIT #{offset}, #{limit}")
    List<WalletLedgerPO> selectByAccountId(@Param("tenantId") Long tenantId, 
                                           @Param("accountId") Long accountId,
                                           @Param("offset") int offset, 
                                           @Param("limit") int limit);
    
    /**
     * 查询指定订单的所有钱包流水记录（用于运维诊断）
     * 
     * @param tenantId 租户ID
     * @param bizOrderId 业务订单ID
     * @param limit 限制数量
     * @return 流水记录列表
     */
    @Select("SELECT * FROM bc_wallet_ledger " +
            "WHERE tenant_id = #{tenantId} " +
            "AND biz_order_id = #{bizOrderId} " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<WalletLedgerPO> selectByBizOrderIdForForensics(@Param("tenantId") Long tenantId,
                                                         @Param("bizOrderId") Long bizOrderId,
                                                         @Param("limit") int limit);
}
