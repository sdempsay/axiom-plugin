package org.dempsay.axiom.plugin;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.dempsay.axiom.model.Violation;
import org.dempsay.axiom.plugin.AnnotationHarvester.HarvestedIntent;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;
import org.dempsay.utils.exceptional.api.ExceptionalSupplier;

/**
 * Validates {@code catalog.yaml}, stamps {@code ${project.version}}, and attaches
 * {@code classifier=agent-catalog} type {@code yaml}.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 1.0.0
 */
@Mojo(
        name = "catalog",
        defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class CatalogMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(property = "axiom.skip", defaultValue = "false")
    public boolean skip;

    @Parameter(defaultValue = "false")
    public boolean required;

    @Parameter(defaultValue = "true")
    public boolean stampVersion;

    @Parameter(defaultValue = "true")
    public boolean embedInJar;

    @Parameter(defaultValue = "false")
    public boolean harvestAnnotations;

    @Parameter(property = "axiom.catalogFile")
    public File catalogFile;

    @Parameter(defaultValue = "${project.build.directory}/axiom/catalog.yaml")
    public File outputCatalog;

    @Parameter(defaultValue = "${project.build.directory}/axiom/report.md")
    public File outputReport;

    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/axiom/catalog.yaml")
    public File embedFile;

    @Parameter(defaultValue = "${project.build.directory}/axiom/agent-catalog-examples.zip")
    public File examplesZip;

    @Component
    public MavenProjectHelper projectHelper;

    /**
     * Runs catalog validate / stamp / attach.
     *
     * @throws MojoExecutionException on I/O failure
     * @throws MojoFailureException when the catalog is required and missing, or invalid
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("axiom.skip=true, skipping catalog");
            return;
        }
        final Path source = Objects.nonNull(catalogFile)
                ? catalogFile.toPath()
                : project.getBasedir().toPath().resolve("src/main/resources/agent-catalog/catalog.yaml");
        final List<HarvestedIntent> harvested = harvestAnnotations ? scanHarvest() : null;
        final CatalogWork.Request request = new CatalogWork.Request(
                source,
                required,
                stampVersion,
                project.getVersion(),
                outputCatalog.toPath(),
                outputReport.toPath(),
                embedInJar ? embedFile.toPath() : null,
                harvested,
                examplesZip.toPath());
        final ExceptionalResponse<CatalogWork.Outcome> response = CatalogWork.run(request);
        if (response.wasError()) {
            throw new MojoExecutionException("Failed to process catalog " + source);
        }
        final CatalogWork.Outcome outcome = response.response();
        if (outcome.skipped()) {
            getLog().info(outcome.message());
            return;
        }
        if (outcome.failed()) {
            for (final Violation violation : outcome.violations()) {
                getLog().error(violation.toString());
            }
            throw new MojoFailureException(outcome.message());
        }
        projectHelper.attachArtifact(project, "yaml", "agent-catalog", outcome.stampedCatalog().toFile());
        if (Objects.nonNull(outcome.examplesZip())) {
            projectHelper.attachArtifact(project, "zip", "agent-catalog-examples", outcome.examplesZip().toFile());
        }
        getLog().info(outcome.message());
    }

    private List<HarvestedIntent> scanHarvest() throws MojoExecutionException {
        final Path classes = Path.of(project.getBuild().getOutputDirectory());
        final ExceptionalResponse<URLClassLoader> loader = ExceptionalSupplier.of(() -> {
            final List<URL> urls = new ArrayList<>();
            urls.add(classes.toUri().toURL());
            for (final String element : project.getCompileClasspathElements()) {
                urls.add(Path.of(element).toUri().toURL());
            }
            return new URLClassLoader(urls.toArray(URL[]::new), getClass().getClassLoader());
        }).execute();
        if (loader.wasError()) {
            throw new MojoExecutionException("Failed to scan classes for @AgentCapability");
        }
        final ExceptionalResponse<List<HarvestedIntent>> scanned = AnnotationHarvester.scan(
                classes, loader.response());
        if (scanned.wasError()) {
            throw new MojoExecutionException("Failed to scan classes for @AgentCapability");
        }
        return scanned.response();
    }
}
