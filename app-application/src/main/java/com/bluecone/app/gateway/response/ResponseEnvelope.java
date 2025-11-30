package com.bluecone.app.gateway.response;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unified response envelope for gateway APIs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseEnvelope<T> {

    @Builder.Default
    private String code = "OK";
    @Builder.Default
    private String message = "success";
    private T data;
    private String traceId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
