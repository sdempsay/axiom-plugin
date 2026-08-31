package org.dempsay.axiom.model;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves files under the plugin repo's {@code schema/} tree from Maven or IDE cwd.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
final class CatalogPaths {

    private CatalogPaths() { }

    /**
     * @return plugin repository root (directory that contains {@code schema/})
     */
    static Path pluginRoot() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            final Path example = current.resolve("schema")
                    .resolve("examples")
                    .resolve("exceptional.catalog.yaml");
            if (Files.isRegularFile(example)) {
                return current;
            }
            current = current.getParent();
        }
        throw new IllegalStateException(
                "schema/examples/exceptional.catalog.yaml not found from " + Path.of("").toAbsolutePath());
    }

    /**
     * @return Exceptional example catalog
     */
    static Path exceptionalCatalog() {
        return pluginRoot().resolve("schema").resolve("examples").resolve("exceptional.catalog.yaml");
    }

    /**
     * @return published JSON Schema
     */
    static Path schemaDocument() {
        return pluginRoot().resolve("schema").resolve("axiom-catalog-1.json");
    }
}
