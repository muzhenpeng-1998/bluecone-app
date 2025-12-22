package com.bluecone.app.api.admin.finance;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.billing.api.dto.*;
import com.bluecone.app.billing.application.BillingApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * 订阅计费管理接口（后台）
 */
@Tag(name = "🎛️ 平台管理后台 > 财务管理 > 账单管理", description = "平台后台订阅计费管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/billing")
@RequiredArgsConstructor
public class BillingAdminController {
    
    private final BillingApplicationService billingApplicationService;
    
    /**
     * 获取所有可用的套餐 SKU
     */
    @GetMapping("/plans")
    public List<PlanSkuDTO> listPlans() {
        log.info("[billing-admin] 获取套餐列表");
        return billingApplicationService.listPlans();
    }
    
    /**
     * 创建账单（返回支付参数）
     */
    @PostMapping("/invoices")
    public CreateInvoiceResult createInvoice(@RequestHeader("X-Tenant-Id") Long tenantId,
                                            @Valid @RequestBody CreateInvoiceCommand command) {
        // 设置租户ID
        command.setTenantId(tenantId);
        
        // 如果没有提供幂等键，自动生成
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            command.setIdempotencyKey("INV-" + UUID.randomUUID().toString());
        }
        
        log.info("[billing-admin] 创建账单，tenantId={}, planSkuId={}, idempotencyKey={}", 
                tenantId, command.getPlanSkuId(), command.getIdempotencyKey());
        
        return billingApplicationService.createInvoice(command);
    }
    
    /**
     * 分页查询账单
     */
    @GetMapping("/invoices")
    public Page<InvoiceDTO> listInvoices(@RequestHeader("X-Tenant-Id") Long tenantId,
                                        @RequestParam(defaultValue = "1") int pageNum,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        log.info("[billing-admin] 查询账单列表，tenantId={}, pageNum={}, pageSize={}", tenantId, pageNum, pageSize);
        return billingApplicationService.listInvoices(tenantId, pageNum, pageSize);
    }
    
    /**
     * 获取当前订阅
     */
    @GetMapping("/subscription")
    public SubscriptionDTO getSubscription(@RequestHeader("X-Tenant-Id") Long tenantId) {
        log.info("[billing-admin] 查询订阅，tenantId={}", tenantId);
        return billingApplicationService.getSubscription(tenantId);
    }
    
    /**
     * 续费订阅（生成续费账单）
     */
    @PostMapping("/subscription/renew")
    public CreateInvoiceResult renewSubscription(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                @Valid @RequestBody RenewSubscriptionCommand command) {
        // 设置租户ID
        command.setTenantId(tenantId);
        
        // 如果没有提供幂等键，自动生成
        if (command.getIdempotencyKey() == null || command.getIdempotencyKey().isEmpty()) {
            command.setIdempotencyKey("RENEW-" + UUID.randomUUID().toString());
        }
        
        log.info("[billing-admin] 续费订阅，tenantId={}, planSkuId={}, idempotencyKey={}", 
                tenantId, command.getPlanSkuId(), command.getIdempotencyKey());
        
        return billingApplicationService.renewSubscription(command);
    }
}
