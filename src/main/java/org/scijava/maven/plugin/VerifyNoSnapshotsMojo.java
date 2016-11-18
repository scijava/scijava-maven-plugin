/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2016 Board of Regents of the University of
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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;

/**
 * Mojo wrapper for the {@link SnapshotFinder}.
 * <p>
 * Parameters:
 * <ul>
 * <li>failFast - end execution after first failure (default: false)</li>
 * <li>groupIds - an inclusive list of groupIds. Errors will only be reported
 * for projects whose groupIds are contained this list. (default: empty - all
 * groupIds considered)</li>
 * <li>groupId - Singular groupIds option. Will be appended to groupIds if both
 * are specified.</li>
 * </ul>
 * </p>
 *
 * @goal verify-no-snapshots
 * @phase validate
 */
public class VerifyNoSnapshotsMojo extends AbstractMojo {

	// -- Parameters --

	/** @parameter default-value="${session}" */
	private MavenSession mavenSession;

	/** @parameter default-value="${project}" */
	private MavenProject mavenProject;

	/**
	 * @component role =
	 *            "org.apache.maven.shared.dependency.tree.DependencyTreeBuilder"
	 */
	private DependencyTreeBuilder dependencyTreeBuilder;

	/** @component role = "org.apache.maven.project.MavenProjectBuilder" */
	private MavenProjectBuilder projectBuilder;

	/** @parameter expression="${localRepository}" */
	private ArtifactRepository localRepository;

	/** @parameter expression="${reactorProjects}" */
	private List<MavenProject> reactorModules;

	/** @parameter property="failFast" default-value=false */
	private Boolean failFast;

	/** @parameter property="groupId" */
	private String groupId;

	/** @parameter property="groupIds" */
	private List<Object> groupIds;

	// -- Mojo API Methods --

	/**
	 * Entry point for mojo execution
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			reactorModules =
				DependencyUtils.findEffectiveReactor(reactorModules, mavenSession,
					mavenProject, projectBuilder, localRepository);
		}
		catch (final ProjectBuildingException exc) {
			getLog().warn("Error during project construction:\n" + exc.getMessage(),
				exc);
		}

		// Enter recursive project checking
		final SnapshotFinder fs =
			new SnapshotFinder(projectBuilder, localRepository, mavenProject
				.getRemoteArtifactRepositories());

		fs.setLog(getLog());
		fs.setFailFast(failFast);
		fs.setGroupIds(getGroupIds());
		fs.setReactorModules(reactorModules);

		try {
			DependencyUtils.checkDependencies(mavenProject, localRepository,
				dependencyTreeBuilder, fs);
		}
		catch (final SciJavaDependencyException e) {
			throw new MojoFailureException(e.getMessage() +
				"\nTo disable Maven Enforcer rules for local development, re-run" +
				" Maven\n with the -Denforcer.skip property set.\n");
		}
	}

	// -- Helper methods --

	private Set<String> getGroupIds() {
		final Set<String> ids = new HashSet<String>();
		if (groupIds != null) {
			for (final Object id : groupIds)
				ids.add(id == null ? null : id.toString());
		}
		if (groupId != null) ids.add(groupId);
		return ids;
	}
}
