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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;

import java.io.File;
import java.io.IOException;

@Mojo(name = "build-environment", requiresProject = true, requiresOnline = true)
public class BuildEnvironmentMojo extends AbstractCopyJarsMojo {

	/**
	 * Project
	 */
	@Parameter(defaultValue = "${project}", required=true, readonly = true)
	private MavenProject project;

	/**
	 * Use this parameter to build the environment for a specific platform
	 * Possible values: "linux32", "linux64, "win32", "win64",
	 */
	@Parameter(property = "platform")
	private String platform;

	/**
	 * To build the environment without adding a JRE, set this to false
	 */
	@Parameter(property = "downloadJRE")
	private boolean downloadJRE = true;

	/**
	 * Use this paramter to add specific JRE version ot environment.
	 * By default, the platform specific JRE on https://downloads.imagej.net/java/ will be used.
	 * Possible values: any subfolder of https://downloads.imagej.net/java/ (e.g. "1.8.0_66", "1.8.0_172")
	 */
	@Parameter(property = "jreVersion")
	private String jreVersion;

	/**
	 * The dependency resolver to.
	 */
	@Component
	private DependencyResolver dependencyResolver;

	private DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();

	private File appDir;

	@Override
	public void execute() throws MojoExecutionException {

		if(platform == null) platform = getPlatform();

		// Keep backward compatibility
		handleBackwardCompatibility();

		if (appDirectory == null) {
			if (hasIJ1Dependency(project)) getLog().info(
				"Property '" + appDirectoryProperty + "' unset; Skipping build-environment");
			return;
		}
		final String interpolated = interpolate(appDirectory, project, session);
		appDir = new File(interpolated);

		if (appSubdirectory == null) {
			getLog().info("No property name for the " + appSubdirectoryProperty +
				" directory location was specified; Installing in default location");
		}

		if (!appDir.isDirectory()) {
			getLog().warn(
				"'" + appDirectory + "'" +
					(interpolated.equals(appDirectory) ? "" : " (" + appDirectory + ")") +
					" is not an SciJava application directory; Skipping build-environment");
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
						if (!ignoreDependencies)
							installArtifact(result.getArtifact(), appDir, false, deleteOtherVersionsPolicy);
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

		if(downloadJRE) {
			try {
				JavaDownloader.downloadJava(appDir, platform, jreVersion);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private static String getPlatform() {
		final boolean is64bit =
				System.getProperty("os.arch", "").contains("64");
		final String osName = System.getProperty("os.name", "<unknown>");
		if (osName.equals("Linux")) return "linux" + (is64bit ? "64" : "32");
		if (osName.equals("Mac OS X")) return "macosx";
		if (osName.startsWith("Windows")) return "win" + (is64bit ? "64" : "32");
		// System.err.println("Unknown platform: " + osName);
		return osName.toLowerCase();
	}
}
