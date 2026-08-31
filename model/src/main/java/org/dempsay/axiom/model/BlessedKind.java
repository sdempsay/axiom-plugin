package org.dempsay.axiom.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Kind of blessed symbol named by an intent.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public enum BlessedKind {
    @JsonProperty("method")
    METHOD,
    @JsonProperty("type")
    TYPE,
    @JsonProperty("package")
    PACKAGE
}
