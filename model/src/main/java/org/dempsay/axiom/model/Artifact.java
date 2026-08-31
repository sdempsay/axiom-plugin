package org.dempsay.axiom.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Maven coordinates of the catalog-producing artifact.
 *
 * @param groupId Maven groupId
 * @param artifactId Maven artifactId
 * @param version Maven version, optional in source catalogs
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Artifact(String groupId, String artifactId, String version) {
}
