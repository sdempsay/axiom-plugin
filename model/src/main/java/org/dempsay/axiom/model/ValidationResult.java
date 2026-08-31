package org.dempsay.axiom.model;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of validating a catalog document.
 *
 * @param catalog parsed catalog when mapping succeeded; otherwise {@code null}
 * @param violations empty when the catalog is valid
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public record ValidationResult(Catalog catalog, List<Violation> violations) {

    /**
     * @return true when there are no violations and a catalog was parsed
     */
    public boolean isValid() {
        return violations.isEmpty() && Objects.nonNull(catalog);
    }
}
