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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProjectBuilder;

/**
 * Mojo wrapper for the {@link SnapshotFinder}.
 *
 * @goal verify-no-snapshots
 * @phase validate
 */
public class VerifyNoSnapshotsMojo extends AbstractMojo {

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

		// Enter recursive project checking
		final SnapshotFinder fs =
			new SnapshotFinder(m_projectBuilder, m_localRepository, failEarly);

		fs.setLog(getLog());

		// Failure at the end of execution
		try {
			fs.checkProject(mavenProject);
		}
		catch (SnapshotException e) {
			throw new MojoFailureException(e.getMessage());
		}
	}
}