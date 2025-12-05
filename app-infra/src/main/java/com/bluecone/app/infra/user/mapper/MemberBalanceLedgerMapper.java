package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberBalanceLedgerDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_balance_ledger 表对应的 Mapper。
 */
@Mapper
public interface MemberBalanceLedgerMapper extends BaseMapper<MemberBalanceLedgerDO> {
}
