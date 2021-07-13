package org.scijava.packages.plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;
import jdepend.framework.PackageFilter;

public class NoSubpackageDependenceRule implements EnforcerRule {

	private boolean includeTests = true;
	private List<String> includedPackages = new ArrayList<>();
	private List<String> excludedPackages = new ArrayList<>();

	@Override
	public void execute(EnforcerRuleHelper helper) throws EnforcerRuleException {
		try {
			executeSubpackageDependenceCheckIfNecessary(helper);
		} catch (ExpressionEvaluationException e) {
			throw new EnforcerRuleException("Unable to lookup an expression " + e.getLocalizedMessage(), e);
		} catch (IOException e) {
			throw new EnforcerRuleException("Unable to access target directory " + e.getLocalizedMessage(), e);
		}
	}

	private void executeSubpackageDependenceCheckIfNecessary(EnforcerRuleHelper helper)
			throws ExpressionEvaluationException, IOException, EnforcerRuleException {
		DirectoriesWithClasses directories = new DirectoriesWithClasses(helper, "subpackage dependence", includeTests);
		if (directories.directoriesWithClassesFound()) {
			executeSubpackageDependenceCheck(helper, directories);
		} else {
			helper.getLog().info("No directories with classes to check for subpackage dependence found.");
		}
	}

	private void executeSubpackageDependenceCheck(EnforcerRuleHelper helper, Iterable<File> directories) throws IOException, EnforcerRuleException {
		JDepend jdepend = createJDepend(helper);
		for (File directory : directories) {
			jdepend.addDirectory(directory.getAbsolutePath());
		}
		jdepend.analyze();

		Map<JavaPackage, List<JavaPackage>> subpackageLists = new HashMap<>();
		for (JavaPackage p : jdepend.getPackages()) {
			// create running List of subpackages, evaluate all previously added to the Map
			List<JavaPackage> subpackages = new ArrayList<>();
			for (JavaPackage other : subpackageLists.keySet()) {
				String dString = p.getName();
				String oString = other.getName();
				if (dString == oString) continue;
				if (dString.contains(oString)) subpackageLists.get(other).add(p);
				if (oString.contains(dString)) subpackages.add(other);
			}
			subpackageLists.put(p, subpackages);
		}
		Map<JavaPackage, List<JavaPackage>> subpackageDependence = new HashMap<>();
		for (Entry<JavaPackage, List<JavaPackage>> e : subpackageLists.entrySet()) {
			Collection<JavaPackage> efferents = e.getKey().getEfferents();
			List<JavaPackage> subpackagesDependedUpon = efferents.stream().filter(e.getValue()::contains).collect(Collectors.toList());
			subpackageDependence.put(e.getKey(), subpackagesDependedUpon);
		}
		for (List<JavaPackage> l : subpackageDependence.values()) {
			if (!l.isEmpty()) {
				throw new EnforcerRuleException("Some packages depend on subpackages:" +
					new SubpackageDependenceOutput(subpackageDependence).getOutput());
			}
		}
	}

	protected JDepend createJDepend(EnforcerRuleHelper helper) {
		if (!includedPackages.isEmpty()) {
			helper.getLog().warn("Subpackage dependence rule check is restricted to check only these packages: " + includedPackages);
		}
		if (!excludedPackages.isEmpty()) {
			helper.getLog().warn("These packages were excluded from subpackage dependence rule check: " + excludedPackages);
		}
		return new JDepend(PackageFilter.all()
				.including(includedPackages)
				.excluding(excludedPackages));
	}

	public void setIncludeTests(boolean includeTests) {
		this.includeTests = includeTests;
	}

	public void setIncludedPackages(List<String> includedPackages) {
		this.includedPackages = includedPackages;
	}

	public void setExcludedPackages(List<String> excludedPackages) {
		this.excludedPackages = excludedPackages;
	}

	@Override
	public boolean isCacheable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResultValid(EnforcerRule cachedRule) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getCacheId() {
		// TODO Auto-generated method stub
		return null;
	}

}