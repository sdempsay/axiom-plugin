package org.dempsay.axiom.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.dempsay.axiom.model.Catalog;
import org.dempsay.axiom.model.CatalogFormat;
import org.dempsay.axiom.model.CatalogValidator;
import org.dempsay.axiom.model.Intent;
import org.dempsay.axiom.model.Severity;
import org.dempsay.axiom.model.Snippet;
import org.dempsay.axiom.model.ValidationResult;
import org.dempsay.axiom.model.Violation;
import org.dempsay.axiom.plugin.AnnotationHarvester.HarvestedIntent;
import org.dempsay.axiom.plugin.AnnotationHarvester.MergeResult;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Validates a catalog file, stamps the project version, and writes the stamped YAML plus report.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
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
     * @param embedPath {@code META-INF/axiom/catalog.yaml} destination, or {@code null} to skip
     * @param harvested harvested annotations, or {@code null} when harvest is off
     * @param examplesZip zip destination, or {@code null} to skip
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public record Request(
            Path catalogFile,
            boolean required,
            boolean stampVersion,
            String projectVersion,
            Path outputCatalog,
            Path outputReport,
            Path embedPath,
            List<HarvestedIntent> harvested,
            Path examplesZip) {

        /**
         * Request without embed, harvest, or examples zip.
         *
         * @param catalogFile source catalog
         * @param required fail when missing
         * @param stampVersion stamp project version
         * @param projectVersion Maven version
         * @param outputCatalog stamped YAML
         * @param outputReport report markdown
         * @return request
         */
        public static Request of(
                final Path catalogFile,
                final boolean required,
                final boolean stampVersion,
                final String projectVersion,
                final Path outputCatalog,
                final Path outputReport) {
            return new Request(
                    catalogFile,
                    required,
                    stampVersion,
                    projectVersion,
                    outputCatalog,
                    outputReport,
                    null,
                    null,
                    null);
        }
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
     * @param examplesZip written examples zip, or {@code null}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public record Outcome(
            boolean skipped,
            boolean failed,
            String message,
            Path stampedCatalog,
            Catalog catalog,
            List<Violation> violations,
            Path examplesZip) {
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
                return ExceptionalResponse.success(failed(
                        "Catalog is required but missing: " + catalogFile, null, List.of()));
            }
            return ExceptionalResponse.success(new Outcome(
                    true,
                    false,
                    "No catalog at " + catalogFile + ", skipping",
                    null,
                    null,
                    List.of(),
                    null));
        }
        final ExceptionalResponse<ValidationResult> validated = CatalogValidator.validate(catalogFile);
        if (validated.wasError()) {
            return ExceptionalResponse.failure();
        }
        final ValidationResult result = validated.response();
        if (!result.isValid()) {
            return ExceptionalResponse.success(failed("Catalog validation failed", result.catalog(), result.violations()));
        }
        Catalog catalog = result.catalog();
        if (Objects.nonNull(request.harvested())) {
            final MergeResult merged = AnnotationHarvester.merge(catalog, request.harvested());
            if (!merged.isValid()) {
                return ExceptionalResponse.success(failed(
                        "Catalog harvest failed", merged.catalog(), merged.violations()));
            }
            catalog = merged.catalog();
        }
        if (request.stampVersion() || Objects.isNull(catalog.artifact().version())
                || catalog.artifact().version().isBlank()) {
            catalog = catalog.withVersion(request.projectVersion());
        }
        final Catalog stamped = catalog;
        return CatalogFormat.write(request.outputCatalog(), stamped)
                .chain((listener, written) -> writeOutputs(request, written, stamped, catalogFile.getParent()));
    }

    private static ExceptionalResponse<Outcome> writeOutputs(
            final Request request,
            final Path written,
            final Catalog catalog,
            final Path catalogDir) {
        return ExceptionalSupplier.of(() -> {
            if (Objects.nonNull(request.outputReport())) {
                writeFile(request.outputReport(), reportMarkdown(catalog));
            }
            if (Objects.nonNull(request.embedPath())) {
                final Path parent = request.embedPath().getParent();
                if (Objects.nonNull(parent)) {
                    Files.createDirectories(parent);
                }
                Files.copy(written, request.embedPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            final Path zip = writeExamplesZip(catalogDir, catalog, request.examplesZip());
            return new Outcome(false, false, summary(catalog), written, catalog, List.of(), zip);
        }).execute();
    }

    private static Path writeExamplesZip(
            final Path catalogDir,
            final Catalog catalog,
            final Path zipFile) throws IOException {
        if (Objects.isNull(zipFile) || Objects.isNull(catalogDir)) {
            return null;
        }
        final Set<Path> files = snippetFiles(catalogDir, catalog);
        if (files.isEmpty()) {
            return null;
        }
        final Path parent = zipFile.getParent();
        if (Objects.nonNull(parent)) {
            Files.createDirectories(parent);
        }
        final ExceptionalResponse<Path> written = zipSnippets(zipFile, catalogDir, files);
        if (written.wasError()) {
            throw new IOException("Failed to write examples zip " + zipFile);
        }
        return written.response();
    }

    private static Set<Path> snippetFiles(final Path catalogDir, final Catalog catalog) {
        final Set<Path> files = new LinkedHashSet<>();
        for (final Intent intent : catalog.intents()) {
            addSnippet(files, catalogDir, intent.snippetRef());
            if (Objects.nonNull(intent.snippets())) {
                for (final Snippet snippet : intent.snippets()) {
                    addSnippet(files, catalogDir, snippet.ref());
                }
            }
        }
        return files;
    }

    private static void addSnippet(final Set<Path> files, final Path catalogDir, final String ref) {
        if (Objects.isNull(ref) || ref.isBlank()) {
            return;
        }
        final Path file = catalogDir.resolve(ref).normalize();
        if (Files.isRegularFile(file) && file.startsWith(catalogDir)) {
            files.add(file);
        }
    }

    private static void writeFile(final Path path, final String content) throws IOException {
        final Path parent = path.getParent();
        if (Objects.nonNull(parent)) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, content);
    }

    private static ExceptionalResponse<Path> zipSnippets(
            final Path zipFile,
            final Path catalogDir,
            final Set<Path> files) {
        return ExceptionalResource.of(
                () -> new ZipOutputStream(Files.newOutputStream(zipFile)),
                zip -> {
                    for (final Path file : files) {
                        final String name = catalogDir.relativize(file).toString().replace('\\', '/');
                        zip.putNextEntry(new ZipEntry(name));
                        Files.copy(file, zip);
                        zip.closeEntry();
                    }
                    return zipFile;
                }).execute();
    }

    private static Outcome failed(
            final String message,
            final Catalog catalog,
            final List<Violation> violations) {
        return new Outcome(false, true, message, null, catalog, violations, null);
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
