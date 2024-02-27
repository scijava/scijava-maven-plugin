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

import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

import java.util.Collection;

/**
 * Maven project dependency graph builder API, neutral against Maven 2 or Maven 3.
 *
 * @author Hervé Boutemy
 * @since 2.0
 */
public interface DependencyGraphBuilder
{
    /**
     * Build the dependency graph.
     *
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @return the dependency graph
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter )
        throws DependencyGraphBuilderException;

    /**
     * Build the dependency graph, with a hack to include dependencies contained in the reactor projects
     * but that are not yet compiled, which is the minimum prerequisite for Maven core's
     * ReactorReader to find them. Notice that this hack hasn't been done for Maven 2.
     * <p>Notice: If Maven core did collect instead of resolving dependencies (ie did not try to get the
     * artifacts but only the poms), probably this hack wouldn't be necessary even for people requiring
     * the dependency graph before compiling. TODO: for Maven 3, use Aether to collect dependencies.</p>
     *
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @param reactorProjects Collection of those projects contained in the reactor (can be <code>null</code>).
     * @return the dependency graph
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter,
                                         Collection<MavenProject> reactorProjects )
        throws DependencyGraphBuilderException;
}
