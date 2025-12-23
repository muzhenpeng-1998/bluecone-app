package com.bluecone.app.infra.wechat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * bc_wechat_authorized_app 表对应的 Mapper（旧版本，用于兼容现有代码）。
 * 
 * @deprecated 建议使用 {@link com.bluecone.app.infra.wechat.openplatform.WechatAuthorizedAppMapper}
 */
@Deprecated
@Mapper
public interface WechatAuthorizedAppLegacyMapper extends BaseMapper<WechatAuthorizedAppDO> {
}

