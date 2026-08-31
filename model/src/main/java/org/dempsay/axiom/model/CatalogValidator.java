package org.dempsay.axiom.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Parses YAML catalogs and validates them against schemaVersion 1 rules.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public final class CatalogValidator {

    private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private CatalogValidator() { }

    /**
     * Reads {@code catalogFile} and validates it. Snippet paths are resolved relative to the
     * catalog file's parent directory.
     *
     * @param catalogFile catalog YAML
     * @return I/O failure, or a result with violations and/or a parsed catalog
     */
    public static ExceptionalResponse<ValidationResult> validate(final Path catalogFile) {
        Objects.requireNonNull(catalogFile, "catalogFile");
        final Path absolute = catalogFile.toAbsolutePath().normalize();
        return ExceptionalSupplier.of(() -> Files.readString(absolute))
                .execute()
                .then(raw -> validateYaml(raw, absolute.getParent()));
    }

    /**
     * Validates already-loaded YAML. Snippet paths are resolved against {@code catalogDir}.
     *
     * @param yaml catalog YAML
     * @param catalogDir directory containing the catalog file
     * @return parsed catalog and any violations
     */
    public static ValidationResult validateYaml(final String yaml, final Path catalogDir) {
        final ExceptionalResponse<JsonNode> parsed = ExceptionalSupplier.of(() -> YAML_MAPPER.readTree(yaml))
                .execute();
        if (parsed.wasError()) {
            return new ValidationResult(null, List.of(new Violation(null, "catalog", "YAML is not valid")));
        }
        final JsonNode root = parsed.response();
        if (Objects.isNull(root) || !root.isObject()) {
            return new ValidationResult(null, List.of(new Violation(null, "catalog", "root must be a mapping")));
        }
        final List<Violation> violations = new ArrayList<>();
        validateDocument(root, catalogDir, violations);
        Catalog catalog = null;
        if (violations.isEmpty()) {
            final ExceptionalResponse<Catalog> mapped = ExceptionalSupplier.of(
                    () -> YAML_MAPPER.treeToValue(root, Catalog.class)).execute();
            if (mapped.wasError()) {
                violations.add(new Violation(null, "catalog", "could not map into the Java model"));
            } else {
                catalog = mapped.response();
            }
        }
        return new ValidationResult(catalog, List.copyOf(violations));
    }

    private static void validateDocument(
            final JsonNode root,
            final Path catalogDir,
            final List<Violation> violations) {
        if (!root.has("schemaVersion") || root.get("schemaVersion").asInt(-1) != 1) {
            violations.add(new Violation(null, "schemaVersion", "must be 1"));
        }
        validateArtifact(root.get("artifact"), violations);
        validateOwner(root.get("owner"), violations);
        final JsonNode intents = root.get("intents");
        if (Objects.isNull(intents) || !intents.isArray() || intents.isEmpty()) {
            violations.add(new Violation(null, "intents", "must contain at least one entry"));
            return;
        }
        final Set<String> ids = new HashSet<>();
        int index = 0;
        for (final JsonNode intent : intents) {
            validateIntent(intent, index, catalogDir, ids, violations);
            index++;
        }
    }

    private static void validateArtifact(final JsonNode artifact, final List<Violation> violations) {
        if (Objects.isNull(artifact) || !artifact.isObject()) {
            violations.add(new Violation(null, "artifact", "is required"));
            return;
        }
        requireText(artifact, "groupId", null, violations);
        requireText(artifact, "artifactId", null, violations);
    }

    private static void validateOwner(final JsonNode owner, final List<Violation> violations) {
        if (Objects.isNull(owner) || owner.isNull()) {
            return;
        }
        if (!owner.isObject()) {
            violations.add(new Violation(null, "owner", "must be a mapping"));
            return;
        }
        requireText(owner, "repo", null, violations);
    }

    private static void validateIntent(
            final JsonNode intent,
            final int index,
            final Path catalogDir,
            final Set<String> ids,
            final List<Violation> violations) {
        if (Objects.isNull(intent) || !intent.isObject()) {
            violations.add(new Violation("intents[" + index + "]", "intent", "must be a mapping"));
            return;
        }
        final String id = text(intent, "id");
        final String intentId = Objects.isNull(id) || id.isBlank() ? "intents[" + index + "]" : id;
        if (Objects.isNull(id) || id.isBlank()) {
            violations.add(new Violation(intentId, "id", "is required"));
        } else if (!ids.add(id)) {
            violations.add(new Violation(id, "id", "duplicate intent id"));
        }
        requireText(intent, "title", intentId, violations);
        requireText(intent, "severity", intentId, violations);
        requireText(intent, "language", intentId, violations);
        requireBlessed(intent.get("blessed"), intentId, violations);
        requireText(intent, "snippetRef", intentId, violations);
        final String snippetRef = text(intent, "snippetRef");
        if (Objects.nonNull(snippetRef) && !snippetRef.isBlank()) {
            requireExistingFile(catalogDir, snippetRef, intentId, "snippetRef", violations);
        }
        validateSnippets(intent.get("snippets"), snippetRef, intentId, catalogDir, violations);
        validateAntiPatterns(intent.get("antiPatterns"), intentId, violations);
    }

    private static void requireBlessed(
            final JsonNode blessed,
            final String intentId,
            final List<Violation> violations) {
        if (Objects.isNull(blessed) || !blessed.isObject()) {
            violations.add(new Violation(intentId, "blessed", "is required"));
            return;
        }
        final String symbol = text(blessed, "symbol");
        if (Objects.isNull(symbol) || symbol.isBlank()) {
            violations.add(new Violation(intentId, "blessed.symbol", "is required"));
        }
        final String kind = text(blessed, "kind");
        if (Objects.isNull(kind) || kind.isBlank()) {
            violations.add(new Violation(intentId, "blessed.kind", "is required"));
        }
    }

    private static void validateSnippets(
            final JsonNode snippets,
            final String snippetRef,
            final String intentId,
            final Path catalogDir,
            final List<Violation> violations) {
        if (Objects.isNull(snippets) || snippets.isNull()) {
            return;
        }
        if (!snippets.isArray()) {
            violations.add(new Violation(intentId, "snippets", "must be a list"));
            return;
        }
        int primaryCount = 0;
        String primaryRef = null;
        int index = 0;
        for (final JsonNode snippet : snippets) {
            final String field = "snippets[" + index + "]";
            if (Objects.isNull(snippet) || !snippet.isObject()) {
                violations.add(new Violation(intentId, field, "must be a mapping"));
                index++;
                continue;
            }
            requireText(snippet, "ref", intentId, violations);
            requireText(snippet, "role", intentId, violations);
            final String ref = text(snippet, "ref");
            if (Objects.nonNull(ref) && !ref.isBlank()) {
                requireExistingFile(catalogDir, ref, intentId, field + ".ref", violations);
            }
            final String role = text(snippet, "role");
            if ("primary".equalsIgnoreCase(role)) {
                primaryCount++;
                primaryRef = ref;
            }
            index++;
        }
        if (primaryCount != 1) {
            violations.add(new Violation(intentId, "snippets", "must contain exactly one role: primary"));
        } else if (Objects.nonNull(snippetRef) && !snippetRef.equals(primaryRef)) {
            violations.add(new Violation(intentId, "snippets", "primary ref must match snippetRef"));
        }
    }

    private static void validateAntiPatterns(
            final JsonNode antiPatterns,
            final String intentId,
            final List<Violation> violations) {
        if (Objects.isNull(antiPatterns) || antiPatterns.isNull()) {
            return;
        }
        if (!antiPatterns.isArray()) {
            violations.add(new Violation(intentId, "antiPatterns", "must be a list"));
            return;
        }
        int index = 0;
        for (final JsonNode anti : antiPatterns) {
            final String field = "antiPatterns[" + index + "]";
            if (Objects.isNull(anti) || !anti.isObject()) {
                violations.add(new Violation(intentId, field, "must be a mapping"));
                index++;
                continue;
            }
            requireText(anti, "pattern", intentId, violations);
            requireText(anti, "message", intentId, violations);
            final String pattern = text(anti, "pattern");
            if (Objects.nonNull(pattern) && !pattern.isBlank()) {
                final ExceptionalResponse<Pattern> compiled = ExceptionalSupplier.of(() -> Pattern.compile(pattern))
                        .execute();
                if (compiled.wasError()) {
                    violations.add(new Violation(intentId, "antiPatterns.pattern", "is not a valid regex"));
                }
            }
            index++;
        }
    }

    private static void requireExistingFile(
            final Path catalogDir,
            final String ref,
            final String intentId,
            final String field,
            final List<Violation> violations) {
        if (Objects.isNull(catalogDir) || !Files.isRegularFile(catalogDir.resolve(ref))) {
            violations.add(new Violation(intentId, field, "snippet file is missing"));
        }
    }

    private static void requireText(
            final JsonNode node,
            final String field,
            final String intentId,
            final List<Violation> violations) {
        final String value = text(node, field);
        if (Objects.isNull(value) || value.isBlank()) {
            violations.add(new Violation(intentId, field, "is required"));
        }
    }

    private static String text(final JsonNode node, final String field) {
        if (Objects.isNull(node) || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        final JsonNode value = node.get(field);
        if (value.isTextual() || value.isNumber() || value.isBoolean()) {
            return value.asText();
        }
        return null;
    }
}
