package com.bluecone.app.api.admin.store;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.core.tenant.TenantContext;
import com.bluecone.app.store.api.StoreStaffFacade;
import com.bluecone.app.store.api.dto.StoreStaffView;
import com.bluecone.app.store.application.command.AddStoreStaffCommand;
import com.bluecone.app.store.application.command.BatchBindStoreStaffCommand;
import com.bluecone.app.store.application.command.ChangeStoreStaffRoleCommand;
import com.bluecone.app.store.application.command.RemoveStoreStaffCommand;
import com.bluecone.app.store.application.query.StoreStaffListQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 【平台管理后台】门店员工管理接口
 * 
 * <h3>📋 职责范围：</h3>
 * <ul>
 *   <li>门店员工关系的增删改查（建立用户与门店的绑定关系）</li>
 *   <li>员工角色管理（门店店长、收银员、厨师等角色分配）</li>
 *   <li>批量员工绑定（支持一次性为门店分配多个员工）</li>
 *   <li>员工权限隔离（确保员工只能访问所属门店的数据）</li>
 * </ul>
 * 
 * <h3>👥 使用场景：</h3>
 * <ul>
 *   <li><b>新门店开业</b>：批量为门店分配员工（店长、收银员、厨师等）</li>
 *   <li><b>员工入职</b>：为新员工绑定工作门店</li>
 *   <li><b>员工调动</b>：解除旧门店绑定，建立新门店绑定</li>
 *   <li><b>员工离职</b>：解除员工与门店的绑定关系</li>
 *   <li><b>角色调整</b>：变更员工在门店的角色（如收银员升职为店长）</li>
 * </ul>
 * 
 * <h3>👤 员工角色定义：</h3>
 * <ul>
 *   <li><b>STORE_MANAGER</b> - 店长：拥有门店管理权限</li>
 *   <li><b>CASHIER</b> - 收银员：负责收款结算</li>
 *   <li><b>CHEF</b> - 厨师：负责后厨制作</li>
 *   <li><b>WAITER</b> - 服务员：负责点餐服务</li>
 *   <li><b>DELIVERY</b> - 配送员：负责外卖配送</li>
 * </ul>
 * 
 * <h3>🔐 权限要求：</h3>
 * <ul>
 *   <li><b>store:staff:view</b> - 查看门店员工列表</li>
 *   <li><b>store:staff:manage</b> - 管理门店员工（增删改）</li>
 * </ul>
 * 
 * <h3>🛡️ 安全机制：</h3>
 * <ul>
 *   <li><b>租户隔离</b>：自动校验用户和门店是否属于同一租户</li>
 *   <li><b>门店隔离</b>：员工只能查看和操作所属门店的数据</li>
 *   <li><b>角色权限</b>：不同角色拥有不同的操作权限</li>
 * </ul>
 * 
 * <h3>🔗 关联接口：</h3>
 * <ul>
 *   <li>{@link StoreAdminController} - 门店基本信息管理</li>
 *   <li>{@link StoreDeviceAdminController} - 门店设备管理</li>
 * </ul>
 * 
 * <h3>📍 API 路径规范：</h3>
 * <pre>
 * GET    /api/admin/stores/staff/list  - 查询门店员工列表
 * POST   /api/admin/stores/staff       - 添加门店员工
 * DELETE /api/admin/stores/staff       - 移除门店员工
 * PUT    /api/admin/stores/staff/role  - 调整员工角色
 * POST   /api/admin/stores/staff/batch-bind - 批量绑定员工
 * </pre>
 * 
 * @author BlueCone Team
 * @since 1.0.0
 * @see StoreStaffFacade 门店员工领域门面
 */
@Tag(name = "Admin/Store - 员工管理", description = "平台管理后台 - 门店员工关系管理接口")
@RestController
@RequestMapping("/api/admin/stores/staff")
public class StoreStaffAdminController {

    /** 门店员工领域门面 */
    private final StoreStaffFacade storeStaffFacade;

    public StoreStaffAdminController(StoreStaffFacade storeStaffFacade) {
        this.storeStaffFacade = storeStaffFacade;
    }

