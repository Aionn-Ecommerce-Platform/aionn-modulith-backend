package com.aionn.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

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

    @Test
    void controllersDoNotExposeApplicationResultTypes() {
        methods()
                .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
                .and().haveModifier(JavaModifier.PUBLIC)
                .should(new ArchCondition<>("return REST response DTOs instead of application result types") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        String returnType = method.reflect().getGenericReturnType().getTypeName();
                        boolean exposesResult = returnType.contains(".application.dto.")
                                && (returnType.contains("Result") || returnType.contains("PageResult"));
                        events.add(new SimpleConditionEvent(method, !exposesResult,
                                method.getFullName() + " returns " + returnType));
                    }
                })
                .check(PRODUCTION_CLASSES);
    }
}
