package com.agnitio.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.agnitio", importOptions = {ImportOption.DoNotIncludeTests.class})
public class CleanArchitectureLayersTest {

    @ArchTest
    static final ArchRule domain_must_not_depend_on_outer_layers =
            noClasses().that().resideInAPackage("..com.agnitio.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..com.agnitio.application..",
                            "..com.agnitio.infrastructure..",
                            "..com.agnitio.api..",
                            "..com.agnitio.worker.."
                    )
                    .because("Domain layer must be completely decoupled from outer layers");

    @ArchTest
    static final ArchRule domain_must_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..com.agnitio.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.fasterxml.jackson..",
                            "software.amazon.awssdk.."
                    )
                    .because("Domain must be pure Java and never depend on Spring, JPA, Jackson, or AWS SDK");

    @ArchTest
    static final ArchRule application_must_not_depend_on_infrastructure_or_bootstraps =
            noClasses().that().resideInAPackage("..com.agnitio.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..com.agnitio.infrastructure..",
                            "..com.agnitio.api..",
                            "..com.agnitio.worker.."
                    )
                    .because("Application layer can only depend on domain, never on infrastructure or bootstraps");

    @ArchTest
    static final ArchRule application_must_not_depend_on_frameworks =
            noClasses().that().resideInAPackage("..com.agnitio.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence.."
                    )
                    .because("Application use cases and ports must be framework-independent");

    @ArchTest
    static final ArchRule infrastructure_must_not_depend_on_bootstraps =
            noClasses().that().resideInAPackage("..com.agnitio.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "..com.agnitio.api..",
                            "..com.agnitio.worker.."
                    )
                    .because("Infrastructure layer must not depend on application bootstraps (photo-api or image-worker)");
}
