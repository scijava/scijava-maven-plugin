/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2020 SciJava developers.
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * {@link SciJavaDependencyChecker} implementation that fails when it encounters
 * a SNAPSHOT dependency or parent.
 * <p>
 * Options:
 * </p>
 * <ul>
 * <li>verbose - prints full inheritance paths to all failures (default: false)</li>
 * <li>failEarly - end execution after first failure (default: false)</li>
 * <li>groupIds - an inclusive list of groupIds. Errors will only be reported
 * for projects whose groupIds are contained this list.</li>
 * </ul>
 *
 * @author Mark Hiner
 */
public class SnapshotFinder extends AbstractSciJavaDependencyChecker {

	// -- Parameters --

	private final List<ArtifactRepository> remoteRepositories;
	private final MavenProjectBuilder projectBuilder;
	private final ArtifactRepository localRepository;

	// -- Fields --

	private final Map<DependencyNode, Result> results =
		new HashMap<>();

	private Set<MavenProject> reactorModules = new HashSet<>();

	// -- Constructor --

	public SnapshotFinder(final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository,
		final List<ArtifactRepository> remoteRepositories)
	{
		this.projectBuilder = projectBuilder;
		this.localRepository = localRepository;
		this.remoteRepositories = remoteRepositories;
	}

	// -- SciJavaDependencyChecker API --

	/**
	 * Using this method, a set of projects can be allowlisted to accept as
	 * SNAPSHOT couplings. This is because, for a given reactor, building a Maven
	 * project from the top level is inherently reproducible even with
	 * inter-dependent SNAPSHOT-coupled modules.
	 * <p>
	 * NB: Reproducibility is NOT guaranteed if an individual module is built
	 * instead of the whole reactor. This could trigger the downloading of a
	 * remote SNAPSHOT dependency which may or may not be compatible.
	 * </p>
	 *
	 * @param modules A list of all {@link MavenProject}s in the core project's
	 *          reactor.
	 */
	public void setReactorModules(final List<MavenProject> modules) {
		reactorModules = new HashSet<>(modules);
	}

	@Override
	public String makeExceptionMessage() {
		if (!failed()) return null;
		// Header
		String message =
			"\nThe following artifacts either - are SNAPSHOT versions (V), contain "
				+ "\nSNAPSHOT parents (P), or contain SNAPSHOT dependencies (D):\n\n";

		// Because we are using the Maven 2 DependencyTree, artifacts can be listed
		// multiple times if multiple components depend on them. Thus we need to
		// merge our dependencies to a set of artifacts.
		final Map<Artifact, Result> mergedResults = new HashMap<>();

		for (final DependencyNode node : results.keySet()) {
			final Artifact a = node.getArtifact();
			final Result r = results.get(node);
			if (mergedResults.containsKey(a)) {
				mergedResults.get(a).merge(r);
			}
			else mergedResults.put(a, r);
		}

		// Take the list of artifacts and add any failures to the exception message
		for (final Artifact a : mergedResults.keySet()) {
			final Result r = mergedResults.get(a);
			if (r.failed() && matches(a.getGroupId())) {
				message +=
					r.failTags() + " " + a.getGroupId() + ":" + a.getArtifactId() + ":" +
						a.getVersion() + "\n";
			}
		}

		// Add a note about failing fast, if appropriate
		if (isFailFast()) {
			message += "\nThere may be others but <failFast> was set to false\n";
		}

		return message;
	}

	// -- DependencyNodeVisitor API --

	@Override
	public boolean visit(final DependencyNode node) {

		final Artifact a = node.getArtifact();
		MavenProject pom = null;
		Result r = null;
		try {
			pom = getProject(a);

			// for the root node, we want to check its parents but we don't care if
			// it is a SNAPSHOT itself.
			if (isRoot(node)) {
				r = new Result();
				checkParent(pom, r);
			}
			else {
				r = containsSnapshots(pom);
			}
		}
		catch (final ProjectBuildingException e) {
			r = new Result();
			r.setFailTag(" Failed to build pom.");
		}

		// save the results
		results.put(node, r);

		// set failure and propagate to parent nodes
		if (r.failed() && matches(a.getGroupId())) {
			setFailed();
			markParent(node);
		}

		return !stopVisit();
	}

	// -- Helper methods --

	/**
	 * Recursively sets the {@link Result}s of all parents of the given node as
	 * having {@link Result#badDep()}.
	 *
	 * @param badDependency A failed {@link DependencyNode}.
	 */
	private void markParent(final DependencyNode badDependency) {
		final DependencyNode parent = badDependency.getParent();

		if (parent != null && !isRoot(parent)) {
			final Result r = results.get(parent);
			// badDep returns the value previously set for the Result#dep field.
			// We only set badDep here, thus if it was already true we know we've
			// already ascended this path before and do not need to recurse further.
			if (!r.badDep()) markParent(parent);
		}
	}

