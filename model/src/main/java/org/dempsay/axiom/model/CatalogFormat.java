package org.dempsay.axiom.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Byte-stable YAML serializer for catalog documents.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public final class CatalogFormat {

    private static final ObjectMapper YAML_MAPPER = YAMLMapper.builder()
            .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
            .serializationInclusion(JsonInclude.Include.NON_EMPTY)
            .disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build();

    private CatalogFormat() { }

    /**
     * Serializes {@code catalog} to YAML with stable field order.
     *
     * @param catalog catalog to write
     * @return YAML document, or serialization failure
     */
    public static ExceptionalResponse<String> toYaml(final Catalog catalog) {
        Objects.requireNonNull(catalog, "catalog");
        return ExceptionalSupplier.of(() -> YAML_MAPPER.writeValueAsString(catalog)).execute();
    }

    /**
     * Writes {@code catalog} as YAML, creating parent directories as needed.
     *
     * @param path output file
     * @param catalog catalog to write
     * @return the written path, or I/O failure
     */
    public static ExceptionalResponse<Path> write(final Path path, final Catalog catalog) {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(catalog, "catalog");
        return toYaml(catalog).then(yaml -> {
            final Path parent = path.getParent();
            if (Objects.nonNull(parent)) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, yaml);
            return path;
        });
    }
}
