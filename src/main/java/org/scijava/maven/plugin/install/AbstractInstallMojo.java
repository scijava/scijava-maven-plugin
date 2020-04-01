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

package org.scijava.maven.plugin.install;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.ObjectBasedValueSource;
import org.codehaus.plexus.interpolation.PrefixAwareRecursionInterceptor;
import org.codehaus.plexus.interpolation.PrefixedValueSourceWrapper;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.util.FileUtils;
import org.scijava.util.VersionUtils;

/**
 * Base class for mojos to copy .jar artifacts and their dependencies into a
 * SciJava application directory structure.
 * 
 * @author Johannes Schindelin
 */
public abstract class AbstractInstallMojo extends AbstractMojo {

	/**
	 * Path to a SciJava application directory (e.g. ImageJ.app) to which
	 * artifacts are copied.
	 * <p>
	 * If it is not a directory, no .jar files are copied.
	 * </p>
	 */
	@Parameter(property = APP_DIRECTORY_PROPERTY, required = false)
	String appDirectory;

	/**
	 * The name of the property pointing to the subdirectory (beneath e.g.
	 * {@code jars/} or {@code plugins/}) to which the artifact should be copied.
	 * <p>
	 * If no property of that name exists, no subdirectory will be used.
	 * </p>
	 */
	@Parameter(property = APP_SUBDIRECTORY_PROPERTY, required = false)
	String appSubdirectory;

	/**
	 * Whether to delete other versions when copying the files.
	 * <p>
	 * When copying a file and its dependencies to a SciJava application directory
	 * and there are other versions of the same file, we can warn or delete those
	 * other versions.
	 * </p>
	 */
	@Parameter(property = DELETE_OTHER_VERSIONS_POLICY_PROPERTY, defaultValue = "older")
	OtherVersions deleteOtherVersionsPolicy;

	/**
	 * If this option is set to <code>true</code>, only the artifact will be
	 * copied - without its dependencies.
	 */
	@Parameter(property = IGNORE_DEPENDENCIES_PROPERTY, defaultValue = "false")
	boolean ignoreDependencies;

	@Parameter(defaultValue = "${session}")
	MavenSession session;

	@Parameter( defaultValue = "${mojoExecution}", readonly = true )
	MojoExecution mojoExecution;

	protected static final String APP_DIRECTORY_PROPERTY = "scijava.app.directory";
	protected static final String APP_SUBDIRECTORY_PROPERTY = "scijava.app.subdirectory";
	protected static final String DELETE_OTHER_VERSIONS_POLICY_PROPERTY = "scijava.deleteOtherVersions";
	protected static final String IGNORE_DEPENDENCIES_PROPERTY = "scijava.ignoreDependencies";

	public enum OtherVersions {
			always, older, never
	}

	protected boolean hasIJ1Dependency(final MavenProject project) {
		final List<Dependency> dependencies = project.getDependencies();
		for (final Dependency dependency : dependencies) {
			final String artifactId = dependency.getArtifactId();
			if ("ij".equals(artifactId) || "imagej".equals(artifactId)) return true;
		}
		return false;
	}

	protected String interpolate(final String original,
		final MavenProject project)
		throws MojoExecutionException
	{
		if (original == null || original.indexOf("${") < 0) return original;
		try {
			RegexBasedInterpolator interpolator = new RegexBasedInterpolator();

			interpolator.addValueSource(new EnvarBasedValueSource());
			interpolator.addValueSource(new PropertiesBasedValueSource(System
				.getProperties()));

			List<String> synonymPrefixes = new ArrayList<>();
			synonymPrefixes.add("project.");
			synonymPrefixes.add("pom.");

			if (project != null) {
				PrefixedValueSourceWrapper modelWrapper =
					new PrefixedValueSourceWrapper(new ObjectBasedValueSource(project
						.getModel()), synonymPrefixes, true);
				interpolator.addValueSource(modelWrapper);

				PrefixedValueSourceWrapper pomPropertyWrapper =
					new PrefixedValueSourceWrapper(new PropertiesBasedValueSource(project
						.getModel().getProperties()), synonymPrefixes, true);
				interpolator.addValueSource(pomPropertyWrapper);
			}

			if (session != null) {
				interpolator.addValueSource(new PropertiesBasedValueSource(session
					.getExecutionProperties()));
			}

			RecursionInterceptor recursionInterceptor =
				new PrefixAwareRecursionInterceptor(synonymPrefixes, true);
			return interpolator.interpolate(original, recursionInterceptor);
		}
		catch (Exception e) {
			throw new MojoExecutionException("Could not interpolate '" + original +
				"'", e);
		}
	}

	protected void installArtifact(final Artifact artifact,
		final File appDir, final boolean force,
		final OtherVersions otherVersionsPolicy) throws IOException
	{
		installArtifact(artifact, appDir, "", force, otherVersionsPolicy);
	}

