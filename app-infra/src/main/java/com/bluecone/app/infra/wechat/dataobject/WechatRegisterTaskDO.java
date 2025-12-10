package com.bluecone.app.infra.wechat.dataobject;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 微信小程序快速注册任务表映射，表名：bc_wechat_register_task。
 * 记录从发起到回调的完整生命周期。
 */
@Data
@TableName("bc_wechat_register_task")
public class WechatRegisterTaskDO {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户 ID，对应 bc_tenant.id
     */
    private Long tenantId;

    /**
     * 首家门店 ID，对应 bc_store.id，可为空（租户级注册时）
     */
    private Long storeId;

    /**
     * 注册类型：FORMAL-正式小程序，TRIAL-试用小程序
     */
    private String registerType;

    /**
     * 微信侧注册任务编号/申请单号（如接口返回）
     */
    private String wechatTaskNo;

    /**
     * 注册/授权成功后的小程序 appid，成功后回填
     */
    private String authorizerAppid;

    /**
     * 发给微信开放平台的请求报文快照（JSON）
     */
    private String requestPayloadJson;

    /**
     * 微信同步返回的报文快照（JSON）
     */
    private String responsePayloadJson;

    /**
     * 任务状态：0-待发起，1-处理中，2-成功，3-失败
     */
    private Integer status;

    /**
     * 失败错误码（微信返回的 errcode 等）
     */
    private String failCode;

    /**
     * 失败原因描述，方便运营定位问题
     */
    private String failReason;

    /**
     * 接收到微信注册/认证结果回调的次数
     */
    private Integer notifyCount;

    /**
     * 最近一次收到微信回调的时间
     */
    private LocalDateTime lastNotifyAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}

