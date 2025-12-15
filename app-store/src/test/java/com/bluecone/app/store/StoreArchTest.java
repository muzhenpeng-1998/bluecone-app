package com.bluecone.app.store;

import com.bluecone.app.platform.archkit.AbstractArchTestTemplate;
import com.bluecone.app.platform.archkit.ContextRules;
import com.bluecone.app.platform.archkit.EventRules;
import com.bluecone.app.platform.archkit.IdRules;
import com.bluecone.app.platform.archkit.LayerRules;
import com.bluecone.app.platform.archkit.NamingRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Architecture governance tests for app-store module.
 * 
 * <p>Validates that the store module follows BlueCone platform architecture rules.</p>
 */
class StoreArchTest extends AbstractArchTestTemplate {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new StoreArchTest().importClasses("com.bluecone.app.store");
    }

    @Test
    void shouldFollowLayerRules() {
        // Apply layer rules
        LayerRules.BUSINESS_SHOULD_NOT_DEPEND_ON_INFRA_IMPL.check(classes);
        LayerRules.DOMAIN_SHOULD_NOT_DEPEND_ON_OUTER_LAYERS.check(classes);
        LayerRules.APPLICATION_SHOULD_USE_DOMAIN_REPOSITORIES.check(classes);
    }

    @Test
    void shouldFollowIdRules() {
        // Apply ID generation rules
        IdRules.NO_DIRECT_ULID_INSTANTIATION.check(classes);
        IdRules.NO_DIRECT_SNOWFLAKE_INSTANTIATION.check(classes);
        IdRules.DOMAIN_ENTITIES_NO_ID_IMPL_DEPENDENCY.check(classes);
    }

    @Test
    void shouldFollowContextRules() {
        // Apply context and data access rules
        ContextRules.CONTROLLERS_NO_DIRECT_MAPPER_ACCESS.check(classes);
        ContextRules.CONTROLLERS_NO_DIRECT_REPOSITORY_ACCESS.check(classes);
        ContextRules.GATEWAY_USES_APPLICATION_SERVICES.check(classes);
    }

    @Test
    void shouldFollowEventRules() {
        // Apply event publishing rules
        EventRules.NO_DIRECT_OUTBOX_ACCESS.check(classes);
        EventRules.USE_EVENT_PUBLISHER.check(classes);
    }

    @Test
    void shouldFollowNamingRules() {
        // Apply naming conventions
        NamingRules.NO_INTERNAL_ID_IN_API_DTO.check(classes);
    }
}

