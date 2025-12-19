package com.bluecone.app.wallet.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletFreezePO;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 钱包冻结记录Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface WalletFreezeMapper extends BaseMapper<WalletFreezePO> {
    
    /**
     * 根据幂等键查询冻结记录
     */
    @Select("SELECT * FROM bc_wallet_freeze WHERE tenant_id = #{tenantId} AND idem_key = #{idemKey}")
    WalletFreezePO selectByIdemKey(@Param("tenantId") Long tenantId, @Param("idemKey") String idemKey);
    
    /**
     * 根据业务单ID查询冻结记录
     */
    @Select("SELECT * FROM bc_wallet_freeze WHERE tenant_id = #{tenantId} AND biz_type = #{bizType} AND biz_order_id = #{bizOrderId}")
    WalletFreezePO selectByBizOrderId(@Param("tenantId") Long tenantId, 
                                       @Param("bizType") String bizType, 
                                       @Param("bizOrderId") Long bizOrderId);
    
    /**
     * 使用乐观锁更新冻结记录
     */
    @Update("UPDATE bc_wallet_freeze SET " +
            "status = #{freeze.status}, committed_at = #{freeze.committedAt}, released_at = #{freeze.releasedAt}, " +
            "reverted_at = #{freeze.revertedAt}, version = #{freeze.version} + 1, " +
            "updated_at = #{freeze.updatedAt}, updated_by = #{freeze.updatedBy} " +
            "WHERE tenant_id = #{freeze.tenantId} AND id = #{freeze.id} AND version = #{freeze.version}")
    int updateWithVersion(@Param("freeze") WalletFreezePO freeze);
    
    /**
     * 查询过期的冻结记录
     */
    @Select("SELECT * FROM bc_wallet_freeze WHERE status = 'FROZEN' AND expires_at < #{now} ORDER BY frozen_at LIMIT #{limit}")
    List<WalletFreezePO> selectExpiredFreezes(@Param("now") LocalDateTime now, @Param("limit") int limit);
}
