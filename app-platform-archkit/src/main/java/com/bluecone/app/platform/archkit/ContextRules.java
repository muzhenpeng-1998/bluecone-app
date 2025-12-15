package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Context and data access governance rules for BlueCone platform.
 * 
 * <p>Enforces that controllers/gateways do not directly access data layer,
 * and must go through application services.</p>
 */
public class ContextRules {

    /**
     * Controllers should not directly access MyBatis mappers.
     * They must go through application services.
     */
    public static final ArchRule CONTROLLERS_NO_DIRECT_MAPPER_ACCESS =
            noClasses()
                    .that().resideInAPackage("..gateway..")
                    .or().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Mapper")
                    .because("Controllers should use application services, not directly access mappers");

    /**
     * Controllers should not directly access repositories.
     * They must go through application services.
     */
    public static final ArchRule CONTROLLERS_NO_DIRECT_REPOSITORY_ACCESS =
            noClasses()
                    .that().resideInAPackage("..gateway..")
                    .or().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .because("Controllers should use application services, not directly access repositories");

    /**
     * Gateway layer should only depend on application services, not domain directly.
     */
    public static final ArchRule GATEWAY_USES_APPLICATION_SERVICES =
            noClasses()
                    .that().resideInAPackage("..gateway..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..domain..repository..")
                    .because("Controllers should depend on application services, not domain repositories directly");

    /**
     * Check all context rules against the given classes.
     */
    public static void checkAll(JavaClasses classes) {
        CONTROLLERS_NO_DIRECT_MAPPER_ACCESS.check(classes);
        CONTROLLERS_NO_DIRECT_REPOSITORY_ACCESS.check(classes);
        GATEWAY_USES_APPLICATION_SERVICES.check(classes);
    }
}

