package com.bluecone.app.api.admin.finance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.security.admin.RequireAdminPermission;
import com.bluecone.app.wallet.infra.persistence.po.WalletAccountPO;
import com.bluecone.app.wallet.infra.persistence.po.WalletLedgerPO;
import com.bluecone.app.wallet.infra.persistence.po.RechargeOrderPO;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletAccountMapper;
import com.bluecone.app.wallet.infra.persistence.mapper.WalletLedgerMapper;
import com.bluecone.app.wallet.infra.persistence.mapper.RechargeOrderMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 钱包管理后台接口
 * 
 * 提供钱包的查询功能：
 * - 查询用户余额
 * - 查询充值记录
 * - 查询流水记录
 * 
 * 权限要求：
 * - 查看：wallet:view
 * - 管理：wallet:manage
 */
@Tag(name = "🎛️ 平台管理后台 > 财务管理 > 钱包管理", description = "平台后台钱包管理接口")
@Slf4j
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class WalletAdminController {
    
    private final WalletAccountMapper accountMapper;
    private final WalletLedgerMapper ledgerMapper;
    private final RechargeOrderMapper rechargeOrderMapper;
    
    /**
     * 查询用户余额
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 账户列表
     */
    @GetMapping("/balances")
    @RequireAdminPermission("wallet:view")
    public Page<WalletBalanceView> getBalances(@RequestHeader("X-Tenant-Id") Long tenantId,
                                               @RequestParam(required = false) Long userId,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        log.info("查询钱包余额: tenantId={}, userId={}, page={}, size={}", tenantId, userId, page, size);
        
        LambdaQueryWrapper<WalletAccountPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletAccountPO::getTenantId, tenantId);
        if (userId != null) {
            wrapper.eq(WalletAccountPO::getUserId, userId);
        }
        wrapper.orderByDesc(WalletAccountPO::getCreatedAt);
        
        Page<WalletAccountPO> accountPage = accountMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为视图对象
        Page<WalletBalanceView> viewPage = new Page<>(page, size, accountPage.getTotal());
        List<WalletBalanceView> views = accountPage.getRecords().stream()
                .map(this::toBalanceView)
                .collect(Collectors.toList());
        viewPage.setRecords(views);
        
        return viewPage;
    }
    
    /**
     * 查询充值记录
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID（可选）
     * @param status 充值状态（可选）
     * @param page 页码
     * @param size 每页大小
     * @return 充值记录列表
     */
    @GetMapping("/recharges")
    @RequireAdminPermission("wallet:view")
    public Page<RechargeRecordView> getRecharges(@RequestHeader("X-Tenant-Id") Long tenantId,
                                                 @RequestParam(required = false) Long userId,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        log.info("查询充值记录: tenantId={}, userId={}, status={}, page={}, size={}", 
                tenantId, userId, status, page, size);
        
        LambdaQueryWrapper<RechargeOrderPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrderPO::getTenantId, tenantId);
        if (userId != null) {
            wrapper.eq(RechargeOrderPO::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(RechargeOrderPO::getStatus, status);
        }
        wrapper.orderByDesc(RechargeOrderPO::getCreatedAt);
        
        Page<RechargeOrderPO> rechargePage = rechargeOrderMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为视图对象
        Page<RechargeRecordView> viewPage = new Page<>(page, size, rechargePage.getTotal());
        List<RechargeRecordView> views = rechargePage.getRecords().stream()
                .map(this::toRechargeView)
                .collect(Collectors.toList());
        viewPage.setRecords(views);
        
        return viewPage;
    }
    
    /**
     * 查询流水记录
     * 
     * @param tenantId 租户ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 每页大小
     * @return 流水记录列表
     */
    @GetMapping("/ledgers")
    @RequireAdminPermission("wallet:view")
    public Page<LedgerRecordView> getLedgers(@RequestHeader("X-Tenant-Id") Long tenantId,
                                             @RequestParam Long userId,
                                             @RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        log.info("查询流水记录: tenantId={}, userId={}, page={}, size={}", tenantId, userId, page, size);
        
        LambdaQueryWrapper<WalletLedgerPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WalletLedgerPO::getTenantId, tenantId);
        wrapper.eq(WalletLedgerPO::getUserId, userId);
        wrapper.orderByDesc(WalletLedgerPO::getCreatedAt);
        
        Page<WalletLedgerPO> ledgerPage = ledgerMapper.selectPage(new Page<>(page, size), wrapper);
        
        // 转换为视图对象
        Page<LedgerRecordView> viewPage = new Page<>(page, size, ledgerPage.getTotal());
        List<LedgerRecordView> views = ledgerPage.getRecords().stream()
                .map(this::toLedgerView)
                .collect(Collectors.toList());
        viewPage.setRecords(views);
        
        return viewPage;
    }
    
    /**
     * 转换为余额视图
     */
    private WalletBalanceView toBalanceView(WalletAccountPO account) {
        WalletBalanceView view = new WalletBalanceView();
        view.setUserId(account.getUserId());
        view.setAvailableBalance(account.getAvailableBalance());
        view.setFrozenBalance(account.getFrozenBalance());
        view.setTotalRecharged(account.getTotalRecharged());
        view.setTotalConsumed(account.getTotalConsumed());
        view.setCurrency(account.getCurrency());
        view.setStatus(account.getStatus());
        view.setCreatedAt(account.getCreatedAt());
        view.setUpdatedAt(account.getUpdatedAt());
        return view;
    }
    
    /**
     * 转换为充值记录视图
     */
    private RechargeRecordView toRechargeView(RechargeOrderPO order) {
        RechargeRecordView view = new RechargeRecordView();
        view.setId(order.getId());
        view.setRechargeId(order.getRechargeNo());
        view.setUserId(order.getUserId());
        view.setRechargeAmount(order.getRechargeAmount());
        view.setBonusAmount(order.getBonusAmount());
        view.setTotalAmount(order.getTotalAmount());
        view.setCurrency(order.getCurrency());
        view.setStatus(order.getStatus());
        view.setPayChannel(order.getPayChannel());
        view.setPayNo(order.getPayNo());
        view.setRechargeRequestedAt(order.getRechargeRequestedAt());
        view.setRechargeCompletedAt(order.getRechargeCompletedAt());
        view.setCreatedAt(order.getCreatedAt());
        return view;
    }
    
    /**
     * 转换为流水记录视图
     */
    private LedgerRecordView toLedgerView(WalletLedgerPO ledger) {
        LedgerRecordView view = new LedgerRecordView();
        view.setId(ledger.getId());
        view.setLedgerNo(ledger.getLedgerNo());
        view.setUserId(ledger.getUserId());
        view.setBizType(ledger.getBizType());
        view.setBizOrderId(ledger.getBizOrderId());
        view.setBizOrderNo(ledger.getBizOrderNo());
        view.setAmount(ledger.getAmount());
        view.setBalanceBefore(ledger.getBalanceBefore());
        view.setBalanceAfter(ledger.getBalanceAfter());
        view.setCurrency(ledger.getCurrency());
        view.setRemark(ledger.getRemark());
        view.setCreatedAt(ledger.getCreatedAt());
        return view;
    }
    
    // ===== DTO类 =====
    
    @Data
    public static class WalletBalanceView {
        private Long userId;
        private BigDecimal availableBalance;
        private BigDecimal frozenBalance;
        private BigDecimal totalRecharged;
        private BigDecimal totalConsumed;
        private String currency;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    public static class RechargeRecordView {
        private Long id;
        private String rechargeId;
        private Long userId;
        private BigDecimal rechargeAmount;
        private BigDecimal bonusAmount;
        private BigDecimal totalAmount;
        private String currency;
        private String status;
        private String payChannel;
        private String payNo;
        private LocalDateTime rechargeRequestedAt;
        private LocalDateTime rechargeCompletedAt;
        private LocalDateTime createdAt;
    }
    
    @Data
    public static class LedgerRecordView {
        private Long id;
        private String ledgerNo;
        private Long userId;
        private String bizType;
        private Long bizOrderId;
        private String bizOrderNo;
        private BigDecimal amount;
        private BigDecimal balanceBefore;
        private BigDecimal balanceAfter;
        private String currency;
        private String remark;
        private LocalDateTime createdAt;
    }
}
