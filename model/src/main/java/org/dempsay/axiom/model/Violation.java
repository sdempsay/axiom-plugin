package org.dempsay.axiom.model;

import java.util.Objects;

/**
 * One catalog validation failure.
 *
 * @param intentId intent id when the failure is per-intent; otherwise {@code null}
 * @param field field or dotted path that failed
 * @param message what is wrong
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record Violation(String intentId, String field, String message) {

    /**
     * Formats the failing intent id and field for CLI and plugin output.
     *
     * @return human-readable violation
     */
    @Override
    public String toString() {
        if (Objects.isNull(intentId) || intentId.isBlank()) {
            return field + ": " + message;
        }
        return intentId + " " + field + ": " + message;
    }
}
