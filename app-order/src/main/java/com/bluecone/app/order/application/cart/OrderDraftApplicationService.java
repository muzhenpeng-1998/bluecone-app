package com.bluecone.app.order.application.cart;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.api.cart.OrderDraftFacade;
import com.bluecone.app.order.api.cart.dto.AddDraftItemCommandDTO;
import com.bluecone.app.order.api.cart.dto.ChangeDraftItemQuantityCommandDTO;
import com.bluecone.app.order.api.cart.dto.ClearDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.LockDraftCommandDTO;
import com.bluecone.app.order.api.cart.dto.OrderDraftItemDTO;
import com.bluecone.app.order.api.cart.dto.OrderDraftViewDTO;
import com.bluecone.app.order.api.cart.dto.RemoveDraftItemCommandDTO;
import com.bluecone.app.order.api.dto.ConfirmOrderItemDTO;
import com.bluecone.app.order.domain.enums.OrderSource;
import com.bluecone.app.order.domain.enums.OrderStatus;
import com.bluecone.app.order.infra.cache.OrderDraftCacheService;
import com.bluecone.app.order.domain.model.Order;
import com.bluecone.app.order.domain.repository.OrderDraftRepository;
import com.bluecone.app.order.domain.service.CartDomainService;
import com.bluecone.app.infra.redis.lock.DistributedLock;
import com.bluecone.app.infra.redis.lock.LockProperties;
import com.bluecone.app.core.tenant.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 订单草稿 / 购物车应用服务。
 * 缓存策略：
 * - 读：优先走 Caffeine，其次 Redis，未命中回源 DB 并回填。
 * - 写：DB 成功后刷新缓存或驱逐。
 */
@Service
@RequiredArgsConstructor
public class OrderDraftApplicationService implements OrderDraftFacade {

    private static final Logger log = LoggerFactory.getLogger(OrderDraftApplicationService.class);
    private static final String DEFAULT_CHANNEL = "WECHAT_MINI";
    private static final OrderSource DEFAULT_SCENE = OrderSource.DINE_IN;

