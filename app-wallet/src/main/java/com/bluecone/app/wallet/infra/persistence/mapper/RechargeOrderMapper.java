package com.bluecone.app.wallet.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.wallet.infra.persistence.po.RechargeOrderPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 充值单Mapper
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Mapper
public interface RechargeOrderMapper extends BaseMapper<RechargeOrderPO> {
    
    /**
     * 乐观锁更新充值单
     */
    int updateWithVersion(@Param("po") RechargeOrderPO po);
}
