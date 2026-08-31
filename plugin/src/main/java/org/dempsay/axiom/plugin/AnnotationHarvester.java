package org.dempsay.axiom.plugin;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.dempsay.axiom.annotations.AgentCapability;
import org.dempsay.axiom.model.AntiPattern;
import org.dempsay.axiom.model.Catalog;
import org.dempsay.axiom.model.Intent;
import org.dempsay.axiom.model.Severity;
import org.dempsay.axiom.model.Violation;
import org.dempsay.utils.exceptional.api.ExceptionalResource;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Optional {@link AgentCapability} harvest. YAML wins on field conflict; harvest never
 * invents {@code snippetRef}.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
public final class AnnotationHarvester {

    private AnnotationHarvester() { }

    /**
     * Harvested fields from one annotation (no snippet).
     *
     * @param id intent id
     * @param title title
     * @param severity required or available
     * @param triggers lookup hints
     * @param antiPatternPatterns vernacular regexes
     */
    public record HarvestedIntent(
            String id,
            String title,
            Severity severity,
            List<String> triggers,
            List<String> antiPatternPatterns) {
    }

    /**
     * Result of merging harvest into a YAML catalog.
     *
     * @param catalog merged catalog when valid
     * @param violations empty when merge succeeded
     */
    public record MergeResult(Catalog catalog, List<Violation> violations) {

        /**
         * @return true when there are no violations
         */
        public boolean isValid() {
            return violations.isEmpty();
        }
    }

    /**
     * Scans {@code classesDir} for {@link AgentCapability} on types and methods.
     *
     * @param classesDir compiled classes root
     * @param loader class loader that can see those classes and the annotation
     * @return harvested intents, or I/O failure
     */
    public static ExceptionalResponse<List<HarvestedIntent>> scan(
            final Path classesDir,
            final ClassLoader loader) {
        Objects.requireNonNull(classesDir, "classesDir");
        Objects.requireNonNull(loader, "loader");
        if (!Files.isDirectory(classesDir)) {
            return ExceptionalResponse.success(List.of());
        }
        return ExceptionalResource.of(
                () -> Files.walk(classesDir),
                stream -> collect(stream, classesDir, loader)).execute();
    }

    /**
     * Merges harvested rows into {@code catalog}. YAML wins on field conflict. An id that
     * exists only in harvest fails for missing {@code snippetRef}.
     *
     * @param catalog YAML catalog
     * @param harvested harvested rows
     * @return merged catalog or violations
     */
    public static MergeResult merge(final Catalog catalog, final List<HarvestedIntent> harvested) {
        Objects.requireNonNull(catalog, "catalog");
        if (Objects.isNull(harvested) || harvested.isEmpty()) {
            return new MergeResult(catalog, List.of());
        }
        final Map<String, Intent> byId = new LinkedHashMap<>();
        for (final Intent intent : catalog.intents()) {
            byId.put(intent.id(), intent);
        }
        final List<Violation> violations = new ArrayList<>();
        for (final HarvestedIntent harvestedIntent : harvested) {
            final Intent yaml = byId.get(harvestedIntent.id());
            if (Objects.isNull(yaml)) {
                violations.add(new Violation(
                        harvestedIntent.id(),
                        "snippetRef",
                        "harvest does not invent snippets; add snippetRef in catalog.yaml"));
                continue;
            }
            byId.put(harvestedIntent.id(), mergeIntent(yaml, harvestedIntent));
        }
        if (!violations.isEmpty()) {
            return new MergeResult(catalog, List.copyOf(violations));
        }
        return new MergeResult(catalog.withIntents(List.copyOf(byId.values())), List.of());
    }

    private static List<HarvestedIntent> collect(
            final Stream<Path> stream,
            final Path classesDir,
            final ClassLoader loader) {
        final List<HarvestedIntent> found = new ArrayList<>();
        final List<Path> classFiles = stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(".class"))
                .toList();
        for (final Path classFile : classFiles) {
            final String binaryName = binaryName(classesDir, classFile);
            if (binaryName.endsWith("package-info") || binaryName.endsWith("module-info")) {
                continue;
            }
            final ExceptionalResponse<Object> loaded = ExceptionalSupplier.of(
                    () -> (Object) Class.forName(binaryName, false, loader)).execute();
            if (loaded.wasError()) {
                continue;
            }
            addCapabilities((Class<?>) loaded.response(), found);
        }
        return List.copyOf(found);
    }

    private static void addCapabilities(final Class<?> type, final List<HarvestedIntent> found) {
        final AgentCapability onType = type.getAnnotation(AgentCapability.class);
        if (Objects.nonNull(onType)) {
            found.add(fromAnnotation(onType));
        }
        for (final Method method : type.getDeclaredMethods()) {
            final AgentCapability onMethod = method.getAnnotation(AgentCapability.class);
            if (Objects.nonNull(onMethod)) {
                found.add(fromAnnotation(onMethod));
            }
        }
    }

    private static HarvestedIntent fromAnnotation(final AgentCapability capability) {
        return new HarvestedIntent(
                capability.id(),
                capability.title(),
                capability.severity() == org.dempsay.axiom.annotations.Severity.REQUIRED
                        ? Severity.REQUIRED
                        : Severity.AVAILABLE,
                List.of(capability.triggers()),
                List.of(capability.antiPatterns()));
    }

    private static String binaryName(final Path classesDir, final Path classFile) {
        final String relative = classesDir.relativize(classFile).toString().replace('\\', '/');
        return relative.substring(0, relative.length() - ".class".length()).replace('/', '.');
    }

    private static Intent mergeIntent(final Intent yaml, final HarvestedIntent harvested) {
        return new Intent(
                yaml.id(),
                firstNonBlank(yaml.title(), harvested.title()),
                Objects.nonNull(yaml.severity()) ? yaml.severity() : harvested.severity(),
                yaml.language(),
                yaml.blessed(),
                yaml.snippetRef(),
                yaml.snippets(),
                firstNonEmpty(yaml.triggers(), harvested.triggers()),
                firstNonEmpty(yaml.antiPatterns(), toAntiPatterns(harvested.antiPatternPatterns())),
                yaml.never(),
                yaml.docs(),
                yaml.supersedes());
    }

    private static String firstNonBlank(final String yaml, final String harvested) {
        if (Objects.nonNull(yaml) && !yaml.isBlank()) {
            return yaml;
        }
        return harvested;
    }

    private static <T> List<T> firstNonEmpty(final List<T> yaml, final List<T> harvested) {
        if (Objects.nonNull(yaml) && !yaml.isEmpty()) {
            return yaml;
        }
        return harvested;
    }

    private static List<AntiPattern> toAntiPatterns(final List<String> patterns) {
        if (Objects.isNull(patterns) || patterns.isEmpty()) {
            return List.of();
        }
        final List<AntiPattern> antiPatterns = new ArrayList<>();
        for (final String pattern : patterns) {
            antiPatterns.add(new AntiPattern(pattern, "Avoid vernacular form: " + pattern));
        }
        return List.copyOf(antiPatterns);
    }
}
