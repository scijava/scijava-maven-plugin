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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.scijava.maven.plugin.util.PomEditor;
import org.scijava.maven.plugin.util.VersionVisitor;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.version.Version;
import org.xml.sax.SAXException;

/**
 * Bumps dependency and parent versions in SciJava projects.
 * 
 * @author Johannes Schindelin
 */
@Mojo(name = "bump", requiresProject = true, requiresOnline = true)
public class BumpMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject project;

	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	private RepositorySystemSession repositorySystemSession;

	@Component
	private RepositorySystem repositorySystem;

	@Component(role = ArtifactRepositoryLayout.class)
	private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

	@Component
	private ArtifactRepositoryFactory artifactRepositoryFactory;

	@Parameter(defaultValue = "${project.remoteRepositories}", required = true, readonly = true)
	private List<RemoteRepository> remoteRepositories;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			final File file = project.getFile();
			final PomEditor editor = new PomEditor(new FileInputStream(file), getLog());
			editor.visitVersions(new VersionVisitor() {

				@Override
				public String visit(String groupId, String artifactId, String version) throws MojoExecutionException {
					return latestVersion(groupId, artifactId);
				}

			});
			editor.write(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		} catch (final IOException e) {
			throw new MojoExecutionException("Could not read POM", e);
		}
		catch (ParserConfigurationException e) {
			throw new MojoExecutionException("Could not parse POM", e);
		}
		catch (SAXException e) {
			throw new MojoExecutionException("Could not parse POM", e);
		}
		catch (XPathExpressionException e) {
			throw new MojoExecutionException("Could not extract information from POM", e);
		}
	}

	@Component
	private ProjectDependenciesResolver projectDependenciesResolver;

	private String latestVersion(String groupId, String artifactId) throws MojoExecutionException {
		final VersionRangeRequest request = new VersionRangeRequest();
		final Artifact artifact = new DefaultArtifact(groupId, artifactId, null, "[0,)");
		request.setArtifact(artifact);
		makeImageJRepositoryKnown();
		List<RemoteRepository> repositories = new ArrayList<RemoteRepository>();
		repositories.addAll(remoteRepositories);
		request.setRepositories(repositories);
		VersionRangeResult result;
		try {
			result = repositorySystem.resolveVersionRange(repositorySystemSession, request);
			final List<Version> list = result.getVersions();
			for (int i = list.size() - 1; i >= 0; i--) {
				final String version = list.get(i).toString();
				if (version.endsWith("-SNAPSHOT")) continue;
				return version;
			}
			getLog().warn("Found no candidates for " + groupId + ":" + artifactId + "; Skipping");
			return null;
		}
		catch (VersionRangeResolutionException e) {
			throw new MojoExecutionException("Could not resolve version for " + groupId + ":" + artifactId, e);
		}
	}

	private final static String IMAGEJ_REPOSITORY_URL =
		"http://maven.imagej.net/content/groups/public";

	private void makeImageJRepositoryKnown() throws MojoExecutionException
	{
		for (final RemoteRepository repository : remoteRepositories) {
			final String url = repository.getUrl();
			if (IMAGEJ_REPOSITORY_URL.equals(url)) return;
		}

		final ArtifactRepositoryLayout layout = repositoryLayouts.get("default");

			if (layout == null) {
				throw new MojoExecutionException("default", "Invalid repository layout",
					"Invalid repository layout: default");
			}

			final RemoteRepository imagej = new RemoteRepository("imagej.public",
				"default", IMAGEJ_REPOSITORY_URL);
			remoteRepositories.add(imagej);
	}

}
