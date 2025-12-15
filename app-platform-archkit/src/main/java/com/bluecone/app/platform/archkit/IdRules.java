package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * ID generation governance rules for BlueCone platform.
 * 
 * <p>Enforces that business code uses platform ID services instead of
 * directly instantiating ID generators.</p>
 */
public class IdRules {

    /**
     * Business code must not directly instantiate ULID generators.
     * Use IdService instead.
     */
    public static final ArchRule NO_DIRECT_ULID_INSTANTIATION =
            noClasses()
                    .that().resideOutsideOfPackage("..id..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameContaining("UlidFactory")
                    .because("Business code should use IdService, not directly instantiate ULID generators");

    /**
     * Business code must not directly instantiate Snowflake ID generators.
     * Use IdService instead.
     */
    public static final ArchRule NO_DIRECT_SNOWFLAKE_INSTANTIATION =
            noClasses()
                    .that().resideOutsideOfPackage("..id..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameContaining("SnowflakeLongIdGenerator")
                    .because("Business code should use IdService, not directly instantiate Snowflake generators");

    /**
     * Domain entities should not have direct dependency on ID generation implementation.
     */
    public static final ArchRule DOMAIN_ENTITIES_NO_ID_IMPL_DEPENDENCY =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().haveSimpleNameEndingWith("Entity")
                    .or().haveSimpleNameEndingWith("Aggregate")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..id..core..")
                    .because("Domain entities should not depend on ID generation implementation details");

    /**
     * Check all ID rules against the given classes.
     */
    public static void checkAll(JavaClasses classes) {
        NO_DIRECT_ULID_INSTANTIATION.check(classes);
        NO_DIRECT_SNOWFLAKE_INSTANTIATION.check(classes);
        DOMAIN_ENTITIES_NO_ID_IMPL_DEPENDENCY.check(classes);
    }
}

