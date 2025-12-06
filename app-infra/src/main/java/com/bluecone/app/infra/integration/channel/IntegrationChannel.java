package com.bluecone.app.infra.integration.channel;

import com.bluecone.app.infra.integration.domain.IntegrationChannelType;
import com.bluecone.app.infra.integration.entity.IntegrationDeliveryEntity;
import com.bluecone.app.infra.integration.entity.IntegrationSubscriptionEntity;
import com.bluecone.app.infra.integration.model.IntegrationDeliveryResult;

/**
 * 通道统一接口，封装网络调用与适配逻辑。
 */
public interface IntegrationChannel {

    /**
     * 通道类型标识。
     */
    IntegrationChannelType type();

    /**
     * 发送投递任务。
     *
     * @param delivery      投递任务
     * @param subscription  订阅配置
     * @return 结果，绝不抛出 checked 异常
     */
    IntegrationDeliveryResult send(IntegrationDeliveryEntity delivery, IntegrationSubscriptionEntity subscription);
}
