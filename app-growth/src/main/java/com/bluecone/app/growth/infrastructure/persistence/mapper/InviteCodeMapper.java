package com.bluecone.app.growth.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.growth.infrastructure.persistence.po.InviteCodePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 邀请码Mapper
 */
@Mapper
public interface InviteCodeMapper extends BaseMapper<InviteCodePO> {
    
    /**
     * 增加邀请计数
     */
    @Update("UPDATE bc_growth_invite_code SET invites_count = invites_count + 1 WHERE id = #{id}")
    int incrementInvitesCount(@Param("id") Long id);
    
    /**
     * 增加成功邀请计数
     */
    @Update("UPDATE bc_growth_invite_code SET successful_invites_count = successful_invites_count + 1 WHERE id = #{id}")
    int incrementSuccessfulInvitesCount(@Param("id") Long id);
}
