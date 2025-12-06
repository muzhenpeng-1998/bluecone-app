package com.bluecone.app.payment.infra.persistence.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfig;
import com.bluecone.app.payment.domain.channel.PaymentChannelConfigRepository;
import com.bluecone.app.payment.domain.channel.PaymentChannelType;
import com.bluecone.app.payment.domain.channel.WeChatChannelSecrets;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * 支付渠道配置仓储实现（MyBatis-Plus）。
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
                .eq(PaymentChannelConfigDO::getChannelCode, channelType == null ? null : channelType.getCode())
                .eq(PaymentChannelConfigDO::getEnabled, 1)
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
        PaymentChannelType channelType = doObj.getChannelCode() == null ? null : PaymentChannelType.fromCode(doObj.getChannelCode());
        WeChatChannelSecrets secrets = null;
        if (channelType == PaymentChannelType.WECHAT_JSAPI || channelType == PaymentChannelType.WECHAT_NATIVE) {
            secrets = WeChatChannelSecrets.builder()
                    .mchId(doObj.getMchId())
                    .appId(doObj.getAppId())
                    .subMchId(doObj.getSubMchId())
                    .encApiV3Key(doObj.getEncApiV3Key())
                    .encSerialNo(doObj.getEncSerialNo())
                    .build();
        }
        return PaymentChannelConfig.builder()
                .id(doObj.getId())
                .tenantId(doObj.getTenantId())
                .storeId(doObj.getStoreId())
                .channelType(channelType)
                .enabled(doObj.getEnabled() != null && doObj.getEnabled() == 1)
                .notifyUrl(doObj.getNotifyUrl())
                .weChatSecrets(secrets)
                .extJson(doObj.getExtJson())
                .createdAt(doObj.getCreatedAt())
                .updatedAt(doObj.getUpdatedAt())
                .build();
    }

    private PaymentChannelConfigDO toDO(PaymentChannelConfig config) {
        PaymentChannelConfigDO doObj = new PaymentChannelConfigDO();
        doObj.setId(config.getId());
        doObj.setTenantId(config.getTenantId());
        doObj.setStoreId(config.getStoreId());
        doObj.setChannelCode(config.getChannelType() == null ? null : config.getChannelType().getCode());
        doObj.setEnabled(config.isEnabled() ? 1 : 0);
        doObj.setNotifyUrl(config.getNotifyUrl());
        if (config.getWeChatSecrets() != null) {
            doObj.setMchId(config.getWeChatSecrets().getMchId());
            doObj.setAppId(config.getWeChatSecrets().getAppId());
            doObj.setSubMchId(config.getWeChatSecrets().getSubMchId());
            doObj.setEncApiV3Key(config.getWeChatSecrets().getEncApiV3Key());
            doObj.setEncSerialNo(config.getWeChatSecrets().getEncSerialNo());
        }
        doObj.setExtJson(config.getExtJson());
        doObj.setCreatedAt(config.getCreatedAt());
        doObj.setUpdatedAt(config.getUpdatedAt());
        return doObj;
    }
}
