package com.bluecone.app.infra.user.query;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会员读模型 Mapper，执行多表查询。
 */
@Mapper
public interface MemberReadMapper {

    List<MemberListViewDO> selectMemberList(@Param("tenantId") Long tenantId,
                                            @Param("keyword") String keyword,
                                            @Param("levelId") Long levelId,
                                            @Param("status") Integer status,
                                            @Param("minGrowth") Integer minGrowth,
                                            @Param("maxGrowth") Integer maxGrowth,
                                            @Param("joinStart") LocalDateTime joinStart,
                                            @Param("joinEnd") LocalDateTime joinEnd,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    long countMemberList(@Param("tenantId") Long tenantId,
                         @Param("keyword") String keyword,
                         @Param("levelId") Long levelId,
                         @Param("status") Integer status,
                         @Param("minGrowth") Integer minGrowth,
                         @Param("maxGrowth") Integer maxGrowth,
                         @Param("joinStart") LocalDateTime joinStart,
                         @Param("joinEnd") LocalDateTime joinEnd);
}
