package org.dempsay.axiom.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Whether a missing intent may fail consumer CI later ({@code required}) or is only preferred
 * ({@code available}). The pilot does not fail consumer builds.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public enum Severity {
    @JsonProperty("required")
    REQUIRED,
    @JsonProperty("available")
    AVAILABLE
}
