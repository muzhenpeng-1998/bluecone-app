package com.bluecone.app.member.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.member.infra.persistence.po.PointsAccountPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 积分账户表 Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface PointsAccountMapper extends BaseMapper<PointsAccountPO> {
    
    /**
     * 根据会员ID查询积分账户
     */
    @Select("SELECT * FROM bc_points_account WHERE tenant_id = #{tenantId} AND member_id = #{memberId} LIMIT 1")
    PointsAccountPO selectByMemberId(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);
    
    /**
     * 使用乐观锁更新积分账户
     */
    @Update("UPDATE bc_points_account SET " +
            "available_points = #{availablePoints}, " +
            "frozen_points = #{frozenPoints}, " +
            "version = version + 1, " +
            "updated_at = NOW() " +
            "WHERE id = #{id} AND version = #{version}")
    int updateWithVersion(PointsAccountPO account);
}
