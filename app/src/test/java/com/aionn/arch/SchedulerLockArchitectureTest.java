package com.aionn.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

class SchedulerLockArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(
                    "com.aionn.identity.infrastructure.scheduling",
                    "com.aionn.inventory.infrastructure.scheduling",
                    "com.aionn.notification.infrastructure.scheduling",
                    "com.aionn.ordering.infrastructure.scheduling",
                    "com.aionn.payment.infrastructure.scheduling",
                    "com.aionn.promotion.infrastructure.scheduling",
                    "com.aionn.shipping.infrastructure.scheduling");

    @Test
    void everySingletonBusinessSchedulerHasADistributedLock() {
        methods()
                .that().areDeclaredInClassesThat().resideInAPackage("com.aionn..infrastructure.scheduling..")
                .and().areAnnotatedWith(Scheduled.class)
                .should().beAnnotatedWith(SchedulerLock.class)
                .check(PRODUCTION_CLASSES);
    }
}
