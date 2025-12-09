package com.bluecone.app.resource.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资源绑定关系持久层 Mapper。
 */
@Mapper
public interface ResourceBindingMapper extends BaseMapper<ResourceBindingDO> {

    /**
     * 查询某业务对象特定用途下的绑定列表。
     */
    @Select("SELECT * FROM bc_res_binding WHERE owner_type = #{ownerType} AND owner_id = #{ownerId} AND purpose = #{purpose} ORDER BY sort_order ASC")
    List<ResourceBindingDO> listByOwner(@Param("ownerType") String ownerType,
                                        @Param("ownerId") Long ownerId,
                                        @Param("purpose") String purpose);
}
