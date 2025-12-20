package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.dto.MerchantOrderView;
import com.bluecone.app.order.application.MerchantFulfillmentAppService;
import com.bluecone.app.order.application.command.CompleteOrderCommand;
import com.bluecone.app.order.application.command.MarkReadyCommand;
import com.bluecone.app.order.application.command.StartOrderCommand;
import com.bluecone.app.order.domain.enums.OrderErrorCode;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.model.OrderActionLog;
import com.bluecone.app.order.domain.repository.OrderActionLogRepository;
import com.bluecone.app.order.domain.repository.OrderRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 商户履约流转应用服务实现（M3：开始制作 + 出餐完成 + 订单完成 + 幂等 + 并发保护）。
 * 
 * <h3>核心设计：</h3>
 * <ul>
 *   <li>幂等保护：通过 bc_order_action_log 表的唯一键（actionKey）保证同一 requestId 只执行一次</li>
 *   <li>乐观锁：通过订单版本号（expectedVersion）防止并发冲突，确保状态流转正确</li>
 *   <li>审计追溯：记录每次履约操作的操作人、时间、结果</li>
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
 */
@Slf4j
@Service
public class MerchantFulfillmentAppServiceImpl implements MerchantFulfillmentAppService {

    private final OrderRepository orderRepository;
    private final OrderActionLogRepository actionLogRepository;
    private final ObjectMapper objectMapper;