	protected void installArtifact(final Artifact artifact,
		final File appDir, final String appSubdir, final boolean force,
		final OtherVersions otherVersionsPolicy) throws IOException
	{
		if (!"jar".equals(artifact.getType())) return;

		final File source = artifact.getFile();
		final File targetDirectory;

		if (appSubdir != null && !appSubdir.equals("")) {
			targetDirectory = new File(appDir, appSubdir);
		}
		else if (isIJ1Plugin(source)) {
			targetDirectory = new File(appDir, "plugins");
		}
		else if (isBioFormatsArtifact(artifact)) {
			targetDirectory = new File(appDir, "jars/bio-formats");
		}
		else {
			targetDirectory = new File(appDir, "jars");
		}
		final String fileName = "Fiji_Updater".equals(artifact.getArtifactId())
			? artifact.getArtifactId() + ".jar" : source.getName();
		final File target = new File(targetDirectory, fileName);

		boolean newerVersion = false;
		final Path directoryPath = Paths.get(appDir.toURI());
		final Path targetPath = Paths.get(target.toURI());
		final Collection<Path> otherVersions = //
			getEncroachingVersions(directoryPath, targetPath);
		if (otherVersions != null && !otherVersions.isEmpty()) {
			for (final Path other : otherVersions) {
				final Path otherName = other.getFileName();
				switch (otherVersionsPolicy) {
					case never:
						getLog().warn("Possibly incompatible version exists: " + otherName);
						break;
					case older:
						final String toInstall = artifact.getVersion();
						final Matcher matcher = versionPattern.matcher(otherName.toString());
						if (!matcher.matches()) break;
						final String group = matcher.group(VERSION_INDEX);
						if (group == null) {
							newerVersion = true;
							getLog().warn("Impenetrable version suffix for file: " +
								otherName);
						}
						else {
							final String otherVersion = group.substring(1);
							newerVersion = VersionUtils.compare(toInstall, otherVersion) < 0;
							if (majorVersion(toInstall) != majorVersion(otherVersion)) {
								getLog().warn(
									"Found other version that is incompatible according to SemVer: " +
										otherVersion);
							}
						}
						if (newerVersion) break;
						//$FALL-THROUGH$
					case always:
						if (Files.deleteIfExists(other)) {
							getLog().info("Deleted overridden " + otherName);
							newerVersion = false;
						}
						else getLog().warn("Could not delete overridden " + otherName);
						break;
				}
			}
		}

		if (!force && target.exists() &&
			target.lastModified() > source.lastModified())
		{
			getLog().info("Dependency " + fileName + " is already there; skipping");
		}
		else if (newerVersion) {
			getLog().info("A newer version for " + fileName + " was detected; skipping");
		}
		else {
			getLog().info("Copying " + fileName + " to " + targetDirectory);
			FileUtils.copyFile(source, target);
		}
	}

	private static boolean isBioFormatsArtifact(final Artifact artifact) {
		final String fileName = artifact.getFile().getName();
		return "ome".equals(artifact.getGroupId()) ||
			("loci".equals(artifact.getGroupId()) && (fileName.startsWith(
				"scifio-4.4.") || fileName.startsWith("jai_imageio-4.4.")));
	}

	private static boolean isIJ1Plugin(final File file) {
		final String name = file.getName();
		if (name.indexOf('_') < 0 || !file.exists()) return false;
		if (file.isDirectory()) {
			return new File(file, "src/main/resources/plugins.config").exists();
		}
		if (!name.endsWith(".jar")) return false;

		try (final JarFile jar = new JarFile(file)) {
			for (final JarEntry entry : Collections.list(jar.entries())) {
				if (entry.getName().equals("plugins.config")) {
					jar.close();
					return true;
				}
			}
		}
		catch (final Throwable t) {
			// obviously not a plugin...
		}
		return false;
	}

	private final static Pattern versionPattern = Pattern.compile("(.+?)"
		+ "(-\\d+(\\.\\d+|\\d{7})+[a-z]?\\d?(-[A-Za-z0-9.]+?|\\.GA)*?)?"
		+ "((-(swing|swt|sources|javadoc|native|linux-x86|linux-x86_64|linux-armhf|linux-ppc64le|macosx-x86_64|windows-x86|windows-x86_64|android-x86|android-x86_64|android-arm|android-arm64|ios-x86_64|ios-arm64|natives-windows|natives-macos|natives-linux))?(\\.jar(-[a-z]*)?))");
	private final static int PREFIX_INDEX = 1;
	private final static int VERSION_INDEX = 2;
	private final static int SUFFIX_INDEX = 5;

	/**
	 * Extracts the major version (according to SemVer) from a version string.
	 * If no dot is found, the input is returned.
	 * 
	 * @param v
	 *            SemVer version string
	 * @return The major version (according to SemVer) as {@code String}.
	 */
	private String majorVersion( String v )
	{
		final int dot = v.indexOf('.');
		return dot < 0 ? v : v.substring(0, dot);
	}

	/**
	 * Looks for files in {@code directory} with the same base name as
	 * {@code file}.
	 *
	 * @param directory The directory to walk to find possible duplicates.
	 * @param file A {@link Path} to the target (from which the base name is
	 *          derived).
	 * @return A collection of {@link Path}s to files of the same base name.
	 */
	private Collection<Path> getEncroachingVersions(final Path directory, final Path file) {
		final Matcher matcher = versionPattern.matcher(file.getFileName().toString());
		if (!matcher.matches()) return null;

		final String prefix = matcher.group(PREFIX_INDEX);
		final String suffix = matcher.group(SUFFIX_INDEX);

		Collection<Path> result = new ArrayList<>();
		try {
			result = Files.walk(directory)
			.filter(path -> path.getFileName().toString().startsWith(prefix))
			.filter(path -> {
				final Matcher matcherIterator = versionPattern.matcher(path.getFileName().toString());
				return matcherIterator.matches() &&
					prefix.equals(matcherIterator.group(PREFIX_INDEX)) &&
					suffix.equals(matcherIterator.group(SUFFIX_INDEX));
			})
			.filter(path -> !path.getFileName().toString().equals(file.getFileName().toString()))
			.collect(Collectors.toCollection(ArrayList::new));
			return result;
		} catch (IOException e) {
			getLog().error(e);
		} finally {
			result = new ArrayList<>();
		}

		return result;
	}
}
