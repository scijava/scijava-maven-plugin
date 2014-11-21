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

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.model.Build;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Runs the annotation processor of the <tt>scijava-common</tt> artifact even inside Eclipse.
 * 
 * @author Johannes Schindelin
 */
@Mojo(name = "eclipse-helper", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
	requiresProject = true,
	requiresDependencyResolution = ResolutionScope.COMPILE)
public class EclipseHelperMojo extends AbstractMojo {

	private static final String SCIJAVA_COMMON_ARTIFACTID = "scijava-common";
	private static final String SCIJAVA_COMMON_GROUPID = "org.scijava";

	@Parameter(defaultValue="${project}", required = true, readonly = true)
	private MavenProject currentProject;

	@Override
	public void execute() throws MojoExecutionException {
		final Build build = currentProject.getBuild();
		if (build == null) return;
		final String buildDirectoryName = build.getDirectory();
		if (buildDirectoryName == null) return;
		final File buildDirectory = new File(buildDirectoryName);
		if (!buildDirectory.exists()) return;

		if (!dependsOnScijavaCommon(currentProject)) return;

		getLog().info("Parsing SciJava annotations");
		try {
			final List<String> elements = currentProject.getCompileClasspathElements();
			final URL[] classpath = new URL[elements.size() + 1];
			classpath[0] = buildDirectory.toURI().toURL();
			for (int i = 1; i < classpath.length; i++) {
				classpath[i] = new URL("file:" + elements.get(i - 1));
			}
			getLog().debug("Using class path '" + classpath + "' to execute EclipseHelper");
			final ClassLoader loader = new URLClassLoader(classpath);
			final Class<?> helper = loader.loadClass("org.scijava.annotations.EclipseHelper");
			final Method main = helper.getMethod("main", String[].class);

			final Thread thread = Thread.currentThread();
			final ClassLoader previousLoader = thread.getContextClassLoader();
			try {
				thread.setContextClassLoader(loader);
				main.invoke(null, (Object) new String[0]);
			}
			catch (Exception e) {
				throw new MojoExecutionException("Could not execute EclipseHelper's main() method", e);
			}
			finally {
				thread.setContextClassLoader(previousLoader);
			}
		}
		catch (DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("Could not get the class path", e);
		}
		catch (MalformedURLException e) {
			throw new MojoExecutionException("Could not build class path for EclipseHelper", e);
		}
		catch (ClassNotFoundException e) {
			throw new MojoExecutionException("Could not load EclipseHelper", e);
		}
		catch (NoSuchMethodException e) {
			throw new MojoExecutionException("Could not find EclipseHelper's main() method", e);
		}
		catch (SecurityException e) {
			throw new MojoExecutionException("Could not access EclipseHelper's main() method", e);
		}
	}

	/**
	 * Determines whether the project depends (in-)directly on <a
	 * href="https://github.com/scijava/scijava-common/">
	 * <code>scijava-common</code></a>.
	 * 
	 * @return true iff the project depends on <code>scijava-common</code>
	 */
	private boolean dependsOnScijavaCommon(final MavenProject project) {
		final List<Dependency> dependencies = project.getCompileDependencies();
		if (dependencies == null) return false;

		for (final Dependency dependency : dependencies) {
			if (dependency == null) continue;
			if (SCIJAVA_COMMON_ARTIFACTID.equals(dependency.getArtifactId()) &&
				SCIJAVA_COMMON_GROUPID.equals(dependency.getGroupId()))
			{
				return true;
			}
		}

		return false;
	}

}
