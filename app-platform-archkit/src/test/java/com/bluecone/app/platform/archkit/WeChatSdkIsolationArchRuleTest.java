package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/**
 * 微信 SDK 隔离架构规则测试。
 * <p>
 * Phase 3 验收：确保除 app-wechat 外，任何模块不得 import WxJava SDK。
 * </p>
 */
class WeChatSdkIsolationArchRuleTest {

    /**
     * 导入所有应用类（排除测试类）
     */
    private static final JavaClasses ALL_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.bluecone.app");

    @Test
    void shouldOnlyWeChatModuleUseWxJavaSdk() {
        WeChatSdkIsolationArchRule.ONLY_WECHAT_MODULE_CAN_USE_WXJAVA_SDK.check(ALL_CLASSES);
    }
}

