package org.scijava.maven.plugin.enforcer;

import java.io.IOException;

import jdepend.framework.JDepend;

public class JDependMock extends JDepend {
	private boolean addDirectoryThrowsException;
	private boolean containsCycles;
	private boolean subpackageDependence;

	public void setAddDirectoryThrowsException(boolean addDirectoryThrowsException) {
		this.addDirectoryThrowsException = addDirectoryThrowsException;
	}

	@Override
	public void addDirectory(String name) throws IOException {
		if (addDirectoryThrowsException) {
			throw new IOException();
		}
		super.addDirectory(name);
	}

	@Override
	public boolean containsCycles() {
		return containsCycles;
	}

	public boolean containsSubpackageDependence() {
		return subpackageDependence;
	}

	public void setContainsCycles(boolean containsCycles) {
		this.containsCycles = containsCycles;
	}

	public void setSubpackageDependence(boolean dependsOnSubpackage) {
		this.subpackageDependence = dependsOnSubpackage;
	}
}
