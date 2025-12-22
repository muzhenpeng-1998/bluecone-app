package com.bluecone.app.api.open;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.product.runtime.application.StoreMenuSnapshotProvider;
import com.bluecone.app.product.runtime.model.StoreMenuSnapshotData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * 门店菜单 Open API（Prompt 08）。
 * <p>
 * 提供高性能的菜单查询接口，支持多级缓存（L1/L2）和版本校验。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>小程序/H5 拉取门店菜单</li>
 *   <li>第三方平台对接（美团/饿了么等）</li>
 *   <li>POS 机/自助点餐机拉取菜单</li>
 * </ul>
 * <p>
 * 性能特点：
 * <ul>
 *   <li>L1 缓存（Caffeine）：毫秒级响应</li>
 *   <li>L2 缓存（Redis）：10ms 级响应</li>
 *   <li>数据库回源：100ms 级响应</li>
 *   <li>版本校验：定期采样，确保缓存一致性</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@RestController
@RequestMapping("/api/open/stores")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "👤 C端开放接口 > 门店相关", description = "高性能菜单查询接口，支持多级缓存")
public class StoreMenuOpenController {

    private final StoreMenuSnapshotProvider storeMenuSnapshotProvider;
    private final ObjectMapper objectMapper;

    /**
     * 获取门店菜单快照。
     * <p>
     * 该接口会：
     * <ol>
     *   <li>优先从 L1 缓存（Caffeine）读取</li>
     *   <li>L1 miss 后从 L2 缓存（Redis）读取</li>
     *   <li>L2 miss 后从数据库加载</li>
     *   <li>定期采样校验版本号，确保缓存一致性</li>
     * </ol>
     * <p>
     * 缓存键格式：{tenantId}:{epoch}:{storeId}:{channel}:{orderScene}
     * <p>
     * 当商品/分类/属性/小料变更后，通过 {@link com.bluecone.app.core.cacheepoch.api.CacheEpochProvider#bumpEpoch(long, String)}
     * 自动失效缓存。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP），默认 ALL
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT），默认 DEFAULT
     * @return 菜单快照 JSON 或结构化对象
     */
    @GetMapping("/{storeId}/menu")
    @Operation(summary = "获取门店菜单快照", description = "高性能菜单查询，支持多级缓存和版本校验")
    public ApiResponse<Map<String, Object>> getStoreMenu(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,
            
            @Parameter(description = "门店ID", required = true)
            @PathVariable Long storeId,
            
            @Parameter(description = "渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String channel,
            
            @Parameter(description = "订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）", example = "DEFAULT")
            @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene
    ) {
        log.info("获取门店菜单: tenantId={}, storeId={}, channel={}, orderScene={}", 
                tenantId, storeId, channel, orderScene);

        // 从 Provider 获取快照（自动处理多级缓存）
        Optional<StoreMenuSnapshotData> snapshotOpt = storeMenuSnapshotProvider.getOrLoad(
                tenantId, storeId, channel, orderScene);

        if (snapshotOpt.isEmpty()) {
            log.warn("门店菜单快照不存在: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    tenantId, storeId, channel, orderScene);
            return ApiResponse.success(null);
        }

        StoreMenuSnapshotData snapshot = snapshotOpt.get();
        
        // 解析 menu_json 为结构化对象
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> menuData = objectMapper.readValue(snapshot.menuJson(), Map.class);
            
            // 添加 version 字段
            menuData.put("version", snapshot.version());
            
            log.info("门店菜单快照返回成功: tenantId={}, storeId={}, channel={}, orderScene={}, version={}", 
                    tenantId, storeId, channel, orderScene, snapshot.version());
            
            return ApiResponse.success(menuData);
        } catch (JsonProcessingException e) {
            log.error("解析门店菜单快照失败: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    tenantId, storeId, channel, orderScene, e);
            return ApiResponse.fail("MENU-500-001", "解析菜单快照失败");
        }
    }

    /**
     * 获取门店菜单快照（返回原始 JSON 字符串）。
     * <p>
     * 该接口直接返回 {@code menu_json} 字符串，不进行解析，性能更高。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP），默认 ALL
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT），默认 DEFAULT
     * @return 菜单快照 JSON 字符串
     */
    @GetMapping("/{storeId}/menu/raw")
    @Operation(summary = "获取门店菜单快照（原始 JSON）", description = "直接返回 menu_json 字符串，性能更高")
    public ApiResponse<String> getStoreMenuRaw(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,
            
            @Parameter(description = "门店ID", required = true)
            @PathVariable Long storeId,
            
            @Parameter(description = "渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String channel,
            
            @Parameter(description = "订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）", example = "DEFAULT")
            @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene
    ) {
        log.info("获取门店菜单（原始JSON）: tenantId={}, storeId={}, channel={}, orderScene={}", 
                tenantId, storeId, channel, orderScene);

        // 从 Provider 获取快照（自动处理多级缓存）
        Optional<StoreMenuSnapshotData> snapshotOpt = storeMenuSnapshotProvider.getOrLoad(
                tenantId, storeId, channel, orderScene);

        if (snapshotOpt.isEmpty()) {
            log.warn("门店菜单快照不存在: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    tenantId, storeId, channel, orderScene);
            return ApiResponse.success(null);
        }

        StoreMenuSnapshotData snapshot = snapshotOpt.get();
        
        log.info("门店菜单快照返回成功（原始JSON）: tenantId={}, storeId={}, channel={}, orderScene={}, version={}", 
                tenantId, storeId, channel, orderScene, snapshot.version());
        
        return ApiResponse.success(snapshot.menuJson());
    }
}

