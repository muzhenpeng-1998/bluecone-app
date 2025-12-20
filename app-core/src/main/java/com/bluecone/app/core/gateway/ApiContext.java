package com.bluecone.app.core.gateway;

import com.bluecone.app.core.apicontract.ApiSide;
import com.bluecone.app.core.apicontract.ContextType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * API网关上下文
 * 
 * <p>每个请求的上下文对象，携带请求的元数据和解析结果。
 * 
 * <h3>设计目的</h3>
 * <ul>
 *   <li>统一管理请求级别的上下文信息</li>
 *   <li>在中间件链中传递和共享数据</li>
 *   <li>解耦各中间件之间的依赖关系</li>
 *   <li>提供响应结果的临时存储</li>
 * </ul>
 * 
 * <h3>架构说明</h3>
 * <p>这是app-core中的最小版本，避免循环依赖。
 * app-application中有扩展版本，包含更多字段。</p>
 * 
 * <h3>生命周期</h3>
 * <ol>
 *   <li>请求进入网关时创建ApiContext</li>
 *   <li>经过中间件链，逐步解析和填充上下文</li>
 *   <li>业务处理器使用上下文中的信息</li>
 *   <li>响应返回后，上下文销毁</li>
 * </ol>
 * 
 * <h3>中间件协作</h3>
 * <p>中间件通过ApiContext进行协作：</p>
 * <ul>
 *   <li>TenantMiddleware：解析租户ID</li>
 *   <li>UserMiddleware：解析用户信息</li>
 *   <li>StoreMiddleware：解析门店信息</li>
 *   <li>InventoryMiddleware：解析库存策略</li>
 * </ul>
 * 
 * @author BlueCone
 * @since 1.0.0
 * @see ApiSide
 * @see ContextType
 */
@Getter
@Builder
public class ApiContext {

    /**
     * 追踪ID
     * 全局唯一的请求追踪标识，用于日志关联和问题排查
     * 格式：UUID或雪花ID
     */
    private final String traceId;
    
    /**
     * 请求时间
     * 请求到达网关的时间戳，用于计算请求耗时
     */
    private final LocalDateTime requestTime;

    /**
     * API侧面
     * 标识当前请求的逻辑侧面，如：
     * <ul>
     *   <li>USER - 用户端（C端）</li>
     *   <li>MERCHANT - 商户端（B端）</li>
     *   <li>ADMIN - 管理端（运营后台）</li>
     *   <li>OPEN - 开放平台</li>
     * </ul>
     * 用于控制不同侧面的权限和功能
     */
    @Setter
    private ApiSide apiSide;

    /**
     * 必需的上下文类型集合
     * 
     * <p>这些上下文必须成功解析，否则请求会被拒绝。
     * 
     * <p>例如：用户端订单接口需要USER和STORE上下文，
     * 如果解析失败，会返回401或403错误。
     * 
     * @see ContextType
     */
    @Setter
    @Builder.Default
    private EnumSet<ContextType> requiredContexts = EnumSet.noneOf(ContextType.class);

    /**
     * 可选的上下文类型集合
     * 
     * <p>这些上下文尽力解析，解析失败不会阻塞请求。
     * 
     * <p>例如：某些接口可以在有INVENTORY上下文时提供更丰富的信息，
     * 但没有INVENTORY上下文也能正常工作。
     * 
     * @see ContextType
     */
    @Setter
    @Builder.Default
    private EnumSet<ContextType> optionalContexts = EnumSet.noneOf(ContextType.class);

    /**
     * 租户ID
     * 当前请求所属的租户标识，用于多租户数据隔离
     */
    @Setter
    private String tenantId;

    /**
     * 用户ID
     * 当前请求的用户标识，从认证Token中解析
     */
    @Setter
    private Long userId;

    /**
     * 门店ID
     * 当前请求关联的门店标识，从请求头或Token中解析
     */
    @Setter
    private Long storeId;

    /**
     * 客户端类型
     * 标识请求来源的客户端类型，如：
     * <ul>
     *   <li>WEB - 网页端</li>
     *   <li>MOBILE - 移动APP</li>
     *   <li>MINI_PROGRAM - 小程序</li>
     *   <li>H5 - 移动网页</li>
     * </ul>
     */
    @Setter
    private String clientType;

    /**
     * 响应对象
     * 业务处理器的响应结果临时存储
     * 在中间件链结束后统一序列化返回
     */
    @Setter
    private Object response;

    /**
     * 异常对象
     * 请求处理过程中发生的异常
     * 用于全局异常处理器统一处理
     */
    @Setter
    private Exception error;

    /**
     * 可变属性Map
     * 
     * <p>用于中间件和处理器之间传递自定义数据。
     * 
     * <p><b>使用场景：</b>
     * <ul>
     *   <li>中间件解析的临时数据</li>
     *   <li>跨中间件的状态传递</li>
     *   <li>业务处理器的上下文扩展</li>
     * </ul>
     * 
     * <p><b>注意事项：</b>
     * <ul>
     *   <li>避免存储大对象，影响性能</li>
     *   <li>使用明确的key命名，避免冲突</li>
     *   <li>不要依赖attributes的顺序</li>
     * </ul>
     */
    @Builder.Default
    private final Map<String, Object> attributes = new HashMap<>();

    /**
     * 获取自定义属性
     * 
     * @param key 属性键
     * @return 属性值，不存在返回null
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * 设置自定义属性
     * 
     * @param key 属性键
     * @param value 属性值
     */
    public void putAttribute(String key, Object value) {
        attributes.put(key, value);
    }
}

