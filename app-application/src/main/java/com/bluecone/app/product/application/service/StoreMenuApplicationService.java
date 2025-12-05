package com.bluecone.app.product.application.service;

import com.bluecone.app.product.application.command.RebuildStoreMenuSnapshotCommand;
import com.bluecone.app.product.application.query.StoreMenuSnapshotQuery;
import com.bluecone.app.product.dao.entity.BcStoreMenuSnapshot;
import com.bluecone.app.product.domain.repository.StoreMenuSnapshotRepository;
import com.bluecone.app.product.domain.service.StoreMenuSnapshotDomainService;
import com.bluecone.app.product.dto.StoreMenuSnapshotDTO;
import com.bluecone.app.user.application.CurrentUserContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 门店菜单应用服务：编排领域服务与仓储，负责快照的重建和读取。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StoreMenuApplicationService {

    private final StoreMenuSnapshotDomainService storeMenuSnapshotDomainService;
    private final StoreMenuSnapshotRepository storeMenuSnapshotRepository;
    private final CurrentUserContext currentUserContext;

    /**
     * 触发快照重建并返回最新快照。
     */
    public StoreMenuSnapshotDTO rebuildSnapshot(RebuildStoreMenuSnapshotCommand command) {
        Long tenantId = currentUserContext.getCurrentTenantId();
        String channel = command.getChannel() != null ? command.getChannel() : "ALL";
        String scene = command.getOrderScene() != null ? command.getOrderScene() : "DEFAULT";
        BcStoreMenuSnapshot snapshot = storeMenuSnapshotDomainService.rebuildAndSaveSnapshot(
                tenantId, command.getStoreId(), channel, scene);
        return toDTO(snapshot);
    }

    /**
     * 读取当前快照，读路径只命中快照表，适合高并发。
     */
    public StoreMenuSnapshotDTO getSnapshot(StoreMenuSnapshotQuery query) {
        Long tenantId = currentUserContext.getCurrentTenantId();
        String channel = query.getChannel() != null ? query.getChannel() : "ALL";
        String scene = query.getOrderScene() != null ? query.getOrderScene() : "DEFAULT";
        Optional<BcStoreMenuSnapshot> snapshotOpt = storeMenuSnapshotRepository
                .findByTenantAndStoreAndChannelAndScene(tenantId, query.getStoreId(), channel, scene);
        return snapshotOpt.map(this::toDTO).orElse(null);
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
