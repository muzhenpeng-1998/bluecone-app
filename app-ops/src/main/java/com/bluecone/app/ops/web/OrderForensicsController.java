package com.bluecone.app.ops.web;

import com.bluecone.app.ops.api.dto.forensics.OrderForensicsView;
import com.bluecone.app.ops.service.OrderForensicsQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * 订单诊断控制器
 * 
 * 提供订单全链路诊断接口，用于运维排查问题
 * 
 * 安全性：
 * - 该接口受 OpsConsoleAccessInterceptor 保护，需要提供有效的 ops token
 * - 所有查询强制进行租户隔离验证
 * - 只读接口，不修改任何业务状态
 */
@Slf4j
@RestController
@RequestMapping("/ops/api/orders")
@RequiredArgsConstructor
public class OrderForensicsController {
    
    private final OrderForensicsQueryService forensicsQueryService;
    
    /**
     * 获取订单诊断视图
     * 
     * @param orderId 订单ID（路径参数）
     * @param tenantId 租户ID（查询参数，必须）
     * @return 订单诊断视图
     */
    @GetMapping("/{orderId}/forensics")
    public OrderForensicsView getForensics(
        @PathVariable Long orderId,
        @RequestParam Long tenantId
    ) {
        log.info("[OrderForensicsController] Get forensics: orderId={}, tenantId={}", orderId, tenantId);
        
        // 参数验证
        if (orderId == null || orderId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid orderId");
        }
        
        if (tenantId == null || tenantId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tenantId is required");
        }
        
        try {
            return forensicsQueryService.queryForensics(tenantId, orderId);
        } catch (IllegalArgumentException e) {
            log.warn("[OrderForensicsController] Invalid request: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("[OrderForensicsController] Failed to query forensics: orderId={}, tenantId={}", 
                      orderId, tenantId, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                                             "Failed to query forensics: " + e.getMessage());
        }
    }
}
