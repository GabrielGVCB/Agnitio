package com.agnitio.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.agnitio", importOptions = {ImportOption.DoNotIncludeTests.class})
public class NamingConventionsTest {

    @ArchTest
    static final ArchRule use_cases_should_be_suffixed_with_use_case =
            classes().that().resideInAPackage("..com.agnitio.application.usecase..")
                    .should().haveSimpleNameEndingWith("UseCase")
                    .because("Use case classes should follow the '...UseCase' naming convention");

    @ArchTest
    static final ArchRule ports_should_be_interfaces =
            classes().that().resideInAPackage("..com.agnitio.application.ports..")
                    .should().beInterfaces()
                    .because("Application ports are interfaces defining required boundaries");
}
