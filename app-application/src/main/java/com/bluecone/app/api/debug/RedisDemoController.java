package com.bluecone.app.controller;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.infra.redis.core.RedisKeyBuilder;
import com.bluecone.app.infra.redis.core.RedisKeyNamespace;
import com.bluecone.app.infra.redis.core.RedisOps;

/**
 * Redis 功能演示接口，方便逐项验证 RedisOps 封装。
 * <p>
 * curl 示例见各方法注释，可直接复制执行。
 * </p>
 */
@Tag(name = "🛠️ 开发调试 > 缓存调试", description = "Redis功能测试接口")
@RestController
@RequestMapping("/api/demo/redis")
public class RedisDemoController {

    private final RedisOps redisOps;
    private final RedisKeyBuilder redisKeyBuilder;

    public RedisDemoController(RedisOps redisOps, RedisKeyBuilder redisKeyBuilder) {
        this.redisOps = redisOps;
        this.redisKeyBuilder = redisKeyBuilder;
    }

    /**
     * 写入字符串并可设置 TTL。
     * curl -X POST http://localhost:8080/api/demo/redis/string -H "Content-Type: application/json" -d '{"bizId":"demo","value":"hello","ttlSeconds":60}'
     */
    @PostMapping("/string")
    public Map<String, Object> setString(@RequestBody SetStringRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.CACHE, request.bizId());
        redisOps.setString(key, request.value(), toDuration(request.ttlSeconds()));
        return Map.of("key", key, "value", redisOps.getString(key));
    }

    /**
     * 获取字符串。
     * curl "http://localhost:8080/api/demo/redis/string?bizId=demo"
     */
    @GetMapping("/string")
    public Map<String, Object> getString(@RequestParam String bizId) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.CACHE, bizId);
        return Map.of("key", key, "value", redisOps.getString(key));
    }

    /**
     * 写入对象，演示 JSON 序列化。
     * curl -X POST http://localhost:8080/api/demo/redis/object -H "Content-Type: application/json" -d '{"bizId":"user1","name":"Alice","age":18,"ttlSeconds":120}'
     */
    @PostMapping("/object")
    public Map<String, Object> setObject(@RequestBody SetObjectRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.USER, request.bizId());
        DemoUser user = new DemoUser(request.name(), request.age());
        redisOps.setObject(key, user, toDuration(request.ttlSeconds()));
        return Map.of("key", key, "value", redisOps.getObject(key, DemoUser.class));
    }

    /**
     * 获取对象。
     * curl "http://localhost:8080/api/demo/redis/object?bizId=user1"
     */
    @GetMapping("/object")
    public Map<String, Object> getObject(@RequestParam String bizId) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.USER, bizId);
        return Map.of("key", key, "value", redisOps.getObject(key, DemoUser.class));
    }

    /**
     * 写入哈希字段。
     * curl -X POST http://localhost:8080/api/demo/redis/hash -H "Content-Type: application/json" -d '{"bizId":"order123","field":"status","value":"PAID"}'
     */
    @PostMapping("/hash")
    public Map<String, Object> hSet(@RequestBody HashRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.ORDER, request.bizId());
        redisOps.hSet(key, request.field(), request.value());
        return Map.of("key", key, "value", redisOps.hGetAll(key, String.class));
    }

    /**
     * 获取哈希全部字段。
     * curl "http://localhost:8080/api/demo/redis/hash?bizId=order123"
     */
    @GetMapping("/hash")
    public Map<String, Object> hGetAll(@RequestParam String bizId) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.ORDER, bizId);
        return Map.of("key", key, "value", redisOps.hGetAll(key, String.class));
    }

    /**
     * 列表入队，可选 left/right。
     * curl -X POST http://localhost:8080/api/demo/redis/list/push -H "Content-Type: application/json" -d '{"bizId":"queue","direction":"left","value":"job1"}'
     */
    @PostMapping("/list/push")
    public Map<String, Object> pushList(@RequestBody ListPushRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.LOCK, request.bizId());
        if ("left".equalsIgnoreCase(request.direction())) {
            redisOps.lPush(key, request.value());
        } else {
            redisOps.rPush(key, request.value());
        }
        return Map.of("key", key, "value", redisOps.lRange(key, 0, -1, Object.class));
    }

    /**
     * 列表弹出，side=left/right。
     * curl "http://localhost:8080/api/demo/redis/list/pop?bizId=queue&side=left"
     */
    @GetMapping("/list/pop")
    public Map<String, Object> popList(@RequestParam String bizId, @RequestParam(defaultValue = "left") String side) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.LOCK, bizId);
        Object value = "right".equalsIgnoreCase(side)
                ? redisOps.rPop(key, Object.class)
                : redisOps.lPop(key, Object.class);
        return Map.of("key", key, "popped", value, "remain", redisOps.lRange(key, 0, -1, Object.class));
    }

    /**
     * 向集合添加元素。
     * curl -X POST http://localhost:8080/api/demo/redis/set -H "Content-Type: application/json" -d '{"bizId":"feature","values":["A","B","C"]}'
     */
    @PostMapping("/set")
    public Map<String, Object> addSet(@RequestBody SetRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.IDEMPOTENT, request.bizId());
        redisOps.sAdd(key, request.values().toArray());
        return Map.of("key", key, "members", redisOps.sMembers(key, Object.class));
    }

    /**
     * 检查集合成员。
     * curl "http://localhost:8080/api/demo/redis/set/check?bizId=feature&value=A"
     */
    @GetMapping("/set/check")
    public Map<String, Object> checkSet(@RequestParam String bizId, @RequestParam String value) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.IDEMPOTENT, bizId);
        return Map.of("key", key, "isMember", redisOps.sIsMember(key, value));
    }

    /**
     * 向有序集合写入元素。
     * curl -X POST http://localhost:8080/api/demo/redis/zset -H "Content-Type: application/json" -d '{"bizId":"rank","value":"userA","score":99.5}'
     */
    @PostMapping("/zset")
    public Map<String, Object> addZSet(@RequestBody ZSetRequest request) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.RATE_LIMIT, request.bizId());
        redisOps.zAdd(key, request.value(), request.score());
        return Map.of("key", key, "members", redisOps.zRange(key, 0, -1, Object.class));
    }

    /**
     * 读取有序集合。
     * curl "http://localhost:8080/api/demo/redis/zset?bizId=rank"
     */
    @GetMapping("/zset")
    public Map<String, Object> getZSet(@RequestParam String bizId) {
        String key = redisKeyBuilder.build(RedisKeyNamespace.RATE_LIMIT, bizId);
        return Map.of("key", key, "members", redisOps.zRange(key, 0, -1, Object.class));
    }

    private Duration toDuration(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds <= 0) {
            return null;
        }
        return Duration.ofSeconds(ttlSeconds);
    }

    private record SetStringRequest(String bizId, String value, Long ttlSeconds) {
    }

    private record SetObjectRequest(String bizId, String name, Integer age, Long ttlSeconds) {
    }

    private record DemoUser(String name, Integer age) {
    }

    private record HashRequest(String bizId, String field, String value) {
    }

    private record ListPushRequest(String bizId, String direction, Object value) {
    }

    private record SetRequest(String bizId, List<Object> values) {
    }

    private record ZSetRequest(String bizId, Object value, double score) {
    }
}
