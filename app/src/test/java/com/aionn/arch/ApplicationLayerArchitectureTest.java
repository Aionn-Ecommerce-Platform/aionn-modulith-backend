package com.aionn.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ApplicationLayerArchitectureTest {

    private static final JavaClasses PRODUCTION_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.aionn");

    @Test
    void applicationLayerDoesNotDependOnInfrastructureOrProviderSdks() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..",
                        "com.stripe..",
                        "com.twilio..",
                        "com.cloudinary..",
                        "org.opensearch..",
                        "org.springframework.data..",
                        "jakarta.persistence..")
                .check(PRODUCTION_CLASSES);
    }
}