    /**
     * 查询门店员工列表
     * 
     * <p>支持按以下条件筛选：</p>
     * <ul>
     *   <li>门店ID（必需）</li>
     *   <li>用户ID（可选，用于查询某个用户在该门店的角色）</li>
     *   <li>角色（可选，用于查询某个角色的所有员工）</li>
     * </ul>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * GET /api/admin/stores/staff/list?storeId=12345
     * GET /api/admin/stores/staff/list?storeId=12345&role=STORE_MANAGER
     * GET /api/admin/stores/staff/list?storeId=12345&userId=67890
     * </pre>
     * 
     * <h4>响应示例：</h4>
     * <pre>
     * [
     *   {
     *     "userId": 67890,
     *     "userName": "张三",
     *     "storeId": 12345,
     *     "storeName": "总店",
     *     "role": "STORE_MANAGER",
     *     "roleDesc": "店长",
     *     "bindAt": "2024-01-15T10:00:00"
     *   }
     * ]
     * </pre>
     * 
     * @param storeId 门店ID（必需）
     * @param userId 用户ID（可选）
     * @param role 员工角色（可选）
     * @return 员工列表
     */
    @Operation(
        summary = "查询门店员工列表",
        description = "查询指定门店的员工列表，支持按用户ID和角色筛选"
    )
    @GetMapping("/list")
    public ApiResponse<List<StoreStaffView>> list(
            @RequestParam Long storeId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String role) {
        Long tenantId = requireTenantId();
        
        // 构建查询条件
        StoreStaffListQuery query = new StoreStaffListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setUserId(userId);
        query.setRole(role);
        
        return ApiResponse.success(storeStaffFacade.list(query));
    }

    /**
     * 为门店新增一名员工
     * 
     * <p>建立用户与门店的绑定关系，并分配角色。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * POST /api/admin/stores/staff
     * Body:
     * {
     *   "storeId": 12345,
     *   "userId": 67890,
     *   "role": "CASHIER",
     *   "remark": "新入职收银员"
     * }
     * </pre>
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>同一用户不能在同一门店重复绑定</li>
     *   <li>用户必须已经在系统中注册</li>
     *   <li>用户和门店必须属于同一租户</li>
     * </ul>
     * 
     * @param command 添加员工命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "添加门店员工",
        description = "为门店添加一名员工，建立用户与门店的绑定关系"
    )
    @PostMapping
    public ApiResponse<Void> add(@RequestBody AddStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.addStaff(command);
        return ApiResponse.success();
    }

    /**
     * 移除门店员工
     * 
     * <p>解除用户与门店的绑定关系，员工将无法再访问该门店的数据。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * DELETE /api/admin/stores/staff
     * Body:
     * {
     *   "storeId": 12345,
     *   "userId": 67890,
     *   "reason": "员工离职"
     * }
     * </pre>
     * 
     * <h4>注意事项：</h4>
     * <ul>
     *   <li>移除操作不会删除用户账号，仅解除与门店的关系</li>
     *   <li>如果用户绑定了多个门店，只会解除当前门店的绑定</li>
     *   <li>建议先备份重要数据再执行移除操作</li>
     * </ul>
     * 
     * @param command 移除员工命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "移除门店员工",
        description = "解除用户与门店的绑定关系，员工将无法再访问该门店"
    )
    @DeleteMapping
    public ApiResponse<Void> remove(@RequestBody RemoveStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.removeStaff(command);
        return ApiResponse.success();
    }

    /**
     * 调整门店员工角色
     * 
     * <p>变更员工在门店的角色，如收银员升职为店长。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * PUT /api/admin/stores/staff/role
     * Body:
     * {
     *   "storeId": 12345,
     *   "userId": 67890,
     *   "newRole": "STORE_MANAGER",
     *   "reason": "升职为店长"
     * }
     * </pre>
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>员工必须已绑定该门店</li>
     *   <li>新角色必须是系统预定义的角色</li>
     *   <li>角色变更会影响员工的操作权限</li>
     * </ul>
     * 
     * @param command 角色变更命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "调整员工角色",
        description = "变更员工在门店的角色，如收银员升职为店长"
    )
    @PutMapping("/role")
    public ApiResponse<Void> changeRole(@RequestBody ChangeStoreStaffRoleCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.changeRole(command);
        return ApiResponse.success();
    }

    /**
     * 批量绑定门店员工
     * 
     * <p>一次性为门店分配多个员工，适用于新门店开业场景。</p>
     * 
     * <h4>请求示例：</h4>
     * <pre>
     * POST /api/admin/stores/staff/batch-bind
     * Body:
     * {
     *   "storeId": 12345,
     *   "staffList": [
     *     {"userId": 67890, "role": "STORE_MANAGER"},
     *     {"userId": 67891, "role": "CASHIER"},
     *     {"userId": 67892, "role": "CHEF"}
     *   ]
     * }
     * </pre>
     * 
     * <h4>业务规则：</h4>
     * <ul>
     *   <li>批量操作采用"部分成功"策略，已存在的绑定会跳过</li>
     *   <li>建议单次批量操作不超过100个员工</li>
     *   <li>操作失败会返回失败的员工列表</li>
     * </ul>
     * 
     * @param command 批量绑定命令对象
     * @return 操作结果
     */
    @Operation(
        summary = "批量绑定员工",
        description = "一次性为门店分配多个员工，适用于新门店开业场景"
    )
    @PostMapping("/batch-bind")
    public ApiResponse<Void> batchBind(@RequestBody BatchBindStoreStaffCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storeStaffFacade.batchBindStaff(command);
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
