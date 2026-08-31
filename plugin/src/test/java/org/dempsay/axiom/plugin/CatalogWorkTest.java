package org.dempsay.axiom.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dempsay.axiom.plugin.CatalogWork.Outcome;
import org.dempsay.axiom.plugin.CatalogWork.Request;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Catalog goal behavior without Maven (C2 CatalogMojoTest cases).
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
class CatalogWorkTest {

    @TempDir
    private Path temp;

    @Test
    void validCatalogAttachesClassifier() throws Exception {
        final Path catalog = writeValidCatalog(temp.resolve("src"));
        final Path output = temp.resolve("target/axiom/catalog.yaml");
        final Outcome outcome = run(Request.of(
                catalog, true, true, "1.2.3-TEST", output, temp.resolve("target/axiom/report.md")));
        assertFalse(outcome.failed());
        assertFalse(outcome.skipped());
        assertTrue(Files.isRegularFile(output));
        assertTrue(Files.isRegularFile(temp.resolve("target/axiom/report.md")));
        assertTrue(Files.readString(output).contains("version: 1.2.3-TEST"));
        assertTrue(outcome.message().contains("Axiom catalog: 1 intents"));
        assertEquals("1.2.3-TEST", outcome.catalog().artifact().version());
    }

    @Test
    void invalidCatalogFailsBuild() throws Exception {
        Files.writeString(temp.resolve("catalog.yaml"), """
                schemaVersion: 1
                artifact:
                  groupId: org.example
                  artifactId: lib
                intents:
                  - id: sample_intent
                    title: Sample
                    severity: available
                    language: java
                    blessed:
                      kind: method
                    snippetRef: missing.java
                """);
        final Outcome outcome = run(Request.of(
                temp.resolve("catalog.yaml"),
                true,
                true,
                "1.0",
                temp.resolve("out.yaml"),
                temp.resolve("report.md")));
        assertTrue(outcome.failed());
        assertFalse(outcome.violations().isEmpty());
    }

    @Test
    void stampsProjectVersion() throws Exception {
        final Path catalog = writeValidCatalog(temp.resolve("src"));
        final String source = Files.readString(catalog);
        assertFalse(source.contains("1.2.3-TEST"));
        final Outcome outcome = run(Request.of(
                catalog,
                true,
                true,
                "1.2.3-TEST",
                temp.resolve("stamped.yaml"),
                temp.resolve("report.md")));
        assertFalse(outcome.failed());
        assertEquals("1.2.3-TEST", outcome.catalog().artifact().version());
        assertTrue(Files.readString(outcome.stampedCatalog()).contains("version: 1.2.3-TEST"));
    }

    @Test
    void missingOptionalCatalogSkips() {
        final Outcome outcome = run(Request.of(
                temp.resolve("no-such.yaml"),
                false,
                true,
                "1.0",
                temp.resolve("out.yaml"),
                temp.resolve("report.md")));
        assertTrue(outcome.skipped());
        assertFalse(outcome.failed());
        assertFalse(Files.isRegularFile(temp.resolve("out.yaml")));
    }

    @Test
    void missingRequiredCatalogFails() {
        final Outcome outcome = run(Request.of(
                temp.resolve("no-such.yaml"),
                true,
                true,
                "1.0",
                temp.resolve("out.yaml"),
                temp.resolve("report.md")));
        assertTrue(outcome.failed());
        assertFalse(outcome.skipped());
        assertTrue(outcome.message().contains("required"));
    }

    @Test
    void embedInJarWritesMetaInf() throws Exception {
        final Path catalog = writeValidCatalog(temp.resolve("src"));
        final Path embed = temp.resolve("classes/META-INF/axiom/catalog.yaml");
        final Path zip = temp.resolve("target/axiom/agent-catalog-examples.zip");
        final Outcome outcome = run(new Request(
                catalog,
                true,
                true,
                "1.2.3-TEST",
                temp.resolve("target/axiom/catalog.yaml"),
                temp.resolve("target/axiom/report.md"),
                embed,
                null,
                zip));
        assertFalse(outcome.failed());
        assertTrue(Files.isRegularFile(embed));
        assertTrue(Files.readString(embed).contains("sample_intent"));
        assertTrue(Files.isRegularFile(zip));
        assertTrue(Files.size(zip) > 0);
        assertEquals(zip, outcome.examplesZip());
    }

    private static Outcome run(final Request request) {
        final ExceptionalResponse<Outcome> response = CatalogWork.run(request);
        assertFalse(response.wasError());
        return response.response();
    }

    private static Path writeValidCatalog(final Path dir) throws Exception {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("snippet.java"), "doIt();");
        final Path catalog = dir.resolve("catalog.yaml");
        Files.writeString(catalog, """
                schemaVersion: 1
                artifact:
                  groupId: org.example
                  artifactId: lib
                intents:
                  - id: sample_intent
                    title: Sample
                    severity: available
                    language: java
                    blessed:
                      symbol: org.example.Lib.doIt
                      kind: method
                    snippetRef: snippet.java
                """);
        return catalog;
    }
}
