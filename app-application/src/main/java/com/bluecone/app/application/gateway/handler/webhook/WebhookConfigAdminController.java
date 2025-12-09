package com.bluecone.app.application.gateway.handler.webhook;

import com.bluecone.app.api.ApiResponse;
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

@RestController
@RequestMapping("/api/admin/webhook-configs")
public class WebhookConfigAdminController {

    private final WebhookConfigAppService webhookConfigAppService;

    public WebhookConfigAdminController(WebhookConfigAppService webhookConfigAppService) {
        this.webhookConfigAppService = webhookConfigAppService;
    }

    @GetMapping
    public ApiResponse<List<WebhookConfigView>> list() {
        return ApiResponse.success(webhookConfigAppService.listByCurrentTenant());
    }

    @PostMapping
    public ApiResponse<WebhookConfigView> create(@RequestBody WebhookConfigCreateRequest request) {
        return ApiResponse.success(webhookConfigAppService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<WebhookConfigView> update(@PathVariable("id") Long id,
                                                 @RequestBody WebhookConfigUpdateRequest request) {
        request.setId(id);
        return ApiResponse.success(webhookConfigAppService.update(request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        webhookConfigAppService.delete(id);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/test")
    public ApiResponse<WebhookConfigTestResult> test(@PathVariable("id") Long id,
                                                     @RequestBody(required = false) WebhookConfigTestRequest request) {
        if (request == null) {
            request = new WebhookConfigTestRequest();
        }
        request.setId(id);
        return ApiResponse.success(webhookConfigAppService.test(request));
    }
}
