package com.aionn.sharedkernel.testing.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

public final class SharedKernelArchRules {

        public static final String SHARED_KERNEL_PACKAGE = "com.aionn.sharedkernel..";

        private SharedKernelArchRules() {
        }

        public static final ArchRule NO_DUPLICATE_MONEY = noClasses()
                        .that().resideOutsideOfPackage(SHARED_KERNEL_PACKAGE)
                        .should().haveSimpleName("Money")
                        .as("no class outside " + SHARED_KERNEL_PACKAGE + " should redefine the 'Money' value object")
                        .because("Money is owned by the shared-kernel; consume com.aionn.sharedkernel.domain.vo.Money "
                                        + "instead of re-declaring it (single source of truth, R9.1)")
                        .allowEmptyShould(true);

        public static final ArchRule NO_DUPLICATE_PHONE_NUMBER = noClasses()
                        .that().resideOutsideOfPackage(SHARED_KERNEL_PACKAGE)
                        .should().haveSimpleName("PhoneNumber")
                        .as("no class outside " + SHARED_KERNEL_PACKAGE
                                        + " should redefine the 'PhoneNumber' value object")
                        .because("PhoneNumber and its E.164 regex are owned by the shared-kernel; consume "
                                        + "com.aionn.sharedkernel.domain.vo.PhoneNumber instead of re-declaring the phone "
                                        + "format (single source of truth, R9.1)")
                        .allowEmptyShould(true);

        public static final ArchRule NO_DUPLICATE_ULID = noClasses()
                        .that().resideOutsideOfPackage(SHARED_KERNEL_PACKAGE)
                        .should().haveSimpleName("Ulid")
                        .orShould().haveSimpleName("ULID")
                        .as("no class outside " + SHARED_KERNEL_PACKAGE + " should redefine ULID generation/validation")
                        .because("ULID generation, validation and its Crockford Base32 regex are owned by the "
                                        + "shared-kernel; consume com.aionn.sharedkernel.util.IdGenerator / "
                                        + "com.aionn.sharedkernel.domain.id.BaseId instead of re-implementing ULID "
                                        + "(single source of truth, R9.1)")
                        .allowEmptyShould(true);

        public static final ArchRule NO_DUPLICATE_SHARED_VALUE_OBJECTS = noClasses()
                        .that().resideOutsideOfPackage(SHARED_KERNEL_PACKAGE)
                        .should().haveSimpleName("Money")
                        .orShould().haveSimpleName("PhoneNumber")
                        .orShould().haveSimpleName("Ulid")
                        .orShould().haveSimpleName("ULID")
                        .as("no class outside " + SHARED_KERNEL_PACKAGE
                                        + " should redefine a shared-kernel value object (Money, PhoneNumber, ULID)")
                        .because("Money, PhoneNumber and ULID are owned by the shared-kernel; consume the kernel "
                                        + "types instead of re-declaring them (single source of truth, R9.1, R9.3)")
                        .allowEmptyShould(true);

        public static void checkAll(JavaClasses classes) {
                NO_DUPLICATE_MONEY.check(classes);
                NO_DUPLICATE_PHONE_NUMBER.check(classes);
                NO_DUPLICATE_ULID.check(classes);
        }
}
