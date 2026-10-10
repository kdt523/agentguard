package io.agentguard.core.drift;

import static org.assertj.core.api.Assertions.assertThat;

import io.agentguard.core.model.ToolAnnotations;
import io.agentguard.core.model.ToolDefinition;
import io.agentguard.core.model.ToolSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

class ToolHasherTest {

  private static final JsonMapper M = JsonMapper.builder().build();

  private static JsonNode json(String s) {
    return M.readTree(s);
  }

  private static ToolDefinition tool(String desc, String schema) {
    return new ToolDefinition("t", desc, json(schema), null, null);
  }

  @Test
  void canonical_sortsKeysAndStripsWhitespace() {
    assertThat(CanonicalJson.write(json("{ \"b\": [1, 2.50], \"a\": {\"y\":true,\"x\":null} }")))
        .isEqualTo("{\"a\":{\"x\":null,\"y\":true},\"b\":[1,2.5]}");
  }

  @Test
  void canonical_normalisesNumbers() {
    assertThat(
            List.of(
                CanonicalJson.number(new BigDecimal("1.000")),
                CanonicalJson.number(new BigDecimal("0.00")),
                CanonicalJson.number(new BigDecimal("1E+3"))))
        .containsExactly("1", "0", "1000");
  }

  @Test
  void canonical_escapesControlAndNonAscii() {
    assertThat(CanonicalJson.write(M.getNodeFactory().stringNode("a\"\\\n\r\t\u0001​")))
        .isEqualTo("\"a\\\"\\\\\\n\\r\\t\\u0001\\u200b\"");
  }

  @Test
  void canonical_nfcNormalisesStrings() {
    String decomposed = "é";
    String composed = "é";

    assertThat(CanonicalJson.write(M.getNodeFactory().stringNode(decomposed)))
        .isEqualTo(CanonicalJson.write(M.getNodeFactory().stringNode(composed)));
  }

  @Test
  void canonical_null_writesNull() {
    assertThat(CanonicalJson.write(null)).isEqualTo("null");
  }

  @Test
  void sha256_invisibleCharAdded_changesHash() {
    assertThat(ToolHasher.sha256(tool("Adds​", "{}")))
        .isNotEqualTo(ToolHasher.sha256(tool("Adds", "{}")));
  }

  @Test
  void sha256_annotationsChange_changesHash() {
    var a =
        new ToolDefinition("t", "d", null, new ToolAnnotations(null, true, null, null, null), null);
    var b =
        new ToolDefinition("t", "d", null, new ToolAnnotations(null, false, null, null, "T"), null);

    assertThat(ToolHasher.sha256(a)).isNotEqualTo(ToolHasher.sha256(b));
  }

  @Test
  void hashed_fillsToolAndSnapshotHashes() {
    var s =
        new ToolSnapshot("k", "n", "1", Instant.EPOCH, List.of(tool("x", "{}")), null, null, null);

    var h = ToolHasher.hashed(s);

    assertThat(h.tools().get(0).sha256()).hasSize(64);
    assertThat(h.snapshotSha256()).isEqualTo(ToolHasher.snapshotSha256(s.tools()));
  }

  @Test
  void snapshotSha256_isOrderIndependent() {
    var a = new ToolDefinition("a", "1", null, null, null);
    var b = new ToolDefinition("b", "2", null, null, null);

    assertThat(ToolHasher.snapshotSha256(List.of(a, b)))
        .isEqualTo(ToolHasher.snapshotSha256(List.of(b, a)));
  }

  @Property(tries = 200)
  void keyOrderPermutation_sameHash(@ForAll("objects") List<String> keys, @ForAll long seed) {
    List<String> shuffled = new ArrayList<>(keys);
    Collections.shuffle(shuffled, new java.util.Random(seed));
    ObjectNode a = M.createObjectNode();
    ObjectNode b = M.createObjectNode();
    keys.forEach(k -> a.put(k, k.length()));
    shuffled.forEach(k -> b.put(k, k.length()));

    assertThat(ToolHasher.sha256(new ToolDefinition("t", "d", a, null, null)))
        .isEqualTo(ToolHasher.sha256(new ToolDefinition("t", "d", b, null, null)));
  }

  @Property(tries = 300)
  void oneCharChangeInDescription_differentHash(
      @ForAll @StringLength(min = 1, max = 60) String desc,
      @ForAll @IntRange(min = 0, max = 59) int pos,
      @ForAll("printable") char replacement) {
    int i = pos % desc.length();
    String changed = desc.substring(0, i) + replacement + desc.substring(i + 1);
    net.jqwik.api.Assume.that(
        !java.text.Normalizer.normalize(changed, java.text.Normalizer.Form.NFC)
            .equals(java.text.Normalizer.normalize(desc, java.text.Normalizer.Form.NFC)));

    assertThat(ToolHasher.sha256(tool(changed, "{}")))
        .isNotEqualTo(ToolHasher.sha256(tool(desc, "{}")));
  }

  @Provide
  Arbitrary<List<String>> objects() {
    return Arbitraries.strings()
        .alpha()
        .ofMinLength(1)
        .ofMaxLength(8)
        .list()
        .uniqueElements()
        .ofMaxSize(10);
  }

  @Provide
  Arbitrary<Character> printable() {
    return Arbitraries.chars().range(' ', '~');
  }
}
