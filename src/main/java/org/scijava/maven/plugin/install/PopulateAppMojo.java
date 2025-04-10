/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2025 SciJava developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

package org.scijava.maven.plugin.install;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

/**
 * Copies .jar artifacts and their dependencies into a SciJava application
 * directory structure.
 * <p>
 * ImageJ 1.x plugins (identified by containing a plugins.config file) get
 * copied to the plugins/ subdirectory and all other .jar files to jars/.
 * However, you can override this decision by setting the
 * scijava.app.subdirectory property to a specific subdirectory. It expects the
 * location of the SciJava application directory to be specified in the
 * scijava.app.directory property (which can be set on the Maven command-line).
 * If said property is not set, the populate-app goal is skipped.
 * </p>
 * 
 * @author Johannes Schindelin
 * @author Stefan Helfrich
 * @author Philipp Hanslovsky
 */
@Mojo(name = "populate-app", requiresProject = true, requiresOnline = true)
public class PopulateAppMojo extends AbstractInstallMojo {

	/**
	 * Project
	 */
	@Parameter(defaultValue = "${project}", required=true, readonly = true)
	private MavenProject project;

	/**
	 * If this option is set to <code>true</code>, optional dependencies will not be installed.
	 */
	@Parameter(property = IGNORE_OPTIONAL_DEPENDENCIES_PROPERTY, defaultValue = "false")
	private boolean ignoreOptionalDependencies;

	/**
	 * The dependency resolver to.
	 */
	@Component
	private DependencyResolver dependencyResolver;

	private DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();

	private File appDir;

	@Component
	private ProjectBuilder mavenProjectBuilder;

	@Override
	public void execute() throws MojoExecutionException {
		if (appDirectory == null) {
			getLog().info("Property '" + APP_DIRECTORY_PROPERTY +
				"' unset; skipping populate-app.");
			return;
		}
		final String interpolated = interpolate(appDirectory, project);
		appDir = new File(interpolated);

		if (appSubdirectory == null) {
			getLog().info("Property " + APP_SUBDIRECTORY_PROPERTY +
				" unset; installing in default location.");
		}

		if (!appDir.isDirectory()) {
			getLog().warn(
				"'" + appDirectory + "'" +
					(interpolated.equals(appDirectory) ? "" : " (" + appDirectory + ")") +
					" is not a SciJava application directory; skipping populate-app.");
			return;
		}

		// Initialize coordinate for resolving
		coordinate.setGroupId(project.getGroupId());
		coordinate.setArtifactId(project.getArtifactId());
		coordinate.setVersion(project.getVersion());
		coordinate.setType(project.getPackaging());

		try {
			final TransformableFilter scopeAndNotOptionalFilter =
					makeTransformableFilterDefaultExclusions(ignoreOptionalDependencies);

			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject( project );

			Iterable<ArtifactResult> resolveDependencies = dependencyResolver
					.resolveDependencies(buildingRequest, coordinate, scopeAndNotOptionalFilter);
				for (ArtifactResult result : resolveDependencies) {
					Artifact artifact = result.getArtifact();
					try {
						if (project.getArtifact().equals(artifact)) {
							installArtifact(artifact, appDir, appSubdirectory, false,
								deleteOtherVersionsPolicy);
							continue;
						}
						// Resolution of the subdirectory for dependencies is handled in installArtifact
						if (!ignoreDependencies) {
							String subdir = getAppSubDirectoryProperty(artifact);

							installArtifact(artifact, appDir, subdir, false, deleteOtherVersionsPolicy);
						}
					}
					catch (IOException e) {
						throw new MojoExecutionException("Couldn't download artifact " +
							result.getArtifact() + ": " + e.getMessage(), e);
					}
				}
		}
		catch (DependencyResolverException e) {
			throw new MojoExecutionException(
				"Couldn't resolve dependencies for artifact: " + e.getMessage(), e);
		}
	}

	private String getAppSubDirectoryProperty(Artifact artifact) {
		try {
			ProjectBuildingResult build = mavenProjectBuilder.build(artifact, //
				session.getProjectBuildingRequest());
			Properties properties = build.getProject().getProperties();
			String subdir = (String) properties.get(APP_SUBDIRECTORY_PROPERTY);
			return subdir;
		}
		catch (ProjectBuildingException e) {
			// TODO: log.debug( "Couldn't determine " + APP_SUBDIRECTORY_PROPERTY + " for " + artifact, e );
			return null;
		}
	}
}
