package com.bluecone.app.store.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * app-store 模块的 ID 隔离架构测试。
 *
 * <p>确保 app-store 模块只依赖 app-id-api，不访问 internal 包。
 * 
 * <p>例外：MyBatis TypeHandler（如 Ulid128BinaryTypeHandler）允许在数据访问层使用。
 */
@AnalyzeClasses(
        packages = "com.bluecone.app.store",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ArchIdIsolationTest {

    /**
     * 禁止访问 app-id 的 internal 包。
     */
    @ArchTest
    static final ArchRule NO_INTERNAL_PACKAGE_ACCESS =
            noClasses()
                    .that().resideInPackage("com.bluecone.app.store..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.bluecone.app.id.internal..")
                    .because("app-store 模块不能访问 app-id 的 internal 包，只能使用 app-id-api 中的接口（包括 com.bluecone.app.id.mybatis 等公开的 SPI）。");
}
