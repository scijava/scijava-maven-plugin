package org.scijava.packages.rules;

import static org.scijava.packages.rules.CollectionOutput.joinCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.scijava.packages.rules.CollectionOutput.Appender;
import org.scijava.packages.rules.comparator.JavaClassNameComparator;
import org.scijava.packages.rules.comparator.JavaPackageNameComparator;

import jdepend.framework.JavaClass;
import jdepend.framework.JavaPackage;

public class SubpackageDependenceOutput {

	private Map<JavaPackage, List<JavaPackage>> packages;
	private StringBuilder output;

	public SubpackageDependenceOutput(Map<JavaPackage, List<JavaPackage>> packages) {
		this.packages = packages;
	}

	public String getOutput() {
		output = new StringBuilder();
		for (Map.Entry<JavaPackage, List<JavaPackage>> e : collectAndSortCycles()) {
			if (!e.getValue().isEmpty()) appendOutputForSubpackageDependence(e);
		}
		return output.toString();
	}

	private List<Map.Entry<JavaPackage, List<JavaPackage>>> collectAndSortCycles() {
		List<Map.Entry<JavaPackage, List<JavaPackage>>> orderedList = new ArrayList<>();
		for (Map.Entry<JavaPackage, List<JavaPackage>> e : packages.entrySet()) {
			Collections.sort(e.getValue(), JavaPackageNameComparator.INSTANCE);
			orderedList.add(e);
		}
		Collections.sort(orderedList, (e1, e2) -> JavaPackageNameComparator.INSTANCE.compare(e1.getKey(), e2.getKey()));
		return orderedList;
	}

	private void appendOutputForSubpackageDependence(Map.Entry<JavaPackage, List<JavaPackage>> subpackageDependence) {
		JavaPackage p = subpackageDependence.getKey();
		packages.remove(p);
		appendHeaderForSubpackageDependence(subpackageDependence);
		for (JavaPackage cyclicPackage : subpackageDependence.getValue()) {
			appendOutputForSubPackage(p, cyclicPackage);
		}
	}

	private void appendHeaderForSubpackageDependence(Map.Entry<JavaPackage, List<JavaPackage>> entry) {
		output.append("\n\n").append("Package " + entry.getKey().getName() + " depends on subpackages: ");
	}

	private void appendOutputForSubPackage(final JavaPackage javaPackage, JavaPackage subpackage) {
		if (javaPackage.equals(subpackage)) {
			return;
		}
		List<JavaClass> dependentClasses = getOrderedDependentClasses(javaPackage, subpackage);
		if (!dependentClasses.isEmpty()) {
			appendOutputForDependentCyclicPackage(subpackage, dependentClasses);
		}
	}

	private List<JavaClass> getOrderedDependentClasses(JavaPackage javaPackage, JavaPackage cyclicPackage) {
		List<JavaClass> dependentClasses = new ArrayList<>();
		Collection<JavaClass> allClasses = javaPackage.getClasses();
		for (JavaClass javaClass : allClasses) {
			if (javaClass.getImportedPackages().contains(cyclicPackage)) {
				dependentClasses.add(javaClass);
			}
		}
		Collections.sort(dependentClasses, JavaClassNameComparator.INSTANCE);
		return dependentClasses;
	}

	private void appendOutputForDependentCyclicPackage(JavaPackage cyclicPackage,
			List<JavaClass> dependentClasses) {
		output.append("\n        ").append(cyclicPackage.getName()).append(" (");
		appendOutputForCyclicPackageClasses(dependentClasses);
		output.append(")");
	}

	private void appendOutputForCyclicPackageClasses(List<JavaClass> dependentClasses) {
		joinCollection(dependentClasses, output, new Appender<JavaClass>() {
			@Override
			public void append(JavaClass packageClass) {
				output.append(packageClass.getName().substring(packageClass.getPackageName().length() + 1));
			}
		}, ", ");
	}

}
