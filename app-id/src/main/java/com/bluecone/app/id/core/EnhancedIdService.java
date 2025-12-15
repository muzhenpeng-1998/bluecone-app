package com.bluecone.app.id.core;

import com.bluecone.app.id.api.IdScope;
import com.bluecone.app.id.api.IdService;
import com.bluecone.app.id.api.ResourceType;
import com.bluecone.app.id.segment.SegmentLongIdGenerator;
import de.huxhorn.sulky.ulid.ULID;

/**
 * 增强版 ID 服务实现，支持 BlueCone ID v2 三层体系。
 * 
 * <p>功能清单：
 * <ul>
 *   <li>ULID 生成：128 位全局唯一标识</li>
 *   <li>Long ID 生成：基于号段模式，高性能、无时钟依赖</li>
 *   <li>Public ID 生成：带资源类型前缀的对外 ID</li>
 *   <li>Public ID 校验：格式、类型、ULID 合法性校验</li>
 * </ul>
 */
public class EnhancedIdService implements IdService {
    
    private final UlidIdGenerator ulidIdGenerator;
    private final SegmentLongIdGenerator segmentLongIdGenerator;
    private final PublicIdFactory publicIdFactory;
    
    /**
     * 构造函数。
     * 
     * @param ulidIdGenerator ULID 生成器
     * @param segmentLongIdGenerator 号段 Long ID 生成器
     * @param publicIdFactory Public ID 工厂
     */
    public EnhancedIdService(UlidIdGenerator ulidIdGenerator,
                             SegmentLongIdGenerator segmentLongIdGenerator,
                             PublicIdFactory publicIdFactory) {
        if (ulidIdGenerator == null) {
            throw new IllegalArgumentException("ulidIdGenerator 不能为 null");
        }
        if (segmentLongIdGenerator == null) {
            throw new IllegalArgumentException("segmentLongIdGenerator 不能为 null");
        }
        if (publicIdFactory == null) {
            throw new IllegalArgumentException("publicIdFactory 不能为 null");
        }
        
        this.ulidIdGenerator = ulidIdGenerator;
        this.segmentLongIdGenerator = segmentLongIdGenerator;
        this.publicIdFactory = publicIdFactory;
    }
    
    @Override
    public Ulid128 nextUlid() {
        ULID.Value value = ulidIdGenerator.nextValue();
        return new Ulid128(value.getMostSignificantBits(), value.getLeastSignificantBits());
    }
    
    @Override
    public String nextUlidString() {
        return ulidIdGenerator.nextUlid();
    }
    
    @Override
    public byte[] nextUlidBytes() {
        return nextUlid().toBytes();
    }
    
    @Override
    public long nextLong(IdScope scope) {
        return segmentLongIdGenerator.nextId(scope);
    }
    
    @Override
    public String nextPublicId(ResourceType type) {
        Ulid128 ulid = nextUlid();
        return publicIdFactory.create(type, ulid);
    }
    
    @Override
    public void validatePublicId(ResourceType expectedType, String publicId) {
        publicIdFactory.validate(expectedType, publicId);
    }
}

