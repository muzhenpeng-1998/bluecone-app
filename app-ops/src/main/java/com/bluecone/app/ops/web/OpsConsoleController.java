package com.bluecone.app.ops.web;

import com.bluecone.app.ops.api.dto.OpsSummary;
import com.bluecone.app.ops.api.dto.drill.ConsumeItem;
import com.bluecone.app.ops.api.dto.drill.IdemConflictItem;
import com.bluecone.app.ops.api.dto.drill.OutboxItem;
import com.bluecone.app.ops.api.dto.drill.PageResult;
import com.bluecone.app.ops.service.OpsDrillService;
import com.bluecone.app.ops.service.OpsSummaryService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ops")
    public class OpsConsoleController {

    private final OpsSummaryService opsSummaryService;
    private final OpsDrillService opsDrillService;

    public OpsConsoleController(final OpsSummaryService opsSummaryService,
                                final OpsDrillService opsDrillService) {
        this.opsSummaryService = opsSummaryService;
        this.opsDrillService = opsDrillService;
    }

    @GetMapping("/api/summary")
    public OpsSummary summary() {
        return opsSummaryService.getSummary();
    }

    @GetMapping("/api/outbox")
    public PageResult<OutboxItem> outbox(@RequestParam("status") String status,
                                         @RequestParam(value = "cursor", required = false) String cursor,
                                         @RequestParam(value = "limit", required = false) Integer limit) {
        validateOutboxStatus(status);
        Long beforeId = parseCursor(cursor);
        int resolvedLimit = limit == null ? 50 : limit;
        return opsDrillService.listOutbox(status, beforeId, resolvedLimit);
    }

    @GetMapping("/api/consume")
    public PageResult<ConsumeItem> consume(@RequestParam("group") String group,
                                           @RequestParam("status") String status,
                                           @RequestParam(value = "cursor", required = false) String cursor,
                                           @RequestParam(value = "limit", required = false) Integer limit) {
        validateConsumeGroup(group);
        validateConsumeStatus(status);
        Long beforeId = parseCursor(cursor);
        int resolvedLimit = limit == null ? 50 : limit;
        return opsDrillService.listConsume(group, status, beforeId, resolvedLimit);
    }

    @GetMapping("/api/idempotency/conflicts")
    public PageResult<IdemConflictItem> idemConflicts(@RequestParam(value = "cursor", required = false) String cursor,
                                                      @RequestParam(value = "limit", required = false) Integer limit) {
        Long beforeId = parseCursor(cursor);
        int resolvedLimit = limit == null ? 50 : limit;
        return opsDrillService.listIdemConflicts(beforeId, resolvedLimit);
    }

    /**
     * Serve the minimal HTML console page.
     */
    @GetMapping("/console")
    public ResponseEntity<String> console() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/ops/ops-console.html");
        byte[] bytes = resource.getContentAsByteArray();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "html", StandardCharsets.UTF_8));
        return new ResponseEntity<>(new String(bytes, StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid cursor");
        }
    }

    private void validateOutboxStatus(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!("READY".equals(s) || "FAILED".equals(s) || "PROCESSING".equals(s) || "SENT".equals(s))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid outbox status");
        }
    }

    private void validateConsumeStatus(String status) {
        String s = status == null ? "" : status.trim().toUpperCase();
        if (!("PROCESSING".equals(s) || "SUCCEEDED".equals(s) || "FAILED".equals(s) || "RETRY".equals(s))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consume status");
        }
    }

    private void validateConsumeGroup(String group) {
        if (group == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "group is required");
        }
        String trimmed = group.trim();
        if (!trimmed.matches("^[A-Za-z0-9_]{2,32}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid consumer group");
        }
    }
}
