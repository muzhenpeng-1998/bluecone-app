package com.bluecone.app.core.apicontract;

/**
 * 上下文类型枚举
 * 
 * <p>定义请求可以解析的上下文数据类型。
 * 
 * <h3>设计目的</h3>
 * <p>通过声明式的方式指定接口需要哪些上下文，由中间件自动解析和注入。
 * 
 * <h3>使用方式</h3>
 * <p>在API路由配置中声明required和optional上下文：</p>
 * <pre>{@code
 * ApiRoute.builder()
 *     .path("/api/orders")
 *     .requiredContexts(EnumSet.of(ContextType.USER, ContextType.STORE))
 *     .optionalContexts(EnumSet.of(ContextType.INVENTORY))
 *     .build();
 * }</pre>
 * 
 * <h3>中间件协作</h3>
 * <p>每种上下文类型对应一个中间件：</p>
 * <ul>
 *   <li>STORE → StoreMiddleware</li>
 *   <li>USER → UserMiddleware</li>
 *   <li>PRODUCT → ProductMiddleware</li>
 *   <li>INVENTORY → InventoryMiddleware</li>
 * </ul>
 * 
 * @author BlueCone
 * @since 1.0.0
 * @see com.bluecone.app.core.gateway.ApiContext
 */
public enum ContextType {

    /**
     * 门店上下文
     * 
     * <p>包含门店的快照数据，如门店ID、名称、状态、营业时间等。
     * 
     * <p><b>解析来源：</b>
     * <ul>
     *   <li>请求头：X-Store-Id</li>
     *   <li>Token中的storeId字段</li>
     *   <li>URL路径参数</li>
     * </ul>
     * 
     * <p><b>适用场景：</b>
     * <ul>
     *   <li>商户端接口：需要知道当前操作的门店</li>
     *   <li>用户端接口：需要验证门店状态和营业时间</li>
     *   <li>订单接口：需要关联门店信息</li>
     * </ul>
     * 
     * <p><b>解析失败处理：</b>
     * <ul>
     *   <li>如果是required：返回403 Forbidden</li>
     *   <li>如果是optional：继续处理，但上下文为空</li>
     * </ul>
     */
    STORE,

    /**
     * 用户上下文
     * 
     * <p>包含已认证用户的快照数据，如用户ID、昵称、会员等级等。
     * 
     * <p><b>解析来源：</b>
     * <ul>
     *   <li>Authorization请求头中的JWT Token</li>
     *   <li>解析Token获取userId</li>
     *   <li>从数据库或缓存加载用户信息</li>
     * </ul>
     * 
     * <p><b>适用场景：</b>
     * <ul>
     *   <li>用户端接口：需要用户身份信息</li>
     *   <li>订单接口：需要关联下单用户</li>
     *   <li>会员接口：需要用户会员等级</li>
     * </ul>
     * 
     * <p><b>解析失败处理：</b>
     * <ul>
     *   <li>如果是required：返回401 Unauthorized</li>
     *   <li>如果是optional：继续处理，但上下文为空</li>
     * </ul>
     */
    USER,

    /**
     * 商品上下文
     * 
     * <p>包含商品的标识和快照数据，如商品ID、SKU ID、价格、库存等。
     * 
     * <p><b>解析来源：</b>
     * <ul>
     *   <li>请求参数：productId、skuId</li>
     *   <li>请求体：商品列表</li>
     *   <li>从数据库或缓存加载商品信息</li>
     * </ul>
     * 
     * <p><b>适用场景：</b>
     * <ul>
     *   <li>商品详情接口：需要商品完整信息</li>
     *   <li>加购接口：需要验证商品状态和价格</li>
     *   <li>下单接口：需要商品价格和库存</li>
     * </ul>
     * 
     * <p><b>解析失败处理：</b>
     * <ul>
     *   <li>如果是required：返回404 Not Found</li>
     *   <li>如果是optional：继续处理，但上下文为空</li>
     * </ul>
     */
    PRODUCT,

    /**
     * 库存上下文
     * 
     * <p>包含库存策略的快照数据，如库存模式、库存数量、预扣策略等。
     * 
     * <p><b>解析来源：</b>
     * <ul>
     *   <li>基于门店和商品解析库存策略</li>
     *   <li>从库存服务查询实时库存</li>
     *   <li>从缓存获取库存快照</li>
     * </ul>
     * 
     * <p><b>适用场景：</b>
     * <ul>
     *   <li>加购接口：需要验证库存是否充足</li>
     *   <li>下单接口：需要预扣库存</li>
     *   <li>商品列表：需要展示库存状态</li>
     * </ul>
     * 
     * <p><b>解析失败处理：</b>
     * <ul>
     *   <li>如果是required：返回503 Service Unavailable</li>
     *   <li>如果是optional：继续处理，但上下文为空</li>
     * </ul>
     */
    INVENTORY
}

