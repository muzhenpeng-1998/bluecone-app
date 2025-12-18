package com.bluecone.app.order.domain.enums;

import java.util.Arrays;

/**
 * 订单主状态。
 * 
 * <h3>状态收口 V1：Canonical 状态与兼容性策略</h3>
 * <p>
 * 为解决历史遗留的重复语义状态（WAIT_PAY/PENDING_PAYMENT、WAIT_ACCEPT/PENDING_ACCEPT、CANCELED/CANCELLED），
 * 以及草稿态混入订单主状态的问题，制定以下规范：
 * </p>
 * 
 * <h4>Canonical 状态集合（订单主表应使用）：</h4>
 * <ul>
 *   <li>WAIT_PAY - 待支付</li>
 *   <li>WAIT_ACCEPT - 待接单</li>
 *   <li>ACCEPTED - 已接单</li>
 *   <li>IN_PROGRESS - 制作中/服务中</li>
 *   <li>READY - 已出餐/待取货</li>
 *   <li>COMPLETED - 已完成</li>
 *   <li>CANCELED - 已取消</li>
 *   <li>REFUNDED - 已退款</li>
 *   <li>CLOSED - 已关闭</li>
 * </ul>
 * 
 * <h4>非 Canonical 状态（保留兼容，不应写入订单主表）：</h4>
 * <ul>
 *   <li>INIT - 初始化状态（仅用于创建订单瞬时态，应立即流转到 WAIT_PAY）</li>
 *   <li>PAID - 已支付（应立即流转到 WAIT_ACCEPT，不应停留）</li>
 *   <li>DRAFT - 草稿态（仅购物车/结算态，不应落订单主表）</li>
 *   <li>LOCKED_FOR_CHECKOUT - 草稿锁定态（仅结算态，不应落订单主表）</li>
 *   <li>PENDING_CONFIRM - 待确认态（仅结算态，不应落订单主表）</li>
 *   <li>PENDING_PAYMENT - 重复语义，应使用 WAIT_PAY</li>
 *   <li>PENDING_ACCEPT - 重复语义，应使用 WAIT_ACCEPT</li>
 *   <li>CANCELLED - 重复语义，应使用 CANCELED</li>
 * </ul>
 * 
 * <h4>为什么要做状态收口：</h4>
 * <ul>
 *   <li>避免因重复语义导致业务判断遗漏（如 canAccept 只判断 WAIT_ACCEPT 而忘记 PENDING_ACCEPT）</li>
 *   <li>避免因草稿态混入主状态导致查询混乱（如统计订单量时误算草稿单）</li>
 *   <li>统一开发团队对状态的理解，降低代码审查和维护成本</li>
 *   <li>为后续状态机重构和多状态字段拆分打好基础</li>
 * </ul>
 * 
 * <h4>开发规范：</h4>
 * <ul>
 *   <li>新代码必须使用 Canonical 状态</li>
 *   <li>业务判断必须使用 normalize() 后的结果或专用方法（如 isPayPending()、canAccept()）</li>
 *   <li>旧代码读取时使用 fromCodeNormalized() 保证返回 Canonical</li>
 *   <li>禁止在订单主表写入 DRAFT/LOCKED_FOR_CHECKOUT/PENDING_CONFIRM</li>
 * </ul>
 */
public enum OrderStatus {

    // ===== Canonical 状态（订单主表应使用） =====
    WAIT_PAY("WAIT_PAY", "待支付"),
    WAIT_ACCEPT("WAIT_ACCEPT", "待接单"),
    ACCEPTED("ACCEPTED", "已接单"),
    IN_PROGRESS("IN_PROGRESS", "制作中/服务中"),
    READY("READY", "已出餐/待取货"),
    COMPLETED("COMPLETED", "已完成"),
    CANCELED("CANCELED", "已取消"),
    REFUNDED("REFUNDED", "已退款"),
    CLOSED("CLOSED", "已关闭"),

    // ===== 非 Canonical 状态（保留兼容，不应写入订单主表） =====
    
    /**
     * 初始化状态（仅用于创建订单瞬时态，应立即流转到 WAIT_PAY）。
     * <p>⚠️ 不应落订单主表，仅用于创建订单时的瞬时状态。</p>
     */
    INIT("INIT", "初始化"),
    
    /**
     * 已支付状态（应立即流转到 WAIT_ACCEPT，不应停留）。
     * <p>⚠️ 不应作为终态停留，支付成功后应立即流转到 WAIT_ACCEPT。</p>
     */
    PAID("PAID", "已支付"),
    
