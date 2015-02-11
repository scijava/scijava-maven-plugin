/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2015 Board of Regents of the University of
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.AndArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;

/**
 * Utility class for initiating Maven-based dependency checks.
 * <p>
 * <ul>
 * <li>The {@link #checkDependencies} methods can be used to initiate one or
 * more {@link SciJavaDependencyChecker}s in visiting a Maven dependency tree.
 * Uses {@link DependencyTreeBuilder} instead of {@code DependencyGraphBuidler}
 * to get the more verbose Maven 2 dependency tree.</li>
 * </ul>
 * </p>
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

	// -- Helper methods --

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
