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

package org.scijava.maven.plugin;

import java.util.Set;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;

/**
 * {@link DependencyNodeVisitor} with convenience methods for logging and
 * configuration.
 *
 * @author Mark Hiner
 */
public interface SciJavaDependencyChecker extends DependencyNodeVisitor {

	/**
	 * @param log {@link Log} to use by the {@link #debug}, {@link #error} and
	 *          {@link #info} methods.
	 */
	void setLog(final Log log);

	/**
	 * @param message Prints to the current {@link Log}'s debug output, if a log
	 *          is present.
	 */
	void debug(final String message);

	/**
	 * @param message Prints to the current {@link Log}'s error output, if a log
	 *          is present.
	 */
	void error(final String message);

	/**
	 * @param message Prints to the current {@link Log}'s info output, if a log is
	 *          present.
	 */
	void info(final String message);

	/**
	 * @param groupIds An optional inclusive list of Maven groupIds. If not null,
	 *          implementations should only fail on artifacts whose groupIds are
	 *          contained in this set.
	 */
	void setGroupIds(final Set<String> groupIds);

	/**
	 * @param groupId Maven groupId to test.
	 * @return true if the given groupId should be considered for checker failure.
	 */
	boolean matches(final String groupId);

	/**
	 * @param failFast Flag indicating if a checker implementation should continue
	 *          after encountering its first error.
	 */
	void setFailFast(final boolean failFast);

	/**
	 * @return If true, this checker should stop visiting nodes after it
	 *         encounters its first error.
	 */
	boolean isFailFast();

	/**
	 * Convenience method to check if a node is a root.
	 *
	 * @param node {@link DependencyNode} to test.
	 * @return true iff the given node is a root (no parents).
	 */
	boolean isRoot(final DependencyNode node);

	/**
	 * Mark this checker as in a failing state.
	 */
	void setFailed();

	/**
	 * @return True if this checker is in a failed state.
	 */
	boolean failed();

	/**
	 * @return true if this checker should stop visiting nodes.
	 */
	boolean stopVisit();

	/**
	 * @return A formatted message reporting any encountered failures.
	 */
	String makeExceptionMessage();
}