    /**
     * 草稿态（仅购物车/结算态，不应落订单主表）。
     * <p>⚠️ 仅用于购物车草稿，不应写入订单主表。normalize() 映射到 WAIT_PAY。</p>
     */
    DRAFT("DRAFT", "草稿/预下单"),
    
    /**
     * 草稿锁定态（仅结算态，不应落订单主表）。
     * <p>⚠️ 仅用于结算锁定，不应写入订单主表。normalize() 映射到 WAIT_PAY。</p>
     */
    LOCKED_FOR_CHECKOUT("LOCKED_FOR_CHECKOUT", "草稿锁定"),
    
    /**
     * 待确认态（仅结算态，不应落订单主表）。
     * <p>⚠️ 仅用于结算确认，不应写入订单主表。normalize() 映射到 WAIT_PAY。</p>
     */
    PENDING_CONFIRM("PENDING_CONFIRM", "待确认"),
    
    /**
     * 待支付（重复语义，应使用 WAIT_PAY）。
     * <p>⚠️ 不推荐使用，保留仅为兼容旧数据。新代码应使用 WAIT_PAY。normalize() 映射到 WAIT_PAY。</p>
     */
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),
    
    /**
     * 待接单（重复语义，应使用 WAIT_ACCEPT）。
     * <p>⚠️ 不推荐使用，保留仅为兼容旧数据。新代码应使用 WAIT_ACCEPT。normalize() 映射到 WAIT_ACCEPT。</p>
     */
    PENDING_ACCEPT("PENDING_ACCEPT", "待接单"),
    
    /**
     * 已取消（重复语义，应使用 CANCELED）。
     * <p>⚠️ 不推荐使用，保留仅为兼容旧数据。新代码应使用 CANCELED。normalize() 映射到 CANCELED。</p>
     */
    CANCELLED("CANCELLED", "已取消");

    private final String code;
    private final String desc;

    OrderStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 根据 code 查找状态枚举。
     * <p>⚠️ 注意：此方法会原样返回所有状态（包括非 Canonical），<b>不应直接用于业务判断</b>。</p>
     * <p>业务判断应使用 {@link #fromCodeNormalized(String)} 或调用 {@link #normalize()} 后再判断。</p>
     * 
     * @param code 状态码
     * @return 状态枚举，未找到返回 null
     */
    public static OrderStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(item -> item.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据 code 查找状态枚举，并归一化为 Canonical 状态。
     * <p>此方法保证返回 Canonical 状态，避免重复语义导致的业务判断遗漏。</p>
     * 
     * <h4>映射规则：</h4>
     * <ul>
     *   <li>PENDING_PAYMENT → WAIT_PAY</li>
     *   <li>PENDING_ACCEPT → WAIT_ACCEPT</li>
     *   <li>CANCELLED → CANCELED</li>
     *   <li>INIT/DRAFT/LOCKED_FOR_CHECKOUT/PENDING_CONFIRM → WAIT_PAY（草稿态统一归为待支付）</li>
     *   <li>PAID → WAIT_ACCEPT（已支付应流转到待接单）</li>
     *   <li>其他 Canonical 状态 → 自身</li>
     * </ul>
     * 
     * @param code 状态码
     * @return 归一化后的 Canonical 状态，未找到返回 null
     */
    public static OrderStatus fromCodeNormalized(String code) {
        OrderStatus status = fromCode(code);
        return status == null ? null : status.normalize();
    }

    /**
     * 将当前状态归一化为 Canonical 状态。
     * <p>用于统一业务判断口径，避免重复语义状态导致的遗漏。</p>
     * 
     * <h4>为什么需要 normalize：</h4>
     * <p>
     * 历史原因导致存在 WAIT_PAY/PENDING_PAYMENT、WAIT_ACCEPT/PENDING_ACCEPT、CANCELED/CANCELLED 等重复语义状态。
     * 如果业务代码只判断 WAIT_ACCEPT 而忘记 PENDING_ACCEPT，会导致线上事故（如本应可接单的订单无法接单）。
     * </p>
     * 
     * <h4>映射规则：</h4>
     * <ul>
     *   <li>PENDING_PAYMENT → WAIT_PAY</li>
     *   <li>PENDING_ACCEPT → WAIT_ACCEPT</li>
     *   <li>CANCELLED → CANCELED</li>
     *   <li>INIT/DRAFT/LOCKED_FOR_CHECKOUT/PENDING_CONFIRM → WAIT_PAY（草稿态统一归为待支付）</li>
     *   <li>PAID → WAIT_ACCEPT（已支付应流转到待接单）</li>
     *   <li>Canonical 状态 → 自身</li>
     * </ul>
     * 
     * @return 归一化后的 Canonical 状态
     */
    public OrderStatus normalize() {
        return switch (this) {
            // 重复语义映射
            case PENDING_PAYMENT -> WAIT_PAY;
            case PENDING_ACCEPT -> WAIT_ACCEPT;
            case CANCELLED -> CANCELED;
            
            // 草稿态/结算态映射到待支付
            case INIT, DRAFT, LOCKED_FOR_CHECKOUT, PENDING_CONFIRM -> WAIT_PAY;
            
            // 已支付映射到待接单
            case PAID -> WAIT_ACCEPT;
            
            // Canonical 状态返回自身
            default -> this;
        };
    }

    /**
     * 判断订单是否处于终态（不可再流转的最终状态）。
     * <p>终态订单不应再允许状态变更，用于防御性校验。</p>
     * 
     * @return true 表示终态（COMPLETED/CANCELED/REFUNDED/CLOSED）
     */
    public boolean isTerminal() {
        OrderStatus canonical = this.normalize();
        return canonical == COMPLETED 
            || canonical == CANCELED 
            || canonical == REFUNDED 
            || canonical == CLOSED;
    }

    /**
     * 判断订单是否处于待支付状态。
     * <p>自动兼容 PENDING_PAYMENT，避免业务判断遗漏。</p>
     * 
     * @return true 表示待支付（WAIT_PAY 或 PENDING_PAYMENT）
     */
    public boolean isPayPending() {
        OrderStatus canonical = this.normalize();
        return canonical == WAIT_PAY;
    }

    /**
     * 判断订单是否处于待接单状态。
     * <p>自动兼容 PENDING_ACCEPT，避免业务判断遗漏。</p>
     * 
     * @return true 表示待接单（WAIT_ACCEPT 或 PENDING_ACCEPT）
     */
    public boolean isAcceptPending() {
        OrderStatus canonical = this.normalize();
        return canonical == WAIT_ACCEPT;
    }

    /**
     * 判断订单是否允许商户接单。
     * <p>自动兼容 PENDING_ACCEPT，避免"只判断 WAIT_ACCEPT 而忘记 PENDING_ACCEPT"的线上事故。</p>
     * 
     * <h4>为什么需要兼容 PENDING_ACCEPT：</h4>
     * <p>
     * 旧代码可能将状态写为 PENDING_ACCEPT，如果 canAccept() 只判断 WAIT_ACCEPT，
     * 会导致本应可接单的订单无法接单，影响商户运营和用户体验。
     * </p>
     * 
     * @return true 表示可接单（WAIT_ACCEPT 或 PENDING_ACCEPT）
     */
    public boolean canAccept() {
        return this.isAcceptPending();
    }

    /**
     * 判断订单是否允许取消。
     * <p>根据业务规则，以下状态允许取消：</p>
     * <ul>
     *   <li>待支付（WAIT_PAY/PENDING_PAYMENT）- 用户未支付，可随时取消</li>
     *   <li>待接单（WAIT_ACCEPT/PENDING_ACCEPT）- 商户未接单，可取消</li>
     *   <li>已接单（ACCEPTED）- 商户已接单但未开始制作，可协商取消</li>
     *   <li>草稿态（DRAFT/LOCKED_FOR_CHECKOUT/PENDING_CONFIRM）- 草稿可直接取消</li>
     * </ul>
     * 
     * <p>以下状态不允许取消：</p>
     * <ul>
     *   <li>制作中（IN_PROGRESS）- 商户已投入成本，不可取消</li>
     *   <li>已出餐（READY）- 商品已制作完成，不可取消</li>
     *   <li>已完成（COMPLETED）- 订单已完成，不可取消，只能发起退款</li>
     *   <li>已取消（CANCELED/CANCELLED）- 已经取消，不可重复取消</li>
     *   <li>已退款（REFUNDED）- 已退款，不可再取消</li>
     *   <li>已关闭（CLOSED）- 已关闭，不可再取消</li>
     * </ul>
     * 
     * @return true 表示可取消
     */
    public boolean canCancel() {
        OrderStatus canonical = this.normalize();
        return canonical == WAIT_PAY 
            || canonical == WAIT_ACCEPT 
            || canonical == ACCEPTED;
    }
}
