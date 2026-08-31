package org.dempsay.axiom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Catalog validator failure cases from C1.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
class ValidateCatalogTest {

    @TempDir
    private Path temp;

    @Test
    void validCatalogPasses() {
        final ExceptionalResponse<ValidationResult> response = CatalogValidator.validate(
                CatalogPaths.exceptionalCatalog());
        assertFalse(response.wasError());
        assertTrue(response.response().isValid(), () -> response.response().violations().toString());
        final Catalog catalog = response.response().catalog();
        assertEquals(1, catalog.schemaVersion());
        assertEquals("exceptional", catalog.artifact().artifactId());
        assertEquals(1, catalog.intents().size());
        assertEquals("external_failure", catalog.intents().get(0).id());
    }

    @Test
    void duplicateIntentIdFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
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
                  - id: sample_intent
                    title: Other
                    severity: available
                    language: java
                    blessed:
                      symbol: org.example.Lib.other
                      kind: method
                    snippetRef: snippet.java
                """);
        assertField(validate(catalog), "id");
    }

    @Test
    void missingIntentIdFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
                schemaVersion: 1
                artifact:
                  groupId: org.example
                  artifactId: lib
                intents:
                  - title: Sample
                    severity: available
                    language: java
                    blessed:
                      symbol: org.example.Lib.doIt
                      kind: method
                    snippetRef: snippet.java
                """);
        assertField(validate(catalog), "id");
    }

    @Test
    void missingBlessedSymbolFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
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
                    snippetRef: snippet.java
                """);
        assertField(validate(catalog), "blessed.symbol");
    }

    @Test
    void missingSnippetRefFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
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
                """);
        assertField(validate(catalog), "snippetRef");
    }

    @Test
    void missingSnippetFileFails() throws Exception {
        final Path catalog = writeCatalog("""
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
                    snippetRef: missing.java
                """);
        assertField(validate(catalog), "snippetRef");
    }

    @Test
    void invalidRegexFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
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
                    antiPatterns:
                      - pattern: "("
                        message: bad regex
                """);
        assertField(validate(catalog), "antiPatterns.pattern");
    }

    @Test
    void missingArtifactCoordinatesFails() throws Exception {
        writeSnippet();
        final Path catalog = writeCatalog("""
                schemaVersion: 1
                artifact:
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
        assertField(validate(catalog), "groupId");
    }

    @Test
    void missingCatalogFileIsIoError() {
        final ExceptionalResponse<ValidationResult> response = CatalogValidator.validate(
                temp.resolve("no-such.yaml"));
        assertTrue(response.wasError());
    }

    private Path writeCatalog(final String yaml) throws Exception {
        final Path file = temp.resolve("catalog.yaml");
        Files.writeString(file, yaml);
        return file;
    }

    private void writeSnippet() throws Exception {
        Files.writeString(temp.resolve("snippet.java"), "doIt();");
    }

    private static ValidationResult validate(final Path catalog) {
        final ExceptionalResponse<ValidationResult> response = CatalogValidator.validate(catalog);
        assertFalse(response.wasError());
        final ValidationResult result = response.response();
        assertFalse(result.isValid(), () -> "expected violations, got valid catalog");
        return result;
    }

    private static void assertField(final ValidationResult result, final String field) {
        final List<Violation> violations = result.violations();
        assertTrue(
                violations.stream().anyMatch(violation -> field.equals(violation.field())),
                () -> "expected field " + field + " in " + violations);
    }
}
