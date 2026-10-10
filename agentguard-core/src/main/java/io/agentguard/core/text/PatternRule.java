package io.agentguard.core.text;

import java.util.regex.Pattern;

/** A named, compiled phrase pattern with the confidence of a match. */
public record PatternRule(String name, Pattern pattern, double confidence) {}
