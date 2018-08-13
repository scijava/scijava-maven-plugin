/*
 * #%L
 * ImageJ software for multidimensional image processing and analysis.
 * %%
 * Copyright (C) 2012 - 2016 Board of Regents of the University of
 * Wisconsin-Madison, Broad Institute of MIT and Harvard, and Max Planck
 * Institute of Molecular Cell Biology and Genetics.
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
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
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
public abstract class AbstractCopyJarsMojo extends AbstractMojo {

	/**
	 * Path to the ImageJ.app/ directory to which artifacts are copied.
	 * <p>
	 * If it is not a directory, no .jar files are copied.
	 * </p>
	 */
	@Deprecated
	@Parameter(property = imagejDirectoryProperty, required = false)
	String imagejDirectory;

	/**
	 * Path to a SciJava application directory (e.g. ImageJ.app) to which
	 * artifacts are copied.
	 * <p>
	 * If it is not a directory, no .jar files are copied.
	 * </p>
	 */
	@Parameter(property = appDirectoryProperty, required = false)
	String appDirectory;

	/**
	 * The name of the property pointing to the subdirectory (beneath e.g.
	 * {@code jars/} or {@code plugins/}) to which the artifact should be copied.
	 * <p>
	 * If no property of that name exists, no subdirectory will be used.
	 * </p>
	 */
	@Deprecated
	@Parameter(property = imagejSubdirectoryProperty, required = false)
	String imagejSubdirectory;

	/**
	 * The name of the property pointing to the subdirectory (beneath e.g.
	 * {@code jars/} or {@code plugins/}) to which the artifact should be copied.
	 * <p>
	 * If no property of that name exists, no subdirectory will be used.
	 * </p>
	 */
	@Parameter(property = appSubdirectoryProperty, required = false)
	String appSubdirectory;

	/**
	 * Whether to delete other versions when copying the files.
	 * <p>
	 * When copying a file and its dependencies to an ImageJ.app/ directory and
	 * there are other versions of the same file, we can warn or delete those
	 * other versions.
	 * </p>
	 */
	@Deprecated
	@Parameter(property = deleteOtherVersionsProperty)
	boolean deleteOtherVersions;

	/**
	 * Whether to delete other versions when copying the files.
	 * <p>
	 * When copying a file and its dependencies to an ImageJ.app/ directory and
	 * there are other versions of the same file, we can warn or delete those
	 * other versions.
	 * </p>
	 */
	@Deprecated
	@Parameter(property = imagejDeleteOtherVersionsPolicyProperty)
	OtherVersions imagejDeleteOtherVersionsPolicy;

	/**
	 * Whether to delete other versions when copying the files.
	 * <p>
	 * When copying a file and its dependencies to a SciJava application directory
	 * and there are other versions of the same file, we can warn or delete those
	 * other versions.
	 * </p>
	 */
	@Parameter(property = deleteOtherVersionsPolicyProperty, defaultValue = "older")
	OtherVersions deleteOtherVersionsPolicy;

	/**
	 * If this option is set to <code>true</code>, only the artifact will be
	 * copied - without its dependencies.
	 */
	@Parameter(property = ignoreDependenciesProperty, defaultValue = "false")
	boolean ignoreDependencies;

	@Parameter(defaultValue = "${session}")
	MavenSession session;

	@Parameter( defaultValue = "${mojoExecution}", readonly = true )
	MojoExecution mojoExecution;

	public static final String imagejDirectoryProperty = "imagej.app.directory";
	public static final String imagejSubdirectoryProperty = "imagej.app.subdirectory";
	public static final String deleteOtherVersionsProperty = "delete.other.versions";
	public static final String imagejDeleteOtherVersionsPolicyProperty = "imagej.deleteOtherVersions";

	public static final String appDirectoryProperty = "scijava.app.directory";
	public static final String appSubdirectoryProperty = "scijava.app.subdirectory";
	public static final String deleteOtherVersionsPolicyProperty = "scijava.deleteOtherVersions";
	public static final String ignoreDependenciesProperty = "scijava.ignoreDependencies";

	public enum OtherVersions {
			always, older, never
	}

	/**
	 * Handles the backward compatibility with properties previously defined by
	 * imagej-maven-plugin.
	 */
	void handleBackwardCompatibility() {
		ExpressionEvaluator evaluator = new PluginParameterExpressionEvaluator(session, mojoExecution);

		try {
			// If at least one scijava.* property is set, ignore imagej.* properties
			if (evaluator.evaluate("${" + appDirectoryProperty + "}") == null &&
				evaluator.evaluate("${" + appSubdirectoryProperty + "}") == null &&
				evaluator.evaluate("${" + deleteOtherVersionsPolicyProperty + "}") == null)
			{

				// Keep backwards compatibility to delete.other.versions
				if (evaluator.evaluate("${"+deleteOtherVersionsProperty+"}") != null) {
					getLog().warn("Property '" + deleteOtherVersionsProperty + "' is deprecated. Use '"+ deleteOtherVersionsPolicyProperty +"' instead");
					deleteOtherVersionsPolicy = deleteOtherVersions ? OtherVersions.older : OtherVersions.never;
				}

				// Keep backwards compatibility to imagej.app.directory
				// Use imagejDirectory if it is set (directly or via imagej.app.directory)
				if (imagejDirectory != null) {
					if (evaluator.evaluate("${"+imagejDirectoryProperty+"}") == null) {
						getLog().warn("Configuration property 'imagejDirectory' is deprecated. Use 'appDirectory' instead");
					} else {
						getLog().warn("Property '" + imagejDirectoryProperty + "' is deprecated. Use '"+ appDirectoryProperty +"' instead");
					}
					appDirectory = imagejDirectory;
				}

				// Keep backwards compatibility to imagej.app.subdirectory
				// Use imagejSubdirectory if it is set (directly or via imagej.app.subdirectory)
				if (imagejSubdirectory != null) {
					if (evaluator.evaluate("${"+imagejSubdirectoryProperty+"}") == null) {
						getLog().warn("Configuration property 'imagejSubdirectory' is deprecated. Use 'appSubdirectory' instead");
					} else {
						getLog().warn("Property '" + imagejSubdirectoryProperty + "' is deprecated. Use '"+ appSubdirectoryProperty +"' instead");
					}
					appSubdirectory = imagejSubdirectory;
				}

				// Keep backwards compatibility to imagej.deleteOtherVersions
				// Use imagejDeleteOtherVersionsPolicy if it is set (directly or via imagej.deleteOtherVersions)
				if (imagejDeleteOtherVersionsPolicy != null) {
					if (evaluator.evaluate("${"+imagejDeleteOtherVersionsPolicyProperty+"}") == null) {
						getLog().warn("Configuration property 'imagejDeleteOtherVersionsPolicy' is deprecated. Use 'deleteOtherVersionsPolicy' instead");
					} else {
						getLog().warn("Property '" + imagejDeleteOtherVersionsPolicyProperty + "' is deprecated. Use '"+ deleteOtherVersionsPolicyProperty +"' instead");
					}
					deleteOtherVersionsPolicy = imagejDeleteOtherVersionsPolicy;
				}
			}
		}
		catch (ExpressionEvaluationException e) {
			getLog().warn(e);
		}
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
		final MavenProject project, final MavenSession session)
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
		final File imagejDirectory, final boolean force,
		final OtherVersions otherVersionsPolicy) throws IOException
	{
		installArtifact(artifact, imagejDirectory, "", force, otherVersionsPolicy);
	}

	protected void installArtifact(final Artifact artifact,
		final File imagejDirectory, final String subdirectory, final boolean force,
		final OtherVersions otherVersionsPolicy) throws IOException
	{
		if (!"jar".equals(artifact.getType())) return;

		final File source = artifact.getFile();
		final File targetDirectory;

		if (subdirectory != null && !subdirectory.equals("")) {
			targetDirectory = new File(imagejDirectory, subdirectory);
		} else if (isIJ1Plugin(source)) {
			targetDirectory = new File(imagejDirectory, "plugins");
		}
		else if ("ome".equals(artifact.getGroupId()) ||
			("loci".equals(artifact.getGroupId()) && (source.getName().startsWith(
				"scifio-4.4.") || source.getName().startsWith("jai_imageio-4.4."))))
		{
			targetDirectory = new File(imagejDirectory, "jars/bio-formats");
		}
		else {
			targetDirectory = new File(imagejDirectory, "jars");
		}
		final String fileName = "Fiji_Updater".equals(artifact.getArtifactId())
			? artifact.getArtifactId() + ".jar" : source.getName();
		final File target = new File(targetDirectory, fileName);

		boolean newerVersion = false;
		final Path directoryPath = Paths.get(imagejDirectory.toURI());
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
		+ "((-(swing|swt|sources|javadoc|native|linux-x86|linux-x86_64|macosx-x86_64|windows-x86|windows-x86_64|android-arm|android-x86|natives-windows|natives-macos|natives-linux))?(\\.jar(-[a-z]*)?))");
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
