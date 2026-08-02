package com.aionn.arch;

import com.aionn.sharedkernel.testing.arch.SharedKernelArchRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class SharedKernelArchitectureTest {

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages("com.aionn");

    @Test
    void noModuleRedefinesMoney() {
        SharedKernelArchRules.NO_DUPLICATE_MONEY.check(IMPORTED_CLASSES);
    }

    @Test
    void noModuleRedefinesPhoneNumber() {
        SharedKernelArchRules.NO_DUPLICATE_PHONE_NUMBER.check(IMPORTED_CLASSES);
    }

    @Test
    void noModuleRedefinesUlid() {
        SharedKernelArchRules.NO_DUPLICATE_ULID.check(IMPORTED_CLASSES);
    }

    @Test
    void noClassUsesLocalDateTime() {
        SharedKernelArchRules.NO_LOCAL_DATE_TIME.check(IMPORTED_CLASSES);
    }

    @Test
    void noDomainClassCallsBareInstantNow() {
        SharedKernelArchRules.NO_BARE_INSTANT_NOW_IN_DOMAIN.check(IMPORTED_CLASSES);
    }

    @Test
    void productionClassesUseInjectedClockForCurrentTime() {
        SharedKernelArchRules.NO_DIRECT_SYSTEM_TIME_IN_PRODUCTION.check(IMPORTED_CLASSES);
    }
}
