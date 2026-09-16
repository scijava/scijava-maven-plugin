/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2026 SciJava developers.
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.profiles.DefaultProfileManager;
import org.apache.maven.profiles.ProfileManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.scijava.maven.plugin.dependency.graph.DependencyGraphBuilder;
import org.scijava.maven.plugin.dependency.tree.DependencyNode;
import org.scijava.maven.plugin.dependency.tree.DependencyTreeBuilder;
import org.scijava.maven.plugin.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.PlexusContainer;

/**
 * Utility class for initiating Maven-based dependency checks.
 * <ul>
 * <li>The {@link #checkDependencies} methods can be used to initiate one or
 * more {@link SciJavaDependencyChecker}s in visiting a Maven dependency tree.
 * Uses {@link DependencyTreeBuilder} instead of {@link DependencyGraphBuilder}
 * to get the more verbose Maven 2 dependency tree.</li>
 * </ul>
 *
 * @author Mark Hiner
 */
public final class DependencyUtils {

	// -- Public utility methods --

	/**
	 * Convenience {@link #checkDependencies} method. Sets {@code scope} to
	 * {@link Artifact#SCOPE_RUNTIME}.
	 *
	 * @param mavenProject Base pom to check.
	 * @param artifactRepository Repository to use when resolving artifacts.
	 * @param dependencyTreeBuilder {@link DependencyTreeBuilder} instance to use
	 *          to build a dependency tree.
	 * @param checkers A list of one or more {@link SciJavaDependencyChecker}s.
	 *          Each will visit the constructed dependency tree.
	 * @throws SciJavaDependencyException If one or more of the given checkers
	 *           visitations ultimately returns {@code true}, indicating a failed
	 *           state was discovered.n
	 */
	public static void checkDependencies(final MavenProject mavenProject,
		final ArtifactRepository artifactRepository,
		final DependencyTreeBuilder dependencyTreeBuilder,
		final SciJavaDependencyChecker... checkers)
		throws SciJavaDependencyException
	{
		checkDependencies(mavenProject, artifactRepository, dependencyTreeBuilder,
			Artifact.SCOPE_RUNTIME, checkers);
	}

	/**
	 * @param mavenProject Base pom to check.
	 * @param artifactRepository Repository to use when resolving artifacts.
	 * @param dependencyTreeBuilder {@link DependencyTreeBuilder} instance to use
	 *          to build a dependency tree.
	 * @param scope Dependency scope to use. See {@link Artifact} SCOPE constants.
	 * @param checkers A list of one or more {@link SciJavaDependencyChecker}s.
	 *          Each will visit the constructed dependency tree.
	 * @throws SciJavaDependencyException If one or more of the given checkers
	 *           visitations ultimately returns {@code true}, indicating a failed
	 *           state was discovered.n
	 */
	public static void checkDependencies(final MavenProject mavenProject,
		final ArtifactRepository artifactRepository,
		final DependencyTreeBuilder dependencyTreeBuilder, final String scope,
		final SciJavaDependencyChecker... checkers)
		throws SciJavaDependencyException
	{
		final ArtifactFilter artifactFilter = createResolvingArtifactFilter(scope);
		try {
			// Build the dependency tree that will be visited by each checker.
			final DependencyNode root =
				dependencyTreeBuilder.buildDependencyTree(mavenProject,
					artifactRepository, artifactFilter);

			String failureMessage = "";

			// Iterate over each checker, visiting the dependency tree and aggregating
			// failure messages.
			for (final SciJavaDependencyChecker checker : checkers) {
				if (root.accept(checker)) {
					failureMessage += checker.makeExceptionMessage();
				}
			}

			// throw an exception if one or more checker failed.
			if (!failureMessage.isEmpty()) {
				throw new SciJavaDependencyException(failureMessage);
			}
		}
		catch (final DependencyTreeBuilderException e) {
			throw new SciJavaDependencyException(e.getMessage());
		}
	}

	/**
	 * Manually constructs an list of effective reactor projects by recursively
	 * searching parent and submodule projects. This allows the intention of the
	 * reactor to be preserved, as long as it is fully available on disk, even
	 * when building a submodule directly.
	 *
	 * @param defaultReactor Return value to use if a comprehensive list can not
	 *          be discovered.
	 * @param baseProject {@link MavenProject} where invocation started.
	 * @return A list of MavenProjects that can be treated as though within the
	 *         current reactor.
	 * @throws ProjectBuildingException
	 */
	public static List<MavenProject> findEffectiveReactor(
		final List<MavenProject> defaultReactor, final MavenSession session,
		final MavenProject baseProject, final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository) throws ProjectBuildingException
	{
		final Set<MavenProject> reactor = new HashSet<>();
		final Set<MavenProject> visited = new HashSet<>();
		final ProfileManager profileManager = getProfileManager(session);

		findEffectiveReactor(reactor, visited, baseProject, baseProject,
			projectBuilder, localRepository, profileManager);

		if (reactor.size() <= 1 || !reactor.contains(baseProject)) return defaultReactor;
		return new ArrayList<>(reactor);
	}

