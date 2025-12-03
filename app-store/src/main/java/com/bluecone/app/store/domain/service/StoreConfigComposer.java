package com.bluecone.app.store.domain.service;

import com.bluecone.app.store.domain.model.StoreConfig;

/**
 * 门店配置合成器（可选），用于将默认配置、租户级配置、门店级配置进行叠加。
 * <p>当前项目尚未实现多层配置，暂留扩展点，未来可在此处理“配置继承 + 覆盖”的逻辑。</p>
 */
public interface StoreConfigComposer {

    /**
     * 合成最终可用的门店配置，后续可按租户/品牌/门店三层叠加。
     *
     * @param baseConfig 门店基础配置
     * @return 合成后的配置（目前返回原样）
     */
    StoreConfig compose(StoreConfig baseConfig);
}
