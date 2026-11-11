package com.agnitio.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

@AnalyzeClasses(packages = "com.agnitio", importOptions = {ImportOption.DoNotIncludeTests.class})
public class RestControllerRulesTest {

    @ArchTest
    static final ArchRule controllers_must_reside_in_api_or_infrastructure =
            classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().resideInAnyPackage("..com.agnitio.api..", "..com.agnitio.infrastructure..")
                    .because("Controllers are entrypoint adapters and must reside in api or infrastructure packages");

    @ArchTest
    static final ArchRule controllers_must_not_return_domain_entities =
            methods().that().areDeclaredInClassesThat().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .should().notHaveRawReturnType(resideInAPackage("..com.agnitio.domain.."))
                    .because("Controllers must return API response DTOs, never domain entities directly");
}
