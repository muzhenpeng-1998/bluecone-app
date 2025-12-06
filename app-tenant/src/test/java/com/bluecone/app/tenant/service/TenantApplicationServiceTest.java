package com.bluecone.app.tenant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.entity.TenantAuditLog;
import com.bluecone.app.tenant.dao.entity.TenantMedia;
import com.bluecone.app.tenant.dao.entity.TenantPlan;
import com.bluecone.app.tenant.dao.entity.TenantPlatformAccount;
import com.bluecone.app.tenant.dao.entity.TenantProfile;
import com.bluecone.app.tenant.dao.entity.TenantSettings;
import com.bluecone.app.tenant.dao.service.ITenantAuditLogService;
import com.bluecone.app.tenant.dao.service.ITenantBillingService;
import com.bluecone.app.tenant.dao.service.ITenantMediaService;
import com.bluecone.app.tenant.dao.service.ITenantPlanService;
import com.bluecone.app.tenant.dao.service.ITenantPlatformAccountService;
import com.bluecone.app.tenant.dao.service.ITenantProfileService;
import com.bluecone.app.tenant.dao.service.ITenantService;
import com.bluecone.app.tenant.dao.service.ITenantSettingsService;
import com.bluecone.app.tenant.model.TenantDetail;
import com.bluecone.app.tenant.model.command.CreateTenantCommand;
import com.bluecone.app.tenant.model.command.UpdateTenantBasicInfoCommand;
import com.bluecone.app.tenant.service.impl.TenantApplicationServiceImpl;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TenantApplicationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantService tenantService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantProfileService tenantProfileService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantPlatformAccountService tenantPlatformAccountService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantSettingsService tenantSettingsService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantPlanService tenantPlanService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantBillingService tenantBillingService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantMediaService tenantMediaService;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ITenantAuditLogService tenantAuditLogService;

    @InjectMocks
    private TenantApplicationServiceImpl tenantApplicationService;

    @Test
    void createTenant_success() {
        when(tenantService.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(1L);
            return true;
        });
        when(tenantProfileService.save(any(TenantProfile.class))).thenReturn(true);
        when(tenantSettingsService.saveBatch(anyList())).thenReturn(true);
        when(tenantAuditLogService.save(any(TenantAuditLog.class))).thenReturn(true);

        Long tenantId = tenantApplicationService.createTenant(new CreateTenantCommand(
                "BlueCone Store",
                "Alice",
                "13800000000",
                "alice@bluecone.com",
                "demo tenant",
                (byte) 1,
                "BlueCone Ltd.",
                "BIZ123",
                null,
                "Alice",
                "IDNO",
                "Shenzhen",
                9L,
                null,
                null));

        assertEquals(1L, tenantId);
        verify(tenantProfileService).save(any(TenantProfile.class));
        verify(tenantAuditLogService).save(any(TenantAuditLog.class));
    }

    @Test
    void updateTenantBasicInfo_updatesAndAudits() {
        Tenant tenant = new Tenant();
        tenant.setId(2L);
        when(tenantService.getById(2L)).thenReturn(tenant);
        when(tenantService.updateById(any(Tenant.class))).thenReturn(true);
        when(tenantAuditLogService.save(any(TenantAuditLog.class))).thenReturn(true);

        tenantApplicationService.updateTenantBasicInfo(new UpdateTenantBasicInfoCommand(
                2L, "Updated Name", "Bob", "13900000000", "bob@bluecone.com", "note", 1, 8L));

        verify(tenantService).updateById(any(Tenant.class));
        verify(tenantAuditLogService).save(any(TenantAuditLog.class));
    }

    @Test
    void getTenantDetail_returnsAggregatedView() {
        Tenant tenant = new Tenant();
        tenant.setId(3L);
        tenant.setTenantCode("TEN001");
        tenant.setTenantName("Tenant Detail");
        tenant.setStatus(1);
        tenant.setContactPerson("C1");
        tenant.setContactPhone("13000000000");
        when(tenantService.getById(3L)).thenReturn(tenant);

        TenantProfile profile = new TenantProfile();
        profile.setTenantId(3L);
        profile.setBusinessName("Biz Name");
        LambdaQueryChainWrapper<TenantProfile> profileQuery = mock(LambdaQueryChainWrapper.class);
        when(tenantProfileService.lambdaQuery()).thenReturn(profileQuery);
        when(profileQuery.eq(any(), any())).thenReturn(profileQuery);
        when(profileQuery.one()).thenReturn(profile);

        TenantSettings planIdSetting = new TenantSettings();
        planIdSetting.setTenantId(3L);
        planIdSetting.setKeyName("plan.id");
        planIdSetting.setKeyValue("10");
        TenantSettings expireSetting = new TenantSettings();
        expireSetting.setTenantId(3L);
        expireSetting.setKeyName("plan.expireAt");
        expireSetting.setKeyValue(LocalDateTime.now().plusDays(30).toString());
        LambdaQueryChainWrapper<TenantSettings> settingsQuery = mock(LambdaQueryChainWrapper.class);
        when(tenantSettingsService.lambdaQuery()).thenReturn(settingsQuery);
        when(settingsQuery.eq(any(), any())).thenReturn(settingsQuery);
        when(settingsQuery.list()).thenReturn(List.of(planIdSetting, expireSetting));

        TenantPlan plan = new TenantPlan();
        plan.setId(10L);
        plan.setPlanName("Pro");
        plan.setPrice(BigDecimal.TEN);
        when(tenantPlanService.getById(10L)).thenReturn(plan);

        TenantPlatformAccount account = new TenantPlatformAccount();
        account.setId(5L);
        account.setPlatformType("wechat");
        LambdaQueryChainWrapper<TenantPlatformAccount> accountQuery = mock(LambdaQueryChainWrapper.class);
        when(tenantPlatformAccountService.lambdaQuery()).thenReturn(accountQuery);
        when(accountQuery.eq(any(), any())).thenReturn(accountQuery);
        when(accountQuery.list()).thenReturn(List.of(account));

        TenantMedia media = new TenantMedia();
        media.setId(6L);
        media.setMediaType("avatar");
        LambdaQueryChainWrapper<TenantMedia> mediaQuery = mock(LambdaQueryChainWrapper.class);
        when(tenantMediaService.lambdaQuery()).thenReturn(mediaQuery);
        when(mediaQuery.eq(any(), any())).thenReturn(mediaQuery);
        when(mediaQuery.list()).thenReturn(List.of(media));

        TenantDetail detail = tenantApplicationService.getTenantDetail(3L);

        assertThat(detail.getTenantId()).isEqualTo(3L);
        assertThat(detail.getPlanInfo()).isNotNull();
        assertThat(detail.getPlatformAccounts()).hasSize(1);
    }
}
