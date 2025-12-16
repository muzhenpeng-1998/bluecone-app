package com.bluecone.app.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.web.ResolvePublicId;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import org.springframework.web.bind.annotation.*;

/**
 * 商户侧门店接口（Public ID Governance 示范）。
 * 
 * <p>改造要点：</p>
 * <ul>
 *   <li>入参：使用 publicId（String）替代 Long storeId</li>
 *   <li>解析：使用 @ResolvePublicId 自动解析为 Long 主键</li>
 *   <li>校验：自动执行 Scope Guard 校验（租户隔离 + 门店隔离）</li>
 *   <li>响应：返回 publicId，不暴露 Long storeId</li>
 * </ul>
 * 
 * <p>对比旧接口：</p>
 * <pre>
 * // 旧接口（不推荐）
 * &#64;GetMapping("/detail")
 * public ApiResponse&lt;StoreBaseView&gt; detail(&#64;RequestParam Long storeId) {
 *     // 问题：暴露内部主键，存在枚举越权风险
 *     return ApiResponse.success(storeFacade.getStoreBase(tenantId, storeId));
 * }
 * 
 * // 新接口（推荐）
 * &#64;GetMapping("/stores/{storeId}")
 * public ApiResponse&lt;StoreBaseView&gt; detail(&#64;PathVariable &#64;ResolvePublicId(type=STORE) Long storePk) {
 *     // 优点：publicId 自动解析 + Scope Guard 校验，防止越权
 *     return ApiResponse.success(storeFacade.getStoreBase(tenantId, storePk));
 * }
 * </pre>
 */
@RestController
@RequestMapping("/api/merchant/stores")
public class MerchantStoreController {

    private final StoreFacade storeFacade;

    public MerchantStoreController(StoreFacade storeFacade) {
        this.storeFacade = storeFacade;
    }

    /**
     * 获取门店详情（使用 Long 主键注入）。
     * 
     * <p>推荐场景：服务层接口已优化为接收 Long 主键</p>
     * 
     * <p>请求示例：</p>
     * <pre>
     * GET /api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ
     * </pre>
     * 
     * <p>执行流程：</p>
     * <ol>
     *   <li>提取 storeId 参数：sto_01HN8X5K9G3QRST2VW4XYZ</li>
     *   <li>校验格式：前缀 sto_ + 26 位 ULID</li>
     *   <li>查询主键：SELECT id FROM bc_store WHERE tenant_id=? AND public_id=?</li>
     *   <li>Scope Guard：校验 tenantId 和 storePk</li>
     *   <li>注入参数：storePk = 12345（Long）</li>
     *   <li>调用服务：storeFacade.getStoreBase(tenantId, storePk)</li>
     * </ol>
     * 
     * @param storePk 门店主键（自动从 publicId 解析）
     * @return 门店详情视图
     */
    @GetMapping("/{storeId}")
    public ApiResponse<StoreBaseView> detail(
            @PathVariable("storeId") @ResolvePublicId(type = ResourceType.STORE) Long storePk) {
        // storePk 已自动解析并通过 Scope Guard 校验
        Long tenantId = requireTenantId();
        StoreBaseView view = storeFacade.getStoreBase(tenantId, storePk);
        return ApiResponse.success(view);
    }

    /**
     * 获取门店详情（使用 ResolvedPublicId 注入）。
     * 
     * <p>推荐场景：需要同时使用 publicId 和主键（如日志/审计）</p>
     * 
     * <p>请求示例：</p>
     * <pre>
     * GET /api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ/full
     * </pre>
     * 
     * @param resolved 完整解析结果（包含 type/publicId/tenantId/pk）
     * @return 门店详情视图
     */
    @GetMapping("/{storeId}/full")
    public ApiResponse<StoreDetailResponse> detailWithPublicId(
            @PathVariable("storeId") @ResolvePublicId(type = ResourceType.STORE) ResolvedPublicId resolved) {
        // 提取主键和 publicId
        Long storePk = resolved.asLong();
        String publicId = resolved.publicId();
        
        // 调用服务
        Long tenantId = requireTenantId();
        StoreDetailQuery query = new StoreDetailQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storePk);
        
        StoreBaseView view = storeFacade.detail(query);
        
        // 响应中包含 publicId（不暴露 Long storeId）
        return ApiResponse.success(new StoreDetailResponse(
                publicId,
                view.getName(),
                view.getShortName(),
                view.getLogoUrl()
        ));
    }

    /**
     * 查询门店列表（可选参数示范）。
     * 
     * <p>请求示例：</p>
     * <pre>
     * GET /api/merchant/stores?storeId=sto_01HN8X5K9G3QRST2VW4XYZ
     * GET /api/merchant/stores  （不传 storeId，查询所有）
     * </pre>
     * 
     * @param storePk 门店主键（可选，自动从 publicId 解析）
     * @return 门店列表
     */
    @GetMapping
    public ApiResponse<java.util.List<StoreBaseView>> list(
            @RequestParam(value = "storeId", required = false)
            @ResolvePublicId(type = ResourceType.STORE, required = false) Long storePk) {
        // storePk 可能为 null（未传 storeId 参数）
        Long tenantId = requireTenantId();
        // 业务逻辑：根据 storePk 过滤或查询所有
        if (storePk != null) {
            StoreBaseView view = storeFacade.getStoreBase(tenantId, storePk);
            return ApiResponse.success(java.util.List.of(view));
        } else {
            // 查询租户下所有门店（实际业务中可能需要分页）
            return ApiResponse.success(java.util.List.of());
        }
    }

    /**
     * 响应 DTO（仅包含 publicId，不暴露 Long storeId）。
     */
    public record StoreDetailResponse(
            String storePublicId,
            String name,
            String shortName,
            String logoUrl
    ) {}

    /**
     * 从上下文获取租户 ID（辅助方法）。
     */
    private Long requireTenantId() {
        // 实际实现：从 TenantContext 或 ApiContext 获取
        String tenantId = com.bluecone.app.infra.tenant.TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("租户上下文未设置");
        }
        return Long.parseLong(tenantId);
    }
}

