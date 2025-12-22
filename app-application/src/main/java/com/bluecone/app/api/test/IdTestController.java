package com.bluecone.app.api.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;

/**
 * ID 模块测试 Controller
 * 
 * <p>用于验证 ID 模块重构后的运行时行为。
 * 
 * <p><b>注意：</b>此 Controller 仅用于开发/测试环境，生产环境应禁用。
 */
@Tag(name = "🛠️ 开发调试 > ID 相关调试", description = "ID模块测试接口")
@RestController
@RequestMapping("/test/id")
public class IdTestController {

    private final IdService idService;

    public IdTestController(IdService idService) {
        this.idService = idService;
    }

    /**
     * 测试 ULID 生成
     * 
     * GET /test/id/ulid?count=10
     */
    @GetMapping("/ulid")
    public Map<String, Object> testUlid(@RequestParam(name = "count", defaultValue = "10") int count) {
        Map<String, Object> result = new HashMap<>();
        Set<String> ulids = new HashSet<>();
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            String ulid = idService.nextUlidString();
            ulids.add(ulid);
        }
        long duration = System.currentTimeMillis() - startTime;
        
        result.put("count", count);
        result.put("unique", ulids.size());
        result.put("duplicates", count - ulids.size());
        result.put("duration_ms", duration);
        result.put("samples", ulids.stream().limit(5).toArray());
        result.put("success", ulids.size() == count);
        
