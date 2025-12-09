package com.bluecone.app.resource.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 资源绑定关系持久层 Mapper。
 */
@Mapper
public interface ResourceBindingMapper extends BaseMapper<ResourceBindingDO> {

    @Select("SELECT * FROM bc_res_binding " +
            "WHERE tenant_id = #{tenantId} AND owner_type = #{ownerType} AND owner_id = #{ownerId} AND purpose = #{purpose} AND resource_object_id = #{resourceObjectId} " +
            "LIMIT 1")
    ResourceBindingDO findByOwnerAndObject(@Param("tenantId") Long tenantId,
                                           @Param("ownerType") String ownerType,
                                           @Param("ownerId") Long ownerId,
                                           @Param("purpose") String purpose,
                                           @Param("resourceObjectId") Long resourceObjectId);
}
