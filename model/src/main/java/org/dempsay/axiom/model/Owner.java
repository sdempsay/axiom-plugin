package org.dempsay.axiom.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Owning repository for the catalog.
 *
 * @param repo GitHub {@code owner/name}, or later GitLab {@code group/project}
 * @param contact optional contact label
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record Owner(String repo, String contact) {
}
