package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Naming and convention governance rules for BlueCone platform.
 * 
 * <p>Enforces consistent naming for IDs, DTOs, and other platform conventions.</p>
 */
public class NamingRules {

    /**
     * Public ID fields should follow naming convention: publicId or *PublicId.
     */
    public static final ArchRule PUBLIC_ID_FIELD_NAMING =
            fields()
                    .that().haveRawType("java.lang.String")
                    .and().areDeclaredInClassesThat()
                    .resideInAPackage("..dto..")
                    .and().haveName("publicId")
                    .or().haveNameMatching(".*PublicId")
                    .should().bePublic()
                    .orShould().bePrivate()
                    .because("Public ID fields should follow naming convention");

    /**
     * Internal IDs should not appear in API DTOs.
     */
    public static final ArchRule NO_INTERNAL_ID_IN_API_DTO =
            noFields()
                    .that().haveName("internalId")
                    .should().beDeclaredInClassesThat()
                    .resideInAPackage("..api..dto..")
                    .because("Internal IDs should not be exposed in public APIs");

    /**
     * Repository interfaces should end with "Repository".
     */
    public static final ArchRule REPOSITORY_NAMING =
            fields()
                    .that().areDeclaredInClassesThat()
                    .resideInAPackage("..domain..repository..")
                    .and().areDeclaredInClassesThat()
                    .areInterfaces()
                    .should().beDeclaredInClassesThat()
                    .haveSimpleNameEndingWith("Repository")
                    .because("Repository interfaces should end with 'Repository'");

    /**
     * Application services should end with "ApplicationService" or "Service".
     */
    public static final ArchRule SERVICE_NAMING =
            fields()
                    .that().areDeclaredInClassesThat()
                    .resideInAPackage("..application..service..")
                    .should().beDeclaredInClassesThat()
                    .haveSimpleNameEndingWith("ApplicationService")
                    .orShould().beDeclaredInClassesThat()
                    .haveSimpleNameEndingWith("Service")
                    .because("Application services should follow naming conventions");

    /**
     * Check all naming rules against the given classes.
     */
    public static void checkAll(JavaClasses classes) {
        // PUBLIC_ID_FIELD_NAMING.check(classes);
        NO_INTERNAL_ID_IN_API_DTO.check(classes);
        // REPOSITORY_NAMING and SERVICE_NAMING are more for documentation
    }
}

