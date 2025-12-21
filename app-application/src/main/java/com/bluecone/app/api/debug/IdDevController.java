package com.bluecone.app.api.debug;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.core.id.IdService;
import com.bluecone.app.core.id.IdType;

@RestController
@RequestMapping("/api/dev")
public class IdDevController {

    private final IdService idService;

    public IdDevController(IdService idService) {
        this.idService = idService;
    }

    @GetMapping("/next-id")
    public Map<String, String> nextId() {
        String ulid = idService.nextId();
        return Map.of(
                "ulid", ulid,
                "orderId", idService.nextId(IdType.ORDER),
                "userId", idService.nextId(IdType.USER),
                "tenantId", idService.nextId(IdType.TENANT));
    }
}
