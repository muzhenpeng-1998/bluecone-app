package com.bluecone.app.arch;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ID 使用方式治理的 ArchUnit 架构测试。
 *
 * <p>目标：强制所有非 app-id 模块通过 IdService / PublicIdCodec / TypedId 等门面使用 ID 能力，
 * 禁止直接依赖 ULID 三方库或 app-id 内部实现。
 * 
 * <p>例外：MyBatis TypeHandler（如 Ulid128BinaryTypeHandler）允许在数据访问层使用。
 */
@AnalyzeClasses(
        packages = "com.bluecone.app",
        importOptions = {ImportOption.DoNotIncludeTests.class}
)
public class ArchIdGovernanceTest {

    /**
     * 规则 1：除 app-id 模块之外，禁止直接依赖 ULID 三方库。
     */
    @ArchTest
    static final ArchRule ULID_LIBRARY_ONLY_IN_ID_MODULE =
            noClasses()
                    .that().resideOutsideOfPackage("com.bluecone.app.id..")
                    .should().dependOnClassesThat().resideInAnyPackage("de.huxhorn.sulky.ulid..")
                    .because("ULID 三方库只能由 app-id 模块封装使用，其他模块必须通过 IdService 获取 ID，"
                            + "请注入 com.bluecone.app.id.api.IdService 或 PublicIdCodec 代替。");

    /**
     * 规则 2：除 app-id 模块之外，禁止依赖 app-id 内部实现包（internal.*）。
     *
     * <p>所有外部模块只能依赖 app-id-api 中的接口，不能访问 internal 包。
     */
    @ArchTest
    static final ArchRule NO_EXTERNAL_ACCESS_TO_INTERNAL =
            noClasses()
                    .that().resideOutsideOfPackage("com.bluecone.app.id..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.bluecone.app.id.internal..")
                    .because("非 app-id 模块不能访问 internal 包，只能依赖 app-id-api 中的接口。"
                            + "请使用 com.bluecone.app.id.api.*、com.bluecone.app.id.publicid.api.*、"
                            + "com.bluecone.app.id.typed.api.*、com.bluecone.app.id.segment.*、"
                            + "com.bluecone.app.id.mybatis.* 等 API 接口。");

    /**
     * 规则 3：API/Controller/Web 层不得依赖 Ulid128（internal_id），对外应只暴露 public_id 字符串或 TypedId。
     */
    @ArchTest
    static final ArchRule API_LAYER_MUST_NOT_DEPEND_ON_INTERNAL_ULID =
            noClasses()
                    .that().resideInAnyPackage(
                            "com.bluecone.app..api..",
                            "com.bluecone.app..controller..",
                            "com.bluecone.app..web..")
                    .should().dependOnClassesThat().haveFullyQualifiedName("com.bluecone.app.id.core.Ulid128")
                    .because("API/Controller 层对外不应暴露 internal_id(Ulid128)，"
                            + "请只返回 String 类型的 public_id（通过 PublicIdCodec 生成）或 TypedId（会被 Jackson 序列化为 public_id 字符串）。");
}
