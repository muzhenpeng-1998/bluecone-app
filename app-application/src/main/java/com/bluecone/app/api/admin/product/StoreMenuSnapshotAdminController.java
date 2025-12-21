package com.bluecone.app.api.admin.product;

import com.bluecone.app.core.api.ApiResponse;
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.domain.service.StoreMenuSnapshotDomainService;
import com.bluecone.app.product.dto.StoreMenuSnapshotDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * 门店菜单快照管理接口（Admin）。
 * <p>
 * Prompt 07: 提供触发重建菜单快照的接口，用于手动刷新门店菜单。
 * <p>
 * 使用场景：
 * <ul>
 *   <li>商品/分类/属性/小料变更后，手动触发重建</li>
 *   <li>定时任务触发重建（如每日凌晨刷新）</li>
 *   <li>测试环境验证菜单快照结构</li>
 * </ul>
 *
 * @author System
 * @since 2025-12-21
 */
@RestController
@RequestMapping("/api/admin/store-menu-snapshots")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "门店菜单快照管理（Admin）", description = "门店菜单快照的构建与查询接口")
public class StoreMenuSnapshotAdminController {

    private final StoreMenuSnapshotDomainService storeMenuSnapshotDomainService;

    /**
     * 触发重建指定门店/渠道/场景的菜单快照。
     * <p>
     * 该接口会：
     * <ol>
     *   <li>查询门店可售商品配置</li>
     *   <li>批量加载商品及其关联数据（SKU/规格/属性/小料/分类）</li>
     *   <li>执行过滤规则（enabled + 定时展示窗口）</li>
     *   <li>使用 {@link com.bluecone.app.product.infrastructure.assembler.UnifiedOptionGroupAssembler} 构建统一选项组</li>
     *   <li>序列化为 JSON 并保存到 {@code bc_store_menu_snapshot} 表</li>
     *   <li>version 自增</li>
     * </ol>
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP），默认 ALL
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT），默认 DEFAULT
     * @param now        当前时间，用于定时展示判断（为 null 时使用服务器当前时间）
     * @return 重建后的快照信息
     */
    @PostMapping("/rebuild")
    @Operation(summary = "触发重建菜单快照", description = "重建指定门店/渠道/场景的菜单快照，支持定时展示过滤")
    public ApiResponse<StoreMenuSnapshotDTO> rebuildSnapshot(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,
            
            @Parameter(description = "门店ID", required = true)
            @RequestParam Long storeId,
            
            @Parameter(description = "渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String channel,
            
            @Parameter(description = "订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）", example = "DEFAULT")
            @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene,
            
            @Parameter(description = "当前时间，用于定时展示判断（格式：yyyy-MM-dd'T'HH:mm:ss）", example = "2025-12-25T12:00:00")
            @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime now
    ) {
        log.info("触发重建菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}, now={}", 
                tenantId, storeId, channel, orderScene, now);

        BcStoreMenuSnapshot snapshot = storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                tenantId, storeId, channel, orderScene, now);

        StoreMenuSnapshotDTO dto = toDTO(snapshot);
        
        log.info("菜单快照重建成功: tenantId={}, storeId={}, channel={}, orderScene={}, version={}", 
                tenantId, storeId, channel, orderScene, snapshot.getVersion());

        return ApiResponse.success(dto);
    }

    /**
     * 查询指定门店/渠道/场景的菜单快照。
     *
     * @param tenantId   租户ID
     * @param storeId    门店ID
     * @param channel    渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP），默认 ALL
     * @param orderScene 订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT），默认 DEFAULT
     * @return 菜单快照信息
     */
    @GetMapping
    @Operation(summary = "查询菜单快照", description = "查询指定门店/渠道/场景的菜单快照")
    public ApiResponse<StoreMenuSnapshotDTO> getSnapshot(
            @Parameter(description = "租户ID", required = true)
            @RequestParam Long tenantId,
            
            @Parameter(description = "门店ID", required = true)
            @RequestParam Long storeId,
            
            @Parameter(description = "渠道（ALL, DINE_IN, TAKEAWAY, DELIVERY, PICKUP）", example = "ALL")
            @RequestParam(required = false, defaultValue = "ALL") String channel,
            
            @Parameter(description = "订单场景（DEFAULT, BREAKFAST, LUNCH, DINNER, NIGHT）", example = "DEFAULT")
            @RequestParam(required = false, defaultValue = "DEFAULT") String orderScene
    ) {
        log.info("查询菜单快照: tenantId={}, storeId={}, channel={}, orderScene={}", 
                tenantId, storeId, channel, orderScene);

        BcStoreMenuSnapshot snapshot = storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                tenantId, storeId, channel, orderScene, null);

        if (snapshot == null) {
            log.warn("菜单快照不存在: tenantId={}, storeId={}, channel={}, orderScene={}", 
                    tenantId, storeId, channel, orderScene);
            return ApiResponse.success(null);
        }

        StoreMenuSnapshotDTO dto = toDTO(snapshot);
        
        return ApiResponse.success(dto);
    }

    private StoreMenuSnapshotDTO toDTO(BcStoreMenuSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        return StoreMenuSnapshotDTO.builder()
                .storeId(snapshot.getStoreId())
                .channel(snapshot.getChannel())
                .orderScene(snapshot.getOrderScene())
                .version(snapshot.getVersion())
                .menuJson(snapshot.getMenuJson())
                .generatedAt(snapshot.getGeneratedAt())
                .build();
    }
}

