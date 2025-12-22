package com.bluecone.app.user.controller;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.user.application.account.AccountApplicationService;
import com.bluecone.app.user.dto.account.AccountSummaryDTO;
import com.bluecone.app.user.dto.account.BalanceLedgerItemDTO;
import com.bluecone.app.user.dto.account.PointsLedgerItemDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员账户相关接口。
 */
@Tag(name = "👤 C端开放接口 > 用户相关", description = "会员账户管理接口")
@RestController
@RequestMapping("/api/member/account")
@Validated
public class AccountController {

    private final AccountApplicationService accountApplicationService;

    public AccountController(AccountApplicationService accountApplicationService) {
        this.accountApplicationService = accountApplicationService;
    }

    /**
     * 账户汇总。
     */
    @GetMapping("/summary")
    public ApiResponse<AccountSummaryDTO> summary() {
        AccountSummaryDTO summary = accountApplicationService.getCurrentAccountSummary();
        return ApiResponse.success(summary);
    }

    /**
     * 积分流水列表。
     */
    @GetMapping("/points/ledger")
    public ApiResponse<List<PointsLedgerItemDTO>> listPointsLedger(@RequestParam(defaultValue = "1") int pageNo,
                                                                   @RequestParam(defaultValue = "20") int pageSize) {
        List<PointsLedgerItemDTO> list = accountApplicationService.listPointsLedger(null, null, pageNo, pageSize);
        return ApiResponse.success(list);
    }

    /**
     * 储值流水列表。
     */
    @GetMapping("/balance/ledger")
    public ApiResponse<List<BalanceLedgerItemDTO>> listBalanceLedger(@RequestParam(defaultValue = "1") int pageNo,
                                                                     @RequestParam(defaultValue = "20") int pageSize) {
        List<BalanceLedgerItemDTO> list = accountApplicationService.listBalanceLedger(null, null, pageNo, pageSize);
        return ApiResponse.success(list);
    }
}
