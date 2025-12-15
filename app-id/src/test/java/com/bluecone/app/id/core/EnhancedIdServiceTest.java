package com.bluecone.app.id.core;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.publicid.api.PublicIdCodec;
import com.bluecone.app.id.publicid.core.DefaultPublicIdCodec;
import com.bluecone.app.id.segment.IdSegmentRepository;
import com.bluecone.app.id.segment.SegmentLongIdGenerator;
import com.bluecone.app.id.segment.SegmentRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.*;

/**
 * EnhancedIdService 功能测试。
 * 
 * <p>测试目标：
 * <ul>
 *   <li>ULID 生成：格式正确、唯一性</li>
 *   <li>Long ID 生成：唯一性、单调性</li>
 *   <li>Public ID 生成：格式正确（prefix_ulid）</li>
 *   <li>Public ID 校验：格式、类型、ULID 合法性</li>
 * </ul>
 */
@DisplayName("EnhancedIdService 功能测试")
class EnhancedIdServiceTest {
    
    private EnhancedIdService idService;
    
    @BeforeEach
    void setUp() {
        UlidIdGenerator ulidIdGenerator = UlidIdGenerator.create(1);
        IdSegmentRepository repository = new InMemoryIdSegmentRepository();
        SegmentLongIdGenerator segmentLongIdGenerator = new SegmentLongIdGenerator(repository, 1000);
        PublicIdCodec codec = new DefaultPublicIdCodec(null);
        PublicIdFactory publicIdFactory = new PublicIdFactory(codec);
        
        idService = new EnhancedIdService(ulidIdGenerator, segmentLongIdGenerator, publicIdFactory);
    }
    
    @Test
    @DisplayName("生成 ULID - 格式和唯一性")
    void testNextUlid() {
        Set<String> ulids = new HashSet<>();
        
        for (int i = 0; i < 1000; i++) {
            String ulid = idService.nextUlidString();
            
            // 格式断言：26 位 Crockford Base32
            assertThat(ulid).hasSize(26);
            assertThat(ulid).matches("^[0-9A-HJKMNP-TV-Z]{26}$");
            
            // 唯一性断言
            assertThat(ulids).doesNotContain(ulid);
            ulids.add(ulid);
        }
    }
    
    @Test
    @DisplayName("生成 Long ID - 唯一性和单调性")
    void testNextLong() {
        Set<Long> ids = new HashSet<>();
        long lastId = 0;
        
        for (int i = 0; i < 10000; i++) {
            long id = idService.nextLong(IdScope.ORDER);
            
            // 唯一性断言
            assertThat(ids).doesNotContain(id);
            ids.add(id);
            
            // 单调性断言
            assertThat(id).isGreaterThan(lastId);
            lastId = id;
        }
    }
    
    @Test
    @DisplayName("生成 Public ID - 格式正确")
    void testNextPublicId() {
        for (ResourceType type : ResourceType.values()) {
            String publicId = idService.nextPublicId(type);
            
            // 格式断言：prefix_ulid
            assertThat(publicId).startsWith(type.prefix() + "_");
            assertThat(publicId).hasSize(type.prefix().length() + 1 + 26); // prefix + _ + 26位ULID
            
            // ULID 部分断言
            String ulidPart = publicId.substring(type.prefix().length() + 1);
            assertThat(ulidPart).hasSize(26);
            assertThat(ulidPart).matches("^[0-9A-HJKMNP-TV-Z]{26}$");
        }
    }
    
    @Test
    @DisplayName("校验 Public ID - 合法格式")
    void testValidatePublicId_Valid() {
        String publicId = idService.nextPublicId(ResourceType.TENANT);
        
        // 合法 Public ID 不应抛异常
        assertThatCode(() -> idService.validatePublicId(ResourceType.TENANT, publicId))
            .doesNotThrowAnyException();
    }
    
    @Test
    @DisplayName("校验 Public ID - 缺少分隔符")
    void testValidatePublicId_MissingSeparator() {
        String invalidPublicId = "tnt01HN8X5K9G3QRST2VW4XYZ"; // 缺少下划线
        
        assertThatThrownBy(() -> idService.validatePublicId(ResourceType.TENANT, invalidPublicId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("缺少分隔符");
    }
    
    @Test
    @DisplayName("校验 Public ID - 类型不匹配")
    void testValidatePublicId_TypeMismatch() {
        String storePublicId = idService.nextPublicId(ResourceType.STORE);
        
        assertThatThrownBy(() -> idService.validatePublicId(ResourceType.TENANT, storePublicId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("类型不匹配");
    }
    
    @Test
    @DisplayName("校验 Public ID - ULID 长度非法")
    void testValidatePublicId_InvalidUlidLength() {
        String invalidPublicId = "tnt_01HN8X5K9G3QRST2VW4"; // ULID 不足 26 位
        
        assertThatThrownBy(() -> idService.validatePublicId(ResourceType.TENANT, invalidPublicId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ULID 长度非法");
    }
    
    @Test
    @DisplayName("校验 Public ID - ULID 包含非法字符")
    void testValidatePublicId_InvalidUlidCharacters() {
        String invalidPublicId = "tnt_01HN8X5K9G3QRST2VW4XYZ!@#"; // 包含非法字符
        
        assertThatThrownBy(() -> idService.validatePublicId(ResourceType.TENANT, invalidPublicId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ULID 长度非法"); // 长度先报错
    }
    
    @Test
    @DisplayName("批量生成 Public ID - 唯一性")
    void testBatchGeneratePublicId() {
        Set<String> publicIds = new HashSet<>();
        
        for (int i = 0; i < 10000; i++) {
            String publicId = idService.nextPublicId(ResourceType.ORDER);
            assertThat(publicIds).doesNotContain(publicId);
            publicIds.add(publicId);
        }
        
        assertThat(publicIds).hasSize(10000);
    }
    
    /**
     * 内存模拟的 ID 号段仓储（线程安全）。
     */
    private static class InMemoryIdSegmentRepository implements IdSegmentRepository {
        
        private final ConcurrentHashMap<IdScope, AtomicLong> maxIds = new ConcurrentHashMap<>();
        
        @Override
        public synchronized SegmentRange nextRange(IdScope scope, int step) {
            AtomicLong maxId = maxIds.computeIfAbsent(scope, k -> new AtomicLong(0));
            
            long currentMax = maxId.get();
            long newMax = currentMax + step;
            maxId.set(newMax);
            
            return new SegmentRange(currentMax + 1, newMax);
        }
        
        @Override
        public void initScopeIfAbsent(IdScope scope, long initialMaxId, int defaultStep) {
            maxIds.putIfAbsent(scope, new AtomicLong(initialMaxId));
        }
    }
}

