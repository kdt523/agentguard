package io.agentguard.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Phrase patterns for a check, loaded from {@code /patterns/<name>.patterns} on the classpath. File
 * format, one rule per line: {@code confidence | rule-name | regex}; {@code #} starts a comment.
 * Regexes are case-insensitive and Unicode-aware.
 */
public final class PatternLibrary {

  private static final Map<String, PatternLibrary> CACHE = new ConcurrentHashMap<>();

  private final List<PatternRule> rules;

  private PatternLibrary(List<PatternRule> rules) {
    this.rules = List.copyOf(rules);
  }

  /** A pattern hit in some text. */
  public record Hit(PatternRule rule, int start, int end) {}

  /** Loads (and caches) the library called {@code name}, e.g. {@code AG-TP-001}. */
  public static PatternLibrary load(String name) {
    return CACHE.computeIfAbsent(name, PatternLibrary::read);
  }

  public List<PatternRule> rules() {
    return rules;
  }

  /** All matches of all rules in {@code text}. */
  public List<Hit> find(String text) {
    List<Hit> hits = new ArrayList<>();
    for (PatternRule r : rules) {
      Matcher m = r.pattern().matcher(text);
      while (m.find()) {
        hits.add(new Hit(r, m.start(), m.end()));
      }
    }
    return hits;
  }

  /** True when any rule matches. */
  public boolean matches(String text) {
    return rules.stream().anyMatch(r -> r.pattern().matcher(text).find());
  }

  private static PatternLibrary read(String name) {
    String resource = "/patterns/" + name + ".patterns";
    try (InputStream in = PatternLibrary.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("missing pattern file " + resource);
      }
      BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
      List<PatternRule> rules = new ArrayList<>();
      String line;
      int n = 0;
      while ((line = r.readLine()) != null) {
        n++;
        if (!line.isBlank() && !line.stripLeading().startsWith("#")) {
          rules.add(parse(resource, n, line));
        }
      }
      return new PatternLibrary(rules);
    } catch (IOException e) {
      throw new IllegalStateException("cannot read " + resource, e);
    }
  }

  private static PatternRule parse(String resource, int lineNo, String line) {
    String[] parts = line.split("\\|", 3);
    if (parts.length != 3) {
      throw new IllegalStateException(
          resource + ":" + lineNo + " expected 'confidence | name | regex'");
    }
    int flags = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS;
    return new PatternRule(
        parts[1].strip(),
        Pattern.compile(parts[2].strip(), flags),
        Double.parseDouble(parts[0].strip()));
  }
}
