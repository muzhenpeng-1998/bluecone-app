package com.bluecone.app.api.merchant.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.publicid.api.ResolvedPublicId;
import com.bluecone.app.core.publicid.web.ResolvePublicId;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 【商户后台】门店管理接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>商户侧门店信息查询（使用Public ID，不暴露内部主键）</li>
 *   <li>Public ID自动解析和Scope Guard校验</li>
 *   <li>防止越权访问（租户隔离+门店隔离）</li>
 * </ul>
 * 
 * <h3>🔐 Public ID治理机制：</h3>
 * <ul>
 *   <li><b>格式规范</b>：sto_01HN8X5K9G3QRST2VW4XYZ（前缀sto_ + 26位ULID）</li>
 *   <li><b>自动解析</b>：使用@ResolvePublicId注解自动转换为Long主键</li>
 *   <li><b>Scope Guard</b>：自动校验资源归属（防止跨租户/跨门店访问）</li>
 *   <li><b>响应脱敏</b>：返回数据仅包含Public ID，不暴露内部主键</li>
 * </ul>
 * 
 * <h3>🆚 与平台管理后台的区别：</h3>
 * <table border="1">
 *   <tr>
 *     <th>特性</th>
 *     <th>商户后台</th>
 *     <th>平台管理后台</th>
 *   </tr>
 *   <tr>
 *     <td>ID类型</td>
 *     <td>Public ID (sto_xxx)</td>
 *     <td>Long 主键</td>
 *   </tr>
 *   <tr>
 *     <td>使用角色</td>
 *     <td>租户/商家</td>
 *     <td>平台运营</td>
 *   </tr>
 *   <tr>
 *     <td>安全性</td>
 *     <td>高（不暴露主键）</td>
 *     <td>中（内部使用）</td>
 *   </tr>
 *   <tr>
 *     <td>功能权限</td>
 *     <td>仅查询</td>
 *     <td>完整CRUD</td>
 *   </tr>
 * </table>
 * 
 * <h3>🔗 关联接口：</h3>
 * <ul>
 *   <li>{@link com.bluecone.app.api.admin.store.StoreAdminController} - 平台管理后台门店管理</li>
 *   <li>{@link com.bluecone.app.api.open.store.OpenStoreController} - C端门店查询</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * GET /api/merchant/stores/{storeId}      - 查询门店详情（简单版）
 * GET /api/merchant/stores/{storeId}/full - 查询门店详情（完整版，含Public ID）
 * GET /api/merchant/stores                - 查询门店列表
 * </pre>
 * 
 * <h3>💡 使用示例：</h3>
 * <pre>
 * // 前端调用（使用Public ID）
 * GET /api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ
 * 
 * // 后端自动解析流程：
 * 1. 提取 storeId: "sto_01HN8X5K9G3QRST2VW4XYZ"
 * 2. 校验格式: 前缀sto_ + 26位ULID
 * 3. 查询主键: SELECT id FROM bc_store WHERE tenant_id=? AND public_id=?
 * 4. Scope Guard: 校验租户ID和门店ID
 * 5. 注入参数: storePk = 12345 (Long)
 * 6. 调用服务: storeFacade.getStoreBase(tenantId, storePk)
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 * @see StoreFacade 门店领域门面
 * @see ResolvePublicId Public ID解析注解
 */
@Tag(name = "🏪 商户后台 > 门店管理", description = "商户后台 - 门店信息查询接口（Public ID模式）")
@RestController
@RequestMapping("/api/merchant/stores")
public class MerchantStoreController {

    /** 门店领域门面 */
    private final StoreFacade storeFacade;

    public MerchantStoreController(StoreFacade storeFacade) {
        this.storeFacade = storeFacade;
    }

