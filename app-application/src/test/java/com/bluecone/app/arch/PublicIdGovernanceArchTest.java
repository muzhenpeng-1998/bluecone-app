package com.bluecone.app.arch;

import com.bluecone.app.platform.archkit.AbstractArchTestTemplate;
import com.bluecone.app.platform.archkit.PublicIdGovernanceRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.Test;

/**
 * Public ID Governance 架构测试。
 * 
 * <p>验证 Controller 层和 DTO 层是否遵守 Public ID 治理规则。</p>
 * 
 * <p>测试范围：</p>
 * <ul>
 *   <li>app-application 模块的所有 Controller</li>
 *   <li>app-store/app-product 模块的 DTO/View 类</li>
 * </ul>
 */
public class PublicIdGovernanceArchTest extends AbstractArchTestTemplate {

    private static final JavaClasses APPLICATION_CLASSES =
            new PublicIdGovernanceArchTest().importClasses("com.bluecone.app");

    /**
     * 测试 Controller 参数不应暴露 Long 类型的 id。
     */
    @Test
    void controllerParamsShouldNotExposeLongId() {
        PublicIdGovernanceRules.checkControllerParams(APPLICATION_CLASSES);
    }

    /**
     * 测试 DTO/View 字段不应暴露 Long 类型的 id。
     */
    @Test
    void dtoFieldsShouldNotExposeLongId() {
        PublicIdGovernanceRules.checkDtoFields(APPLICATION_CLASSES);
    }

    /**
     * 测试所有 Public ID 治理规则。
     */
    @Test
    void shouldFollowPublicIdGovernanceRules() {
        PublicIdGovernanceRules.checkAll(APPLICATION_CLASSES);
    }
}

