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

package org.scijava.maven.plugin.enforcer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.scijava.maven.plugin.DependencyUtils;
import org.scijava.maven.plugin.SciJavaDependencyException;
import org.scijava.maven.plugin.SnapshotFinder;

/**
 * A {@link EnforcerRule} wrapper for the {@link SnapshotFinder}.
 * <p>
 * Parameters:
 * <ul>
 * <li>failFast - end execution after first failure (default: false)</li>
 * <li>groupIds - an inclusive comma-separated list of groupIds. Errors will
 * only be reported for projects whose groupIds are contained this list.
 * (default: null - all groupIds considered)</li>
 * <li>groupId - Singular groupIds option. Will be appended to groupIds if both
 * are specified. (default: null)</li>
 * </ul>
 * </p>
 *
 * @author Mark Hiner
 */
public class RequireReproducibleBuilds implements EnforcerRule {

	// -- Parameters --

	private final boolean failFast = false;
	private String groupId;
	private String groupIds;

	// -- EnforcerRule API methods --

	/**
	 * Entry point for enforcer rule execution
	 */
	@Override
	public void execute(final EnforcerRuleHelper helper)
		throws EnforcerRuleException
	{
		final Log log = helper.getLog();
		try {
			final MavenProject project = (MavenProject) helper.evaluate("${project}");
			final MavenSession session = (MavenSession) helper.evaluate("${session}");
			final ArtifactRepository localRepository =
				(ArtifactRepository) helper.evaluate("${localRepository}");
			final MavenProjectBuilder projectBuilder =
				(MavenProjectBuilder) helper.getComponent(MavenProjectBuilder.class);
			final DependencyTreeBuilder treeBuilder =
				(DependencyTreeBuilder) helper
					.getComponent(DependencyTreeBuilder.class);

			@SuppressWarnings("unchecked")
			List<MavenProject> reactorModules =
				(List<MavenProject>) helper.evaluate("${reactorProjects}");

			try {
				reactorModules =
					DependencyUtils.findEffectiveReactor(reactorModules, session,
						project, projectBuilder, localRepository);
			}
			catch (final ProjectBuildingException exc) {
				log
					.warn("Error during project construction:\n" + exc.getMessage(), exc);
			}

			// populate groupIds
			final Set<String> ids = new HashSet<String>();
			if (groupId != null) ids.add(groupId);
			if (groupIds != null) {
				for (final String id : groupIds.split(",")) {
					ids.add(id);
				}
			}

			// Enter recursive project checking
			final SnapshotFinder fs =
				new SnapshotFinder(projectBuilder, localRepository, project
					.getRemoteArtifactRepositories());

			fs.setLog(log);
			fs.setFailFast(failFast);
			fs.setGroupIds(ids);
			fs.setReactorModules(reactorModules);

			DependencyUtils.checkDependencies(project, localRepository, treeBuilder,
				fs);
		}
		catch (final ComponentLookupException e) {
			throw new EnforcerRuleException(e.getMessage());
		}
		catch (final ExpressionEvaluationException e) {
			throw new EnforcerRuleException(e.getMessage());
		}
		catch (final SciJavaDependencyException e) {
			throw new EnforcerRuleException(e.getMessage());
		}
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public boolean isResultValid(final EnforcerRule cachedRule) {
		return false;
	}

	@Override
	public String getCacheId() {
		return null;
	}

}
