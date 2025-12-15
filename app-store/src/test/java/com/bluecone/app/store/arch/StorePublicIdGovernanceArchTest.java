package com.bluecone.app.store.arch;

import com.bluecone.app.platform.archkit.AbstractArchTestTemplate;
import com.bluecone.app.platform.archkit.PublicIdGovernanceRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/**
 * Store 模块 Public ID Governance 架构测试。
 * 
 * <p>验证 Store 模块的 DTO/View 类是否遵守 Public ID 治理规则。</p>
 */
public class StorePublicIdGovernanceArchTest extends AbstractArchTestTemplate {

    private static final JavaClasses STORE_CLASSES =
            new StorePublicIdGovernanceArchTest().importClasses("com.bluecone.app.store");

    /**
     * 测试 Store 模块的 DTO/View 字段不应暴露 Long 类型的 id。
     */
    @Test
    void storeDtoFieldsShouldNotExposeLongId() {
        PublicIdGovernanceRules.checkDtoFields(STORE_CLASSES);
    }
}

