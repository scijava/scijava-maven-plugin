/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2018 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, Max Planck
 * Institute of Molecular Cell Biology and Genetics, and KNIME GmbH.
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
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

/**
 * Copies .jar artifacts and their dependencies into a SciJava application
 * directory structure.
 * 
 * ImageJ 1.x plugins (identified by containing a plugins.config file) get
 * copied to the plugins/ subdirectory and all other .jar files to jars/.
 * However, you can override this decision by setting the scijava.app.subdirectory
 * property to a specific subdirectory. It expects the location of the SciJava
 * application directory to be specified in the scijava.app.directory property
 * (which can be set on the Maven command-line). If said property is not set,
 * the copy-jars goal is skipped.
 * 
 * @author Johannes Schindelin
 * @author Stefan Helfrich
 */
@Mojo(name = "copy-jars", requiresProject = true, requiresOnline = true)
public class CopyJarsMojo extends AbstractCopyJarsMojo {

	/**
	 * Project
	 */
	@Parameter(defaultValue = "${project}", required=true, readonly = true)
	private MavenProject project;

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
		// Keep backward compatibility
		handleBackwardCompatibility();

		if (appDirectory == null) {
			if (hasIJ1Dependency(project)) getLog().info(
				"Property '" + APP_DIRECTORY_PROPERTY + "' unset; Skipping copy-jars");
			return;
		}
		final String interpolated = interpolate(appDirectory, project);
		appDir = new File(interpolated);

		if (appSubdirectory == null) {
			getLog().info("No property name for the " + APP_SUBDIRECTORY_PROPERTY +
				" directory location was specified; Installing in default location");
		}

		if (!appDir.isDirectory()) {
			getLog().warn(
				"'" + appDirectory + "'" +
					(interpolated.equals(appDirectory) ? "" : " (" + appDirectory + ")") +
					" is not an SciJava application directory; Skipping copy-jars");
			return;
		}

		// Initialize coordinate for resolving
		coordinate.setGroupId(project.getGroupId());
		coordinate.setArtifactId(project.getArtifactId());
		coordinate.setVersion(project.getVersion());
		coordinate.setType(project.getPackaging());

		try {
			TransformableFilter scopeFilter = ScopeFilter.excluding("system", "provided", "test");

			ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setProject( project );

			Iterable<ArtifactResult> resolveDependencies = dependencyResolver
					.resolveDependencies(buildingRequest, coordinate, scopeFilter);
				for (ArtifactResult result : resolveDependencies) {
					try {
						if (project.getArtifact().equals(result.getArtifact())) {
							installArtifact(result.getArtifact(), appDir, appSubdirectory, false,
								deleteOtherVersionsPolicy);
							continue;
						}
						// Resolution of the subdirectory for dependencies is handled in installArtifact
						if (!ignoreDependencies) {
							ProjectBuildingResult build = mavenProjectBuilder.build(result.getArtifact(), session.getProjectBuildingRequest());
							Properties properties = build.getProject().getProperties();
							String subdir = (String) properties.get( APP_SUBDIRECTORY_PROPERTY );

							installArtifact(result.getArtifact(), appDir, subdir, false, deleteOtherVersionsPolicy);
						}
					}
					catch (IOException e) {
						throw new MojoExecutionException("Couldn't download artifact " +
							result.getArtifact() + ": " + e.getMessage(), e);
					}
					catch ( ProjectBuildingException e )
					{
						throw new MojoExecutionException( "Couldn't determine " +
							APP_SUBDIRECTORY_PROPERTY + " for " + result.getArtifact(), e );
					}
				}
		}
		catch (DependencyResolverException e) {
			throw new MojoExecutionException(
				"Couldn't resolve dependencies for artifact: " + e.getMessage(), e);
		}
	}
}
