package org.dempsay.axiom.model;

/**
 * Snippet file referenced by an intent.
 *
 * @param ref path relative to the catalog file
 * @param role primary or secondary
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record Snippet(String ref, SnippetRole role) {
}
