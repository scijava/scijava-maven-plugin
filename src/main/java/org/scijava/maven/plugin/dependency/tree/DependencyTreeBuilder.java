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
package org.scijava.maven.plugin.dependency.tree;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

/**
 * Builds a tree of dependencies for a given Maven 2 project. Notice that it doesn't fail with Maven 3, but when Maven 2
 * and Maven 3 don't calculate the same transitive dependency result, the tree calculated with this component is
 * consistent with Maven 2 even if run with Maven 3 (see <a
 * href="http://jira.codehaus.org/browse/MSHARED-167">MSHARED-167</a>.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTreeBuilder.java 1595871 2014-05-19 12:38:45Z jvanzyl $
 */
public interface DependencyTreeBuilder
{
    // fields -----------------------------------------------------------------

    /**
     * The plexus role for this component.
     */
    String ROLE = DependencyTreeBuilder.class.getName();

    // public methods ---------------------------------------------------------

    /**
     * Builds a tree of dependencies for the specified Maven project.
     * 
     * @param project the Maven project
     * @param repository the artifact repository to resolve against
     * @param factory the artifact factory to use
     * @param metadataSource the artifact metadata source to use
     * @param collector the artifact collector to use
     * @return the dependency tree of the specified Maven project
     * @throws DependencyTreeBuilderException if the dependency tree cannot be resolved
     * @deprecated As of 1.1, replaced by
     * {@link #buildDependencyTree(MavenProject, ArtifactRepository, ArtifactFactory, ArtifactMetadataSource,
     * ArtifactFilter, ArtifactCollector)}
     */
    DependencyTree buildDependencyTree( MavenProject project, ArtifactRepository repository, ArtifactFactory factory,
                                        ArtifactMetadataSource metadataSource, ArtifactCollector collector )
        throws DependencyTreeBuilderException;

    /**
     * Builds a tree of dependencies for the specified Maven project.
     * 
     * @param project the Maven project
     * @param repository the artifact repository to resolve against
     * @param factory the artifact factory to use
     * @param metadataSource the artifact metadata source to use
     * @param filter the artifact filter to use
     * @param collector the artifact collector to use
     * @return the dependency tree root node of the specified Maven project
     * @throws DependencyTreeBuilderException if the dependency tree cannot be resolved
     * @since 1.1
     */
    DependencyNode buildDependencyTree( MavenProject project, ArtifactRepository repository, ArtifactFactory factory,
                                        ArtifactMetadataSource metadataSource, ArtifactFilter filter,
                                        ArtifactCollector collector )
        throws DependencyTreeBuilderException;

    /**
     * @deprecated doesn't work with Maven 3
     */
    DependencyNode buildDependencyTree( MavenProject project )
        throws DependencyTreeBuilderException;

    /**
     * @since 2.1
     */
    DependencyNode buildDependencyTree( MavenProject project, ArtifactRepository repository, ArtifactFilter filter )
        throws DependencyTreeBuilderException;
}
