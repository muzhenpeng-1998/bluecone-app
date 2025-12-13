package com.bluecone.app.infra.idresolve;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.core.idresolve.spi.PublicIdMapRepository;
import com.bluecone.app.id.core.Ulid128;
import org.springframework.stereotype.Repository;

/**
 * 基于 MyBatis-Plus + MySQL 的 bc_public_id_map 仓储实现。
 */
@Repository
public class MysqlPublicIdMapRepository implements PublicIdMapRepository {

    private final PublicIdMapMapper mapper;
    private final Clock clock;

    public MysqlPublicIdMapRepository(PublicIdMapMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.clock = Clock.systemUTC();
    }

    @Override
    public Optional<Ulid128> findInternalId(long tenantId, String resourceType, String publicId) {
        LambdaQueryWrapper<PublicIdMapDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublicIdMapDO::getTenantId, tenantId)
                .eq(PublicIdMapDO::getResourceType, resourceType)
                .eq(PublicIdMapDO::getPublicId, publicId)
                .eq(PublicIdMapDO::getStatus, 1)
                .select(PublicIdMapDO::getInternalId);
        PublicIdMapDO record = mapper.selectOne(wrapper);
        return Optional.ofNullable(record).map(PublicIdMapDO::getInternalId);
    }

    @Override
    public Map<String, Ulid128> findInternalIds(long tenantId, String resourceType, List<String> publicIds) {
        if (publicIds == null || publicIds.isEmpty()) {
            return Map.of();
        }
        LambdaQueryWrapper<PublicIdMapDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PublicIdMapDO::getTenantId, tenantId)
                .eq(PublicIdMapDO::getResourceType, resourceType)
                .in(PublicIdMapDO::getPublicId, publicIds)
                .eq(PublicIdMapDO::getStatus, 1)
                .select(PublicIdMapDO::getPublicId, PublicIdMapDO::getInternalId);
        List<PublicIdMapDO> records = mapper.selectList(wrapper);
        if (records == null || records.isEmpty()) {
            return Map.of();
        }
        Map<String, Ulid128> result = new HashMap<>(records.size());
        for (PublicIdMapDO record : records) {
            if (record.getPublicId() != null && record.getInternalId() != null) {
                result.put(record.getPublicId(), record.getInternalId());
            }
        }
        return result;
    }

    @Override
    public void insertMapping(long tenantId, String resourceType, String publicId, Ulid128 internalId) {
        Objects.requireNonNull(resourceType, "resourceType");
        Objects.requireNonNull(publicId, "publicId");
        Objects.requireNonNull(internalId, "internalId");
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);

        PublicIdMapDO record = new PublicIdMapDO();
        record.setTenantId(tenantId);
        record.setResourceType(resourceType);
        record.setPublicId(publicId);
        record.setInternalId(internalId);
        record.setStatus(1);
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        mapper.insert(record);
    }
}
