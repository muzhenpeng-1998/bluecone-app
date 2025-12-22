package com.bluecone.app.api.open.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.api.dto.StoreOrderAcceptResult;
import com.bluecone.app.store.api.dto.StoreOrderSnapshot;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 【开放接口】门店查询接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>为C端用户（小程序/H5/第三方）提供门店运行时查询能力</li>
 *   <li>门店基础信息查询（名称、地址、营业时间等）</li>
 *   <li>门店订单快照查询（下单前预校验）</li>
 *   <li>门店接单能力检查（实时判断是否可下单）</li>
 * </ul>
 * 
 * <h3>👥 使用场景：</h3>
 * <ul>
 *   <li><b>门店详情展示</b>：用户浏览门店信息页面</li>
 *   <li><b>下单前校验</b>：检查门店是否营业、是否接单</li>
 *   <li><b>订单预览</b>：获取门店配置用于前端展示</li>
 *   <li><b>第三方集成</b>：外卖平台查询门店状态</li>
 * </ul>
 * 
 * <h3>🎯 核心能力：</h3>
 * <ul>
 *   <li><b>多级缓存</b>：StoreFacade内部实现多级缓存，保证高性能</li>
 *   <li><b>实时校验</b>：结合营业时间、接单开关、库存等多维度判断</li>
 *   <li><b>降级策略</b>：支持缓存降级和兜底逻辑</li>
 * </ul>
 * 
 * <h3>🔐 安全机制：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：自动从上下文获取租户ID</li>
 *   <li><b>只读接口</b>：仅提供查询能力，不支持修改</li>
 *   <li><b>限流保护</b>：高频查询接口需配置限流策略</li>
 * </ul>
 * 
 * <h3>🔗 关联接口：</h3>
 * <ul>
 *   <li>{@link com.bluecone.app.api.admin.store.StoreAdminController} - 平台管理后台</li>
 *   <li>{@link com.bluecone.app.api.merchant.store.MerchantStoreController} - 商户后台</li>
 *   <li>{@link com.bluecone.app.api.open.order.OrderMainFlowController} - 订单主链路</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * GET /api/open/stores/base            - 查询门店基础信息
 * GET /api/open/stores/order-snapshot  - 查询订单视角快照
 * GET /api/open/stores/check-acceptable - 检查是否可接单
 * </pre>
 * 
 * <h3>⚡ 性能优化：</h3>
 * <ul>
 *   <li>基础信息查询：Redis缓存，TTL=5分钟</li>
 *   <li>订单快照：本地缓存+Redis，TTL=30秒</li>
 *   <li>接单检查：实时计算，无缓存（保证准确性）</li>
 * </ul>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 * @see StoreFacade 门店领域门面
 */
@Tag(name = "👤 C端开放接口 > 门店相关", description = "开放接口 - 门店信息查询（C端/小程序）")
@RestController
@RequestMapping("/api/open/stores")
public class OpenStoreController {

    /** 门店领域门面 */
    private final StoreFacade storeFacade;

    public OpenStoreController(StoreFacade storeFacade) {
        this.storeFacade = storeFacade;
    }

    /**
     * 查询门店基础信息
     * 
     * <p>返回门店的展示信息，用于C端页面渲染。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/open/stores/base?storeId=12345
     * Headers:
     *   X-Tenant-Id: 10001  # 由网关自动注入
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "id": 12345,
     *   "name": "总店",
     *   "address": "朝阳区xxx路xxx号",
     *   "contactPhone": "010-12345678",
     *   "logoUrl": "https://cdn.example.com/logo.jpg",
     *   "openingHours": "10:00-22:00",
     *   "status": "OPEN"
     * }
     * </pre>
     * 
     * @param storeId 门店ID
     * @return 门店基础信息
     */
    @Operation(
        summary = "查询门店基础信息",
        description = "查询门店的展示信息，用于C端页面渲染"
    )
    @GetMapping("/base")
    public ApiResponse<StoreBaseView> getBase(@RequestParam Long storeId) {
        Long tenantId = requireTenantId();
        StoreBaseView view = storeFacade.getStoreBase(tenantId, storeId);
        return ApiResponse.success(view);
    }

