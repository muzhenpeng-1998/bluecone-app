package com.bluecone.app.tenant.model.command;

import java.time.LocalDateTime;

/**
 * 创建租户聚合命令。
 * - 只携带创建场景需要的业务字段，不暴露数据库细节
 * - initialPlanId/planExpireAt 允许空，表示创建后先挂免费版
 * - operatorId 预留审计来源，未来可由安全模块注入
 */
public record CreateTenantCommand(
        // 租户名称
        String tenantName,
        // 联系人姓名
        String contactPerson,
        // 联系人电话
        String contactPhone,
        // 联系人邮箱
        String contactEmail,
        // 备注信息
        String remark,
        // 主体类型：1-企业，2-个体工商户，3-个人
        Byte tenantType,
        // 工商主体名称
        String businessName,
        // 营业执照注册号
        String businessLicenseNo,
        // 营业执照图片地址
        String businessLicenseUrl,
        // 法人姓名
        String legalPersonName,
        // 法人身份证号
        String legalPersonIdNo,
        // 主体注册地址
        String address,
        // 操作人 ID，用于审计
        Long operatorId,
        // 初始化套餐 ID，可为空
        Long initialPlanId,
        // 初始化套餐到期时间，可为空
        LocalDateTime planExpireAt) {
}
