package com.bluecone.app.order.application.impl;

import com.bluecone.app.core.error.CommonErrorCode;
import com.bluecone.app.core.exception.BusinessException;
import com.bluecone.app.order.application.CollaborativeOrderSessionAppService;
import com.bluecone.app.order.application.generator.OrderIdGenerator;
import com.bluecone.app.order.domain.model.OrderSession;
import com.bluecone.app.order.domain.repository.OrderSessionRepository;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CollaborativeOrderSessionAppServiceImpl implements CollaborativeOrderSessionAppService {

    private final OrderSessionRepository orderSessionRepository;
    private final OrderIdGenerator orderIdGenerator;

    @Override
    public OrderSession createSession(Long tenantId, Long storeId, Long tableId, Long hostUserId) {
        if (tenantId == null || storeId == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "创建会话需指定租户与门店");
        }
        LocalDateTime now = LocalDateTime.now();
        OrderSession session = OrderSession.builder()
                .id(orderIdGenerator.nextId())
                .tenantId(tenantId)
                .storeId(storeId)
                .tableId(tableId)
                .sessionId(generateSessionId())
                .status("OPEN")
                .version(0)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(hostUserId)
                .updatedBy(hostUserId)
                .build();
        orderSessionRepository.save(session);
        return session;
    }

    @Override
    public OrderSession getSession(Long tenantId, String sessionId) {
        if (tenantId == null || sessionId == null || sessionId.isBlank()) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "缺少会话查询条件");
        }
        return orderSessionRepository.findBySessionId(tenantId, sessionId);
    }

    @Override
    public OrderSession updateSnapshotWithVersionCheck(Long tenantId, String sessionId, Integer expectedVersion, String snapshotJson) {
        OrderSession session = getSession(tenantId, sessionId);
        if (session == null) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "会话不存在或已失效");
        }
        if (expectedVersion != null && !expectedVersion.equals(session.getVersion())) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "会话已被更新，请刷新后重试");
        }
        session.setLastSnapshot(snapshotJson);
        session.setUpdatedAt(LocalDateTime.now());
        boolean ok = orderSessionRepository.updateWithVersionCheck(session);
        if (!ok) {
            throw new BusinessException(CommonErrorCode.BAD_REQUEST, "会话更新冲突，请刷新后重试");
        }

        // TODO: 实时同步扩展点：更新成功后，可通过 WebSocket/SSE 将最新快照与版本推送给该 sessionId 的订阅客户端。
        return session;
    }

    @Override
    public void closeSessionAfterConfirm(Long tenantId, String sessionId) {
        if (sessionId == null) {
            return;
        }
        OrderSession session = orderSessionRepository.findBySessionId(tenantId, sessionId);
        if (session == null) {
            return;
        }
        session.markConfirmed();
        session.setUpdatedAt(LocalDateTime.now());
        orderSessionRepository.updateWithVersionCheck(session);
    }

    private String generateSessionId() {
        // 暂时使用短 UUID，后续可替换为可读编码/短链。
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
