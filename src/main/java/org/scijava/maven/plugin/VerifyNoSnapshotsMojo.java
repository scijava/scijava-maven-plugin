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

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

/**
 * Mojo wrapper for the {@link SnapshotFinder}.
 * <p>
 * Parameters:
 * <ul>
 * <li>failEarly - end execution after first failure (default: false)</li>
 * <li>verbose - prints full inheritance paths to all failures (default: false)</li>
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

	/** @parameter default-value="${project}" */
	private MavenProject mavenProject;

	/** @component role = "org.apache.maven.project.MavenProjectBuilder" */
	private MavenProjectBuilder m_projectBuilder;

	/** @parameter expression="${localRepository}" */
	private ArtifactRepository m_localRepository;

	/** @parameter property="failEarly" default-value=false */
	private Boolean failEarly;

	/** @parameter property="verbose" default-value=false */
	private Boolean verbose;

	/** @parameter property="groupId" */
	private String groupId;

	/** @parameter property="groupIds" */
	@SuppressWarnings("rawtypes")
	private List groupIds;

	// -- Mojo API Methods --

	/**
	 * Entry point for mojo execution
	 */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (groupId != null) addGroup(groupId);

		// Enter recursive project checking
		final SnapshotFinder fs =
			new SnapshotFinder(m_projectBuilder, m_localRepository, failEarly,
				verbose, groupIds);

		fs.setLog(getLog());

		// Failure at the end of execution
		try {
			fs.checkProject(mavenProject);
		}
		catch (SnapshotException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}

	// -- Helper methods --

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void addGroup(final String groupId) {
		if (groupIds == null) {
			groupIds = new ArrayList();
		}
		groupIds.add(groupId);
	}
}
