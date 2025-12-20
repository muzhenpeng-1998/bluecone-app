package com.bluecone.app.api.admin.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.api.StoreDeviceFacade;
import com.bluecone.app.store.api.dto.StoreDeviceView;
import com.bluecone.app.store.application.command.ChangeStoreDeviceStatusCommand;
import com.bluecone.app.store.application.command.RegisterStoreDeviceCommand;
import com.bluecone.app.store.application.command.UpdateStoreDeviceCommand;
import com.bluecone.app.store.application.query.StoreDeviceListQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【平台管理后台】门店设备管理接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>门店设备的注册、配置、状态管理</li>
 *   <li>支持多种设备类型（打印机、POS机、厨房显示屏、自助点餐机等）</li>
 *   <li>设备在线状态监控</li>
 *   <li>设备配置参数管理</li>
 * </ul>
 * 
 * <h3>🖨️ 支持的设备类型：</h3>
 * <ul>
 *   <li><b>PRINTER</b> - 打印机（小票打印、标签打印）</li>
 *   <li><b>POS</b> - POS收银机</li>
 *   <li><b>KDS</b> - 厨房显示屏（Kitchen Display System）</li>
 *   <li><b>KIOSK</b> - 自助点餐机</li>
 *   <li><b>SCALE</b> - 电子秤</li>
 *   <li><b>SCANNER</b> - 扫码枪</li>
 * </ul>
 * 
 * <h3>📊 设备状态：</h3>
 * <ul>
 *   <li><b>ONLINE</b> - 在线（设备正常工作）</li>
 *   <li><b>OFFLINE</b> - 离线（设备未连接）</li>
 *   <li><b>DISABLED</b> - 已停用（人工停用）</li>
 *   <li><b>FAULT</b> - 故障（设备异常）</li>
 * </ul>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>store:device:view</b> - 查看设备信息</li>
 *   <li><b>store:device:manage</b> - 管理设备（注册、配置、状态变更）</li>
 * </ul>
 * 
 * <h3>🛡️ 安全机制：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：设备仅归属一个租户</li>
 *   <li><b>门店隔离</b>：设备仅绑定一个门店</li>
 *   <li><b>设备认证</b>：设备需要通过密钥认证才能接入</li>
 * </ul>
 * 
 * <h3>🔗 关联接口：</h3>
 * <ul>
 *   <li>{@link StoreAdminController} - 门店基本信息管理</li>
 *   <li>{@link StoreStaffAdminController} - 门店员工管理</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * GET    /api/admin/stores/devices/list   - 查询设备列表
 * GET    /api/admin/stores/devices/detail - 查询设备详情
 * POST   /api/admin/stores/devices        - 注册新设备
 * PUT    /api/admin/stores/devices        - 更新设备配置
 * PUT    /api/admin/stores/devices/status - 变更设备状态
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 * @see StoreDeviceFacade 门店设备领域门面
 */
@Tag(name = "Admin/Store - 设备管理", description = "平台管理后台 - 门店设备管理接口")
@RestController
@RequestMapping("/api/admin/stores/devices")
public class StoreDeviceAdminController {

    /** 门店设备领域门面 */
    private final StoreDeviceFacade storeDeviceFacade;

    public StoreDeviceAdminController(StoreDeviceFacade storeDeviceFacade) {
        this.storeDeviceFacade = storeDeviceFacade;
    }

    /**
     * 查询门店设备列表
     * 
     * <p>支持按以下条件筛选：</p>
     * <ul>
     *   <li>门店ID（必需）</li>
     *   <li>设备类型（可选）</li>
     *   <li>设备状态（可选）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/admin/stores/devices/list?storeId=12345
     * GET /api/admin/stores/devices/list?storeId=12345&deviceType=PRINTER
     * GET /api/admin/stores/devices/list?storeId=12345&status=ONLINE
     * </pre>
     * 
     * @param storeId 门店ID（必需）
     * @param deviceType 设备类型（可选）
     * @param status 设备状态（可选）
     * @return 设备列表
     */
    @Operation(
        summary = "查询门店设备列表",
        description = "查询指定门店的所有设备，支持按设备类型和状态筛选"
    )
    @GetMapping("/list")
    public ApiResponse<List<StoreDeviceView>> list(
            @RequestParam Long storeId,
            @RequestParam(required = false) String deviceType,
            @RequestParam(required = false) String status) {
        Long tenantId = requireTenantId();
        
        StoreDeviceListQuery query = new StoreDeviceListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setDeviceType(deviceType);
        query.setStatus(status);
        
        return ApiResponse.success(storeDeviceFacade.list(query));
    }

    /**
     * 查看设备详情
     * 
     * <p>返回设备的完整配置信息，包括：</p>
     * <ul>
     *   <li>基本信息：设备名称、类型、序列号</li>
     *   <li>状态信息：在线状态、最后心跳时间</li>
     *   <li>配置信息：IP地址、端口、驱动类型等</li>
     *   <li>统计信息：累计打印量、故障次数等</li>
     * </ul>
     * 
     * @param storeId 门店ID
     * @param deviceId 设备ID
     * @return 设备详情
     */
    @Operation(
        summary = "查询设备详情",
        description = "查询指定设备的完整信息，包括配置和统计数据"
    )
    @GetMapping("/detail")
    public ApiResponse<StoreDeviceView> detail(
            @RequestParam Long storeId,
            @RequestParam Long deviceId) {
        Long tenantId = requireTenantId();
        return ApiResponse.success(storeDeviceFacade.getById(tenantId, storeId, deviceId));
    }

    /**
     * 注册新设备
     * 
     * <p>为门店添加新的硬件设备。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * POST /api/admin/stores/devices
     * Body:
     * {
     *   "storeId": 12345,
     *   "deviceName": "前台打印机",
     *   "deviceType": "PRINTER",
     *   "serialNumber": "SN123456789",
     *   "ipAddress": "192.168.1.100",
     *   "port": 9100,
     *   "driverType": "ESC/POS",
     *   "config": {
     *     "paperWidth": "58mm",
     *     "autocut": true
     *   }
     * }
     * </pre>
     * 
     * @param command 注册设备命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "注册新设备",
        description = "为门店注册新的硬件设备（打印机、POS机等）"
    )
    @PostMapping
    public ApiResponse<Void> register(@RequestBody RegisterStoreDeviceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.registerDevice(command);
        return ApiResponse.success();
    }

    /**
     * 更新设备配置
     * 
     * <p>修改设备的配置参数，如IP地址、端口、打印机纸张宽度等。</p>
     * 
     * @param command 更新设备命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "更新设备配置",
        description = "修改设备的配置参数"
    )
    @PutMapping
    public ApiResponse<Void> update(@RequestBody UpdateStoreDeviceCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.updateDevice(command);
        return ApiResponse.success();
    }

    /**
     * 变更设备状态
     * 
     * <p>支持的状态变更：</p>
     * <ul>
     *   <li>启用：将停用的设备重新启用</li>
     *   <li>停用：暂时停用设备（如设备维修）</li>
     * </ul>
     * 
     * @param command 状态变更命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "变更设备状态",
        description = "启用或停用设备"
    )
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStoreDeviceStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeDeviceFacade.changeStatus(command);
        return ApiResponse.success();
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
