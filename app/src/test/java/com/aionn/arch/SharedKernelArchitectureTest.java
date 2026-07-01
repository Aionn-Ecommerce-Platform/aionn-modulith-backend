package com.aionn.arch;

import com.aionn.sharedkernel.testing.arch.SharedKernelArchRules;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class SharedKernelArchitectureTest {

    private static final JavaClasses IMPORTED_CLASSES = new ClassFileImporter().importPackages("com.aionn");

    @Test
    void noModuleRedefinesSharedKernelValueObjects() {
        SharedKernelArchRules.checkAll(IMPORTED_CLASSES);
    }

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
    void noModuleRedefinesAnySharedValueObject() {
        SharedKernelArchRules.NO_DUPLICATE_SHARED_VALUE_OBJECTS.check(IMPORTED_CLASSES);
    }
}