	/**
	 * Generate a {@link Result} if the specified {@link MavenProject} is a
	 * SNAPSHOT version or contains a SNAPSHOT in its parent pom hierarchy.
	 * <p>
	 * NB: we do not need to check the dependencies of the given pom, as this is
	 * done implicitly by the visitor pattern. However, pom parents are not
	 * visited! Thus they must be recursively checked.
	 * </p>
	 *
	 * @param pom {@link MavenProject} to check for SNAPSHOT dependencies.
	 */
	private Result containsSnapshots(final MavenProject pom) {
		final Result r = new Result();

		// If there is a parent pom, recurse its hierarchy looking for SNAPSHOTs
		checkParent(pom, r);

		// If the pom itself is bad, mark it as such.
		if (isSnapshot(pom)) {
			r.badVersion();
		}

		return r;
	}

	/**
	 * Recursively check the parents of a given {@link MavenProject} for SNAPSHOT
	 * versions.
	 *
	 * @param pom {@link MavenProject} to check for SNAPSHOT parents.
	 * @param result {@link Result} instance for the original base pom, to record
	 *          if a bad parent is found.
	 */
	private void checkParent(final MavenProject pom, final Result result) {
		if (pom.hasParent()) {
			final MavenProject parent = pom.getParent();
			// We don't record the exact SNAPSHOT parent - just whether or not one
			// was found. So if this parent is a SNAPSHOT, short-circuit and return.
			// Otherwise recurse to the next parent if there is one.
			if (isSnapshot(parent)) {
				result.badParent();
			}
			// Recurse if needed
			checkParent(parent, result);
		}
	}

	/**
	 * Helper method to build a {@link MavenProject} from an {@link Artifact}.
	 *
	 * @param a Artifact specifying the {@link MavenProject} to build.
	 * @return An initialized {@link MavenProject}.
	 * @throws ProjectBuildingException As
	 *           {@link MavenProjectBuilder#buildFromRepository}.
	 */
	private MavenProject getProject(final Artifact a)
		throws ProjectBuildingException
	{
		final MavenProject project =
			projectBuilder
				.buildFromRepository(a, remoteRepositories, localRepository);
		return project;
	}

	/**
	 * Helper method to check if a project is a SNAPSHOT.
	 *
	 * @param pom MavenProject to check.
	 * @return True iff the version of the given project is a SNAPSHOT version,
	 *         and the project is not in the base reactor.
	 */
	private boolean isSnapshot(final MavenProject pom) {
		return pom.getVersion().contains(Artifact.SNAPSHOT_VERSION) &&
			!reactorModules.contains(pom);
	}

	/**
	 * Helper class to track failures by cause.
	 */
	private static class Result {

		// -- Fields --

		private boolean parent = false;
		private boolean dep = false;
		private boolean version = false;
		private String tag = "";

		// -- Result API --

		/**
		 * Marks this {@link Result} as having a bad parent pom.
		 */
		public void badParent() {
			parent = true;
		}

		/**
		 * Marks this {@link Result} as having a bad transitive dependency.
		 *
		 * @return The previous value of this flag (e.g. will return false the first
		 *         time it is called, and true afterwards).
		 */
		public boolean badDep() {
			final boolean oldValue = dep;
			dep = true;
			return oldValue;
		}

		/**
		 * Marks this {@link Result} as being a SNAPSHOT version.
		 */
		public void badVersion() {
			version = true;
		}

		/**
		 * Marks this {@link Result} as failing for an unusual reason (e.g. couldn't
		 * check it due to an exception}. Typically tags are generated dynamically
		 * from the {@link #failTags()} method, but this allows exception messages
		 * to be preserved for final output.
		 *
		 * @param tag Explicit message tag to use for this {@link Result}
		 */
		public void setFailTag(final String tag) {
			this.tag = tag;
		}

		/**
		 * @return true if this {@link Result} has one or more of:
		 *         <ul>
		 *         <li>a bad parent</li>
		 *         <li>bad dependency</li>
		 *         <li>is a SNAPSHOT</li>
		 *         <li>has a custom failure tag set</li>
		 *         </ul>
		 */
		public boolean failed() {
			return !tag.isEmpty() || parent || dep || version;
		}

		/**
		 * @return A string containing all applicable failure causes for this
		 *         {@link Result}.
		 */
		public String failTags() {
			return tag + (version ? " (V) " : "") + (parent ? " (P) " : "") +
				(dep ? " (D) " : "");
		}

		/**
		 * Merges this and a provided {@link Result}'s failure causes. For example,
		 * if this has a bad parent and the provided {@code Result} has a bad
		 * dependency, after the merge this would indicate a bad parent and bad
		 * dependency.
		 * <p>
		 * Custom tag messages are appended.
		 * </p>
		 * 
		 * @param r Result to merge into this.
		 */
		public void merge(final Result r) {
			parent = parent || r.parent;
			dep = dep || r.dep;
			version = version || r.version;

			if (!tag.isEmpty() && r.tag.isEmpty()) {
				tag = tag + " | " + r.tag;
			}
			else tag += r.tag;

		}

	}
}
