package com.bluecone.app.wallet.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.wallet.infra.persistence.po.WalletAccountPO;
import org.apache.ibatis.annotations.*;

/**
 * 钱包账户Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface WalletAccountMapper extends BaseMapper<WalletAccountPO> {
    
    /**
     * 根据用户ID查询账户
     */
    @Select("SELECT * FROM bc_wallet_account WHERE tenant_id = #{tenantId} AND user_id = #{userId}")
    WalletAccountPO selectByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
    
    /**
     * 使用乐观锁更新账户
     */
    @Update("UPDATE bc_wallet_account SET " +
            "available_balance = #{account.availableBalance}, frozen_balance = #{account.frozenBalance}, " +
            "total_recharged = #{account.totalRecharged}, total_consumed = #{account.totalConsumed}, " +
            "status = #{account.status}, version = #{account.version} + 1, " +
            "updated_at = #{account.updatedAt}, updated_by = #{account.updatedBy} " +
            "WHERE tenant_id = #{account.tenantId} AND id = #{account.id} AND version = #{account.version}")
    int updateWithVersion(@Param("account") WalletAccountPO account);
}
