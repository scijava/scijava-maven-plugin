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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;

/**
 * This class recursively checks a specified project, all dependencies, and
 * parent poms of any of these projects, for SNAPSHOT couplings. Any such
 * couplings are reported, and cause a {@link SnapshotException} to be thrown.
 * <p>
 * Options:
 * <ul>
 * <li>failEarly - end execution after first failure (default: false)</li>
 * <li>groupIds - an inclusive list of groupIds. Errors will only be reported
 * for projects whose groupIds are contained this list.</li>
 * <ul>
 * </p>
 *
 * @author Mark Hiner
 */
public class SnapshotFinder {

	// -- Fields --

	@SuppressWarnings("rawtypes")
	private List remoteRepositories;

	private Log log = null;

	private boolean foundSnapshot = false;

	// -- Parameters --

	private final MavenProjectBuilder projectBuilder;

	private final ArtifactRepository localRepository;

	private final Boolean failEarly;

	private final Set<String> groupIds = new HashSet<String>();

	// -- Constructor --

	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository)
	{
		this(projectBuilder, localRepository, false);
	}

	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository, final Boolean failEarly)
	{
		this(projectBuilder, localRepository, failEarly, null);
	}

// -- Public API --

	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository, final Boolean failEarly,
		@SuppressWarnings("rawtypes") final List groupIds)
	{
		this.projectBuilder = projectBuilder;
		this.localRepository = localRepository;
		this.failEarly = failEarly;

		if (groupIds != null) {
			for (int i = 0; i < groupIds.size(); i++) {
				this.groupIds.add((String) groupIds.get(i));
			}
		}
	}

	/**
	 * Recursively checks the given project for SNAPSHOT dependencies.
	 * 
	 * @throws SnapshotException If a SNAPSHOT dependency is discovered
	 */
	public void checkProject(final MavenProject project) throws SnapshotException
	{
		// Set the remote repository list by using the base project
		remoteRepositories = project.getRemoteArtifactRepositories();

		final Set<String> parentGavs = new HashSet<String>();
		final String projectGav = gav(project);
		parentGavs.add(projectGav);

		checkProjectHelper(project, "\t" + projectGav, parentGavs);

		if (foundSnapshot) {
			throw new SnapshotException(
				"Found one or more SNAPSHOT couplings. See error log for more information.");
		}
	}

	/**
	 * Sets the {@link Log} for this instance.
	 */
	public void setLog(final Log log) {
		this.log = log;
	}

//-- Helper methods --

	/**
	 * {@link Log#debug} helper.
	 */
	private void debug(final String message) {
		if (log != null) {
			log.debug(message);
		}
	}

	/**
	 * {@link Log#error} helper.
	 */
	private void error(final String message) {
		if (log != null) {
			log.error(message);
		}
	}

	/**
	 * Recursively checks the parent pom hierarchy and each dependency of the
	 * given project for SNAPSHOT dependencies.
	 */
	private void checkProjectHelper(final MavenProject project,
		final String path, final Set<String> parentGavs) throws SnapshotException
	{
		// Check the parent hierarchy
		checkParent(project, path, parentGavs);

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
					projectBuilder.buildFromRepository(a, remoteRepositories,
						localRepository);

				// Check the processedGavs set to see if we have already processed this
				// particular gav - avoids infinite recursion
				final String depGav = gav(dep);

				if (!parentGavs.contains(depGav)) {
					debug("Checking gav: " + depGav);
					// Mark this gav as processed
					parentGavs.add(depGav);
					// Generate a path for this dependency
					final String depPath = makePath(path, dep);
					debug("checking pom:\n" + depPath);
					// Check for a SNAPSHOT version
					if (dep.getVersion().contains(Artifact.SNAPSHOT_VERSION) &&
						checkGroupId(dep))
					{
						setFailure("Found SNAPSHOT version:\n" + depPath);
					}
					// Recursive call
					checkProjectHelper(dep, depPath, childGavs(parentGavs, depGav));
				}
			}
			catch (ProjectBuildingException e) {
				if (checkGroupId(d.getGroupId())) {
					// Report if the dependency pom could not be built
					error("Could not resolve dependency: " + d + " of path:\n" + path);
				}
			}
		}
	}

	/**
	 * Recursively checks the parent pom looking for SNAPSHOT dependencies.
	 */
	private void checkParent(final MavenProject pom, final String path,
		final Set<String> parentGavs) throws SnapshotException
	{
		// If the current pom has no parent, we're done
		if (pom.hasParent()) {
			final MavenProject parent = pom.getParent();
			final String nextGav = gav(parent);

			// Avoid infinite recursion
			if (!parentGavs.contains(nextGav)) {
				// Mark this gav as processed
				// Generate a path for this parent
				final String parentPath = makePath(path, parent);
				debug("checking parent:\n" + parentPath);

				// Check if the parent pom is a SNAPSHOT
				if (parent.getVersion().contains(Artifact.SNAPSHOT_VERSION) &&
					checkGroupId(parent))
				{
					setFailure("Found SNAPSHOT parent:\n" + parentPath);
				}

				// Recrusive call
				checkParent(parent, parentPath, childGavs(parentGavs, nextGav));
			}
		}
	}

	/**
	 * @return A new Set, created from the union of the given parentGavs and child
	 */
	private Set<String>
		childGavs(final Set<String> parentGavs, final String childGav)
	{
		final Set<String> childGavs = new HashSet<String>(parentGavs);
		childGavs.add(childGav);
		return childGavs;
	}

	/**
	 * @return True iff no set of groupIds was specified, or the specified set
	 *         contains the groupId of the given project.
	 */
	private boolean checkGroupId(final MavenProject project) {
		return checkGroupId(project.getGroupId());
	}

	/**
	 * @return True iff no set of groupIds was specified, or the specified set
	 *         contains the given groupId.
	 */
	private boolean checkGroupId(final String groupId) {
		return groupIds.isEmpty() || groupIds.contains(groupId);
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
	private void setFailure(final String message) throws SnapshotException {
		if (failEarly) {
			throw new SnapshotException(message);
		}
		error(message);
		foundSnapshot = true;
	}
}
