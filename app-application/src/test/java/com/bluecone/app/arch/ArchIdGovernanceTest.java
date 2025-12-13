package com.bluecone.app.arch;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.haveFullyQualifiedName;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.bluecone.app.id.core.Ulid128;
import com.bluecone.app.id.governance.AllowIdInfraAccess;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ID 使用方式治理的 ArchUnit 架构测试。
 *
 * <p>目标：强制所有非 app-id 模块通过 IdService / PublicIdCodec / TypedId 等门面使用 ID 能力，
 * 禁止直接依赖 ULID 三方库或 app-id 内部实现。</p>
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
                    .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
                    .should().notDependOnClassesThat().resideInAnyPackage("de.huxhorn.sulky.ulid..")
                    .because("ULID 三方库只能由 app-id 模块封装使用，其他模块必须通过 IdService 获取 ID，"
                            + "请注入 com.bluecone.app.id.api.IdService 或 PublicIdCodec 代替。");

    /**
     * 规则 2：除 app-id 模块与少数豁免类之外，禁止依赖 app-id 内部实现包（core/metrics/publicid.core/autoconfigure/mybatis）。
     *
     * <p>注意：Ulid128 作为内部 ID 类型允许被持久化/基础设施层使用，单独在规则 3 中约束 API 层。</p>
     */
    @ArchTest
    static final ArchRule ONLY_ID_FACADE_SHOULD_BE_USED_OUTSIDE_ID_MODULE;

    static {
        DescribedPredicate<JavaClass> internalImplPackages =
                resideInAnyPackage(
                        "com.bluecone.app.id.core..",
                        "com.bluecone.app.id.metrics..",
                        "com.bluecone.app.id.publicid.core..",
                        "com.bluecone.app.id.autoconfigure..",
                        "com.bluecone.app.id.mybatis.."
                ).and(not(haveFullyQualifiedName(Ulid128.class.getName())));

        ONLY_ID_FACADE_SHOULD_BE_USED_OUTSIDE_ID_MODULE =
                noClasses()
                        .that().resideOutsideOfPackage("com.bluecone.app.id..")
                        .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
                        .should().notDependOnClassesThat(internalImplPackages)
                        .because("非 app-id 模块只能依赖 com.bluecone.app.id.api.IdService、"
                                + "com.bluecone.app.id.publicid.api.PublicIdCodec 与 com.bluecone.app.id.typed.api.*，"
                                + "请不要直接依赖 app-id 的 core/metrics/publicid.core/autoconfigure/mybatis 等内部实现类。");
    }

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
                    .and().areNotAnnotatedWith(AllowIdInfraAccess.class)
                    .should().notDependOnClassesThat().haveFullyQualifiedName("com.bluecone.app.id.core.Ulid128")
                    .because("API/Controller 层对外不应暴露 internal_id(Ulid128)，"
                            + "请只返回 String 类型的 public_id（通过 PublicIdCodec 生成）或 TypedId（会被 Jackson 序列化为 public_id 字符串）。");
}