    private final OrderDraftRepository orderDraftRepository;
    private final CartDomainService cartDomainService;
    private final OrderDraftCacheService orderDraftCacheService;
    private final DistributedLock distributedLock;
    private final LockProperties lockProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public OrderDraftViewDTO loadCurrentDraft() {
        Context ctx = resolveContext();
        OrderDraftViewDTO cached = orderDraftCacheService.getFromCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene);
        if (cached != null) {
            return cached;
        }
        Optional<Order> draftOpt = orderDraftRepository.findDraft(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene);
        OrderDraftViewDTO view = draftOpt.map(this::toViewDTO).orElseGet(() -> emptyView(ctx));
        orderDraftCacheService.putToCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene, view);
        return view;
    }

    @Override
    @Transactional
    public OrderDraftViewDTO addItem(AddDraftItemCommandDTO command) {
        Context ctx = resolveContext();
        String lockKey = buildLockKey(ctx);
        return executeWithLock(lockKey, () -> {
            Order draft = loadOrCreate(ctx);
            ConfirmOrderItemDTO itemDTO = toConfirmItemDTO(command);
            cartDomainService.addItem(draft, itemDTO);
            draft.recalculateAmounts();
            Order saved = orderDraftRepository.saveDraft(draft);
            OrderDraftViewDTO view = toViewDTO(saved);
            orderDraftCacheService.putToCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene, view);
            return view;
        });
    }

    @Override
    @Transactional
    public OrderDraftViewDTO changeItemQuantity(ChangeDraftItemQuantityCommandDTO command) {
        Context ctx = resolveContext();
        String lockKey = buildLockKey(ctx);
        return executeWithLock(lockKey, () -> {
            Order draft = loadOrThrow(ctx);
            cartDomainService.changeItemQuantity(draft, command.getSkuId(), parseAttrs(command.getAttrsJson()), command.getNewQuantity());
            draft.recalculateAmounts();
            Order saved = orderDraftRepository.saveDraft(draft);
            OrderDraftViewDTO view = toViewDTO(saved);
            orderDraftCacheService.putToCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene, view);
            return view;
        });
    }

    @Override
    @Transactional
    public OrderDraftViewDTO removeItem(RemoveDraftItemCommandDTO command) {
        Context ctx = resolveContext();
        String lockKey = buildLockKey(ctx);
        return executeWithLock(lockKey, () -> {
            Order draft = loadOrThrow(ctx);
            cartDomainService.removeItem(draft, command.getSkuId(), parseAttrs(command.getAttrsJson()));
            draft.recalculateAmounts();
            Order saved = orderDraftRepository.saveDraft(draft);
            OrderDraftViewDTO view = toViewDTO(saved);
            orderDraftCacheService.putToCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene, view);
            return view;
        });
    }

    @Override
    @Transactional
    public OrderDraftViewDTO clearDraft(ClearDraftCommandDTO command) {
        Context ctx = resolveContext();
        String lockKey = buildLockKey(ctx);
        return executeWithLock(lockKey, () -> {
            Optional<Order> draftOpt = orderDraftRepository.findDraft(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene);
            if (draftOpt.isEmpty()) {
                return emptyView(ctx);
            }
            Order draft = draftOpt.get();
            cartDomainService.clearCart(draft);
            orderDraftRepository.deleteDraft(draft.getId());
            orderDraftCacheService.evictCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene);
            return emptyView(ctx);
        });
    }

    @Override
    @Transactional
    public OrderDraftViewDTO lockDraft(LockDraftCommandDTO command) {
        Context ctx = resolveContext();
        String lockKey = buildLockKey(ctx);
        return executeWithLock(lockKey, () -> {
            Order draft = loadOrThrow(ctx);
            draft.assertEditable();
            draft.recalculateAmounts();
            if (command != null && command.getClientPayableAmount() != null) {
                long clientAmount = command.getClientPayableAmount();
                long serverAmount = toCents(draft.getPayableAmount());
                if (clientAmount != serverAmount) {
                    throw new BusinessException(CommonErrorCode.BAD_REQUEST, "金额有变化，请刷新后重试");
                }
            }
            draft.setStatus(OrderStatus.LOCKED_FOR_CHECKOUT);
            Order saved = orderDraftRepository.saveDraft(draft);
            OrderDraftViewDTO view = toViewDTO(saved);
            orderDraftCacheService.putToCache(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene, view);
            return view;
        });
    }

    private Order loadOrCreate(Context ctx) {
        return orderDraftRepository.findDraft(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene)
                .orElseGet(() -> newDraft(ctx));
    }

    private Order loadOrThrow(Context ctx) {
        return orderDraftRepository.findDraft(ctx.tenantId, ctx.storeId, ctx.userId, ctx.channel, ctx.scene)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.BAD_REQUEST, "购物车订单不存在"));
    }

    private Order newDraft(Context ctx) {
        Order draft = new Order();
        draft.setTenantId(ctx.tenantId);
        draft.setStoreId(ctx.storeId);
        draft.setUserId(ctx.userId);
        draft.setChannel(ctx.channel);
        draft.setOrderSource(ctx.sceneEnum);
        draft.setStatus(OrderStatus.DRAFT);
        draft.setPayStatus(null);
        draft.setItems(Collections.emptyList());
        draft.setCurrency("CNY");
        draft.setVersion(0);
        LocalDateTime now = LocalDateTime.now();
        draft.setCreatedAt(now);
        draft.setUpdatedAt(now);
        draft.setSessionVersion(0);
        draft.recalculateAmounts();
        return draft;
    }

    private ConfirmOrderItemDTO toConfirmItemDTO(AddDraftItemCommandDTO command) {
        ConfirmOrderItemDTO dto = new ConfirmOrderItemDTO();
        dto.setProductId(command.getProductId());
        dto.setSkuId(command.getSkuId());
        dto.setQuantity(command.getQuantity());
        dto.setAttrs(parseAttrs(command.getAttrsJson()));
        dto.setRemark(command.getRemark());
        dto.setClientUnitPrice(fromCents(command.getClientUnitPrice()));
        dto.setClientSubtotalAmount(null);
        return dto;
    }

    private OrderDraftViewDTO toViewDTO(Order draft) {
        OrderDraftViewDTO view = new OrderDraftViewDTO();
        view.setOrderId(draft.getId());
        view.setTenantId(draft.getTenantId());
        view.setStoreId(draft.getStoreId());
        view.setUserId(draft.getUserId());
        view.setChannel(draft.getChannel());
        view.setScene(draft.getOrderSource() != null ? draft.getOrderSource().getCode() : null);
        view.setState(draft.getStatus() != null ? draft.getStatus().getCode() : null);
        view.setOriginTotalAmount(toCents(draft.getTotalAmount()));
        view.setDiscountTotalAmount(toCents(draft.getDiscountAmount()));
        view.setPayableTotalAmount(toCents(draft.getPayableAmount()));
        view.setItems(draft.getItems() == null ? Collections.emptyList() : draft.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList()));
        view.setExtJson(toJson(draft.getExt()));
        view.setVersion(draft.getVersion() == null ? 0L : draft.getVersion().longValue());
        return view;
    }

    private OrderDraftItemDTO toItemDTO(com.bluecone.app.order.domain.model.OrderItem item) {
        OrderDraftItemDTO dto = new OrderDraftItemDTO();
        dto.setProductId(item.getProductId());
        dto.setSkuId(item.getSkuId());
        dto.setProductName(item.getProductName());
        dto.setSkuName(item.getSkuName());
        dto.setProductPictureUrl(null);
        dto.setQuantity(item.getQuantity());
        dto.setAttrsJson(toJson(item.getAttrs()));
        dto.setRemark(item.getRemark());
        dto.setOriginUnitPrice(toCents(item.getUnitPrice()));
        dto.setUnitPrice(toCents(item.getUnitPrice()));
        dto.setDiscountAmount(toCents(item.getDiscountAmount()));
        dto.setPayableAmount(toCents(item.getPayableAmount()));
        return dto;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> parseAttrs(String attrsJson) {
        if (!StringUtils.hasText(attrsJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(attrsJson, new TypeReference<>() {
            });
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private long toCents(BigDecimal amount) {
        if (amount == null) {
            return 0L;
        }
        return amount.multiply(BigDecimal.valueOf(100)).longValue();
    }

    private BigDecimal fromCents(Long cents) {
        if (cents == null) {
            return null;
        }
        return BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100));
    }

    private OrderDraftViewDTO emptyView(Context ctx) {
        OrderDraftViewDTO dto = new OrderDraftViewDTO();
        dto.setOrderId(null);
        dto.setTenantId(ctx.tenantId);
        dto.setStoreId(ctx.storeId);
        dto.setUserId(ctx.userId);
        dto.setChannel(ctx.channel);
        dto.setScene(ctx.scene);
        dto.setState(OrderStatus.DRAFT.getCode());
        dto.setOriginTotalAmount(0L);
        dto.setDiscountTotalAmount(0L);
        dto.setPayableTotalAmount(0L);
        dto.setItems(Collections.emptyList());
        dto.setExtJson(null);
        dto.setVersion(0L);
        return dto;
    }

    private String buildLockKey(Context ctx) {
        return "orderDraft:" + ctx.tenantId + ":" + ctx.storeId + ":" + ctx.userId + ":" + ctx.channel + ":" + ctx.scene;
    }

    private <T> T executeWithLock(String bizKey, SupplierWithException<T> supplier) {
        long waitMs = lockProperties.getDefaultWaitTimeMs();
        long leaseMs = lockProperties.getDefaultLeaseTimeMs();
        String owner = ownerId();
        boolean locked = distributedLock.tryLock(bizKey, owner, waitMs, leaseMs);
        if (!locked) {
            throw new BusinessException(CommonErrorCode.CONFLICT, "购物车正被操作，请稍后重试");
        }
        try {
            return supplier.get();
        } finally {
            distributedLock.unlock(bizKey, owner);
        }
    }

    private String ownerId() {
        return "cart-" + UUID.randomUUID() + "-t" + Thread.currentThread().getId();
    }

    private Context resolveContext() {
        String tenantRaw = TenantContext.getTenantId();
        Long tenantId = parseLong(tenantRaw);
        Long storeId = currentStoreId();
        Long userId = currentUserId();
        if (tenantId == null || storeId == null || userId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "租户/门店/用户信息缺失");
        }
        String channel = currentChannel();
        String scene = currentScene();
        return new Context(tenantId, storeId, userId, channel, scene, OrderSource.fromCode(scene) == null ? DEFAULT_SCENE : OrderSource.fromCode(scene));
    }

    private Long currentStoreId() {
        String fromMdc = MDC.get("storeId");
        return parseLong(fromMdc);
    }

    private Long currentUserId() {
        String fromMdc = MDC.get("userId");
        return parseLong(fromMdc);
    }

    private String currentChannel() {
        String channel = MDC.get("channel");
        if (!StringUtils.hasText(channel)) {
            return DEFAULT_CHANNEL;
        }
        return channel;
    }

    private String currentScene() {
        String scene = MDC.get("scene");
        if (!StringUtils.hasText(scene)) {
            return DEFAULT_SCENE.getCode();
        }
        return scene;
    }

    private Long parseLong(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private interface SupplierWithException<T> {
        T get();
    }

    private record Context(Long tenantId, Long storeId, Long userId, String channel, String scene, OrderSource sceneEnum) {
    }
}