	// -- Helper methods --

	/**
	 * Helper method to recursively populate a set of {@link MavenProject}s that
	 * can be considered to be within the same reactor.
	 */
	private static void findEffectiveReactor(final Set<MavenProject> reactor,
		final Set<MavenProject> visited, final MavenProject currentProject,
		final MavenProject target, final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository,
		final ProfileManager profileManager) throws ProjectBuildingException
	{
		// short-circuit if already visited this project
		if (!visited.add(currentProject)) return;

		final File baseDir = currentProject.getBasedir();

		// We only are interested in local projects
		if (baseDir != null && baseDir.exists()) {

			// If the current project lists any modules , then that project itself
			// needs to be included in the reactor
			if (currentProject.getModules().size() > 0) {
				reactor.add(currentProject);
			}

			// Recursively add each submodule to the reactor
			for (final Object o : currentProject.getModules()) {
				final File submodule =
					new File(baseDir.getAbsolutePath() + File.separator + o.toString() +
						File.separator + "pom.xml");
				final MavenProject p =
					projectBuilder.build(submodule, localRepository, profileManager);
				reactor.add(p);
				findEffectiveReactor(reactor, visited, p, target, projectBuilder,
					localRepository, profileManager);
			}
		}

		// Recurse into parent
		if (currentProject.hasParent()) findEffectiveReactor(reactor, visited,
			currentProject.getParent(), target, projectBuilder, localRepository,
			profileManager);

		// Recurse into the aggregator, if any.
		//
		// NB: this is not the same as the parent above. A module's <parent> is
		// often a shared parent POM such as pom-scijava, which knows nothing
		// about its siblings; the aggregator that actually lists the module is
		// then only discoverable by looking at the enclosing directory. Without
		// this step, building a single module of a multi-module project sees an
		// effective reactor of one, and every sibling SNAPSHOT dependency looks
		// like a non-reproducible external artifact.
		final MavenProject aggregator = findAggregator(currentProject,
			projectBuilder, localRepository, profileManager);
		if (aggregator != null) findEffectiveReactor(reactor, visited, aggregator,
			target, projectBuilder, localRepository, profileManager);
	}

	/**
	 * Finds the project aggregating the given one: the nearest enclosing
	 * directory whose POM lists the given project's directory as a module.
	 *
	 * @return the aggregating project, or null if there is none
	 */
	private static MavenProject findAggregator(final MavenProject project,
		final MavenProjectBuilder projectBuilder,
		final ArtifactRepository localRepository,
		final ProfileManager profileManager) throws ProjectBuildingException
	{
		final File baseDir = project.getBasedir();
		if (baseDir == null) return null;
		final File parentDir = baseDir.getParentFile();
		if (parentDir == null) return null;
		final File pom = new File(parentDir, "pom.xml");
		if (!pom.isFile()) return null;

		final MavenProject candidate =
			projectBuilder.build(pom, localRepository, profileManager);
		return declaresModule(candidate, baseDir) ? candidate : null;
	}

	/** Gets whether the given project lists the given directory as a module. */
	// NB: package-private for testing.
	static boolean declaresModule(final MavenProject project,
		final File moduleDir)
	{
		final File baseDir = project.getBasedir();
		if (baseDir == null) return false;
		for (final Object module : project.getModules()) {
			final File dir = new File(baseDir, module.toString());
			if (sameFile(dir, moduleDir)) return true;
		}
		return false;
	}

	/** Compares two files by canonical path, falling back to absolute path. */
	private static boolean sameFile(final File a, final File b) {
		try {
			return a.getCanonicalFile().equals(b.getCanonicalFile());
		}
		catch (final IOException exc) {
			return a.getAbsoluteFile().equals(b.getAbsoluteFile());
		}
	}

	/**
	 * Convenience method to get the {@link ProfileManager} for a given
	 * {@link MavenSession}.
	 */
	@SuppressWarnings("deprecation")
	private static ProfileManager getProfileManager(final MavenSession session) {
		final PlexusContainer container = session.getContainer();
		final Properties execution = session.getExecutionProperties();
		return new DefaultProfileManager(container, execution);
	}

	/**
	 * Helper method to build an {@link ArtifactFilter}. Multiple filters can be
	 * unioned together via the {@link AndArtifactFilter}.
	 *
	 * @param scope See {@link Artifact} scope constants for options.
	 * @return Initialized {@link ArtifactFilter}.
	 */
	private static ArtifactFilter
		createResolvingArtifactFilter(final String scope)
	{
		// Apply scope filter
		final AndArtifactFilter filter = new AndArtifactFilter();
		filter.add(new ScopeArtifactFilter(scope));

		// NB: could add more filters if we want to pre-filter the node list.

		return filter;
	}
}
