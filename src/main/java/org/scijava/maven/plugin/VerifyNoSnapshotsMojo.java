/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 Board of Regents of the University of
 * Wisconsin-Madison.
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

package org.scijava.maven.plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

/**
 * This plugin fills in gaps in the <a
 * href="http://maven.apache.org/enforcer/maven-enforcer-plugin/">maven enforcer
 * plugin's</a> SNAPSHOT dependency checking. For example, the enforcer plugin
 * will not find parent pom SNAPSHOTs if the direct parent of is a release
 * version (that is, it trusts release versions to be sane).
 * <p>
 * This plugin recursively checks the current project, all dependencies, and
 * parent poms of any of these projects, for SNAPSHOT couplings. Any such
 * couplings are reported, and cause the goal to fail.
 * </p>
 * <p>
 * Options:
 * <ul>
 * <li>failEarly - end execution after first failure (default: false)</li>
 * <ul>
 * </p>
 *
 * @goal verify-no-snapshots
 * @phase validate
 */
public class VerifyNoSnapshotsMojo extends AbstractMojo {

	// -- Fields --

	private Set<String> processedGavs = new HashSet<String>();

	private boolean foundSnapshot = false;

	@SuppressWarnings("rawtypes")
	private List remoteRepositories;

	// -- Parameters --

	/** @parameter default-value="${project}" */
	private org.apache.maven.project.MavenProject mavenProject;

	/** @component role = "org.apache.maven.project.MavenProjectBuilder" */
	private MavenProjectBuilder m_projectBuilder;

	/** @parameter expression="${localRepository}" */
	private ArtifactRepository m_localRepository;

	/** @parameter property="failEarly" default-value=false */
	private Boolean failEarly;

	// -- Mojo API Methods --

	/**
	 * Entry point for mojo execution
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		// Set the remote repository list by using the base project
		remoteRepositories = mavenProject.getRemoteArtifactRepositories();

		// Enter recursive project checking
		checkProject(mavenProject);

		// Failure at the end of execution
		if (foundSnapshot) throw new MojoFailureException(
			"Found one or more SNAPSHOT couplings. See error log for more information.");
	}

	// -- Helper methods --

	/**
	 * Seeds {@link #checkProjectHelper(MavenProject, String)} recrusion with the
	 * provided project, and a default path of the project's gav.
	 */
	private void checkProject(final MavenProject project)
		throws MojoFailureException
	{
		checkProjectHelper(project, "\t" + gav(project));
	}

	/**
	 * Recursively checks the parent pom hierarchy and each dependency of the
	 * given project for SNAPSHOT dependencies.
	 */
	private void
		checkProjectHelper(final MavenProject project, final String path)
			throws MojoFailureException
	{
		// Check the parent hierarchy
		checkParent(project, path);

		@SuppressWarnings("unchecked")
		final List<Dependency> dependencies = project.getDependencies();

		// iterate over each dependency
		for (final Dependency d : dependencies) {
			try {
				// Convert the dependency gav to a MavenProject object (pom)
				final Artifact a =
					new DefaultArtifact(d.getGroupId(), d.getArtifactId(), VersionRange
						.createFromVersion(d.getVersion()), d.getScope(), d.getType(), d
						.getClassifier(), project.getArtifact().getArtifactHandler());
				final MavenProject dep =
					m_projectBuilder.buildFromRepository(a, remoteRepositories,
						m_localRepository);

				// Check the processedGavs set to see if we have already processed this
				// particular gav - avoids infinite recursion
				final String gav = gav(dep);
				if (!processedGavs.contains(gav)) {
					// Mark this gav as processed
					processedGavs.add(gav);
					// Generate a path for this dependency
					final String depPath = makePath(path, dep);
					getLog().debug("checking pom:\n" + depPath);
					// Check for a SNAPSHOT version
					if (dep.getVersion().contains(Artifact.SNAPSHOT_VERSION)) {
						setFailure("Found SNAPSHOT version:\n" + depPath);
					}
					// Recursive call
					checkProjectHelper(dep, depPath);
				}
			}
			catch (ProjectBuildingException e) {
				// Report if the dependency pom could not be built
				getLog().error(
					"Could not resolve dependency: " + d + " of path:\n" + path);
			}
		}
	}

	/**
	 * Recursively checks the parent pom looking for SNAPSHOT dependencies.
	 */
	private void checkParent(final MavenProject pom, final String path)
		throws MojoFailureException
	{
		// If the current pom has no parent, we're done
		if (pom.hasParent()) {
			final MavenProject parent = pom.getParent();

			// Check the processedGavs set to see if we have already processed this
			// particular gav - avoids infinite recursion
			if (!processedGavs.contains(gav(parent))) {
				// Mark this gav as processed
				processedGavs.add(gav(parent));
				// Generate a path for this parent
				final String parentPath = makePath(path, parent);
				getLog().debug("checking parent:\n" + parentPath);

				// Check if the current pom is a SNAPSHOT
				if (parent.getVersion().contains(Artifact.SNAPSHOT_VERSION)) {
					setFailure("Found SNAPSHOT parent:\n" + parentPath);
				}

				// Recrusive call
				checkParent(parent, parentPath);
			}
		}
	}

	/**
	 * Prepends the given project's gav, in a separate line, to the given path
	 */
	private String makePath(final String path, final MavenProject project) {
		String newPath = "\t" + gav(project) + "\n" + path;
		return newPath;
	}

	/**
	 * Returns the GAV (groupId:artifactId:version) for the given project
	 */
	private String gav(final MavenProject project) {
		return project.getGroupId() + ":" + project.getArtifactId() + ":" +
			project.getVersion();
	}

	/**
	 * Prints the given message to the error log and marks this mojo execution as
	 * a failure.
	 */
	private void setFailure(final String message) throws MojoFailureException {
		if (failEarly) {
			throw new MojoFailureException(message);
		}
		getLog().error(message);
		foundSnapshot = true;
	}
}
