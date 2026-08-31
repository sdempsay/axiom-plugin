package org.dempsay.axiom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.junit.jupiter.api.Test;

/**
 * Exceptional example catalog and published JSON Schema.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
class SchemaExampleTest {

    @Test
    void exceptionalExampleValidates() {
        final ExceptionalResponse<ValidationResult> response = CatalogValidator.validate(
                CatalogPaths.exceptionalCatalog());
        assertFalse(response.wasError());
        assertTrue(response.response().isValid(), () -> response.response().violations().toString());
    }

    @Test
    void primarySnippetIsOfExecute() throws Exception {
        final Intent intent = loadExternalFailure();
        assertEquals("exceptional/ExceptionalSupplier.java", intent.snippetRef());
        final String primary = Files.readString(snippet(intent.snippetRef()));
        assertTrue(primary.contains("ExceptionalSupplier.of("));
        assertTrue(primary.contains(".execute()"));
        assertTrue(primary.contains("wasError()"));
        assertTrue(primary.contains("response()"));
        assertFalse(primary.contains(".with("));
    }

    @Test
    void secondaryChainSnippetOptional() throws Exception {
        final Intent intent = loadExternalFailure();
        assertEquals(1, intent.snippets().stream().filter(snippet -> snippet.role() == SnippetRole.PRIMARY).count());
        assertEquals(1, intent.snippets().stream().filter(snippet -> snippet.role() == SnippetRole.SECONDARY).count());
        final Snippet secondary = intent.snippets().stream()
                .filter(snippet -> snippet.role() == SnippetRole.SECONDARY)
                .findFirst()
                .orElseThrow();
        final String chain = Files.readString(snippet(secondary.ref()));
        assertTrue(chain.contains(".chain("));
        assertFalse(chain.contains("ExceptionalSupplier.of("));
    }

    @Test
    void schemaDocumentIsDraft202012() throws Exception {
        final Path schemaFile = CatalogPaths.schemaDocument();
        assertTrue(Files.isRegularFile(schemaFile));
        final JsonNode schema = new ObjectMapper().readTree(schemaFile.toFile());
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.get("$schema").asText());
        assertEquals(1, schema.get("properties").get("schemaVersion").get("const").asInt());
        assertTrue(schema.get("required").toString().contains("artifact"));
        assertTrue(schema.get("required").toString().contains("intents"));
    }

    private static Intent loadExternalFailure() {
        final ExceptionalResponse<ValidationResult> response = CatalogValidator.validate(
                CatalogPaths.exceptionalCatalog());
        assertFalse(response.wasError());
        assertTrue(response.response().isValid(), () -> response.response().violations().toString());
        return response.response().catalog().intents().get(0);
    }

    private static Path snippet(final String ref) {
        return CatalogPaths.exceptionalCatalog().getParent().resolve(ref);
    }
}
