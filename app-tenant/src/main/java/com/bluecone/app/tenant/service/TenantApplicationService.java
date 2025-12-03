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
 * 租户聚合的应用服务，对外暴露业务能力。
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
