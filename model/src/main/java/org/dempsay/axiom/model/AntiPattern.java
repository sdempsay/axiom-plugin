package org.dempsay.axiom.model;

/**
 * Source-text regex that later lint-diff and detectors may apply.
 *
 * @param pattern Java regex
 * @param message shown when the pattern matches
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record AntiPattern(String pattern, String message) {
}
