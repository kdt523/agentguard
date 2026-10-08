package io.agentguard.app;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guard rails for the app module: web goes through services; checks implement Check. */
@AnalyzeClasses(packages = "io.agentguard", importOptions = ImportOption.DoNotIncludeTests.class)
class AppArchitectureTest {

  @ArchTest
  static final ArchRule webDoesNotTouchPersistence =
      noClasses()
          .that()
          .resideInAPackage("..web..")
          .should()
          .accessClassesThat()
          .resideInAPackage("..adapter.persistence..")
          .allowEmptyShould(true);

  @ArchTest
  static final ArchRule coreIsFrameworkFree =
      noClasses()
          .that()
          .resideInAPackage("io.agentguard.core..")
          .should()
          .dependOnClassesThat()
          .resideInAnyPackage(
              "org.springframework..", "jakarta.persistence..", "io.modelcontextprotocol..");

  @ArchTest
  static final ArchRule checksImplementCheck =
      classes()
          .that()
          .haveSimpleNameEndingWith("Check")
          .and()
          .areNotInterfaces()
          .should()
          .implement("io.agentguard.core.check.Check")
          .allowEmptyShould(true);
}