        return result;
    }

    /**
     * 测试 long ID 生成（Snowflake）
     * 
     * GET /test/id/long?scope=ORDER&count=10
     */
    @GetMapping("/long")
    public Map<String, Object> testLongId(
            @RequestParam(name = "scope", defaultValue = "ORDER") String scope,
            @RequestParam(name = "count", defaultValue = "10") int count) {
        
        Map<String, Object> result = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        IdScope idScope = IdScope.valueOf(scope);
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            long id = idService.nextLong(idScope);
            ids.add(id);
        }
        long duration = System.currentTimeMillis() - startTime;
        
        result.put("scope", scope);
        result.put("count", count);
        result.put("unique", ids.size());
        result.put("duplicates", count - ids.size());
        result.put("duration_ms", duration);
        result.put("samples", ids.stream().limit(5).toArray());
        result.put("success", ids.size() == count);
        
        return result;
    }

    /**
     * 测试 Public ID 生成
     * 
     * GET /test/id/public?type=ORDER&count=10
     */
    @GetMapping("/public")
    public Map<String, Object> testPublicId(
            @RequestParam(name = "type", defaultValue = "ORDER") String type,
            @RequestParam(name = "count", defaultValue = "10") int count) {
        
        Map<String, Object> result = new HashMap<>();
        Set<String> publicIds = new HashSet<>();
        ResourceType resourceType = ResourceType.valueOf(type);
        
        try {
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < count; i++) {
                String publicId = idService.nextPublicId(resourceType);
                publicIds.add(publicId);
            }
            long duration = System.currentTimeMillis() - startTime;
            
            result.put("type", type);
            result.put("count", count);
            result.put("unique", publicIds.size());
            result.put("duplicates", count - publicIds.size());
            result.put("duration_ms", duration);
            result.put("samples", publicIds.stream().limit(5).toArray());
            result.put("success", true);
        } catch (UnsupportedOperationException e) {
            result.put("error", "PublicId generation not supported: " + e.getMessage());
            result.put("success", false);
        }
        
        return result;
    }

    /**
     * 并发测试 long ID 生成
     * 
     * GET /test/id/concurrent?threads=10&idsPerThread=1000
     */
    @GetMapping("/concurrent")
    public Map<String, Object> testConcurrent(
            @RequestParam(name = "threads", defaultValue = "10") int threads,
            @RequestParam(name = "idsPerThread", defaultValue = "1000") int idsPerThread) throws InterruptedException {
        
        Map<String, Object> result = new HashMap<>();
        Set<Long> allIds = ConcurrentHashMap.newKeySet();
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int t = 0; t < threads; t++) {
            new Thread(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        long id = idService.nextLong(IdScope.ORDER);
                        allIds.add(id);
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        long duration = System.currentTimeMillis() - startTime;
        
        int expectedTotal = threads * idsPerThread;
        int actualUnique = allIds.size();
        int duplicates = expectedTotal - actualUnique;
        
        result.put("threads", threads);
        result.put("ids_per_thread", idsPerThread);
        result.put("expected_total", expectedTotal);
        result.put("actual_unique", actualUnique);
        result.put("duplicates", duplicates);
        result.put("errors", errorCount.get());
        result.put("duration_ms", duration);
        result.put("throughput_per_sec", (expectedTotal * 1000L) / duration);
        result.put("success", duplicates == 0 && errorCount.get() == 0);
        
        return result;
    }

    /**
     * 测试所有 ID 类型
     * 
     * GET /test/id/all
     */
    @GetMapping("/all")
    public Map<String, Object> testAll() {
        Map<String, Object> result = new HashMap<>();
        
        // 测试 ULID
        try {
            String ulid = idService.nextUlidString();
            result.put("ulid", ulid);
            result.put("ulid_length", ulid.length());
            result.put("ulid_success", true);
        } catch (Exception e) {
            result.put("ulid_error", e.getMessage());
            result.put("ulid_success", false);
        }
        
        // 测试 long ID
        try {
            long longId = idService.nextLong(IdScope.ORDER);
            result.put("long_id", longId);
            result.put("long_id_positive", longId > 0);
            result.put("long_id_success", true);
        } catch (Exception e) {
            result.put("long_id_error", e.getMessage());
            result.put("long_id_success", false);
        }
        
        // 测试 Public ID
        try {
            String publicId = idService.nextPublicId(ResourceType.ORDER);
            result.put("public_id", publicId);
            result.put("public_id_has_prefix", publicId.startsWith("ord_"));
            result.put("public_id_success", true);
        } catch (UnsupportedOperationException e) {
            result.put("public_id_note", "PublicId generation not enabled (expected in zero-config mode)");
            result.put("public_id_success", true); // 这是预期的
        } catch (Exception e) {
            result.put("public_id_error", e.getMessage());
            result.put("public_id_success", false);
        }
        
        result.put("overall_success", 
            (Boolean) result.getOrDefault("ulid_success", false) && 
            (Boolean) result.getOrDefault("long_id_success", false));
        
        return result;
    }

    /**
     * 健康检查
     * 
     * GET /test/id/health
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 快速测试
            String ulid = idService.nextUlidString();
            long longId = idService.nextLong(IdScope.ORDER);
            
            result.put("status", "UP");
            result.put("ulid_available", true);
            result.put("long_id_available", true);
            result.put("sample_ulid", ulid);
            result.put("sample_long_id", longId);
        } catch (Exception e) {
            result.put("status", "DOWN");
            result.put("error", e.getMessage());
        }
        
        return result;
    }

    /**
     * 获取 ID 模块配置信息
     * 
     * GET /test/id/info
     */
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("module", "app-id");
        result.put("description", "BlueCone ID Module - Minimal Viable Version");
        result.put("features", new String[]{
            "ULID (26-char string)",
            "long ID (Snowflake by default)",
            "Public ID (optional)"
        });
        result.put("default_strategy", "SNOWFLAKE");
        result.put("zero_config", true);
        result.put("database_required", false);
        
        // 测试可用性
        boolean ulidAvailable = false;
        boolean longIdAvailable = false;
        
        try {
            idService.nextUlidString();
            ulidAvailable = true;
        } catch (Exception ignored) {}
        
        try {
            idService.nextLong(IdScope.ORDER);
            longIdAvailable = true;
        } catch (Exception ignored) {}
        
        result.put("ulid_available", ulidAvailable);
        result.put("long_id_available", longIdAvailable);
        result.put("ready", ulidAvailable && longIdAvailable);
        
        return result;
    }
}