    public MerchantFulfillmentAppServiceImpl(OrderRepository orderRepository,
                                            OrderActionLogRepository actionLogRepository,
                                            @Qualifier("redisObjectMapper") ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.actionLogRepository = actionLogRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 商户开始制作订单（M3 版本：支持幂等 + 乐观锁）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView startOrder(StartOrderCommand command) {
        // 1. 参数校验
        validateStartCommand(command);
        
        // 2. 幂等检查：先写 action_log，利用唯一索引防止重复执行
        String actionKey = OrderActionLog.buildActionKey(
                command.getTenantId(), 
                command.getStoreId(), 
                command.getOrderId(), 
                "START", 
                command.getRequestId());
        OrderActionLog actionLog = tryCreateActionLog(command.getTenantId(), actionKey, command, "START");
        
        // 如果 actionLog 不为 null 且成功，说明是幂等返回（之前已成功）
        if (actionLog != null && actionLog.isSuccess()) {
            log.info("开始制作操作幂等返回：tenantId={}, orderId={}, requestId={}", 
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
                log.warn("开始制作失败，版本冲突：orderId={}, expectedVersion={}, actualVersion={}", 
                        order.getId(), command.getExpectedVersion(), order.getVersion());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_VERSION_CONFLICT.getMessage());
            }
            
            // 5. 状态变更（调用聚合根方法，内部会校验状态约束）
            boolean alreadyStarted = OrderStatus.IN_PROGRESS.equals(order.getStatus());
            LocalDateTime now = LocalDateTime.now();
            order.start(command.getOperatorId(), now);
            
            // 6. 持久化（带乐观锁）
            orderRepository.update(order);
            
            // 7. 更新 action_log 为成功
            MerchantOrderView view = MerchantOrderView.from(order);
            String resultJson = toJson(view);
            actionLog.markSuccess(resultJson);
            actionLogRepository.update(actionLog);
            
            log.info("开始制作成功：orderId={}, operatorId={}, version={}, alreadyStarted={}", 
                    order.getId(), command.getOperatorId(), order.getVersion(), alreadyStarted);
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
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "开始制作失败：" + e.getMessage());
        }
    }

    /**
     * 商户标记订单出餐完成（M3 版本：支持幂等 + 乐观锁）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView markReady(MarkReadyCommand command) {
        // 1. 参数校验
        validateMarkReadyCommand(command);
        
        // 2. 幂等检查
        String actionKey = OrderActionLog.buildActionKey(
                command.getTenantId(), 
                command.getStoreId(), 
                command.getOrderId(), 
                "READY", 
                command.getRequestId());
        OrderActionLog actionLog = tryCreateActionLog(command.getTenantId(), actionKey, command, "READY");
        
        if (actionLog != null && actionLog.isSuccess()) {
            log.info("出餐完成操作幂等返回：tenantId={}, orderId={}, requestId={}", 
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
                log.warn("出餐完成失败，版本冲突：orderId={}, expectedVersion={}, actualVersion={}", 
                        order.getId(), command.getExpectedVersion(), order.getVersion());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_VERSION_CONFLICT.getMessage());
            }
            
            // 5. 状态变更
            boolean alreadyReady = OrderStatus.READY.equals(order.getStatus());
            LocalDateTime now = LocalDateTime.now();
            order.markReady(command.getOperatorId(), now);
            
            // 6. 持久化
            orderRepository.update(order);
            
            // 7. 更新 action_log 为成功
            MerchantOrderView view = MerchantOrderView.from(order);
            String resultJson = toJson(view);
            actionLog.markSuccess(resultJson);
            actionLogRepository.update(actionLog);
            
            log.info("出餐完成成功：orderId={}, operatorId={}, version={}, alreadyReady={}", 
                    order.getId(), command.getOperatorId(), order.getVersion(), alreadyReady);
            return view;
            
        } catch (BusinessException e) {
            actionLog.markFailed(e.getCode(), e.getMessage());
            actionLogRepository.update(actionLog);
            throw e;
        } catch (Exception e) {
            actionLog.markFailed("SYSTEM_ERROR", e.getMessage());
            actionLogRepository.update(actionLog);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "出餐完成失败：" + e.getMessage());
        }
    }

    /**
     * 标记订单完成（M3 版本：支持幂等 + 乐观锁）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public MerchantOrderView completeOrder(CompleteOrderCommand command) {
        // 1. 参数校验
        validateCompleteCommand(command);
        
        // 2. 幂等检查
        String actionKey = OrderActionLog.buildActionKey(
                command.getTenantId(), 
                command.getStoreId(), 
                command.getOrderId(), 
                "COMPLETE", 
                command.getRequestId());
        OrderActionLog actionLog = tryCreateActionLog(command.getTenantId(), actionKey, command, "COMPLETE");
        
        if (actionLog != null && actionLog.isSuccess()) {
            log.info("订单完成操作幂等返回：tenantId={}, orderId={}, requestId={}", 
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
                log.warn("订单完成失败，版本冲突：orderId={}, expectedVersion={}, actualVersion={}", 
                        order.getId(), command.getExpectedVersion(), order.getVersion());
                throw new BusinessException(CommonErrorCode.BAD_REQUEST, OrderErrorCode.ORDER_VERSION_CONFLICT.getMessage());
            }
            
            // 5. 状态变更
            boolean alreadyCompleted = OrderStatus.COMPLETED.equals(order.getStatus());
            LocalDateTime now = LocalDateTime.now();
            order.complete(command.getOperatorId(), now);
            
            // 6. 持久化
            orderRepository.update(order);
            
            // 7. 更新 action_log 为成功
            MerchantOrderView view = MerchantOrderView.from(order);
            String resultJson = toJson(view);
            actionLog.markSuccess(resultJson);
            actionLogRepository.update(actionLog);
            
            log.info("订单完成成功：orderId={}, operatorId={}, version={}, alreadyCompleted={}", 
                    order.getId(), command.getOperatorId(), order.getVersion(), alreadyCompleted);
            return view;
            
        } catch (BusinessException e) {
            actionLog.markFailed(e.getCode(), e.getMessage());
            actionLogRepository.update(actionLog);
            throw e;
        } catch (Exception e) {
            actionLog.markFailed("SYSTEM_ERROR", e.getMessage());
            actionLogRepository.update(actionLog);
            throw new BusinessException(CommonErrorCode.SYSTEM_ERROR, "订单完成失败：" + e.getMessage());
        }
    }

    /**
     * 尝试创建幂等动作日志。
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

    private void validateStartCommand(StartOrderCommand command) {
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

    private void validateMarkReadyCommand(MarkReadyCommand command) {
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

    private void validateCompleteCommand(CompleteOrderCommand command) {
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

    private Long getStoreId(Object command) {
        if (command instanceof StartOrderCommand) {
            return ((StartOrderCommand) command).getStoreId();
        } else if (command instanceof MarkReadyCommand) {
            return ((MarkReadyCommand) command).getStoreId();
        } else if (command instanceof CompleteOrderCommand) {
            return ((CompleteOrderCommand) command).getStoreId();
        }
        return null;
    }

    private Long getOrderId(Object command) {
        if (command instanceof StartOrderCommand) {
            return ((StartOrderCommand) command).getOrderId();
        } else if (command instanceof MarkReadyCommand) {
            return ((MarkReadyCommand) command).getOrderId();
        } else if (command instanceof CompleteOrderCommand) {
            return ((CompleteOrderCommand) command).getOrderId();
        }
        return null;
    }

    private Long getOperatorId(Object command) {
        if (command instanceof StartOrderCommand) {
            return ((StartOrderCommand) command).getOperatorId();
        } else if (command instanceof MarkReadyCommand) {
            return ((MarkReadyCommand) command).getOperatorId();
        } else if (command instanceof CompleteOrderCommand) {
            return ((CompleteOrderCommand) command).getOperatorId();
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
}
