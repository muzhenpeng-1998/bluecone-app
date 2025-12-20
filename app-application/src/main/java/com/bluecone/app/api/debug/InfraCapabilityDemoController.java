package com.bluecone.app.controller;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.infra.redis.idempotent.IdempotentScene;
import com.bluecone.app.infra.redis.idempotent.annotation.Idempotent;
import com.bluecone.app.infra.redis.lock.annotation.DistributedLock;
import com.bluecone.app.infra.redis.ratelimit.RateLimitStrategy;
import com.bluecone.app.infra.redis.ratelimit.annotation.RateLimit;

/**
 * Redis 能力层（锁/限流/幂等）注解演示接口。
 * <p>curl 示例见每个方法注释，可直接复制调用。</p>
 */
@RestController
@RequestMapping("/api/demo/infra")
public class InfraCapabilityDemoController {

    /**
     * 演示分布式锁：相同 clientOrderId 同时只能成功一个请求。
     * curl -X POST "http://localhost:8080/api/demo/infra/lock" -H "Content-Type: application/json" -d '{"clientOrderId":"lock-1001"}'
     */
    @PostMapping("/lock")
    @DistributedLock(key = "'order:create:' + #request.clientOrderId()", waitTime = 200, leaseTime = 3000)
    public Map<String, Object> demoLock(@RequestBody LockRequest request) {
        // 模拟业务耗时，方便观察并发抢锁行为：第二个请求在 waitTime 内无法拿到锁会抛异常
        simulateWork();
        return Map.of(
                "clientOrderId", request.clientOrderId(),
                "lockedAt", Instant.now().toString(),
                "thread", Thread.currentThread().getName());
    }

    /**
     * 演示固定窗口限流：同一 IP 60 秒最多 3 次，超限返回 null（可观察日志）。
     * curl "http://localhost:8080/api/demo/infra/ratelimit?ip=127.0.0.1"
     */
    @GetMapping("/ratelimit")
    @RateLimit(key = "'login:' + #ip", limit = 3, windowSeconds = 60, strategy = RateLimitStrategy.SILENT_DROP)
    public Map<String, Object> demoRateLimit(@RequestParam String ip) {
        return Map.of("ip", ip, "acceptedAt", Instant.now().toString());
    }

    /**
     * 演示幂等：同一 requestId 300 秒内只能成功一次，重复将抛业务异常。
     * curl -X POST "http://localhost:8080/api/demo/infra/idempotent" -H "Content-Type: application/json" -d '{"requestId":"idem-2001"}'
     */
    @PostMapping("/idempotent")
    @Idempotent(key = "'order:create:' + #request.requestId()", scene = IdempotentScene.API, expireSeconds = 300)
    public Map<String, Object> demoIdempotent(@RequestBody IdempotentRequest request) {
        return Map.of(
                "requestId", request.requestId(),
                "processedAt", Instant.now().toString());
    }

    private void simulateWork() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private record LockRequest(String clientOrderId) {
    }

    private record IdempotentRequest(String requestId) {
    }
}
