
package org.scijava.packages.plugin;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;

import jdepend.framework.JDepend;
import jdepend.framework.JavaPackage;

/**
 * Detects the presence of cycles in the package hierarchy.
 *
 * @author Gabriel Selzer
 */
public class NoPackageCyclesRule extends AbstractPackageEnforcementRule {

	private String getPackageCycles(JDepend jdepend) {
		Collection<JavaPackage> packages = jdepend.getPackages();
		return new PackageCycleOutput(new ArrayList<>(packages)).getOutput();
	}

	@Override
	void enforceRule(JDepend jdepend) throws EnforcerRuleException {
		if (jdepend.containsCycles()) {
			throw new EnforcerRuleException("There are package cycles:" +
				getPackageCycles(jdepend));
		}
	}

}
