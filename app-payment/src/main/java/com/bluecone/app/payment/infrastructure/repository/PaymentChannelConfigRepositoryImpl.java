package com.bluecone.app.payment.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfigRepository;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.channel.WeChatChannelSecrets;
import com.bluecone.app.payment.infrastructure.persistence.PaymentChannelConfigDO;
import com.bluecone.app.payment.infrastructure.persistence.PaymentChannelConfigMapper;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 支付渠道配置仓储实现（使用 MyBatis-Plus）。
 */
@Repository
public class PaymentChannelConfigRepositoryImpl implements PaymentChannelConfigRepository {

    private final PaymentChannelConfigMapper mapper;

    public PaymentChannelConfigRepositoryImpl(PaymentChannelConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<PaymentChannelConfig> findByTenantStoreAndChannel(Long tenantId, Long storeId, PaymentChannelType channelType) {
        LambdaQueryWrapper<PaymentChannelConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentChannelConfigDO::getTenantId, tenantId)
                .eq(PaymentChannelConfigDO::getStoreId, storeId)
                .eq(PaymentChannelConfigDO::getChannelType, channelType == null ? null : channelType.getCode())
                .eq(PaymentChannelConfigDO::getStatus, 1)
                .last("limit 1");
        PaymentChannelConfigDO doObj = mapper.selectOne(wrapper);
        return Optional.ofNullable(toDomain(doObj));
    }

    @Override
    public void save(PaymentChannelConfig config) {
        mapper.insert(toDO(config));
    }

    @Override
    public void update(PaymentChannelConfig config) {
        mapper.updateById(toDO(config));
    }

    private PaymentChannelConfig toDomain(PaymentChannelConfigDO doObj) {
        if (doObj == null) {
            return null;
        }
        PaymentChannelType channelType = doObj.getChannelType() == null ? null : PaymentChannelType.fromCode(doObj.getChannelType());
        WeChatChannelSecrets secrets = null;
        if (channelType == PaymentChannelType.WECHAT_JSAPI || channelType == PaymentChannelType.WECHAT_NATIVE) {
            secrets = WeChatChannelSecrets.builder()
                    .mchId(doObj.getMchId())
                    .appId(doObj.getAppId())
                    .subMchId(doObj.getSubMchId())
                    // encryptPayload 可存储敏感配置的密文，后续引入解密服务再拆分
                    .encApiV3Key(doObj.getEncryptPayload())
                    .encSerialNo(null)
                    .build();
        }
        return PaymentChannelConfig.builder()
                .id(doObj.getId())
                .tenantId(doObj.getTenantId())
                .storeId(doObj.getStoreId())
                .channelType(channelType)
                .enabled(doObj.getStatus() != null && doObj.getStatus() == 1)
                .notifyUrl(null)
                .weChatSecrets(secrets)
                .extJson(null)
                .createdAt(doObj.getCreatedAt())
                .updatedAt(doObj.getUpdatedAt())
                .build();
    }

    private PaymentChannelConfigDO toDO(PaymentChannelConfig config) {
        PaymentChannelConfigDO doObj = new PaymentChannelConfigDO();
        doObj.setId(config.getId());
        doObj.setTenantId(config.getTenantId());
        doObj.setStoreId(config.getStoreId());
        doObj.setChannelType(config.getChannelType() == null ? null : config.getChannelType().getCode());
        doObj.setStatus(config.isEnabled() ? 1 : 0);
        if (config.getWeChatSecrets() != null) {
            doObj.setMchId(config.getWeChatSecrets().getMchId());
            doObj.setAppId(config.getWeChatSecrets().getAppId());
            doObj.setSubMchId(config.getWeChatSecrets().getSubMchId());
            doObj.setEncryptPayload(config.getWeChatSecrets().getEncApiV3Key());
        }
        doObj.setCreatedAt(config.getCreatedAt());
        doObj.setUpdatedAt(config.getUpdatedAt());
        return doObj;
    }
}
