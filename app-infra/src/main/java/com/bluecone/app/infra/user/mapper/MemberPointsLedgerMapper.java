package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberPointsLedgerDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_points_ledger 表对应的 Mapper。
 */
@Mapper
public interface MemberPointsLedgerMapper extends BaseMapper<MemberPointsLedgerDO> {
}
