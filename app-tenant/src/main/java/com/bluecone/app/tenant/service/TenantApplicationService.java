package com.bluecone.app.tenant.service;

import com.bluecone.app.tenant.model.TenantDetail;
import com.bluecone.app.tenant.model.TenantSummary;
import com.bluecone.app.tenant.model.command.ChangeTenantPlanCommand;
import com.bluecone.app.tenant.model.command.CreateTenantCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantBasicInfoCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantPlatformAccountCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantProfileCommand;
import com.bluecone.app.tenant.model.query.TenantQuery;
import java.util.List;

/**
 * 租户聚合的应用服务，对外暴露租户核心业务能力。
 *
 * 设计要点：
 * - 作为 app-application Controller 的唯一租户业务入口，避免胖 Controller。
 * - 入参统一使用 Command/Query，避免直接暴露 Entity。
 * - 每个方法都是一个清晰的业务语义（创建租户、变更套餐等），便于审计与扩展。
 * - 预留 operatorId 方便接入安全上下文，审计日志由实现类统一写入。
 */
public interface TenantApplicationService {

    Long createTenant(CreateTenantCommand command);

    void updateTenantBasicInfo(UpdateTenantBasicInfoCommand command);

    void updateTenantProfile(UpdateTenantProfileCommand command);

    void updateTenantPlatformAccount(UpdateTenantPlatformAccountCommand command);

    void changeTenantPlan(ChangeTenantPlanCommand command);

    TenantDetail getTenantDetail(Long tenantId);

    List<TenantSummary> listTenantSummary(TenantQuery query);
}
