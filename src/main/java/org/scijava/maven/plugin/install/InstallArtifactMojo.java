/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2024 SciJava developers.
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

package org.scijava.maven.plugin.install;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.shared.artifact.filter.resolve.TransformableFilter;
import org.apache.maven.shared.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.dependencies.DependableCoordinate;
import org.apache.maven.shared.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.dependencies.resolve.DependencyResolverException;
import org.codehaus.plexus.util.StringUtils;

/**
 * Downloads .jar artifacts and their dependencies into a SciJava application
 * directory structure.
 * <p>
 * ImageJ 1.x plugins (identified by containing a plugins.config file) get
 * copied to the plugins/ subdirectory and all other .jar files to jars/.
 * However, you can override this decision by setting the
 * scijava.app.subdirectory property to a specific subdirectory. It expects the
 * location of the SciJava application directory to be specified in the
 * scijava.app.directory property (which can be set on the Maven command-line).
 * If said property is not set, the install-artifact goal is skipped.
 * </p>
 * 
 * @author Johannes Schindelin
 * @author Stefan Helfrich
 * @author Philipp Hanslovsky
 */
@Mojo(name = "install-artifact", requiresProject=false)
public class InstallArtifactMojo extends AbstractInstallMojo {

	/**
	 * Used to look up Artifacts in the remote repository.
	 */
	@Component
	private ArtifactResolver artifactResolver;

	@Component
	private ArtifactRepositoryFactory artifactRepositoryFactory;
	
	/**
	 * Location of the local repository.
	 */
	@Parameter(property = "localRepository", readonly = true)
	private ArtifactRepository localRepository;

	/**
	 * Map that contains the layouts.
	 */
	@Component(role = ArtifactRepositoryLayout.class)
	private Map<String, ArtifactRepositoryLayout> repositoryLayouts;

