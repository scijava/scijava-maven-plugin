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

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Sets the <tt>project.rootdir</tt> property to the top-level directory of
 * the current Maven project structure.
 * 
 * @author Johannes Schindelin
 */
@Mojo(name = "set-rootdir", defaultPhase = LifecyclePhase.VALIDATE)
public class SetRootDirPropertyMojo extends AbstractMojo {

	/**
	 * You can rename the rootdir property name to another property name if
	 * desired.
	 */
	@Parameter(defaultValue = "rootdir", property = "setRootdir.rootdirPropertyName")
	private String rootdirPropertyName;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject currentProject;

	/**
	 * Contains the full list of projects in the reactor.
	 */
	@Parameter(defaultValue="${reactorProjects}", readonly = true)
	private List<MavenProject> reactorProjects;

	@Override
	public void execute() throws MojoExecutionException {
		if (currentProject.getProperties().getProperty(rootdirPropertyName) != null)
		{
			getLog().debug("Using previously defined rootdir");
			return;
		}

		if (!isLocalProject(currentProject))
			return;

		MavenProject project = currentProject;
		for (;;) {
			final MavenProject parent = project.getParent();
			if (parent == null || !isLocalProject(parent))
				break;
			project = parent;
		}

		final String rootdir = project.getBasedir().getAbsolutePath();
		getLog().info("Setting rootdir: " + rootdir);
		for (final MavenProject reactorProject : reactorProjects)
			reactorProject.getProperties().setProperty(rootdirPropertyName, rootdir);
	}

	/**
	 * Determines whether the project has a valid output directory.
	 *
	 * @param project the Maven project
	 * @return true iff the project is local
	 */
	private static boolean isLocalProject(final MavenProject project) {
		final File baseDir = project.getBasedir();
		return baseDir != null && baseDir.exists();
	}

}
