package com.bluecone.app.member.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.member.infra.persistence.po.MemberPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 会员表 Mapper
 * 
 * @author bluecone
 * @since 2025-12-18
 */
@Mapper
public interface MemberMapper extends BaseMapper<MemberPO> {
    
    /**
     * 根据用户ID查询会员
     */
    @Select("SELECT * FROM bc_member WHERE tenant_id = #{tenantId} AND user_id = #{userId} LIMIT 1")
    MemberPO selectByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
    
    /**
     * 根据会员ID查询会员
     */
    @Select("SELECT * FROM bc_member WHERE tenant_id = #{tenantId} AND id = #{memberId} LIMIT 1")
    MemberPO selectByMemberId(@Param("tenantId") Long tenantId, @Param("memberId") Long memberId);
}
