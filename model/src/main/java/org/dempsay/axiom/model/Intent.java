package org.dempsay.axiom.model;

import java.util.List;

/**
 * One intent fact in a catalog.
 *
 * @param id kebab-or-snake id, stable across versions
 * @param title human title
 * @param severity required or available
 * @param language implementation language
 * @param blessed blessed symbol
 * @param snippetRef primary snippet path relative to the catalog file
 * @param snippets optional snippet list; when present, exactly one primary must match snippetRef
 * @param triggers plain-language and token hints for lookup
 * @param antiPatterns source-text regexes
 * @param never human/agent guidance that is not a detector
 * @param docs optional cold links
 * @param supersedes previous intent ids treated as lookup aliases
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
@SuppressWarnings("checkstyle:ParameterNumber")
public record Intent(
        String id,
        String title,
        Severity severity,
        String language,
        Blessed blessed,
        String snippetRef,
        List<Snippet> snippets,
        List<String> triggers,
        List<AntiPattern> antiPatterns,
        List<String> never,
        List<String> docs,
        List<String> supersedes) {
}
