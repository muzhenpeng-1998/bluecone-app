package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.MerchantOrderCommandAppService;
import com.bluecone.app.order.application.command.MerchantAcceptOrderCommand;
import com.bluecone.app.order.application.command.MerchantRejectOrderCommand;
import com.bluecone.app.order.domain.enums.OrderErrorCode;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.event.OrderAcceptedEvent;
import com.bluecone.app.order.domain.event.OrderRejectedEvent;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.domain.repository.OrderActionLogRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 商户操作订单的应用服务实现（M2：接单 + 拒单 + 幂等 + 并发保护）。
 * 
 * <h3>核心设计：</h3>
 * <ul>
 *   <li>幂等保护：通过 bc_order_action_log 表的唯一键（actionKey）保证同一 requestId 只执行一次</li>
 *   <li>乐观锁：通过订单版本号（expectedVersion）防止并发冲突，确保状态流转正确</li>
 *   <li>审计追溯：记录每次接单/拒单的操作人、时间、原因、结果</li>
 * </ul>
 * 
 * <h3>幂等策略：</h3>
 * <ol>
 *   <li>先尝试写入 bc_order_action_log（actionKey 唯一），如果成功则继续</li>
 *   <li>如果写入失败（唯一键冲突），则查询已有记录：
 *     <ul>
 *       <li>如果之前成功（SUCCESS），直接返回已有结果</li>
 *       <li>如果之前失败（FAILED），抛出异常提示用户使用新的 requestId 重试</li>
 *       <li>如果正在处理（PROCESSING），抛出并发异常</li>
 *     </ul>
 *   </li>
 *   <li>执行订单状态变更（调用聚合根方法）</li>
 *   <li>更新订单（带乐观锁），如果版本冲突则抛出异常</li>
 *   <li>更新 action_log 状态为 SUCCESS，记录结果</li>
 * </ol>
 * 
 * <h3>并发保护：</h3>
 * <ul>
 *   <li>乐观锁：OrderRepository.update() 使用版本号（version）防止并发修改</li>
 *   <li>幂等键：actionKey 唯一索引防止重复执行</li>
 *   <li>事务：整个流程在一个事务内，保证原子性</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantOrderCommandAppServiceImpl implements MerchantOrderCommandAppService {

    private final OrderRepository orderRepository;
    private final OrderActionLogRepository actionLogRepository;
    private final DomainEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 商户接单（M2 版本：支持幂等 + 乐观锁）。
     * 
     * <h4>执行顺序（为什么要这个顺序）：</h4>
     * <ol>
     *   <li>参数校验：快速失败，避免无效请求进入数据库</li>
     *   <li>幂等检查：先写 action_log，利用唯一索引防止重复执行（最早拦截点）</li>
     *   <li>查询订单：获取最新订单状态和版本号</li>
     *   <li>业务校验：门店归属、状态约束、版本号校验</li>
     *   <li>状态变更：调用聚合根方法（领域逻辑封装）</li>
     *   <li>持久化：更新订单（带乐观锁），如果失败则回滚整个事务</li>
     *   <li>更新日志：标记 action_log 为成功，记录结果</li>
     *   <li>发布事件：通知下游系统（如通知用户、推送商户端等）</li>
     * </ol>
     * 
     * <h4>为什么失败要抛错而不是返回失败标识：</h4>
     * <ul>
     *   <li>版本冲突：说明订单状态已被其他人修改，当前操作应该失败并回滚事务</li>
     *   <li>状态约束：订单状态不允许接单（如已接单、已取消），应该失败并提示用户刷新</li>
     *   <li>幂等冲突：如果之前失败，应该提示用户使用新的 requestId 重试，而不是静默返回</li>
     * </ul>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView acceptOrder(MerchantAcceptOrderCommand command) {
        // 1. 参数校验
        validateAcceptCommand(command);
        
        // 2. 幂等检查：先写 action_log，利用唯一索引防止重复执行
        String actionKey = OrderActionLog.buildActionKey(
                command.getTenantId(), 
                command.getStoreId(), 
                command.getOrderId(), 
                "ACCEPT", 
                command.getRequestId());
        OrderActionLog actionLog = tryCreateActionLog(command.getTenantId(), actionKey, command, "ACCEPT");
        
        // 如果 actionLog 不为 null，说明是幂等返回（之前已成功）
        if (actionLog != null && actionLog.isSuccess()) {
            log.info("接单操作幂等返回：tenantId={}, orderId={}, requestId={}", 
                    command.getTenantId(), command.getOrderId(), command.getRequestId());
            return parseResultFromActionLog(actionLog);
        }
        
        try {
            // 3. 查询订单
            Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
            if (order == null) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_NOT_FOUND.getMessage());
            }
            
            // 4. 业务校验
            if (!Objects.equals(order.getStoreId(), command.getStoreId())) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_NOT_BELONG_TO_STORE.getMessage());
            }
            
            // 版本号校验（如果传了 expectedVersion）
            if (command.getExpectedVersion() != null && !Objects.equals(order.getVersion(), command.getExpectedVersion())) {
                String msg = String.format("订单版本冲突：期望版本=%d，当前版本=%d", 
                        command.getExpectedVersion(), order.getVersion());
                log.warn("接单失败，版本冲突：orderId={}, expectedVersion={}, actualVersion={}", 
                        order.getId(), command.getExpectedVersion(), order.getVersion());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_VERSION_CONFLICT.getMessage());
            }
            
            // 5. 状态变更（调用聚合根方法，内部会校验状态约束）
            boolean alreadyAccepted = OrderStatus.ACCEPTED.equals(order.getStatus());
            order.accept(command.getOperatorId());
            
            // 6. 持久化（带乐观锁）
            orderRepository.update(order);
            
            // 7. 更新 action_log 为成功
            MerchantOrderView view = MerchantOrderView.from(order);
            String resultJson = toJson(view);
            actionLog.markSuccess(resultJson);
            actionLogRepository.update(actionLog);
            
            // 8. 发布事件（只在首次接单时发布，幂等返回时不发布）
            if (!alreadyAccepted && OrderStatus.ACCEPTED.equals(order.getStatus())) {
                OrderAcceptedEvent event = new OrderAcceptedEvent(
                        command.getTenantId(),
                        command.getStoreId(),
                        order.getId(),
                        command.getOperatorId(),
                        extractPayOrderId(order),
                        toCents(order.getPayableAmount())
                );
                eventPublisher.publish(event);
                log.info("接单事件已发布：orderId={}, operatorId={}", order.getId(), command.getOperatorId());
            }
            
            log.info("接单成功：orderId={}, operatorId={}, version={}", 
                    order.getId(), command.getOperatorId(), order.getVersion());
            return view;
            
        } catch (BusinessException e) {
            // 业务异常：更新 action_log 为失败
            actionLog.markFailed(e.getCode(), e.getMessage());
            actionLogRepository.update(actionLog);
            throw e;
        } catch (Exception e) {
            // 系统异常：更新 action_log 为失败
            actionLog.markFailed("SYSTEM_ERROR", e.getMessage());
            actionLogRepository.update(actionLog);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "接单失败：" + e.getMessage());
        }
    }

    /**
     * 商户拒单（M2 版本：支持幂等 + 乐观锁）。
     * 
     * <h4>执行顺序（与接单类似，不再重复注释）：</h4>
     * <ol>
     *   <li>参数校验</li>
     *   <li>幂等检查</li>
     *   <li>查询订单</li>
     *   <li>业务校验</li>
     *   <li>状态变更（调用 order.reject()）</li>
     *   <li>持久化（带乐观锁）</li>
     *   <li>更新日志</li>
     *   <li>发布事件</li>
     * </ol>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView rejectOrder(MerchantRejectOrderCommand command) {
        // 1. 参数校验
        validateRejectCommand(command);
        
        // 2. 幂等检查
        String actionKey = OrderActionLog.buildActionKey(
                command.getTenantId(), 
                command.getStoreId(), 
                command.getOrderId(), 
                "REJECT", 
                command.getRequestId());
        OrderActionLog actionLog = tryCreateActionLog(command.getTenantId(), actionKey, command, "REJECT");
        
        if (actionLog != null && actionLog.isSuccess()) {
            log.info("拒单操作幂等返回：tenantId={}, orderId={}, requestId={}", 
                    command.getTenantId(), command.getOrderId(), command.getRequestId());
            return parseResultFromActionLog(actionLog);
        }
        
        try {
            // 3. 查询订单
            Order order = orderRepository.findById(command.getTenantId(), command.getOrderId());
            if (order == null) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_NOT_FOUND.getMessage());
            }
            
            // 4. 业务校验
            if (!Objects.equals(order.getStoreId(), command.getStoreId())) {
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_NOT_BELONG_TO_STORE.getMessage());
            }
            
            if (command.getExpectedVersion() != null && !Objects.equals(order.getVersion(), command.getExpectedVersion())) {
                String msg = String.format("订单版本冲突：期望版本=%d，当前版本=%d", 
                        command.getExpectedVersion(), order.getVersion());
                log.warn("拒单失败，版本冲突：orderId={}, expectedVersion={}, actualVersion={}", 
                        order.getId(), command.getExpectedVersion(), order.getVersion());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_VERSION_CONFLICT.getMessage());
            }
            
            // 5. 状态变更
            boolean alreadyRejected = OrderStatus.CANCELED.equals(order.getStatus().normalize()) 
                    && order.getRejectReasonCode() != null;
            order.reject(command.getOperatorId(), command.getReasonCode(), command.getReasonDesc());
            
            // 6. 持久化
            orderRepository.update(order);
            
            // 7. 更新 action_log 为成功
            MerchantOrderView view = MerchantOrderView.from(order);
            String resultJson = toJson(view);
            actionLog.markSuccess(resultJson);
            actionLogRepository.update(actionLog);
            
            // 8. 发布事件
            if (!alreadyRejected) {
                OrderRejectedEvent event = new OrderRejectedEvent(
                        command.getTenantId(),
                        command.getStoreId(),
                        order.getId(),
                        command.getOperatorId(),
                        command.getReasonCode(),
                        command.getReasonDesc(),
                        extractPayOrderId(order),
                        toCents(order.getPayableAmount())
                );
                eventPublisher.publish(event);
                log.info("拒单事件已发布：orderId={}, operatorId={}, reasonCode={}", 
                        order.getId(), command.getOperatorId(), command.getReasonCode());
            }
            
            log.info("拒单成功：orderId={}, operatorId={}, reasonCode={}, version={}", 
                    order.getId(), command.getOperatorId(), command.getReasonCode(), order.getVersion());
            return view;
            
        } catch (BusinessException e) {
            actionLog.markFailed(e.getCode(), e.getMessage());
            actionLogRepository.update(actionLog);
            throw e;
        } catch (Exception e) {
            actionLog.markFailed("SYSTEM_ERROR", e.getMessage());
            actionLogRepository.update(actionLog);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "拒单失败：" + e.getMessage());
        }
    }

    /**
     * 尝试创建幂等动作日志。
     * 
     * <h4>为什么要用 try-catch 捕获唯一键冲突：</h4>
     * <p>
     * 数据库唯一索引是最可靠的幂等保护机制，避免了分布式锁的复杂性和性能开销。
     * 如果 actionKey 已存在，说明该请求已被处理过（或正在处理），应该返回已有结果。
     * </p>
     * 
     * @return 如果是幂等返回（之前已成功），返回已有的 actionLog；否则返回新创建的 actionLog
     */
    private OrderActionLog tryCreateActionLog(Long tenantId, String actionKey, Object command, String actionType) {
        try {
            OrderActionLog log = OrderActionLog.builder()
                    .tenantId(tenantId)
                    .storeId(getStoreId(command))
                    .orderId(getOrderId(command))
                    .actionType(actionType)
                    .actionKey(actionKey)
                    .operatorId(getOperatorId(command))
                    .status("PROCESSING")
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            actionLogRepository.save(log);
            return log;
        } catch (DuplicateKeyException e) {
            // 唯一键冲突，说明该 requestId 已被处理过，查询已有记录
            log.info("幂等键冲突，查询已有记录：actionKey={}", actionKey);
            OrderActionLog existing = actionLogRepository.findByActionKey(tenantId, actionKey);
            if (existing == null) {
                // 理论上不会出现，除非并发插入后立即删除
                throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "幂等记录查询失败");
            }
            if (existing.isSuccess()) {
                // 之前已成功，直接返回已有结果（幂等返回）
                return existing;
            } else if (existing.isFailed()) {
                // 之前失败，提示用户使用新的 requestId 重试
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                        OrderErrorCode.IDEMPOTENT_ACTION_FAILED.getMessage() + "：" + existing.getErrorMsg());
            } else {
                // 正在处理中（PROCESSING），说明有并发请求
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, 
                        OrderErrorCode.ORDER_CONCURRENT_MODIFICATION.getMessage());
            }
        }
    }

    private void validateAcceptCommand(MerchantAcceptOrderCommand command) {
        if (command == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.PARAM_INVALID.getMessage());
        }
        if (command.getTenantId() == null || command.getOrderId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户ID/订单ID 不能为空");
        }
        if (command.getRequestId() == null || command.getRequestId().isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.REQUEST_ID_REQUIRED.getMessage());
        }
    }

    private void validateRejectCommand(MerchantRejectOrderCommand command) {
        if (command == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.PARAM_INVALID.getMessage());
        }
        if (command.getTenantId() == null || command.getOrderId() == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户ID/订单ID 不能为空");
        }
        if (command.getRequestId() == null || command.getRequestId().isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.REQUEST_ID_REQUIRED.getMessage());
        }
        if (command.getReasonCode() == null || command.getReasonCode().isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.REJECT_REASON_REQUIRED.getMessage());
        }
    }

    private Long getStoreId(Object command) {
        if (command instanceof MerchantAcceptOrderCommand) {
            return ((MerchantAcceptOrderCommand) command).getStoreId();
        } else if (command instanceof MerchantRejectOrderCommand) {
            return ((MerchantRejectOrderCommand) command).getStoreId();
        }
        return null;
    }

    private Long getOrderId(Object command) {
        if (command instanceof MerchantAcceptOrderCommand) {
            return ((MerchantAcceptOrderCommand) command).getOrderId();
        } else if (command instanceof MerchantRejectOrderCommand) {
            return ((MerchantRejectOrderCommand) command).getOrderId();
        }
        return null;
    }

    private Long getOperatorId(Object command) {
        if (command instanceof MerchantAcceptOrderCommand) {
            return ((MerchantAcceptOrderCommand) command).getOperatorId();
        } else if (command instanceof MerchantRejectOrderCommand) {
            return ((MerchantRejectOrderCommand) command).getOperatorId();
        }
        return null;
    }

    private MerchantOrderView parseResultFromActionLog(OrderActionLog actionLog) {
        try {
            return objectMapper.readValue(actionLog.getResultJson(), MerchantOrderView.class);
        } catch (Exception e) {
            log.warn("解析幂等结果失败：actionKey={}, resultJson={}", actionLog.getActionKey(), actionLog.getResultJson(), e);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "解析幂等结果失败");
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractPayOrderId(Order order) {
        if (order == null) {
            return null;
        }
        Map<String, Object> ext = order.getExt();
        if (ext == null) {
            return null;
        }
        Object raw = ext.get("payOrderId");
        if (raw instanceof Number) {
            return ((Number) raw).longValue();
        }
        if (raw instanceof String) {
            try {
                return Long.parseLong((String) raw);
            } catch (NumberFormatException ignored) {
                // ignore parse failure
            }
        }
        return null;
    }

    private Long toCents(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }
}
