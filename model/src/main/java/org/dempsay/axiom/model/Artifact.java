package org.dempsay.axiom.model;

/**
 * Maven coordinates of the catalog-producing artifact.
 *
 * @param groupId Maven groupId
 * @param artifactId Maven artifactId
 * @param version Maven version, optional in source catalogs
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record Artifact(String groupId, String artifactId, String version) {
}
