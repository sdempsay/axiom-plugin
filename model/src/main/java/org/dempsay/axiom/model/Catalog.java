package org.dempsay.axiom.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Catalog(int schemaVersion, Artifact artifact, Owner owner, List<Intent> intents) {

    /**
     * Returns a copy with {@code artifact.version} set to {@code version}.
     *
     * @param version Maven project version to stamp
     * @return catalog with stamped version
     */
    public Catalog withVersion(final String version) {
        Objects.requireNonNull(artifact, "artifact");
        return new Catalog(
                schemaVersion,
                new Artifact(artifact.groupId(), artifact.artifactId(), version),
                owner,
                intents);
    }

    /**
     * Returns a copy with a replacement intent list.
     *
     * @param newIntents intents to store
     * @return catalog with those intents
     */
    public Catalog withIntents(final List<Intent> newIntents) {
        Objects.requireNonNull(newIntents, "newIntents");
        return new Catalog(schemaVersion, artifact, owner, List.copyOf(newIntents));
    }
}
