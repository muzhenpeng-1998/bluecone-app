package com.bluecone.app.application.gateway.handler.webhook;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.application.gateway.dto.webhook.WebhookConfigCreateRequest;
import com.bluecone.app.application.gateway.dto.webhook.WebhookConfigTestRequest;
import com.bluecone.app.application.gateway.dto.webhook.WebhookConfigTestResult;
import com.bluecone.app.application.gateway.dto.webhook.WebhookConfigUpdateRequest;
import com.bluecone.app.application.gateway.dto.webhook.WebhookConfigView;
import com.bluecone.app.application.service.webhook.WebhookConfigAppService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Webhook配置管理控制器
 * 
 * <p>提供Webhook配置的完整生命周期管理功能，包括：
 * <ul>
 *   <li>配置的创建、更新、删除</li>
 *   <li>配置列表查询</li>
 *   <li>配置连通性测试</li>
 * </ul>
 * 
 * <p>所有操作都基于当前租户上下文，确保数据隔离。
 * 
 * @author BlueCone
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/webhook-configs")
public class WebhookConfigAdminController {

    /**
     * Webhook配置应用服务
     * 负责处理Webhook配置相关的业务逻辑
     */
    private final WebhookConfigAppService webhookConfigAppService;

    /**
     * 构造函数注入依赖
     * 
     * @param webhookConfigAppService Webhook配置应用服务实例
     */
    public WebhookConfigAdminController(WebhookConfigAppService webhookConfigAppService) {
        this.webhookConfigAppService = webhookConfigAppService;
    }

    /**
     * 查询当前租户的Webhook配置列表
     * 
     * <p>返回当前租户下所有的Webhook配置信息。
     * 租户上下文由中间件自动注入，无需手动传递。
     * 
     * @return 包含Webhook配置列表的响应对象
     */
    @GetMapping
    public ApiResponse<List<WebhookConfigView>> list() {
        return ApiResponse.success(webhookConfigAppService.listByCurrentTenant());
    }

    /**
     * 创建新的Webhook配置
     * 
     * <p>创建一个新的Webhook配置，配置会自动关联到当前租户。
     * 
     * <p><b>重要业务规则：</b>
     * <ul>
     *   <li>同一租户下，相同事件类型只能配置一个Webhook</li>
     *   <li>URL必须是有效的HTTP/HTTPS地址</li>
     *   <li>创建时会自动生成签名密钥用于验证回调请求</li>
     * </ul>
     * 
     * @param request 创建请求对象，包含URL、事件类型等配置信息
     * @return 包含新创建的Webhook配置视图的响应对象
     */
    @PostMapping
    public ApiResponse<WebhookConfigView> create(@RequestBody WebhookConfigCreateRequest request) {
        return ApiResponse.success(webhookConfigAppService.create(request));
    }

    /**
     * 更新指定的Webhook配置
     * 
     * <p>更新现有的Webhook配置信息。仅允许更新属于当前租户的配置。
     * 
     * <p><b>安全性说明：</b>
     * <ul>
     *   <li>系统会验证配置归属权，防止跨租户操作</li>
     *   <li>更新操作会触发版本号递增，支持乐观锁并发控制</li>
     * </ul>
     * 
     * @param id 配置ID，从URL路径中获取
     * @param request 更新请求对象，包含需要更新的字段
     * @return 包含更新后的Webhook配置视图的响应对象
     */
    @PutMapping("/{id}")
    public ApiResponse<WebhookConfigView> update(@PathVariable("id") Long id,
                                                 @RequestBody WebhookConfigUpdateRequest request) {
        // 将路径参数中的ID设置到请求对象中，确保ID的一致性
        request.setId(id);
        return ApiResponse.success(webhookConfigAppService.update(request));
    }

    /**
     * 删除指定的Webhook配置
     * 
     * <p>删除现有的Webhook配置。采用软删除策略，数据不会真正从数据库中移除。
     * 
     * <p><b>删除影响：</b>
     * <ul>
     *   <li>删除后，该配置关联的事件将不再触发Webhook回调</li>
     *   <li>历史回调记录会保留，用于审计和问题排查</li>
     *   <li>删除操作不可恢复，需谨慎操作</li>
     * </ul>
     * 
     * @param id 配置ID，从URL路径中获取
     * @return 空响应对象，表示删除成功
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        webhookConfigAppService.delete(id);
        return ApiResponse.success(null);
    }

    /**
     * 测试Webhook配置的连通性
     * 
     * <p>向配置的Webhook URL发送测试请求，验证配置是否正确以及目标服务是否可达。
     * 
     * <p><b>测试机制：</b>
     * <ul>
     *   <li>发送模拟的事件数据到目标URL</li>
     *   <li>验证HTTP响应状态码（期望200-299范围）</li>
     *   <li>检查响应时间，超时阈值为5秒</li>
     *   <li>验证签名机制是否正常工作</li>
     * </ul>
     * 
     * <p><b>测试结果包含：</b>
     * <ul>
     *   <li>连通性状态（成功/失败）</li>
     *   <li>响应时间（毫秒）</li>
     *   <li>HTTP状态码</li>
     *   <li>失败原因（如果测试失败）</li>
     * </ul>
     * 
     * @param id 配置ID，从URL路径中获取
     * @param request 测试请求对象（可选），可指定自定义测试数据
     * @return 包含测试结果的响应对象
     */
    @PostMapping("/{id}/test")
    public ApiResponse<WebhookConfigTestResult> test(@PathVariable("id") Long id,
                                                     @RequestBody(required = false) WebhookConfigTestRequest request) {
        // 如果未提供测试请求对象，创建默认的空对象
        if (request == null) {
            request = new WebhookConfigTestRequest();
        }
        // 设置配置ID到请求对象
        request.setId(id);
        return ApiResponse.success(webhookConfigAppService.test(request));
    }
}
