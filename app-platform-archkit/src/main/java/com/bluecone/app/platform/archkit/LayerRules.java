package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Layer architecture rules for BlueCone platform.
 * 
 * <p>Enforces clean separation between layers:</p>
 * <ul>
 *   <li>Domain layer should not depend on application/infra</li>
 *   <li>Application layer should not depend on infra implementation</li>
 *   <li>Business modules should only depend on infra via api/spi</li>
 * </ul>
 */
public class LayerRules {

    /**
     * Business modules must not directly depend on infra implementation classes.
     * They should only use infra APIs/SPIs.
     */
    public static final ArchRule BUSINESS_SHOULD_NOT_DEPEND_ON_INFRA_IMPL =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .or().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infra..impl..")
                    .because("Business logic should depend on abstractions (api/spi), not infra implementations");

    /**
     * Domain layer should be pure and not depend on application/infra layers.
     */
    public static final ArchRule DOMAIN_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infra..", "..gateway..")
                    .because("Domain layer should be independent and contain pure business logic");

    /**
     * Application services should not directly access infrastructure repositories.
     * Use domain repository interfaces instead.
     */
    public static final ArchRule APPLICATION_SHOULD_USE_DOMAIN_REPOSITORIES =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .and().haveSimpleNameEndingWith("ApplicationService")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infra..repository..")
                    .because("Application services should depend on domain repository interfaces, not infra implementations");

    /**
     * Layered architecture validation.
     */
    public static final ArchRule LAYERED_ARCHITECTURE =
            Architectures.layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Gateway").definedBy("..gateway..")
                    .layer("Application").definedBy("..application..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Infra").definedBy("..infra..")
                    .whereLayer("Gateway").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Gateway")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Gateway", "Infra")
                    .because("Layers should follow clean architecture principles");

    /**
     * Check all layer rules against the given classes.
     */
    public static void checkAll(JavaClasses classes) {
        BUSINESS_SHOULD_NOT_DEPEND_ON_INFRA_IMPL.check(classes);
        DOMAIN_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS.check(classes);
        APPLICATION_SHOULD_USE_DOMAIN_REPOSITORIES.check(classes);
        // Note: LAYERED_ARCHITECTURE may be too strict for some modules, apply selectively
    }
}

