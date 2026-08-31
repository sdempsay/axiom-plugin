package org.dempsay.axiom.model;

/**
 * Whether a missing intent may fail consumer CI later ({@code required}) or is only preferred
 * ({@code available}). The pilot does not fail consumer builds.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public enum Severity {
    REQUIRED,
    AVAILABLE
}
