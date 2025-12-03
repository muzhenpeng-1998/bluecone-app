package com.bluecone.app.tenant.dao.service.impl;

import com.bluecone.app.tenant.dao.entity.TenantSnapshot;
import com.bluecone.app.tenant.dao.mapper.TenantSnapshotMapper;
import com.bluecone.app.tenant.dao.service.ITenantSnapshotService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 租户聚合快照 服务实现类
 * </p>
 *
 * @author muzhenpeng
 * @since 2025-12-03
 */
@Service
public class TenantSnapshotServiceImpl extends ServiceImpl<TenantSnapshotMapper, TenantSnapshot> implements ITenantSnapshotService {

}
