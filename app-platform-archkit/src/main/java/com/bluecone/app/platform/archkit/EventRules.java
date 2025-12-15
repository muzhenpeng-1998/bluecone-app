package com.bluecone.app.platform.archkit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Event publishing governance rules for BlueCone platform.
 * 
 * <p>Enforces that business code uses event publisher/outbox pattern,
 * not directly manipulating outbox tables.</p>
 */
public class EventRules {

    /**
     * Business code must not directly access outbox mapper/repository.
     * Use DomainEventPublisher instead.
     */
    public static final ArchRule NO_DIRECT_OUTBOX_ACCESS =
            noClasses()
                    .that().resideOutsideOfPackage("..outbox..")
                    .and().resideOutsideOfPackage("..infra..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameContaining("OutboxMapper")
                    .orShould().dependOnClassesThat()
                    .haveSimpleNameContaining("OutboxRepository")
                    .because("Business code should use DomainEventPublisher, not directly access outbox tables");

    /**
     * Domain events should be published through DomainEventPublisher.
     */
    public static final ArchRule USE_EVENT_PUBLISHER =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .haveSimpleNameContaining("OutboxMapper")
                    .because("Events should be published through DomainEventPublisher, not by directly accessing OutboxMapper");

    /**
     * Event consumers should be idempotent and use EventConsumeRecord.
     */
    public static final ArchRule EVENT_CONSUMERS_SHOULD_BE_IDEMPOTENT =
            noClasses()
                    .that().haveSimpleNameEndingWith("EventConsumer")
                    .or().haveSimpleNameEndingWith("EventHandler")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..mapper..")
                    .because("Event consumers should use idempotency service, not directly access mappers");

    /**
     * Check all event rules against the given classes.
     */
    public static void checkAll(JavaClasses classes) {
        NO_DIRECT_OUTBOX_ACCESS.check(classes);
        USE_EVENT_PUBLISHER.check(classes);
        EVENT_CONSUMERS_SHOULD_BE_IDEMPOTENT.check(classes);
    }
}