	private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.*)::(.+)" );

	/**
	 * Repositories in the format id::[layout]::url or just url, separated by
	 * comma. ie.
	 * central::default::http://repo1.maven.apache.org/maven2,myrepo::::http://repo.acme.com,http://repo.acme2.com
	 */
	@Parameter(property = "remoteRepositories")
	private String remoteRepositories;

	/**
	 * Remote repositories from POM
	 */
	@Parameter(defaultValue = "${project.remoteArtifactRepositories}",
		readonly = true, required = true)
	private List<ArtifactRepository> pomRemoteRepositories;

	/**
	 * The groupId of the artifact to download. Ignored if {@link #artifact} is
	 * used.
	 */
	@Parameter(property = "groupId")
	private String groupId;

	/**
	 * The artifactId of the artifact to download. Ignored if {@link #artifact} is
	 * used.
	 */
	@Parameter(property="artifactId")
	private String artifactId;

	/**
	 * The version of the artifact to download. Ignored if {@link #artifact} is
	 * used.
	 */
	@Parameter(property="version")
	private String version;

	/**
	 * The packaging of the artifact to download. Ignored if {@link #artifact} is
	 * used.
	 */
	@Parameter(property = "packaging", defaultValue = "jar")
	private String packaging = "jar";

	/**
	 * A string of the form groupId:artifactId:version[:packaging].
	 */
	@Parameter(property = "artifact")
	private String artifact;

	/**
	 * The dependency resolver to.
	 */
	@Component
	private DependencyResolver dependencyResolver;

	/**
	 * Whether to force overwriting files.
	 */
	@Parameter(property = "force")
	private boolean force;

	/**
	 * If this option is set to <code>true</code>, optional dependencies will not be installed.
	 */
	@Parameter(property = IGNORE_OPTIONAL_DEPENDENCIES_PROPERTY, defaultValue = "true")
	private boolean ignoreOptionalDependencies;

	/**
	 * The coordinate use for resolving dependencies.
	 */
	private DefaultDependableCoordinate coordinate = new DefaultDependableCoordinate();

	@Component
	private ProjectBuilder mavenProjectBuilder;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (appDirectory == null) {
			throw new MojoExecutionException(
				"The '"+APP_DIRECTORY_PROPERTY+"' property is unset!");
		}
		File appDir = new File(appDirectory);
		if (!appDir.isDirectory() && !appDir.mkdirs()) {
			throw new MojoFailureException("Could not make directory: " +
				appDir);
		}

		if ( appSubdirectory == null )
		{
			getLog().info( "No property name for the " + APP_DIRECTORY_PROPERTY +
					" directory location was specified; Installing in default location" );
		}

		ArtifactRepositoryPolicy always = new ArtifactRepositoryPolicy(true,
			ArtifactRepositoryPolicy.UPDATE_POLICY_ALWAYS,
			ArtifactRepositoryPolicy.CHECKSUM_POLICY_WARN);

		List<ArtifactRepository> repoList = new ArrayList<>();

		// Use repositories provided in POM (if available)
		if (pomRemoteRepositories != null) {
			repoList.addAll(pomRemoteRepositories);
		}

		// Add remote repositories provided as parameter
		if (remoteRepositories != null) {
			String[] repos = remoteRepositories.split(",");
			for (String repo : repos) {
				repoList.add(parseRepository(repo, always));
			}
		}

		// Add ImageJ remote repository
		repoList.add(parseRepository("https://maven.scijava.org/content/groups/public", always));

		/*
		 * Determine GAV to download
		 */
		if (artifactId == null && artifact == null) {
			throw new MojoFailureException(
				"No artifact specified (e.g. by -Dartifact=net.imagej:ij:1.48p)");
		}
		if (artifact != null) {
			String[] tokens = artifact.split(":");
			parseArtifact(tokens);
		}

		/*
		 * Install artifact
		 */
		try {
			ProjectBuildingRequest buildingRequest =
				new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
			buildingRequest.setLocalRepository(localRepository);
			buildingRequest.setRemoteRepositories(repoList);

			final TransformableFilter scopeAndNotOptionalFilter =
					makeTransformableFilterDefaultExclusions(ignoreOptionalDependencies);

			Iterable<ArtifactResult> resolveDependencies = dependencyResolver
				.resolveDependencies(buildingRequest, coordinate, scopeAndNotOptionalFilter);
			for (ArtifactResult result : resolveDependencies) {
				try {
					if ( isSameGAV(coordinate, result.getArtifact()) )
					{
						installArtifact( result.getArtifact(), appDir, appSubdirectory, false, deleteOtherVersionsPolicy );
						continue;
					}
					if (!ignoreDependencies) {
						ProjectBuildingResult build = mavenProjectBuilder.build(result.getArtifact(), session.getProjectBuildingRequest());
						Properties properties = build.getProject().getProperties();
						String subdir = (String) properties.get( APP_SUBDIRECTORY_PROPERTY );

						installArtifact(result.getArtifact(), appDir, subdir, false, deleteOtherVersionsPolicy);
					}
				}
				catch (IOException e) {
					throw new MojoExecutionException("Couldn't download artifact " +
						artifact + ": " + e.getMessage(), e);
				}
				catch ( ProjectBuildingException e )
				{
					throw new MojoExecutionException( "Couldn't determine " +
							APP_SUBDIRECTORY_PROPERTY + " for " + result.getArtifact(), e );
				}
			}
		}
		catch (DependencyResolverException e) {
			throw new MojoExecutionException(
				"Couldn't resolve dependencies for artifact: " + e.getMessage(), e);
		}
	}

	/**
	 * Checks if a {@link DependableCoordinate} and an {@link Artifact} share
	 * the same GAV.
	 *
	 * @param coordinateToCompare
	 *            a {@link DependableCoordinate} instance
	 * @param artifactToCompare
	 *            an {@link Artifact} instance
	 * @return true if both parameters share the same GAV; false otherwise
	 */
	private boolean isSameGAV(final DependableCoordinate coordinateToCompare, final Artifact artifactToCompare) {
		boolean same = coordinateToCompare.getGroupId().equals(artifactToCompare.getGroupId());
		same = same && coordinateToCompare.getArtifactId().equals(artifactToCompare.getArtifactId());
		same = same && coordinateToCompare.getVersion().equals(artifactToCompare.getVersion());
		return same;
	}

	/**
	 * Parses an artifact string of form
	 * {@code groupId:artifactId:version[:packaging]}.
	 * 
	 * @param tokens
	 * @throws MojoFailureException
	 */
	private void parseArtifact(final String[] tokens) throws MojoFailureException {
		if (tokens.length != 3) {
			throw new MojoFailureException(
				"Invalid artifact, you must specify groupId:artifactId:version " +
					artifact);
		}
		groupId = tokens[0];
		artifactId = tokens[1];
		version = tokens[2];

		coordinate.setGroupId(groupId);
		coordinate.setArtifactId(artifactId);
		coordinate.setVersion(version);

		if (tokens.length == 4) {
			coordinate.setType(tokens[3]);
		}
	}

	/**
	 * Parses repository string of form [id::layout::]url
	 *
	 * @param repository {@link String} to be parsed
	 * @param policy The {@link ArtifactRepositoryPolicy} for the repository
	 * @return an {@link ArtifactRepository} instance
	 * @throws MojoFailureException
	 */
	private ArtifactRepository parseRepository(final String repository,
		final ArtifactRepositoryPolicy policy) throws MojoFailureException
	{
		// if it's a simple url
		String id = "temp";
		ArtifactRepositoryLayout layout = getLayout("default");
		String url = repository;

		// if it's an extended repo URL of the form id::layout::url
		if (repository.contains("::")) {
			Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher(repository);
			if (!matcher.matches()) {
				throw new MojoFailureException(repository,
					"Invalid syntax for repository: " + repository,
					"Invalid syntax for repository. Use \"id::layout::url\" or \"URL\".");
			}

			id = matcher.group(1).trim();
			if (!StringUtils.isEmpty(matcher.group(2))) {
				layout = getLayout(matcher.group(2).trim());
			}
			url = matcher.group(3).trim();
		}
		return new MavenArtifactRepository(id, url, layout, policy, policy);
	}

	/**
	 * Determines the layout of a provided repository.
	 *
	 * @param id Id to be queried.
	 * @return An {@link ArtifactRepositoryLayout} instance.
	 * @throws MojoFailureException
	 */
	private ArtifactRepositoryLayout getLayout(final String id)
		throws MojoFailureException
	{
		ArtifactRepositoryLayout layout = repositoryLayouts.get(id);

		if (layout == null) {
			throw new MojoFailureException(id, "Invalid repository layout",
				"Invalid repository layout: " + id);
		}

		return layout;
	}
}
