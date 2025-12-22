package com.bluecone.app.controller;

import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.service.DemoCacheService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 提供简单的 HTTP 接口验证缓存行为：
 * - GET 触发 @Cached 读取
 * - POST 更新并触发 @CacheEvict
 * - DELETE 手工调用 CacheClient.evict
 */
@Tag(name = "🛠️ 开发调试 > 缓存调试", description = "缓存功能测试接口")
@RestController
@RequestMapping("/demo-cache")
public class DemoCacheController {

    private final DemoCacheService demoCacheService;
    private final CacheClient cacheClient;

    public DemoCacheController(DemoCacheService demoCacheService, CacheClient cacheClient) {
        this.demoCacheService = demoCacheService;
        this.cacheClient = cacheClient;
    }

    @GetMapping("/{id}")
    public DemoCacheService.DemoValue get(@PathVariable Long id, @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        bindTenant(tenantId);
        try {
            return demoCacheService.load(id);
        } finally {
            TenantContext.clear();
        }
    }

    @PostMapping("/{id}")
    public DemoCacheService.DemoValue update(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body,
                                             @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        bindTenant(tenantId);
        String payload = body == null ? null : body.get("payload");
        try {
            return demoCacheService.update(id, payload);
        } finally {
            TenantContext.clear();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> evict(@PathVariable Long id, @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId) {
        bindTenant(tenantId);
        try {
            String resolvedTenant = TenantContext.getTenantId();
            cacheClient.evict(CacheProfileName.ORDER_DETAIL, CacheKey.forOrderDetail(resolvedTenant, id));
            return ResponseEntity.noContent().build();
        } finally {
            TenantContext.clear();
        }
    }

    private void bindTenant(String tenantId) {
        TenantContext.setTenantId(tenantId == null || tenantId.isBlank() ? "demo" : tenantId);
    }
}
