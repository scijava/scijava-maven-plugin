/*-
 * #%L
 * A plugin for managing SciJava-based projects.
 * %%
 * Copyright (C) 2014 - 2025 SciJava developers.
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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ResolutionListener;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.artifact.InvalidDependencyVersionException;
import org.scijava.maven.plugin.dependency.tree.traversal.CollectingDependencyNodeVisitor;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

/**
 * Default implementation of <code>DependencyTreeBuilder</code>.
 * 
 * @author Edwin Punzalan
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DefaultDependencyTreeBuilder.java 1595871 2014-05-19 12:38:45Z jvanzyl $
 * @see DependencyTreeBuilder
 */
@Component( role = DependencyTreeBuilder.class )
public class DefaultDependencyTreeBuilder
    extends AbstractLogEnabled
    implements DependencyTreeBuilder
{
    @Requirement
    private MavenSession session;

    @Requirement
    private ArtifactFactory factory;

    @Requirement
    private ArtifactMetadataSource metadataSource;

    /**
     * Artifact collector component.
     */
    @Requirement
    private ArtifactCollector collector;

    // fields -----------------------------------------------------------------

    private ArtifactResolutionResult result;

    // DependencyTreeBuilder methods ------------------------------------------

    /**
     * {@inheritDoc}
     * 
     * @deprecated
     */
    public DependencyTree buildDependencyTree( MavenProject project, ArtifactRepository repository,
                                               ArtifactFactory factory, ArtifactMetadataSource metadataSource,
                                               ArtifactCollector collector )
        throws DependencyTreeBuilderException
    {
        DependencyNode rootNode = buildDependencyTree( project, repository, factory, metadataSource, null, collector );

        CollectingDependencyNodeVisitor collectingVisitor = new CollectingDependencyNodeVisitor();
        rootNode.accept( collectingVisitor );

        return new DependencyTree( rootNode, collectingVisitor.getNodes() );
    }

    /**
     * {@inheritDoc}
     */
    public DependencyNode buildDependencyTree( MavenProject project, ArtifactRepository repository,
                                               ArtifactFactory factory, ArtifactMetadataSource metadataSource,
                                               ArtifactFilter filter, ArtifactCollector collector )
        throws DependencyTreeBuilderException
    {
        DependencyTreeResolutionListener listener = new DependencyTreeResolutionListener( getLogger() );

        try
        {
            @SuppressWarnings( "unchecked" )
            Map<String, Artifact> managedVersions = project.getManagedVersionMap();

            @SuppressWarnings( "unchecked" )
            Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();

            if ( dependencyArtifacts == null )
            {
                dependencyArtifacts = project.createArtifacts( factory, null, null );
            }

            getLogger().debug( "Dependency tree resolution listener events:" );

            // TODO: note that filter does not get applied due to MNG-3236

            result =
                collector.collect( dependencyArtifacts, project.getArtifact(), managedVersions, repository,
                                   project.getRemoteArtifactRepositories(), metadataSource, filter,
                                   Collections.singletonList( (ResolutionListener) listener ) );

            return listener.getRootNode();
        }
        catch ( InvalidDependencyVersionException e )
        {
            throw new DependencyTreeBuilderException( "Invalid dependency version for artifact "
                + project.getArtifact() );
        }
    }

    /**
     * Builds a dependency tree.
     *
     * @param project   MavenProject for which ot build the dependency tree.
     * @return DependencyNode containing the dependency tree for the project.
     * @throws DependencyTreeBuilderException if the dependency tree could not be built.
     */
    public DependencyNode buildDependencyTree( MavenProject project )
        throws DependencyTreeBuilderException
    {
        return buildDependencyTree( project, session.getLocalRepository(), factory,
                                    metadataSource, null, collector );
    }

    /**
     * Builds a dependency tree.
     *
     * @param project       MavenProject for which ot build the dependency tree.
     * @param repository    ArtifactRepository to search fro dependencies.
     * @param filter        Filter to apply when searching for dependencies.
     * @return DependencyNode containing the dependency tree for the project.
     * @throws DependencyTreeBuilderException if the dependency tree could not be built.
     */
    public DependencyNode buildDependencyTree( MavenProject project, ArtifactRepository repository,
                                               ArtifactFilter filter )
        throws DependencyTreeBuilderException
    {
        return buildDependencyTree( project, repository, factory, metadataSource, filter, collector );
    }

    // protected methods ------------------------------------------------------

    protected ArtifactResolutionResult getArtifactResolutionResult()
    {
        return result;
    }
}
