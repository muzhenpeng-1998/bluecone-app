package com.bluecone.app.application.controller.store;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BizException;
import com.bluecone.app.infra.tenant.TenantContext;
import com.bluecone.app.store.api.StorePrintRuleFacade;
import com.bluecone.app.store.api.dto.StorePrintRuleView;
import com.bluecone.app.store.application.command.ChangeStorePrintRuleStatusCommand;
import com.bluecone.app.store.application.command.UpsertStorePrintRulesCommand;
import com.bluecone.app.store.application.query.StorePrintRuleListQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 门店打印规则管理后台 Controller。
 * <p>职责：配置订单事件到设备/模板的打印规则。</p>
 */
@RestController
@RequestMapping("/api/admin/store/print-rule")
public class AdminStorePrintRuleController {

    private final StorePrintRuleFacade storePrintRuleFacade;

    public AdminStorePrintRuleController(StorePrintRuleFacade storePrintRuleFacade) {
        this.storePrintRuleFacade = storePrintRuleFacade;
    }

    /**
     * 查询门店打印规则列表。
     */
    @GetMapping("/list")
    public ApiResponse<List<StorePrintRuleView>> list(@RequestParam Long storeId,
                                                      @RequestParam(required = false) String eventType,
                                                      @RequestParam(required = false) String status) {
        Long tenantId = requireTenantId();
        StorePrintRuleListQuery query = new StorePrintRuleListQuery();
        query.setTenantId(tenantId);
        query.setStoreId(storeId);
        query.setEventType(eventType);
        query.setStatus(status);
        return ApiResponse.success(storePrintRuleFacade.list(query));
    }

    /**
     * 批量新增/更新打印规则。
     */
    @PostMapping("/upsert")
    public ApiResponse<Void> upsert(@RequestBody UpsertStorePrintRulesCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storePrintRuleFacade.upsertRules(command);
        return ApiResponse.success();
    }

    /**
     * 修改单条打印规则状态。
     */
    @PutMapping("/status")
    public ApiResponse<Void> changeStatus(@RequestBody ChangeStorePrintRuleStatusCommand command) {
        Long tenantId = requireTenantId();
        command.setTenantId(tenantId);
        storePrintRuleFacade.changeStatus(command);
        return ApiResponse.success();
    }

    private Long requireTenantId() {
        String tenantIdStr = TenantContext.getTenantId();
        if (tenantIdStr == null || tenantIdStr.isBlank()) {
            throw new BizException(CommonErrorCode.UNAUTHORIZED, "租户未登录或上下文缺失");
        }
        try {
            return Long.parseLong(tenantIdStr);
        } catch (NumberFormatException ex) {
            throw new BizException(CommonErrorCode.BAD_REQUEST, "非法的租户标识");
        }
    }
}
