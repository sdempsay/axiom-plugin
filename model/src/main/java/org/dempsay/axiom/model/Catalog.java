package org.dempsay.axiom.model;

import java.util.List;

/**
 * One catalog document for a producing artifact.
 *
 * @param schemaVersion must be {@code 1}
 * @param artifact Maven coordinates
 * @param owner optional owning repository
 * @param intents at least one intent
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record Catalog(int schemaVersion, Artifact artifact, Owner owner, List<Intent> intents) {
}
