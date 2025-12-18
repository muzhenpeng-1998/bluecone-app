package com.bluecone.app.store.application.service.impl;

import com.bluecone.app.store.application.service.StoreCacheInvalidator;
import org.springframework.stereotype.Component;

/**
 * 门店缓存失效器实现（当前为空实现）。
 * <p>预留扩展点：后续可接入 L1/L2 缓存失效逻辑。</p>
 */
@Component
public class StoreCacheInvalidatorImpl implements StoreCacheInvalidator {
    // 当前为空实现，所有方法使用接口默认实现
    // 后续可在此接入多级缓存失效策略
}
