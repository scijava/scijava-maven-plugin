/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2024 SciJava developers.
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
package org.scijava.maven.plugin.dependency.graph;

// Forked from org.apache.maven.shared:maven-dependency-tree:2.2

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.scijava.maven.plugin.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * Represents an artifact node within a Maven project's dependency graph. Notice there is no support for omitted nodes
 * at the moment, only dependencies kept in the resolved dependency list are available.
 *
 * @author Hervé Boutemy
 * @since 2.0
 */
public interface DependencyNode
{
    /**
     * @return Artifact for this DependencyNode.
     */
    Artifact getArtifact();

    /**
     * @return children of this DependencyNode.
     */
    List<DependencyNode> getChildren();

    /**
     * Applies the specified dependency node visitor to this dependency node and its children.
     * 
     * @param visitor the dependency node visitor to use
     * @return the visitor result of ending the visit to this node
     * @since 1.1
     */
    boolean accept( DependencyNodeVisitor visitor );

    /**
     * Gets the parent dependency node of this dependency node.
     * 
     * @return the parent dependency node
     */
    DependencyNode getParent();

    /**
     * Gets the version or version range for the dependency before dependency management was applied (if any).
     * 
     * @return The dependency version before dependency management or {@code null} if the version was not managed.
     */
    String getPremanagedVersion();

    /**
     * Gets the scope for the dependency before dependency management was applied (if any).
     * 
     * @return The dependency scope before dependency management or {@code null} if the scope was not managed.
     */
    String getPremanagedScope();

    /**
     * A constraint on versions for a dependency. A constraint can either consist of one or more version ranges or a
     * single version.
     * 
     * @return The constraint on the dependency.
     */
    String getVersionConstraint();

    /**
     * Returns a string representation of this dependency node.
     * 
     * @return the string representation
     */
    String toNodeString();
}
