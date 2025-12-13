package com.bluecone.app.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.create.api.CreateRequest;
import com.bluecone.app.core.create.api.CreateWorkWithEvents;
import com.bluecone.app.core.create.api.CreateWorkWithEventsResult;
import com.bluecone.app.core.create.api.IdempotentCreateResult;
import com.bluecone.app.core.create.api.IdempotentCreateTemplate;
import com.bluecone.app.core.create.api.TxMode;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.core.event.EventMetadata;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.store.api.StoreFacade;
import com.bluecone.app.store.api.dto.StoreBaseView;
import com.bluecone.app.store.application.command.ChangeStoreStatusCommand;
import com.bluecone.app.store.application.command.CreateStoreCommand;
import com.bluecone.app.store.application.command.ToggleOpenForOrdersCommand;
import com.bluecone.app.store.application.command.UpdateStoreBaseCommand;
import com.bluecone.app.store.application.command.UpdateStoreCapabilitiesCommand;
import com.bluecone.app.store.application.command.UpdateStoreOpeningHoursCommand;
import com.bluecone.app.store.application.command.UpdateStoreSpecialDaysCommand;
import com.bluecone.app.store.application.query.StoreDetailQuery;
import com.bluecone.app.store.application.query.StoreListQuery;
import com.bluecone.app.store.application.service.StoreCommandService;
import com.bluecone.app.store.event.StoreCreatedEvent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店管理后台 Controller。
 *
 * <p>职责：
 * <ul>
 *     <li>为运营/租户后台提供门店的增删改查接口；</li>
 *     <li>维护门店的营业能力（堂食/外卖/自取等）与营业时间、特殊日配置；</li>
 *     <li>提供门店营业状态、接单开关的管理能力。</li>
 * </ul>
 * </p>
 *
 * <p>设计要点：
 * <ul>
 *     <li>对外暴露的是 HTTP 接口，内部只依赖 {@link StoreFacade}，不直接依赖基础设施（Mapper/Repository）；</li>
 *     <li>所有接口都通过 {@link #requireTenantId()} 从 {@link TenantContext} 中读取当前租户，实现多租户隔离；</li>
 *     <li>统一使用 {@link ApiResponse} 包装返回结果，便于前端统一处理。</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/admin/store")
public class AdminStoreController {

    /**
     * 门店领域门面层。
     *
     * <p>聚合了门店相关的应用服务，Controller 只与门面交互，
     * 不关心门店内部的聚合划分与仓储等基础设施实现。</p>
     */
    private final StoreFacade storeFacade;

    private final StoreCommandService storeCommandService;

    private final IdempotentCreateTemplate idempotentCreateTemplate;

    private final IdService idService;

    /**
     * 通过构造器注入依赖的门店门面与应用服务。
     *
     * @param storeFacade              门店门面，用于承接门店读写能力
     * @param storeCommandService      门店写侧应用服务
     * @param idempotentCreateTemplate 幂等创建模板
     * @param idService                ID 生成服务（用于生成 long 型 store_no 及兜底幂等键）
     */
    public AdminStoreController(StoreFacade storeFacade,
                                StoreCommandService storeCommandService,
                                IdempotentCreateTemplate idempotentCreateTemplate,
                                IdService idService) {
        this.storeFacade = storeFacade;
        this.storeCommandService = storeCommandService;
        this.idempotentCreateTemplate = idempotentCreateTemplate;
        this.idService = idService;
    }

    /**
     * 列表查询门店。
     *
     * <p>请求方法：GET</p>
     * <p>请求路径：/api/admin/store/list</p>
     *
     * <p>说明：
     * <ul>
     *     <li>查询条件通过 {@link StoreListQuery} 直接从查询参数绑定；</li>
     *     <li>方法内部会自动填充当前租户 ID，只返回当前租户下的数据；</li>
     *     <li>返回门店基础信息视图列表，不包含复杂的扩展信息。</li>
     * </ul>
     * </p>
     *
     * @param query 查询条件（如名称、状态、分页信息等），由 Spring MVC 自动绑定。
     * @return 统一响应包装的门店基础信息列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StoreBaseView>> list(StoreListQuery query) {
        // 从租户上下文中获取当前登录的租户 ID（字符串 -> Long），同时做非空校验
        Long tenantId = requireTenantId();
        // 将租户 ID 设置到查询对象中，确保查询数据被限制在当前租户维度
        query.setTenantId(tenantId);
        // 调用门店门面执行查询逻辑，Controller 不直接处理业务细节
        List<StoreBaseView> list = storeFacade.list(query);
        // 使用统一响应结构包装返回结果，前端可统一按 ApiResponse 处理
        return ApiResponse.success(list);
    }

    /**
     * 门店详情（基础信息）。
     *
     * <p>请求方法：GET</p>
     * <p>请求路径：/api/admin/store/detail</p>
     *
     * <p>说明：
     * <ul>
     *     <li>支持通过 storeId 或 storeCode 两种方式查询具体门店；</li>
     *     <li>二者都为可选参数，实际校验逻辑由门店领域层处理（例如至少一个必须非空）；</li>
     *     <li>返回门店的基础视图信息，例如名称、地址、logo 等。</li>
     * </ul>
     * </p>
     *
     * @param storeId   门店 ID，可为空。
     * @param storeCode 门店编码，可为空。
     * @return 统一响应包装的门店基础视图，如果不存在则由领域层决定抛错或返回空。
     */
    @GetMapping("/detail")
    public ApiResponse<StoreBaseView> detail(@RequestParam(required = false) Long storeId,
                                             @RequestParam(required = false) String storeCode) {
        // 从租户上下文中获取当前租户的 Long 类型 ID，确保只查询该租户的数据
        Long tenantId = requireTenantId();
        // 构造门店详情查询对象，用于向门面层传递查询条件
        StoreDetailQuery query = new StoreDetailQuery();
        // 将租户 ID 写入查询对象中，实现多租户数据隔离
        query.setTenantId(tenantId);
        // 将可能存在的 storeId 条件写入查询对象，由领域层决定具体的使用方式
        query.setStoreId(storeId);
        // 将可能存在的 storeCode 条件写入查询对象
        query.setStoreCode(storeCode);
        // 调用门店门面查询门店详情，内部会根据传入条件做校验与查询
        StoreBaseView view = storeFacade.detail(query);
        // 使用统一响应结构包装返回结果
        return ApiResponse.success(view);
    }

    public record CreateStoreResponse(String storePublicId) {}

    /**
     * 创建门店（含默认配置）。
     *
     * <p>请求方法：POST</p>
     * <p>请求路径：/api/admin/store</p>
     *
     * <p>说明：
     * <ul>
     *     <li>通过 {@link CreateStoreCommand} 接收前端传入的门店基础信息与初始配置；</li>
     *     <li>方法内部会补充租户 ID，确保门店归属于当前租户；</li>
     *     <li>返回新建门店的对外 public_id，供前端或其他系统引用。</li>
     * </ul>
     * </p>
     *
     * @param command 创建门店命令对象，从请求体 JSON 反序列化而来。
     * @return 包含门店 public_id 的成功响应。
     */
    @PostMapping
    public ApiResponse<CreateStoreResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idemKey,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idemKeyAlt,
            @RequestBody CreateStoreCommand command) {
        // 获取当前登录租户 ID，并在缺失时抛出未授权异常
        Long tenantId = requireTenantId();
        // 将租户 ID 设置到创建命令中，确保新建门店归属于该租户
        command.setTenantId(tenantId);
        // 解析幂等键：优先使用请求头传入的 Idempotency-Key / X-Idempotency-Key
        String resolvedIdemKey = resolveIdempotencyKey(idemKey, idemKeyAlt);
        // 构造请求摘要（SHA-256）：tenantId + 关键业务字段
        String requestHash = hashCreateStoreRequest(tenantId, command);

        CreateRequest request = new CreateRequest(
                tenantId,
                "STORE_CREATE",
                ResourceType.STORE.prefix(),
                resolvedIdemKey,
                requestHash,
                Duration.ofHours(24),
                Duration.ofSeconds(30),
                TxMode.REQUIRES_NEW,
                false,
                null
        );

        IdempotentCreateResult<String> result = idempotentCreateTemplate.create(
                request,
                (CreateWorkWithEvents<String>) (internalId, publicId) -> {
                    Long storeNo;
                    try {
                        storeNo = idService.nextLongId();
                    } catch (UnsupportedOperationException ex) {
                        storeNo = null;
                    }
                    storeCommandService.createStoreWithPreallocatedIds(command, internalId, publicId, storeNo);

                    StoreCreatedEvent event = new StoreCreatedEvent(
                            internalId,
                            publicId,
                            storeNo,
                            command.getName(),
                            buildEventMetadata(tenantId)
                    );
                    return new CreateWorkWithEventsResult<>(publicId, java.util.List.of(event));
                }
        );

        if (result.inProgress()) {
            throw new BizException(CommonErrorCode.CONFLICT, "创建门店请求正在处理，请稍后重试");
        }

        return ApiResponse.success(new CreateStoreResponse(result.publicId()));
    }

    /**
     * 更新门店基础信息（名称、地址、logo 等）。
     * 前端需传递 configVersion 做乐观锁控制。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/base</p>
     *
     * <p>说明：
     * <ul>
     *     <li>只修改门店的基础属性，不涉及营业能力、营业时间等信息；</li>
     *     <li>{@link UpdateStoreBaseCommand} 中应包含门店 ID 以及 configVersion 字段；</li>
     *     <li>乐观锁（configVersion）由领域层校验，避免覆盖其他用户的并发修改。</li>
     * </ul>
     * </p>
     *
     * @param command 更新门店基础信息的命令对象，从请求体 JSON 反序列化而来。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/base")
    public ApiResponse<Void> updateBase(@RequestBody UpdateStoreBaseCommand command) {
        // 获取当前租户 ID，确保只允许操作本租户的门店
        Long tenantId = requireTenantId();
        // 将租户 ID 写入命令对象，领域层据此做租户隔离校验
        command.setTenantId(tenantId);
        // 调用门店门面执行更新基础信息的用例
        storeFacade.updateStoreBase(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 批量更新门店能力配置（堂食/外卖/自取/预约等）。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/capabilities</p>
     *
     * <p>说明：
     * <ul>
     *     <li>能力配置通常与门店对外提供的服务能力相关，例如是否支持外卖、自提等；</li>
     *     <li>命令对象中可包含多个门店的配置，实现批量开关；</li>
     *     <li>具体的规则校验与落库由领域层处理。</li>
     * </ul>
     * </p>
     *
     * @param command 更新门店能力配置的命令对象。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/capabilities")
    public ApiResponse<Void> updateCapabilities(@RequestBody UpdateStoreCapabilitiesCommand command) {
        // 获取当前租户 ID，保证批量操作只能作用于本租户的门店
        Long tenantId = requireTenantId();
        // 将租户 ID 写入命令对象，作为领域层的安全边界
        command.setTenantId(tenantId);
        // 将能力配置更新请求委托给门店门面处理
        storeFacade.updateCapabilities(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 更新常规营业时间。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/opening-hours</p>
     *
     * <p>说明：
     * <ul>
     *     <li>主要描述门店在一周内的常规营业时间配置，例如周一到周日的开闭店时间；</li>
     *     <li>与特殊日（节假日）配置区分开来，特殊日通过 {@link #updateSpecialDays(UpdateStoreSpecialDaysCommand)} 维护；</li>
     *     <li>是否覆盖已有配置、是否允许跨日等细节由领域层实现。</li>
     * </ul>
     * </p>
     *
     * @param command 更新常规营业时间的命令对象。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/opening-hours")
    public ApiResponse<Void> updateOpeningHours(@RequestBody UpdateStoreOpeningHoursCommand command) {
        // 获取当前租户 ID
        Long tenantId = requireTenantId();
        // 将租户 ID 设置到命令对象中
        command.setTenantId(tenantId);
        // 调用门店门面更新常规营业时间配置
        storeFacade.updateOpeningHours(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 更新特殊日配置。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/special-days</p>
     *
     * <p>说明：
     * <ul>
     *     <li>用于维护节假日、临时调休等特殊日期的营业时间或闭店配置；</li>
     *     <li>通常会覆盖常规营业时间配置；</li>
     *     <li>具体如何与常规配置合并由领域层处理。</li>
     * </ul>
     * </p>
     *
     * @param command 更新特殊日配置的命令对象。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/special-days")
    public ApiResponse<Void> updateSpecialDays(@RequestBody UpdateStoreSpecialDaysCommand command) {
        // 获取当前租户 ID
        Long tenantId = requireTenantId();
        // 将租户 ID 写入命令对象，确保只修改本租户的门店配置
        command.setTenantId(tenantId);
        // 委托门店门面更新特殊日配置
        storeFacade.updateSpecialDays(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 切换门店状态（OPEN / PAUSED / CLOSED）。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/status</p>
     *
     * <p>说明：
     * <ul>
     *     <li>用于整体控制门店的对外营业状态，例如暂停营业、永久关闭等；</li>
     *     <li>状态值通常由前端通过枚举或常量维护，后端通过 {@link ChangeStoreStatusCommand} 接收；</li>
     *     <li>具体状态转换的规则（例如从 CLOSED 是否还能回到 OPEN）由领域层控制。</li>
     * </ul>
     * </p>
     *
     * @param command 切换门店状态的命令对象。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreStatusCommand command) {
        // 获取当前租户 ID
        Long tenantId = requireTenantId();
        // 将租户 ID 写入命令对象，确保状态变更发生在本租户的门店上
        command.setTenantId(tenantId);
        // 调用门店门面执行业务逻辑（状态校验、事���发布等）
        storeFacade.changeStatus(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 切换接单开关。
     *
     * <p>请求方法：PUT</p>
     * <p>请求路径：/api/admin/store/open-for-orders</p>
     *
     * <p>说明：
     * <ul>
     *     <li>通常用于临时控制门店是否接单，而不改变门店整体营业状态；</li>
     *     <li>例如门店仍处于 OPEN 状态，但暂时不接新订单；</li>
     *     <li>具体与第三方平台或前台展示的联动由门店领域或集成层处理。</li>
     * </ul>
     * </p>
     *
     * @param command 切换接单开关的命令对象。
     * @return 空结果的统一成功响应。
     */
    @PutMapping("/open-for-orders")
    public ApiResponse<Void> toggleOpenForOrders(@RequestBody ToggleOpenForOrdersCommand command) {
        // 获取当前租户 ID
        Long tenantId = requireTenantId();
        // 将租户 ID 写入命令对象，确保只操作当前租户的门店
        command.setTenantId(tenantId);
        // 委托门店门面处理接单开关的业务逻辑
        storeFacade.toggleOpenForOrders(command);
        // 返回统一的空成功响应
        return ApiResponse.success();
    }

    /**
     * 从 {@link TenantContext} 中获取当前租户 ID，并做完整的合法性校验。
     *
     * <p>设计目的：
     * <ul>
     *     <li>避免每个接口重复编写租户校验逻辑；</li>
     *     <li>统一抛出业务异常，便于全局异常处理器捕获后返回一致的错误结构；</li>
     *     <li>保证后续业务代码拿到的一定是合法的 Long 类型租户 ID。</li>
     * </ul>
     * </p>
     *
     * @return 当前请求绑定的租户 ID（Long 类型）。
     * @throws BizException 当租户信息缺失或非法时抛出业务异常。
     */
    private Long requireTenantId() {
        // 从线程本地的 TenantContext 中读取当前租户 ID 的字符串表示
        String tenantIdStr = TenantContext.getTenantId();
        // 如果上下文中没有租户信息，或者是空字符串，则认为用户未登录或请求中缺少租户标识
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            // 抛出统一的业务异常，错误码为未授权，提示前端需要重新登录或补充上下文
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            // 将字符串形式的租户 ID 转换成 Long 类型，便于后续数据库查询与比较
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            // 如果转换失败，说明上下文中携带的租户标识不是合法的数字，返回 400 错误
            throw new BizException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }

    private String resolveIdempotencyKey(String primary, String secondary) {
        String key = (primary != null && !primary.isBlank()) ? primary : secondary;
        if (key != null && !key.isBlank()) {
            return key;
        }
        // 不推荐：仅在调用方未提供幂等键时临时生成一个，便于降级使用。
        try {
            return idService.nextUlidString();
        } catch (Exception ex) {
            // 回退到随机字符串，避免因 ID 模块配置问题影响主流程
            return java.util.UUID.randomUUID().toString();
        }
    }

    private String hashCreateStoreRequest(long tenantId, CreateStoreCommand command) {
        String payload = tenantId + "|" +
                safe(command.getName()) + "|" +
                safe(command.getShortName()) + "|" +
                safe(command.getCityCode()) + "|" +
                safe(command.getStoreCode());
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(encoded.length * 2);
            for (byte b : encoded) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private EventMetadata buildEventMetadata(Long tenantId) {
        HashMap<String, String> meta = new HashMap<>();
        if (tenantId != null) {
            meta.put("tenantId", tenantId.toString());
        }
        String traceId = MDC.get("traceId");
        if (traceId != null && !traceId.isBlank()) {
            meta.put("traceId", traceId);
        }
        return meta.isEmpty() ? EventMetadata.empty() : EventMetadata.of(meta);
    }
}
