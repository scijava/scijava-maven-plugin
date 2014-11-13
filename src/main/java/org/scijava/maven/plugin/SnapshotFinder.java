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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 * <li>verbose - prints full inheritance paths to all failures (default: false)</li>
 * <li>failEarly - end execution after first failure (default: false)</li>
 * <li>groupIds - an inclusive list of groupIds. Errors will only be reported
 * for projects whose groupIds are contained this list.</li>
 * <ul>
 * </p>
 *
 * @author Mark Hiner
 */
public class SnapshotFinder {

	// -- Constants --

	private static final String PARENT_FLAG = "BAD PARENT";

	// -- Fields --

	@SuppressWarnings("rawtypes")
	private List remoteRepositories;

	private Log log = null;

	private boolean foundSnapshot = false;

	private Map<String, Set<String>> badGavs =
		new LinkedHashMap<String, Set<String>>();

	// -- Parameters --

	private final MavenProjectBuilder projectBuilder;

	private final ArtifactRepository localRepository;

	private final Boolean failEarly;

	private final Boolean verbose;

	private final Set<String> groupIds = new HashSet<String>();

	// -- Constructors --

	/**
	 * Minimal constructor. Sets:
	 * <ul>
	 * <li>failEarly = false</li>
	 * <li>verbose = false</li>
	 * <li>groupIds = *</li>
	 * </ul>
	 */
	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository)
	{
		this(projectBuilder, localRepository, false);
	}

	/**
	 * Sets:
	 * <ul>
	 * <li>verbose = false</li>
	 * <li>groupIds = *</li>
	 * </ul>
	 */
	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository, final Boolean failEarly)
	{
		this(projectBuilder, localRepository, failEarly, false);
	}

	/**
	 * Sets:
	 * <ul>
	 * <li>groupIds = *</li>
	 * </ul>
	 */
	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository, final Boolean failEarly,
		final Boolean verbose)
	{
		this(projectBuilder, localRepository, failEarly, verbose, null);
	}

	/**
	 * Fully-specified constructor.
	 *
	 * @param projectBuilder - {@link MavenProjectBuilder} reference
	 * @param localRepository - {@link ArtifactRepository} corresponding to the
	 *          local repo. Used to resolve dependencies.
	 * @param failEarly - If true, projects will fail after finding any SNAPSHOT
	 *          problems.
	 * @param verbose - If true, full failing dependency paths will be printed.
	 * @param groupIds - List of zero or more groupIds. If any are specified,
	 *          problems will only be reported if they are of one of these
	 *          groupIds.
	 */
	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository, final Boolean failEarly,
		final Boolean verbose, @SuppressWarnings("rawtypes") final List groupIds)
	{
		this.projectBuilder = projectBuilder;
		this.localRepository = localRepository;
		this.failEarly = failEarly;
		this.verbose = verbose;

		if (groupIds != null) {
			for (int i = 0; i < groupIds.size(); i++) {
				this.groupIds.add((String) groupIds.get(i));
			}
		}
	}

// -- Public API --

	/**
	 * Recursively checks the given project for SNAPSHOT dependencies.
	 * 
	 * @param project - Base {@link MavenProject} (pom) to check.
	 * @throws SnapshotException If a SNAPSHOT dependency is discovered
	 */
	public void checkProject(final MavenProject project) throws SnapshotException
	{
		// Set the remote repository list by using the base project
		remoteRepositories = project.getRemoteArtifactRepositories();

		// Initialize data structures
		final Set<String> parentGavs = new HashSet<String>();
		final String projectGav = gav(project);
		parentGavs.add(projectGav);

		// enter recursion
		checkProjectHelper(project, "\t" + projectGav, parentGavs, null);

		// print failing messages
		if (foundSnapshot) {
			// If not verbose, all reporting occurs at the end.
			if (!verbose) {
				String errorMessage =
					"The following direct dependencies may cause unreproducible builds:\n";

				for (final String directDep : badGavs.keySet()) {
					errorMessage += "\n" + directDep;
					for (final String dep : badGavs.get(directDep)) {
						errorMessage += "\n\t" + dep;
					}
					errorMessage += "\n";
				}

				errorMessage += "\nFor full inheritance trees, run with verbose flag.";

				error(errorMessage);
			}

			// Calling code can catch this SnapshotException and re-throw as needed
			// (e.g. for Mojos or Enforcer rules)
			throw new SnapshotException(
				"Found one or more SNAPSHOT couplings. See error log for more information.");
		}
	}

	/**
	 * Sets the {@link Log} for this instance. This is an optional field that will
	 * be used to report findings, but will not affect success or failure of a
	 * {@link #checkProject(MavenProject)} execution.
	 */
	public void setLog(final Log log) {
		this.log = log;
	}

