package io.agentguard.core.check;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.agentguard.core.check.impl.HiddenInstructionCheck;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class CheckRegistryTest {

  private static final Pattern ID = Pattern.compile("AG-(TP|SH|PE|CF|SC|DR|TF|RT)-\\d{3}");

  @Test
  void defaults_registersAtLeastTwelveChecks() {
    assertThat(CheckRegistry.defaults().all()).hasSizeGreaterThanOrEqualTo(12);
  }

  @Test
  void defaults_idsAreWellFormedAndOwaspRefsPresent() {
    assertThat(CheckRegistry.defaults().all())
        .allSatisfy(
            c -> {
              assertThat(c.id()).matches(ID);
              assertThat(c.owaspRef()).matches("MCP(0[1-9]|10)");
              assertThat(c.title()).isNotBlank();
            });
  }

  @Test
  void defaults_coverEveryScope() {
    CheckRegistry r = CheckRegistry.defaults();

    assertThat(
            List.of(
                r.withScope(Scope.TOOL),
                r.withScope(Scope.SERVER),
                r.withScope(Scope.CROSS_SERVER)))
        .allSatisfy(list -> assertThat(list).isNotEmpty());
  }

  @Test
  void constructor_duplicateIds_rejected() {
    assertThatThrownBy(
            () ->
                new CheckRegistry(
                    List.of(new HiddenInstructionCheck(), new HiddenInstructionCheck())))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void byId_unknown_isEmpty() {
    assertThat(CheckRegistry.defaults().byId("AG-XX-999")).isEmpty();
  }

  @Test
  void with_addsExtraChecks() {
    CheckRegistry base = new CheckRegistry(List.of(new HiddenInstructionCheck()));

    assertThat(base.with(List.of()).all()).hasSize(1);
  }
}
