package com.agnitio.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

@AnalyzeClasses(packages = "com.agnitio", importOptions = {ImportOption.DoNotIncludeTests.class})
public class DomainIsolationRulesTest {

    @ArchTest
    static final ArchRule domain_classes_must_not_have_spring_or_jpa_annotations =
            classes().that().resideInAPackage("..com.agnitio.domain..")
                    .should().notBeAnnotatedWith("jakarta.persistence.Entity")
                    .andShould().notBeAnnotatedWith("jakarta.persistence.Table")
                    .andShould().notBeAnnotatedWith("org.springframework.stereotype.Component")
                    .andShould().notBeAnnotatedWith("org.springframework.stereotype.Service")
                    .andShould().notBeAnnotatedWith("org.springframework.stereotype.Repository")
                    .andShould().notBeAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                    .because("Domain entities must remain pure POJOs without any framework annotations");
}
