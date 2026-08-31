package org.dempsay.axiom.model;

/**
 * Owning repository for the catalog.
 *
 * @param repo GitHub {@code owner/name}, or later GitLab {@code group/project}
 * @param contact optional contact label
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public record Owner(String repo, String contact) {
}
