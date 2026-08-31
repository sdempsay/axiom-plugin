package org.dempsay.axiom.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dempsay.axiom.model.Catalog;
import org.dempsay.axiom.model.CatalogValidator;
import org.dempsay.axiom.model.Severity;
import org.dempsay.axiom.model.ValidationResult;
import org.dempsay.axiom.plugin.AnnotationHarvester.HarvestedIntent;
import org.dempsay.axiom.plugin.AnnotationHarvester.MergeResult;
import org.dempsay.axiom.plugin.CatalogWork.Outcome;
import org.dempsay.axiom.plugin.CatalogWork.Request;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Annotation harvest merge rules (C2 FR4).
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
class AnnotationHarvesterTest {

    @TempDir
    private Path temp;

    @Test
    void harvestMergesYamlWins() throws Exception {
        final Path catalogFile = writeCatalog(temp, "sample_intent", "YAML title");
        final HarvestedIntent harvested = new HarvestedIntent(
                "sample_intent",
                "Harvested title should lose to YAML",
                Severity.REQUIRED,
                List.of("harvest-only-trigger"),
                List.of("Files\\.readString"));
        final Outcome outcome = run(new Request(
                catalogFile,
                true,
                true,
                "1.0",
                temp.resolve("out.yaml"),
                temp.resolve("report.md"),
                null,
                List.of(harvested),
                null));
        assertFalse(outcome.failed(), () -> outcome.violations().toString());
        final Catalog catalog = outcome.catalog();
        assertEquals("YAML title", catalog.intents().get(0).title());
        assertEquals(Severity.AVAILABLE, catalog.intents().get(0).severity());
        assertTrue(catalog.intents().get(0).triggers().contains("yaml-trigger"));
        assertFalse(catalog.intents().get(0).triggers().contains("harvest-only-trigger"));
        assertEquals(1, catalog.intents().get(0).antiPatterns().size());
        assertEquals("Files\\.readString", catalog.intents().get(0).antiPatterns().get(0).pattern());
    }

    @Test
    void harvestWithoutSnippetFailsWhenEnabled() throws Exception {
        final Path catalogFile = writeCatalog(temp, "sample_intent", "YAML title");
        final HarvestedIntent harvested = new HarvestedIntent(
                "missing_intent",
                "No YAML row",
                Severity.AVAILABLE,
                List.of(),
                List.of());
        final Outcome outcome = run(new Request(
                catalogFile,
                true,
                true,
                "1.0",
                temp.resolve("out.yaml"),
                temp.resolve("report.md"),
                null,
                List.of(harvested),
                null));
        assertTrue(outcome.failed());
        assertTrue(outcome.violations().stream().anyMatch(violation -> "snippetRef".equals(violation.field())));
    }

    @Test
    void scanFindsCompiledAnnotation() {
        final Path classes = Path.of("target/test-classes");
        final ExceptionalResponse<List<HarvestedIntent>> scanned = AnnotationHarvester.scan(
                classes, getClass().getClassLoader());
        assertFalse(scanned.wasError());
        assertTrue(scanned.response().stream().anyMatch(intent -> "sample_intent".equals(intent.id())));
    }

    @Test
    void mergeEmptyHarvestIsNoOp() throws Exception {
        final Path catalogFile = writeCatalog(temp, "sample_intent", "YAML title");
        final ExceptionalResponse<ValidationResult> loaded = CatalogValidator.validate(catalogFile);
        final MergeResult merged = AnnotationHarvester.merge(loaded.response().catalog(), List.of());
        assertTrue(merged.isValid());
        assertEquals("YAML title", merged.catalog().intents().get(0).title());
    }

    private static Outcome run(final Request request) {
        final ExceptionalResponse<Outcome> response = CatalogWork.run(request);
        assertFalse(response.wasError());
        return response.response();
    }

    private static Path writeCatalog(final Path dir, final String intentId, final String title) throws Exception {
        Files.writeString(dir.resolve("snippet.java"), "doIt();");
        final Path catalog = dir.resolve("catalog.yaml");
        Files.writeString(catalog, """
                schemaVersion: 1
                artifact:
                  groupId: org.example
                  artifactId: lib
                intents:
                  - id: %s
                    title: %s
                    severity: available
                    language: java
                    blessed:
                      symbol: org.example.Lib.doIt
                      kind: method
                    snippetRef: snippet.java
                    triggers:
                      - yaml-trigger
                """.formatted(intentId, title));
        return catalog;
    }
}
