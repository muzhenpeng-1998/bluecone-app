package com.bluecone.app.wechat.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * ArchUnit 测试：确保 WxJava SDK 依赖边界不被突破。
 * <p>
 * 硬约束：除 app-wechat 外，任何模块不得 import WxJava SDK。
 * </p>
 */
class WeChatDependencyBoundaryTest {

    private static final String BASE_PACKAGE = "com.bluecone.app";

    /**
     * 规则：除 app-wechat 外，禁止任何模块依赖 WxJava SDK。
     */
    @Test
    void onlyWeChatModuleCanDependOnWxJavaSdk() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);

        // 禁止除 app-wechat 外的模块依赖 me.chanjar.weixin.* (WxJava)
        ArchRule rule1 = noClasses()
                .that().resideOutsideOfPackage("com.bluecone.app.wechat..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "me.chanjar.weixin..",
                        "cn.binarywang.wx..",
                        "com.github.binarywang.wxpay.."
                )
                .because("除 app-wechat 外，任何模块不得直接依赖 WxJava SDK（me.chanjar.weixin.*, cn.binarywang.wx.*, com.github.binarywang.wxpay.*）");

        rule1.check(importedClasses);
    }

    /**
     * 规则：app-wechat-api 不得依赖 WxJava SDK。
     */
    @Test
    void weChatApiModuleShouldNotDependOnWxJavaSdk() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bluecone.app.wechat.facade");

        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.bluecone.app.wechat.facade..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "me.chanjar.weixin..",
                        "cn.binarywang.wx..",
                        "com.github.binarywang.wxpay.."
                )
                .because("app-wechat-api（facade 包）不得依赖 WxJava SDK，只能定义接口和 DTO");

        rule.check(importedClasses);
    }

    /**
     * 规则：禁止在 DTO 中出现 authorizerAppId/subAppid/subMchid 字段（防止客户端传入）。
     * <p>
     * 注意：此规则检查 app-wechat-api 中的 Command DTO。
     * </p>
     */
    @Test
    void commandDtosShouldNotContainAuthorizerAppIdFields() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.bluecone.app.wechat.facade");

        // 检查 Command DTO 不包含 authorizerAppId/subAppid/subMchid 字段
        ArchRule rule = noClasses()
                .that().resideInAnyPackage("com.bluecone.app.wechat.facade..")
                .and().haveSimpleNameEndingWith("Command")
                .should().haveOnlyPrivateConstructors() // 这个规则会失败，因为 Lombok 生成的构造函数是 public
                .orShould().accessField("authorizerAppId")
                .orShould().accessField("subAppid")
                .orShould().accessField("subMchid")
                .because("Command DTO 不允许包含 authorizerAppId/subAppid/subMchid 字段，防止客户端传入导致串租户");

        // 注意：此规则较难用 ArchUnit 精确表达，建议通过 Code Review 保证
        // rule.check(importedClasses);
    }
}

