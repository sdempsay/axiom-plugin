package org.dempsay.axiom.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Role of a snippet file attached to an intent.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public enum SnippetRole {
    @JsonProperty("primary")
    PRIMARY,
    @JsonProperty("secondary")
    SECONDARY
}
