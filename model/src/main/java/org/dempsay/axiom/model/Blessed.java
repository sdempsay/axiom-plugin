package org.dempsay.axiom.model;

/**
 * Blessed implementation named by an intent.
 *
 * @param symbol primary entry point, not every overload
 * @param kind method, type, or package
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public record Blessed(String symbol, BlessedKind kind) {
}
