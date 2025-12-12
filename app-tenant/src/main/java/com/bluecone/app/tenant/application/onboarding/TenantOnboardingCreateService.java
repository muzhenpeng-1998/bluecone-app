package com.bluecone.app.tenant.application.onboarding;

import com.bluecone.app.core.domain.IndustryType;
import com.bluecone.app.store.dao.entity.BcStore;
import com.bluecone.app.store.dao.mapper.BcStoreMapper;
import com.bluecone.app.tenant.dao.entity.Tenant;
import com.bluecone.app.tenant.dao.mapper.TenantMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 入驻引导 H5 租户/门店草稿创建服务。
 * <p>仅用于入驻引导流程中创建本地租户草稿和门店草稿，不处理微信开放平台等外部调用。</p>
 */
@Service
@RequiredArgsConstructor
public class TenantOnboardingCreateService {

    private final TenantMapper tenantMapper;
    private final BcStoreMapper bcStoreMapper;

    /**
     * 为入驻引导流程创建一条租户草稿记录。
     * <p>只初始化租户基础信息和入驻状态，不包含套餐、认证等后续内容。</p>
     *
     * @param command 创建租户草稿命令，仅用于入驻引导
     * @return 新建租户的主键 ID
     */
    @Transactional
    public Long createTenantDraftForOnboarding(CreateTenantDraftCommand command) {
        Tenant tenant = new Tenant();
        // 生成全局唯一的租户编码（时间戳 + 随机数）
        tenant.setTenantCode(generateTenantCode());
        // 品牌展示名称
        tenant.setTenantName(command.tenantName());
        // 保持原有租户启用语义：状态置为启用（1）
        tenant.setStatus(1);
        // 入驻状态：0-草稿
        tenant.setOnboardStatus(0);
        // 渠道来源
        tenant.setSourceChannel(command.sourceChannel());
        // 联系电话（入驻联系人）
        tenant.setContactPhone(command.contactPhone());

        tenantMapper.insert(tenant);
        return tenant.getId();
    }

    /**
     * 为入驻引导流程创建一条首店草稿记录。
     * <p>只初始化门店基础信息和入驻状态，不负责完整配置及营业时间等。</p>
     *
     * @param command 创建门店草稿命令，仅用于入驻引导
     * @return 新建门店的主键 ID
     */
    @Transactional
    public Long createStoreDraftForOnboarding(CreateStoreDraftCommand command) {
        BcStore store = new BcStore();
        store.setTenantId(command.tenantId());
        // 生成门店编码，复用现有规则：前缀 S + 时间戳
        store.setStoreCode(generateStoreCode());
        store.setName(command.storeName());
        store.setShortName(command.storeName());
        store.setCityCode(command.city());
        store.setDistrictCode(command.district());
        store.setAddress(command.address());
        // 业态/经营场景映射到行业类型字段
        store.setIndustryType(IndustryType.fromCode(command.bizScene()));
        store.setContactPhone(command.contactPhone());
        // 保持原有门店状态默认值：OPEN
        store.setStatus("OPEN");
        // 入驻状态：0-草稿
        store.setOnboardStatus(0);
        // 草稿阶段默认不开放接单
        store.setOpenForOrders(false);
        // 初始化配置版本
        store.setConfigVersion(1L);
        // 逻辑删除标记默认 false
        store.setIsDeleted(false);

        bcStoreMapper.insert(store);
        return store.getId();
    }

    private String generateTenantCode() {
        // 使用当前时间（精确到秒）作为前缀，便于排序和排查问题
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        // 生成 4 位随机数，降低碰撞概率
        int random = 1000 + new Random().nextInt(9000);
        // 拼接成形如 TEN202512031230309999 的编码
        return "TEN" + LocalDateTime.now().format(formatter) + random;
    }

    private String generateStoreCode() {
        // 与现有 StoreCommandService 保持一致：前缀 S + 时间戳
        return "S" + System.currentTimeMillis();
    }
}
