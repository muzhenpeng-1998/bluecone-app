package com.bluecone.app.application.wallet;

import com.bluecone.app.api.ApiResponse;
import com.bluecone.app.wallet.api.dto.RechargeCreateCommand;
import com.bluecone.app.wallet.api.dto.RechargeCreateResult;
import com.bluecone.app.wallet.api.facade.WalletRechargeFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 钱包充值控制器
 * 
 * @author bluecone
 * @since 2025-12-19
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet/recharge")
@RequiredArgsConstructor
@Tag(name = "钱包充值", description = "钱包充值相关接口")
public class RechargeController {
    
    private final WalletRechargeFacade walletRechargeFacade;
    
    /**
     * 创建充值单
     * 
     * @param request 充值请求
     * @return 充值结果（包含充值单号、支付参数等）
     */
    @PostMapping("/create")
    @Operation(summary = "创建充值单", description = "用户发起钱包充值，创建充值单并返回支付参数")
    public ApiResponse<RechargeCreateResult> createRechargeOrder(
            @Valid @RequestBody RechargeCreateRequest request) {
        
        log.info("创建充值单请求：userId={}, amount={}, payChannel={}, idempotencyKey={}", 
                request.getUserId(), request.getRechargeAmount(), 
                request.getPayChannel(), request.getIdempotencyKey());
        
        // 构建命令
        RechargeCreateCommand command = RechargeCreateCommand.builder()
                .tenantId(request.getTenantId() != null ? request.getTenantId() : 1L) // 默认租户
                .userId(request.getUserId())
                .rechargeAmount(request.getRechargeAmount())
                .payChannel(request.getPayChannel())
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        
        // 调用 Facade
        RechargeCreateResult result = walletRechargeFacade.createRechargeOrder(command);
        
        log.info("创建充值单成功：rechargeNo={}, userId={}, amount={}", 
                result.getRechargeNo(), request.getUserId(), result.getRechargeAmount());
        
        return ApiResponse.success(result);
    }
}
