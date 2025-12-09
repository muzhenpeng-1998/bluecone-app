package com.bluecone.app.resource.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资源对象持久层 Mapper。
 */
@Mapper
public interface ResourceObjectMapper extends BaseMapper<ResourceObjectDO> {

    /**
     * 按租户 + profile + hash 查询资源，便于秒传及去重。
     */
    @Select("SELECT * FROM bc_res_object WHERE tenant_id = #{tenantId} AND profile_code = #{profileCode} AND hash_sha256 = #{hashSha256} LIMIT 1")
    ResourceObjectDO findByTenantProfileHash(@Param("tenantId") Long tenantId,
                                             @Param("profileCode") String profileCode,
                                             @Param("hashSha256") String hashSha256);
}
