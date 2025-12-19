package com.bluecone.app.inventory.application.service;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.event.DomainEventPublisher;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.infra.cache.core.CacheKey;
import com.bluecone.app.infra.cache.facade.CacheClient;
import com.bluecone.app.infra.cache.profile.CacheProfile;
import com.bluecone.app.infra.cache.profile.CacheProfileName;
import com.bluecone.app.infra.cache.profile.CacheProfileRegistry;
import com.bluecone.app.infra.redis.idempotent.annotation.Idempotent;
import com.bluecone.app.inventory.api.InventoryCommandApi;
import com.bluecone.app.inventory.api.dto.AdjustStockCommand;
import com.bluecone.app.inventory.api.dto.ConfirmStockCommand;
import com.bluecone.app.inventory.api.dto.LockStockCommand;
import com.bluecone.app.inventory.api.dto.LockStockResult;
import com.bluecone.app.inventory.api.dto.ReleaseStockCommand;
import com.bluecone.app.inventory.application.assembler.InventoryAssembler;
import com.bluecone.app.inventory.domain.event.InventoryAdjustedEvent;
import com.bluecone.app.inventory.domain.event.InventoryReleasedEvent;
import com.bluecone.app.inventory.domain.event.InventoryReservedEvent;
import com.bluecone.app.inventory.domain.model.InventoryLock;
import com.bluecone.app.inventory.domain.model.InventoryPolicy;
import com.bluecone.app.inventory.domain.model.InventoryStock;
import com.bluecone.app.inventory.domain.repository.InventoryLockRepository;
import com.bluecone.app.inventory.domain.repository.InventoryPolicyRepository;
import com.bluecone.app.inventory.domain.repository.InventoryStockRepository;
import com.bluecone.app.inventory.domain.service.StockAdjustDomainService;
import com.bluecone.app.inventory.domain.service.StockDeductDomainService;
import com.bluecone.app.inventory.domain.service.StockLockDomainService;
import com.bluecone.app.inventory.domain.service.StockReleaseDomainService;
import com.bluecone.app.inventory.domain.type.InventoryLockStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryCommandApplicationService implements InventoryCommandApi {

    private final InventoryStockRepository inventoryStockRepository;
    private final InventoryPolicyRepository inventoryPolicyRepository;
    private final InventoryLockRepository inventoryLockRepository;
    private final StockLockDomainService stockLockDomainService;
    private final StockDeductDomainService stockDeductDomainService;
    private final StockReleaseDomainService stockReleaseDomainService;
    private final StockAdjustDomainService stockAdjustDomainService;
    private final CacheClient cacheClient;
    private final CacheProfileRegistry cacheProfileRegistry;
    /** 领域事件发布器，负责将库存变更写入 Outbox */
    private final DomainEventPublisher domainEventPublisher;

    @Override
    @Idempotent(key = "'inv:lock:' + #command.tenantId + ':' + #command.requestId", expireSeconds = 600)
    @Transactional(rollbackFor = Exception.class)
    public LockStockResult lockStock(LockStockCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getItemId(), "itemId 不能为空");
        Objects.requireNonNull(command.getOrderId(), "orderId 不能为空");
        Objects.requireNonNull(command.getLockQty(), "lockQty 不能为空");
        Objects.requireNonNull(command.getRequestId(), "requestId 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long itemId = command.getItemId();
        Long locationId = command.getLocationId() != null ? command.getLocationId() : 0L;

        InventoryStock stock = inventoryStockRepository.findByTenantStoreItem(
                tenantId, storeId, itemId, locationId);
        if (stock == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不存在，tenantId=" + tenantId
                    + ", storeId=" + storeId + ", itemId=" + itemId + ", locationId=" + locationId);
        }

        InventoryPolicy policy = inventoryPolicyRepository.findByItem(tenantId, storeId, itemId);

        int expireSeconds = (command.getLockExpireSeconds() != null && command.getLockExpireSeconds() > 0)
                ? command.getLockExpireSeconds()
                : 15 * 60;
        LocalDateTime expireAt = LocalDateTime.now().plusSeconds(expireSeconds);

        InventoryLock lock = stockLockDomainService.lock(
                stock,
                policy,
                command.getOrderId(),
                command.getOrderItemId(),
                command.getLockQty(),
                command.getRequestId(),
                expireAt
        );

        evictStockCache(tenantId, storeId, itemId, locationId);
        InventoryReservedEvent event = new InventoryReservedEvent(
                tenantId,
                storeId,
                command.getOrderId(),
                itemId,
                nullSafe(lock.getLockQty()),
                resolveReserveReason(command)
        );
        domainEventPublisher.publish(event);

        return LockStockResult.ok(lock.getId(), InventoryAssembler.toStockView(stock));
    }

    @Override
    @Idempotent(key = "'inv:confirm:' + #command.tenantId + ':' + #command.requestId", expireSeconds = 3600)
    @Transactional(rollbackFor = Exception.class)
    public void confirmStock(ConfirmStockCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getOrderId(), "orderId 不能为空");
        Objects.requireNonNull(command.getRequestId(), "requestId 不能为空");

        List<InventoryLock> locks = inventoryLockRepository.findLockedByOrder(
                command.getTenantId(),
                command.getStoreId(),
                command.getOrderId());
        if (locks == null || locks.isEmpty()) {
            return;
        }
        if (command.getLockIds() != null && !command.getLockIds().isEmpty()) {
            locks = locks.stream()
                    .filter(lock -> command.getLockIds().contains(lock.getId()))
                    .collect(Collectors.toList());
        }
        stockDeductDomainService.confirmLocksAndDeduct(locks, command.getRequestId());
        locks.forEach(lock -> evictStockCache(lock.getTenantId(), lock.getStoreId(), lock.getItemId(), lock.getLocationId()));
    }

    @Override
    @Idempotent(key = "'inv:release:' + #command.tenantId + ':' + #command.requestId", expireSeconds = 1800)
    @Transactional(rollbackFor = Exception.class)
    public void releaseStock(ReleaseStockCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getOrderId(), "orderId 不能为空");
        Objects.requireNonNull(command.getRequestId(), "requestId 不能为空");

        List<InventoryLock> locks = inventoryLockRepository.findLockedByOrder(
                command.getTenantId(),
                command.getStoreId(),
                command.getOrderId());
        if (locks == null || locks.isEmpty()) {
            return;
        }
        if (command.getLockIds() != null && !command.getLockIds().isEmpty()) {
            locks = locks.stream()
                    .filter(lock -> command.getLockIds().contains(lock.getId()))
                    .collect(Collectors.toList());
        }
        // 仅处理 LOCKED 状态
        locks = locks.stream()
                .filter(lock -> lock.getStatus() == InventoryLockStatus.LOCKED)
                .collect(Collectors.toList());
        stockReleaseDomainService.releaseLocks(locks, command.getRequestId(), command.isExpired());
        locks.forEach(lock -> evictStockCache(lock.getTenantId(), lock.getStoreId(), lock.getItemId(), lock.getLocationId()));
        if (!locks.isEmpty()) {
            String releaseReason = resolveReleaseReason(command);
            locks.forEach(lock -> {
                InventoryReleasedEvent event = new InventoryReleasedEvent(
                        lock.getTenantId(),
                        lock.getStoreId(),
                        lock.getOrderId(),
                        lock.getItemId(),
                        nullSafe(lock.getLockQty()),
                        releaseReason
                );
                domainEventPublisher.publish(event);
            });
        }
    }

    @Override
    @Idempotent(key = "'inv:adjust:' + #command.tenantId + ':' + #command.requestId", expireSeconds = 1800)
    @Transactional(rollbackFor = Exception.class)
    public void adjustStock(AdjustStockCommand command) {
        Objects.requireNonNull(command.getTenantId(), "tenantId 不能为空");
        Objects.requireNonNull(command.getStoreId(), "storeId 不能为空");
        Objects.requireNonNull(command.getItemId(), "itemId 不能为空");
        Objects.requireNonNull(command.getQty(), "qty 不能为空");
        Objects.requireNonNull(command.getBizRefType(), "bizRefType 不能为空");
        Objects.requireNonNull(command.getRequestId(), "requestId 不能为空");
        Objects.requireNonNull(command.getAdjustType(), "adjustType 不能为空");

        Long tenantId = command.getTenantId();
        Long storeId = command.getStoreId();
        Long itemId = command.getItemId();
        Long locationId = command.getLocationId() != null ? command.getLocationId() : 0L;

        InventoryStock stock = inventoryStockRepository.findByTenantStoreItem(
                tenantId, storeId, itemId, locationId);
        if (stock == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "库存不存在，无法调整，tenantId=" + tenantId
                    + ", storeId=" + storeId + ", itemId=" + itemId + ", locationId=" + locationId);
        }

        long beforeAvailable = nullSafe(stock.getAvailableQty());
        switch (command.getAdjustType()) {
            case INBOUND -> stockAdjustDomainService.inbound(
                    stock,
                    command.getQty(),
                    command.getBizRefType(),
                    command.getBizRefId(),
                    command.getRequestId());
            case OUTBOUND -> stockAdjustDomainService.outbound(
                    stock,
                    command.getQty(),
                    command.getBizRefType(),
                    command.getBizRefId(),
                    command.getRequestId());
            case INVENTORY_CHECK -> stockAdjustDomainService.adjustTo(
                    stock,
                    command.getQty(), // 约定 qty 表示盘点后的目标总量
                    command.getBizRefType(),
                    command.getBizRefId(),
                    command.getRequestId());
            default -> throw new BusinessException(CommonErrorCode.BAD_REQUEST, "不支持的调整类型");
        }

        evictStockCache(tenantId, storeId, itemId, locationId);
        long afterAvailable = nullSafe(stock.getAvailableQty());
        long delta = afterAvailable - beforeAvailable;
        if (delta != 0L) {
            InventoryAdjustedEvent event = new InventoryAdjustedEvent(
                    tenantId,
                    storeId,
                    itemId,
                    delta,
                    afterAvailable,
                    resolveAdjustSource(command),
                    command.getOperatorId()
            );
            domainEventPublisher.publish(event);
        }
    }

    private void evictStockCache(Long tenantId, Long storeId, Long itemId, Long locationId) {
        CacheProfile profile = cacheProfileRegistry.getProfile(CacheProfileName.INVENTORY_STOCK);
        String bizId = tenantId + ":" + storeId + ":" + (locationId == null ? 0L : locationId) + ":" + itemId;
        CacheKey key = CacheKey.generic(String.valueOf(tenantId), profile.domain(), bizId);
        cacheClient.evict(CacheProfileName.INVENTORY_STOCK, key);
    }

    private String resolveReserveReason(LockStockCommand command) {
        if (command.getReason() != null && !command.getReason().isBlank()) {
            return command.getReason();
        }
        return "ORDER_PLACE";
    }

    private String resolveReleaseReason(ReleaseStockCommand command) {
        if (command.getReason() != null && !command.getReason().isBlank()) {
            return command.getReason();
        }
        return command.isExpired() ? "PAYMENT_TIMEOUT" : "ORDER_CANCEL";
    }

    private String resolveAdjustSource(AdjustStockCommand command) {
        if (command.getSource() != null && !command.getSource().isBlank()) {
            return command.getSource();
        }
        if (command.getBizRefType() != null && !command.getBizRefType().isBlank()) {
            return command.getBizRefType();
        }
        return command.getAdjustType().name();
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }
}
