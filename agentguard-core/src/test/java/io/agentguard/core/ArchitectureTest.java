package io.agentguard.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Guard rails: core stays framework-free, acyclic, and checks implement the Check interface. */
@AnalyzeClasses(
    packages = "io.agentguard.core",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

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

  @ArchTest
  static final ArchRule corePackagesAreFreeOfCycles =
      slices().matching("io.agentguard.core.(*)..").should().beFreeOfCycles();
}
