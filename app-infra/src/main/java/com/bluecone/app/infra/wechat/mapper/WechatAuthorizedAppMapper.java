package com.bluecone.app.infra.wechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.wechat.dataobject.WechatAuthorizedAppDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_wechat_authorized_app 表对应的 Mapper。
 */
@Mapper
public interface WechatAuthorizedAppMapper extends BaseMapper<WechatAuthorizedAppDO> {
}