//-- Helper methods --

	/**
	 * {@link Log#debug} helper.
	 *
	 * @param message - Message to send to attached {@link Log}'s debug stream.
	 */
	private void debug(final String message) {
		if (log != null) {
			log.debug(message);
		}
	}

	/**
	 * {@link Log#error} helper.
	 *
	 * @param message - Message to send to attached {@link Log}'s error stream.
	 */
	private void error(final String message) {
		if (log != null) {
			log.error(message);
		}
	}

	/**
	 * Recursively checks the parent pom hierarchy and each dependency of the
	 * given project for SNAPSHOT dependencies.
	 *
	 * @param project Maven pom to check
	 * @param path A formatted path (e.g. 1 gav per line) representing the
	 *          hierarchy that was followed to this pom
	 * @param parentGavs The set of all gavs which inherit from the current
	 *          project. Used to avoid infinite recursion.
	 * @param directDepGav Should be null for the first iteration. This is used to
	 *          track the current direct dependency of the original base pom. For
	 *          non-verbose execution, errors are reported per-direct dependency.
	 * @throws SnapshotException If a SNAPSHOT dependency is discovered and this
	 *           SnapshotFinder is supposed to fail fast.
	 */
	private void checkProjectHelper(final MavenProject project,
		final String path, final Set<String> parentGavs, final String directDepGav)
		throws SnapshotException
	{
		// Check the parent hierarchy
		checkParent(project, path, parentGavs, directDepGav);

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

				final String depGav = gav(dep);

				// Avoid infinite recursion
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
						setFailure("Found SNAPSHOT version:\n", path, depGav, directDepGav);
					}

					// Recursive call. directDepGav is null only at the top level.
					if (directDepGav == null) {
						checkProjectHelper(dep, depPath, childGavs(parentGavs, depGav),
							depGav);
					}
					else {
						checkProjectHelper(dep, depPath, childGavs(parentGavs, depGav),
							directDepGav);
					}
				}
			}
			catch (ProjectBuildingException e) {
				if (checkGroupId(d.getGroupId())) {
					if (verbose) {
						// Report if the dependency pom could not be built
						// This will happen commonly with dependencies declared with version
						// ranges
						error("Could not resolve dependency: " + d + " of path:\n" + path);
					}
					else {
						flagProblem(directDepGav, gav(d));
					}
				}
			}
		}
	}

	/**
	 * Recursively checks the parent pom looking for SNAPSHOT dependencies.
	 *
	 * @param pom Maven project whose parent to check
	 * @param path A formatted path (e.g. 1 gav per line) representing the
	 *          hierarchy that was followed to this pom
	 * @param parentGavs The set of all gavs which inherit from the current
	 *          project. Used to avoid infinite recursion.
	 * @param directDepGav The current direct dependency of the original base pom.
	 *          For non-verbose execution, errors are reported per-direct
	 *          dependency.
	 * @throws SnapshotException If a SNAPSHOT dependency is discovered and this
	 *           SnapshotFinder is supposed to fail fast.
	 */
	private void checkParent(final MavenProject pom, final String path,
		final Set<String> parentGavs, final String directDepGav)
		throws SnapshotException
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
					setFailure("Found SNAPSHOT parent:\n", path, nextGav, directDepGav);
				}

				// Recrusive call
				checkParent(parent, parentPath, childGavs(parentGavs, nextGav),
					directDepGav);
			}
		}
	}

	/**
	 * @return A new Set, created from the union of the given parentGavs and child
	 */
	private Set<String> childGavs(final Set<String> parentGavs,
		final String childGav)
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
		return makePath(path, gav(project));
	}

	/**
	 * Prepends the given project's gav, in a separate line, to the given path
	 */
	private String makePath(final String path, final String gav) {
		String newPath = "\t" + gav + "\n" + path;
		return newPath;
	}

	/**
	 * Returns the GAV (groupId:artifactId:version) for the given project
	 */
	private String gav(final MavenProject project) {
		return gav(project.getGroupId(), project.getArtifactId(), project
			.getVersion());
	}

	/**
	 * Returns the GAV (groupId:artifactId:version) for the given dependency
	 */
	private String gav(Dependency d) {
		return gav(d.getGroupId(), d.getArtifactId(), d.getVersion());
	}

	/**
	 * Builds a formatted GAV from a given groupId, artifactId and version.
	 */
	private String gav(final String groupId, final String artifactId,
		final String version)
	{
		return groupId + ":" + artifactId + ":" + version;
	}

	/**
	 * Prints the given message to the error log and marks this mojo execution as
	 * a failure.
	 *
	 * @param message Error message to print
	 * @param path A formatted path (e.g. 1 gav per line) representing the
	 *          hierarchy that was followed to this pom
	 * @param gav The gav of the failing project
	 * @param directDepGav The direct dependency of the original base pom from
	 *          which the failing gav was inherited. For non-verbose execution,
	 *          errors are reported per-direct dependency.
	 * @throws SnapshotException If this SnapshotFinder should fail fast.
	 */
	private void setFailure(final String message, final String path,
		final String gav, final String directDepGav) throws SnapshotException
	{
		if (failEarly) {
			throw new SnapshotException(message + makePath(path, gav));
		}

		if (verbose) error(message + makePath(path, gav));
		else flagProblem(directDepGav, gav);

		foundSnapshot = true;
	}

	/**
	 * Helper method to record the problematic gavs for a given direct dependency
	 * of a checked base pom.
	 *
	 * @param directDepGav The direct dependency of the original base pom which
	 *          inherits from a failing gav. If null, indicates a problem in the
	 *          base pom itself and a marker flag is used.
	 * @param gav The problematic gav
	 */
	private void flagProblem(String directDepGav, final String gav) {
		// track failures of the base pom itself using a special flag
		if (directDepGav == null) directDepGav = PARENT_FLAG;

		if (badGavs.get(directDepGav) == null) {
			badGavs.put(directDepGav, new HashSet<String>());
		}

		badGavs.get(directDepGav).add(gav);
	}
}
