/*
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 Board of Regents of the University of
 * Wisconsin-Madison.
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

/**
 * Abstract {@link SciJavaDependencyChecker} superclass, containing useful
 * default implementations.
 * 
 * @author Mark Hiner
 */
public abstract class AbstractSciJavaDependencyChecker implements
	SciJavaDependencyChecker
{

	// -- Fields --

	private boolean failFast = false;

	private boolean failed = false;

	private Set<String> groupIds;

	private Log log;

	// -- SciJavaDependencyNodeVisitor API --

	@Override
	public void setLog(final Log log) {
		this.log = log;
	}

	@Override
	public void debug(final String message) {
		if (log != null) log.debug(message);
	}

	@Override
	public void error(final String message) {
		if (log != null) log.error(message);
	}

	@Override
	public void info(final String message) {
		if (log != null) log.info(message);
	}

	@Override
	public void setGroupIds(final Set<String> groupIds) {
		this.groupIds = groupIds;
	}

	@Override
	public boolean matches(final String groupId) {
		if (groupIds == null || groupIds.isEmpty()) return true;
		return groupIds.contains(groupId);
	}

	@Override
	public void setFailFast(final boolean failFast) {
		this.failFast = failFast;
	}

	@Override
	public boolean isFailFast() {
		return failFast;
	}

	@Override
	public boolean isRoot(final DependencyNode node) {
		return node.getParent() == null;
	}

	@Override
	public void setFailed() {
		failed = true;
	}

	@Override
	public boolean failed() {
		return failed;
	}

	@Override
	public boolean stopVisit() {
		return isFailFast() && failed();
	}

	// -- DependencyNodeVisitor API --

	@Override
	public boolean endVisit(final DependencyNode node) {
		// NB: if this is the root, the return value indicates if there was an error
		// or not in this visitation session.
		return isRoot(node) ? failed() : !stopVisit();
	}
}
