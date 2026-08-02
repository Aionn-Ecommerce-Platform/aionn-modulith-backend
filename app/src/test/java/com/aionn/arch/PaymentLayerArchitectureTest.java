package com.aionn.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class PaymentLayerArchitectureTest {

    private static final JavaClasses PAYMENT_CLASSES =
            new ClassFileImporter().importPackages("com.aionn.payment");

    @Test
    void applicationLayerDoesNotDependOnInfrastructureOrStripeSdk() {
        noClasses()
                .that().resideInAPackage("com.aionn.payment.application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "com.aionn.payment.infrastructure..", "com.stripe..")
                .check(PAYMENT_CLASSES);
    }
}
