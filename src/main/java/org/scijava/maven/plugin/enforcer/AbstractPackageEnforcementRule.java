
package org.scijava.maven.plugin.enforcer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import jdepend.framework.JDepend;
import jdepend.framework.PackageFilter;

/**
 * Common functionality for writing an {@link EnforcerRule} dealing with the
 * package hierarchy.
 * 
 * @author Ben Romberg
 * @author David Burkhart
 * @author Gabriel Selzer
 */
public abstract class AbstractPackageEnforcementRule implements EnforcerRule {

	private List<String> excludedPackages = new ArrayList<>();
	private List<String> includedPackages = new ArrayList<>();
	private boolean includeTests = true;

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		try {
			executePackageCycleCheckIfNecessary(helper);
		}
		catch (ExpressionEvaluationException e) {
			throw new EnforcerRuleException("Unable to lookup an expression " + e
				.getLocalizedMessage(), e);
		}
		catch (IOException e) {
			throw new EnforcerRuleException("Unable to access target directory " + e
				.getLocalizedMessage(), e);
		}
	}

	@Override
	public String getCacheId() {
		return "";
	}

	@Override
	public boolean isCacheable() {
		return false;
	}

	@Override
	public boolean isResultValid(EnforcerRule arg0) {
		return false;
	}

	public void setExcludedPackages(List<String> excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	public void setIncludedPackages(List<String> includedPackages) {
		this.includedPackages = includedPackages;
	}

	public void setIncludeTests(boolean includeTests) {
		this.includeTests = includeTests;
	}

	private void executePackageCheck(EnforcerRuleHelper helper,
		Iterable<File> directories) throws IOException, EnforcerRuleException
	{
		JDepend jdepend = createJDepend(helper);
		for (File directory : directories) {
			jdepend.addDirectory(directory.getAbsolutePath());
		}
		jdepend.analyze();

		enforceRule(jdepend);
	}

	private void executePackageCycleCheckIfNecessary(EnforcerRuleHelper helper)
		throws ExpressionEvaluationException, IOException, EnforcerRuleException
	{
		DirectoriesWithClasses directories = new DirectoriesWithClasses(helper,
			"package cycles", includeTests);
		if (directories.directoriesWithClassesFound()) {
			executePackageCheck(helper, directories);
		}
		else {
			helper.getLog().info(
				"No directories with classes to check for cycles found.");
		}
	}

	protected JDepend createJDepend(EnforcerRuleHelper helper) {
		if (!includedPackages.isEmpty()) {
			helper.getLog().warn(
				"Package cycles rule check is restricted to check only these packages: " +
					includedPackages);
		}
		if (!excludedPackages.isEmpty()) {
			helper.getLog().warn(
				"These packages were excluded from package cycle rule check: " +
					excludedPackages);
		}
		return new JDepend(PackageFilter.all().including(includedPackages)
			.excluding(excludedPackages));
	}

	/**
	 * Enforces the rule
	 * 
	 * @param jdepend a {@link JDepend} containing relevant package information
	 * @throws EnforcerRuleException iff the rule is invalid for the packages
	 *           described by {@code jdepend}
	 */
	abstract void enforceRule(JDepend jdepend) throws EnforcerRuleException;

}
