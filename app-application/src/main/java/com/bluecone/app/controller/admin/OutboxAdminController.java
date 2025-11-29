// File: app-application/src/main/java/com/bluecone/app/controller/admin/OutboxAdminController.java
package com.bluecone.app.controller.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bluecone.app.infra.outbox.entity.OutboxMessageEntity;
import com.bluecone.app.infra.outbox.entity.OutboxMessageStatus;
import com.bluecone.app.infra.outbox.repository.OutboxMessageRepository;
import com.bluecone.app.infra.outbox.service.OutboxDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * Outbox 管理接口（内部使用），支持查询、手动重试、标记 DEAD、手动触发一次扫描。
 *
 * <p>谨慎使用：生产环境仅限运维/管理员操作。</p>
 */
@RestController
@RequestMapping("/api/admin/outbox")
//@PreAuthorize("hasRole('ADMIN')")
public class OutboxAdminController {

    private static final Logger log = LoggerFactory.getLogger(OutboxAdminController.class);

    private final OutboxMessageRepository repository;
    private final OutboxDispatchService dispatchService;

    public OutboxAdminController(final OutboxMessageRepository repository,
                                 final OutboxDispatchService dispatchService) {
        this.repository = repository;
        this.dispatchService = dispatchService;
    }

    /**
     * 分页查询 Outbox 消息，支持按状态/事件/租户/时间范围过滤。
     */
    @GetMapping
    public Page<OutboxMessageEntity> list(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "20") int size,
                                          @RequestParam(required = false) OutboxMessageStatus status,
                                          @RequestParam(required = false) String eventType,
                                          @RequestParam(required = false) Long tenantId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return repository.pageQuery(page, size, status, eventType, tenantId, from, to);
    }

    /**
     * 将指定消息重置为 NEW 状态，并立即触发一次扫描。
     */
    @PostMapping("/{id}/retry")
    public Map<String, Object> retry(@PathVariable Long id,
                                     @RequestParam(defaultValue = "false") boolean incrementRetry) {
        boolean ok = repository.resetToNew(id, incrementRetry);
        if (ok) {
            dispatchService.dispatchDueMessages();
        }
        return Map.of("id", id, "reset", ok);
    }

    /**
     * 将消息标记为 DEAD，停止重试。
     */
    @PostMapping("/{id}/dead")
    public Map<String, Object> markDead(@PathVariable Long id) {
        boolean ok = repository.markDead(id);
        return Map.of("id", id, "dead", ok);
    }

    /**
     * 手动触发一次 outbox 发布扫描。
     */
    @PostMapping("/publish-once")
    public Map<String, Object> publishOnce() {
        dispatchService.dispatchDueMessages();
        log.info("[OutboxAdmin] manual publish triggered");
        return Map.of("triggered", true);
    }
}
