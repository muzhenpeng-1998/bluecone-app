package com.bluecone.app.product.application.menu;

import com.bluecone.app.product.domain.model.readmodel.StoreMenuSnapshot;
import com.bluecone.app.product.domain.repository.ProductRepository;
import com.bluecone.app.product.dto.view.StoreMenuSnapshotView;
import com.bluecone.app.user.application.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 门店菜单快照查询应用服务，用于 C 端菜单高并发读取。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuSnapshotApplicationService {

    private final CurrentUserContext currentUserContext;
    private final ProductRepository productRepository;

    /**
     * 按门店 / 渠道 / 场景查询当前有效的菜单快照。
     */
    public StoreMenuSnapshotView getStoreMenuSnapshot(Long tenantId,
                                                      Long storeId,
                                                      String channel,
                                                      String orderScene) {
        Long resolvedTenantId = tenantId != null ? tenantId : currentUserContext.getCurrentTenantId();
        String resolvedChannel = (channel != null && !channel.isBlank()) ? channel : "ALL";
        String resolvedScene = (orderScene != null && !orderScene.isBlank()) ? orderScene : "DEFAULT";

        StoreMenuSnapshot snapshot = productRepository.loadStoreMenuSnapshot(resolvedTenantId, storeId, resolvedChannel, resolvedScene);
        if (snapshot == null) {
            log.info("菜单快照未找到, tenantId={}, storeId={}, channel={}, scene={}", resolvedTenantId, storeId, resolvedChannel, resolvedScene);
            return null;
        }
        return StoreMenuSnapshotView.builder()
                .tenantId(snapshot.getTenantId())
                .storeId(snapshot.getStoreId())
                .channel(snapshot.getChannel() != null ? snapshot.getChannel().getCode() : resolvedChannel)
                .orderScene(snapshot.getScene() != null ? snapshot.getScene().getCode() : resolvedScene)
                .version(snapshot.getVersion())
                .menuJson(snapshot.getMenuJson())
                .generatedAt(snapshot.getGeneratedAt() != null ? snapshot.getGeneratedAt().toString() : null)
                .build();
    }
}