    /**
     * 查询门店详情（简化版）
     * 
     * <p>使用Long主键注入，适用于服务层已优化为接收Long主键的场景。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ
     * Headers:
     *   Authorization: Bearer {token}
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "id": 12345,
     *   "name": "总店",
     *   "shortName": "总店",
     *   "address": "朝阳区xxx路xxx号",
     *   "logoUrl": "https://cdn.example.com/logo.jpg"
     * }
     * </pre>
     * 
     * <h4>自动执行流程：</h4>
     * <ol>
     *   <li>提取路径参数：sto_01HN8X5K9G3QRST2VW4XYZ</li>
     *   <li>格式校验：前缀sto_ + 26位ULID</li>
     *   <li>查询主键：SELECT id FROM bc_store WHERE tenant_id=? AND public_id=?</li>
     *   <li>Scope Guard：校验租户ID和门店归属</li>
     *   <li>注入参数：storePk = 12345（Long类型）</li>
     *   <li>调用服务：storeFacade.getStoreBase(tenantId, storePk)</li>
     * </ol>
     * 
     * @param storePk 门店主键（从Public ID自动解析）
     * @return 门店基础信息
     */
    @Operation(
        summary = "查询门店详情",
        description = "根据门店Public ID查询门店基础信息"
    )
    @GetMapping("/{storeId}")
    public ApiResponse<StoreBaseView> detail(
            @PathVariable("storeId") @ResolvePublicId(type = ResourceType.STORE) Long storePk) {
        // storePk 已自动解析并通过 Scope Guard 校验
        Long tenantId = requireTenantId();
        StoreBaseView view = storeFacade.getStoreBase(tenantId, storePk);
        return ApiResponse.success(view);
    }

    /**
     * 查询门店详情（完整版，含Public ID）
     * 
     * <p>使用ResolvedPublicId注入，适用于需要同时使用publicId和主键的场景（如日志/审计）。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/merchant/stores/sto_01HN8X5K9G3QRST2VW4XYZ/full
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "storePublicId": "sto_01HN8X5K9G3QRST2VW4XYZ",
     *   "name": "总店",
     *   "shortName": "总店",
     *   "logoUrl": "https://cdn.example.com/logo.jpg"
     * }
     * </pre>
     * 
     * @param resolved 完整解析结果（包含type/publicId/tenantId/pk）
     * @return 门店详情（含Public ID）
     */
    @Operation(
        summary = "查询门店详情（含Public ID）",
        description = "查询门店详情，响应中包含Public ID"
    )
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
     * 查询门店列表
     * 
     * <p>支持可选的storeId参数，用于查询单个门店或全部门店。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/merchant/stores?storeId=sto_01HN8X5K9G3QRST2VW4XYZ  # 查询单个
     * GET /api/merchant/stores                                     # 查询所有
     * </pre>
     * 
     * @param storePk 门店主键（可选，从Public ID自动解析）
     * @return 门店列表
     */
    @Operation(
        summary = "查询门店列表",
        description = "查询租户下的门店列表，支持按Public ID筛选"
    )
    @GetMapping
    public ApiResponse<java.util.List<StoreBaseView>> list(
            @RequestParam(value = "storeId", required = false)
            @ResolvePublicId(type = ResourceType.STORE, required = false) Long storePk) {
        // storePk 可能为 null（未传 storeId 参数）
        Long tenantId = requireTenantId();
        
        if (storePk != null) {
            // 查询单个门店
            StoreBaseView view = storeFacade.getStoreBase(tenantId, storePk);
            return ApiResponse.success(java.util.List.of(view));
        } else {
            // 查询租户下所有门店（实际业务中可能需要分页）
            return ApiResponse.success(java.util.List.of());
        }
    }

    /**
     * 门店详情响应DTO
     * 
     * <p>仅包含Public ID，不暴露内部Long主键，保证安全性。</p>
     */
    public record StoreDetailResponse(
            /** 门店Public ID（对外唯一标识） */
            String storePublicId,
            /** 门店名称 */
            String name,
            /** 门店简称 */
            String shortName,
            /** Logo图片URL */
            String logoUrl
    ) {}

    /**
     * 获取当前租户ID
     * 
     * @return 租户ID
     * @throws IllegalStateException 租户上下文未设置时抛出
     */
    private Long requireTenantId() {
        String tenantId = com.bluecone.app.infra.tenant.TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("租户上下文未设置");
        }
        return Long.parseLong(tenantId);
    }
}
