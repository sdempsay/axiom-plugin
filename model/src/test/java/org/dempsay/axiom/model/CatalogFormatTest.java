package org.dempsay.axiom.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * YAML serialization for stamped catalogs.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
class CatalogFormatTest {

    @TempDir
    private Path temp;

    @Test
    void stampsVersionAndRoundTrips() throws Exception {
        final ExceptionalResponse<ValidationResult> loaded = CatalogValidator.validate(
                CatalogPaths.exceptionalCatalog());
        assertFalse(loaded.wasError());
        assertTrue(loaded.response().isValid());
        final Catalog stamped = loaded.response().catalog().withVersion("9.9.9-TEST");
        final Path examples = CatalogPaths.exceptionalCatalog().getParent();
        Files.createDirectories(temp.resolve("exceptional"));
        Files.copy(examples.resolve("exceptional/ExceptionalSupplier.java"),
                temp.resolve("exceptional/ExceptionalSupplier.java"));
        Files.copy(examples.resolve("exceptional/ExceptionalSupplierChain.java"),
                temp.resolve("exceptional/ExceptionalSupplierChain.java"));
        final Path out = temp.resolve("catalog.yaml");
        final ExceptionalResponse<Path> written = CatalogFormat.write(out, stamped);
        assertFalse(written.wasError());
        final ExceptionalResponse<ValidationResult> reread = CatalogValidator.validate(out);
        assertFalse(reread.wasError());
        assertTrue(reread.response().isValid(), () -> reread.response().violations().toString());
        assertEquals("9.9.9-TEST", reread.response().catalog().artifact().version());
        assertEquals("external_failure", reread.response().catalog().intents().get(0).id());
    }

    @Test
    void writesLowercaseEnums() {
        final ExceptionalResponse<ValidationResult> loaded = CatalogValidator.validate(
                CatalogPaths.exceptionalCatalog());
        final ExceptionalResponse<String> yaml = CatalogFormat.toYaml(loaded.response().catalog());
        assertFalse(yaml.wasError());
        assertTrue(yaml.response().contains("severity: required"));
        assertTrue(yaml.response().contains("kind: method"));
        assertTrue(yaml.response().contains("role: primary"));
        assertFalse(yaml.response().contains("REQUIRED"));
    }
}