    /**
     * 查询订单视角快照
     * 
     * <p>用于下单前的预校验和前端展示，返回门店当前的接单状态和配置。</p>
     * 
     * <h4>包含信息：</h4>
     * <ul>
     *   <li>门店基本信息</li>
     *   <li>营业状态（是否营业）</li>
     *   <li>接单状态（是否接单）</li>
     *   <li>配送配置（起送价、配送费等）</li>
     *   <li>营业能力（堂食/外卖/自取）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/open/stores/order-snapshot?storeId=12345&channelType=MINI_PROGRAM
     * </pre>
     * 
     * <h4>使用场景：</h4>
     * <ul>
     *   <li>用户进入下单页面时调用</li>
     *   <li>前端根据快照展示门店状态</li>
     *   <li>本地预校验（前端判断是否可下单）</li>
     * </ul>
     * 
     * @param storeId 门店ID（可选，优先使用）
     * @param storePublicId 门店Public ID（可选，备选）
     * @param channelType 渠道类型（MINI_PROGRAM/H5/APP）
     * @return 订单视角快照
     */
    @Operation(
        summary = "查询订单视角快照",
        description = "查询门店的订单配置和状态，用于下单前的预校验和展示"
    )
    @GetMapping("/order-snapshot")
    public ApiResponse<StoreOrderSnapshot> getOrderSnapshot(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, name = "storePublicId") String storePublicId,
            @RequestParam(required = false) String channelType) {
        Long tenantId = requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        
        // 当前 StoreFacade 仍按 Long storeId 查询快照，storePublicId 主要用于透传和前端标识
        StoreOrderSnapshot snapshot = storeFacade.getOrderSnapshot(tenantId, storeId, now, channelType);
        return ApiResponse.success(snapshot);
    }

    /**
     * 检查门店是否可接单
     * 
     * <p>实时判断门店当前是否可以接收指定类型的订单。</p>
     * 
     * <h4>校验维度：</h4>
     * <ul>
     *   <li>门店状态：是否营业</li>
     *   <li>营业时间：是否在营业时间内</li>
     *   <li>接单开关：是否开启接单</li>
     *   <li>能力配置：是否支持该业务类型（堂食/外卖/自取）</li>
     *   <li>库存状态：是否有库存（可选）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/open/stores/check-acceptable?storeId=12345&capability=DELIVERY&channelType=MINI_PROGRAM
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * {
     *   "acceptable": true,
     *   "reason": null,
     *   "tips": "当前门店支持外卖配送"
     * }
     * 
     * // 不可接单的情况
     * {
     *   "acceptable": false,
     *   "reason": "NOT_OPEN_FOR_ORDERS",
     *   "tips": "门店暂停接单，请稍后再试"
     * }
     * </pre>
     * 
     * <h4>使用场景：</h4>
     * <ul>
     *   <li>用户点击"立即下单"前调用</li>
     *   <li>订单提交时的后端兜底校验</li>
     *   <li>第三方平台同步门店状态</li>
     * </ul>
     * 
     * @param storeId 门店ID（可选，优先使用）
     * @param storePublicId 门店Public ID（可选，备选）
     * @param capability 业务能力类型（DINE_IN/DELIVERY/TAKEOUT）
     * @param channelType 渠道类型（MINI_PROGRAM/H5/APP）
     * @return 接单检查结果
     */
    @Operation(
        summary = "检查门店是否可接单",
        description = "实时检查门店是否可以接收指定类型的订单"
    )
    @GetMapping("/check-acceptable")
    public ApiResponse<StoreOrderAcceptResult> checkAcceptable(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false, name = "storePublicId") String storePublicId,
            @RequestParam String capability,
            @RequestParam(required = false) String channelType) {
        Long tenantId = requireTenantId();
        LocalDateTime now = LocalDateTime.now();
        
        // 调用领域层实时校验
        StoreOrderAcceptResult result = storeFacade.checkOrderAcceptable(
                tenantId, storeId, capability, now, channelType);
        
        return ApiResponse.success(result);
    }

    /**
     * 获取当前租户ID
     * 
     * @return 租户ID
     * @throws BusinessException 租户上下文缺失时抛出
     */
    private Long requireTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BusinessException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }
}
