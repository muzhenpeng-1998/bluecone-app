package com.bluecone.app.infra.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.user.dataobject.MemberPointsAccountDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_member_points_account 表对应的 Mapper。
 */
@Mapper
public interface MemberPointsAccountMapper extends BaseMapper<MemberPointsAccountDO> {
}
