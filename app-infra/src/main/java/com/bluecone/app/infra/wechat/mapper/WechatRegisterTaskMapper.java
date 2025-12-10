package com.bluecone.app.infra.wechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.wechat.dataobject.WechatRegisterTaskDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_wechat_register_task 表对应的 Mapper。
 */
@Mapper
public interface WechatRegisterTaskMapper extends BaseMapper<WechatRegisterTaskDO> {
}

