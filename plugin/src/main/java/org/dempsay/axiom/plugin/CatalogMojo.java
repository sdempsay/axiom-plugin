package org.dempsay.axiom.plugin;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

import org.dempsay.axiom.model.Violation;
import org.dempsay.utils.exceptional.api.ExceptionalResponse;

/**
 * Validates {@code catalog.yaml}, stamps {@code ${project.version}}, and attaches
 * {@code classifier=agent-catalog} type {@code yaml}.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
@Mojo(name = "catalog", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class CatalogMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;

    @Parameter(property = "axiom.skip", defaultValue = "false")
    public boolean skip;

    @Parameter(defaultValue = "false")
    public boolean required;

    @Parameter(defaultValue = "true")
    public boolean stampVersion;

    @Parameter(property = "axiom.catalogFile")
    public File catalogFile;

    @Parameter(defaultValue = "${project.build.directory}/axiom/catalog.yaml")
    public File outputCatalog;

    @Parameter(defaultValue = "${project.build.directory}/axiom/report.md")
    public File outputReport;

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
        final CatalogWork.Request request = new CatalogWork.Request(
                source,
                required,
                stampVersion,
                project.getVersion(),
                outputCatalog.toPath(),
                outputReport.toPath());
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
        getLog().info(outcome.message());
    }
}
