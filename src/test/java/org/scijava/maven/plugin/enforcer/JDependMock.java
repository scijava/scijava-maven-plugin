
/* ========================================================================
 * This file was adapted from the no-package-cycles-enforcer-rule project:
 * https://github.com/andrena/no-package-cycles-enforcer-rule
 *
 * Copyright 2013 - 2018 David Burkhart, Ben Romberg, Daniel Galan y Martins,
 * Bastian Feigl, Marc Philipp, and Carsten Otto.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ======================================================================== */

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
