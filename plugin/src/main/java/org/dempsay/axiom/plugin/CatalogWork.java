package org.dempsay.axiom.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.dempsay.axiom.model.Catalog;
import org.dempsay.axiom.model.CatalogFormat;
import org.dempsay.axiom.model.CatalogValidator;
import org.dempsay.axiom.model.Intent;
import org.dempsay.axiom.model.Severity;
import org.dempsay.axiom.model.ValidationResult;
import org.dempsay.axiom.model.Violation;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Validates a catalog file, stamps the project version, and writes the stamped YAML plus report.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
public final class CatalogWork {

    private CatalogWork() { }

    /**
     * Inputs for one catalog goal run.
     *
     * @param catalogFile source catalog.yaml
     * @param required fail when the file is missing
     * @param stampVersion overwrite artifact.version with projectVersion
     * @param projectVersion Maven {@code ${project.version}}
     * @param outputCatalog stamped YAML destination
     * @param outputReport markdown report destination
     */
    public record Request(
            Path catalogFile,
            boolean required,
            boolean stampVersion,
            String projectVersion,
            Path outputCatalog,
            Path outputReport) {
    }

    /**
     * Result of one catalog goal run.
     *
     * @param skipped true when there was no catalog and required is false
     * @param failed true when validation failed or a required catalog is missing
     * @param message log / failure text
     * @param stampedCatalog written YAML, or {@code null}
     * @param catalog parsed catalog when written
     * @param violations validation failures
     */
    public record Outcome(
            boolean skipped,
            boolean failed,
            String message,
            Path stampedCatalog,
            Catalog catalog,
            List<Violation> violations) {
    }

    /**
     * Runs validate / stamp / write. I/O failures are {@link ExceptionalResponse#wasError()}.
     *
     * @param request goal inputs
     * @return skip, failure, or written catalog
     */
    public static ExceptionalResponse<Outcome> run(final Request request) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(request.catalogFile(), "catalogFile");
        final Path catalogFile = request.catalogFile().toAbsolutePath().normalize();
        if (!Files.isRegularFile(catalogFile)) {
            if (request.required()) {
                return ExceptionalResponse.success(new Outcome(
                        false,
                        true,
                        "Catalog is required but missing: " + catalogFile,
                        null,
                        null,
                        List.of()));
            }
            return ExceptionalResponse.success(new Outcome(
                    true,
                    false,
                    "No catalog at " + catalogFile + ", skipping",
                    null,
                    null,
                    List.of()));
        }
        final ExceptionalResponse<ValidationResult> validated = CatalogValidator.validate(catalogFile);
        if (validated.wasError()) {
            return ExceptionalResponse.failure();
        }
        final ValidationResult result = validated.response();
        if (!result.isValid()) {
            return ExceptionalResponse.success(new Outcome(
                    false,
                    true,
                    "Catalog validation failed",
                    null,
                    result.catalog(),
                    result.violations()));
        }
        Catalog catalog = result.catalog();
        if (request.stampVersion() || Objects.isNull(catalog.artifact().version())
                || catalog.artifact().version().isBlank()) {
            catalog = catalog.withVersion(request.projectVersion());
        }
        final Catalog stamped = catalog;
        return CatalogFormat.write(request.outputCatalog(), stamped)
                .chain((listener, written) -> writeOutputs(request.outputReport(), written, stamped));
    }

    private static ExceptionalResponse<Outcome> writeOutputs(
            final Path report,
            final Path written,
            final Catalog catalog) {
        return ExceptionalSupplier.of(() -> {
            if (Objects.nonNull(report)) {
                final Path parent = report.getParent();
                if (Objects.nonNull(parent)) {
                    Files.createDirectories(parent);
                }
                Files.writeString(report, reportMarkdown(catalog));
            }
            return new Outcome(false, false, summary(catalog), written, catalog, List.of());
        }).execute();
    }

    private static String reportMarkdown(final Catalog catalog) {
        final Counts counts = counts(catalog);
        return "# Axiom catalog\n\n"
                + "- Artifact: " + gav(catalog) + "\n"
                + "- Intents: " + counts.total() + " (" + counts.required() + " required, "
                + counts.available() + " available)\n";
    }

    private static String summary(final Catalog catalog) {
        final Counts counts = counts(catalog);
        return "Axiom catalog: " + counts.total() + " intents (" + counts.required() + " required, "
                + counts.available() + " available) for " + gav(catalog);
    }

    private static String gav(final Catalog catalog) {
        return catalog.artifact().groupId() + ":" + catalog.artifact().artifactId() + ":"
                + catalog.artifact().version();
    }

    private static Counts counts(final Catalog catalog) {
        int required = 0;
        int available = 0;
        for (final Intent intent : catalog.intents()) {
            if (intent.severity() == Severity.REQUIRED) {
                required++;
            } else {
                available++;
            }
        }
        return new Counts(required + available, required, available);
    }

    private record Counts(int total, int required, int available) {
    }
}
