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
package org.scijava.maven.plugin.dependency.graph.internal;

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
import org.scijava.maven.plugin.dependency.graph.DependencyGraphBuilder;
import org.scijava.maven.plugin.dependency.graph.DependencyGraphBuilderException;
import org.scijava.maven.plugin.dependency.graph.DependencyNode;
import org.scijava.maven.plugin.dependency.tree.DependencyTreeBuilder;
import org.scijava.maven.plugin.dependency.tree.DependencyTreeBuilderException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Wrapper around Maven 2 dependency tree builder.
 *
 * @see DependencyTreeBuilder
 * @author Hervé Boutemy
 * @since 2.0
 */
@Component( role = DependencyGraphBuilder.class, hint = "maven2" )
public class Maven2DependencyGraphBuilder
    extends AbstractLogEnabled
    implements DependencyGraphBuilder
{
    @Requirement
    private DependencyTreeBuilder treeBuilder;

    /**
     * Builds the dependency graph for Maven 2.
     *
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @return DependencyNode containing the dependency graph.
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    public DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter )
        throws DependencyGraphBuilderException
    {
        try
        {
            return buildDependencyNode( null, treeBuilder.buildDependencyTree( project ), filter );
        }
        catch ( DependencyTreeBuilderException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
    }

    /**
     * Builds the dependency graph for Maven 2.
     * <p>
     * notice: the reactor projects are ignored as no work has been done to try to do the same hack as with Maven 3.
     * </p>
     * 
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @param reactorProjects Ignored.
     * @return DependencyNode containing the dependency graph.
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    public DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter,
                                                Collection<MavenProject> reactorProjects )
        throws DependencyGraphBuilderException
    {
        if ( reactorProjects != null )
        {
            getLogger().warn( "Reactor projects ignored - reactor project collection not implemented" );
        }

        return buildDependencyGraph( project, filter );
    }

    private DependencyNode buildDependencyNode( DependencyNode parent,
                                                org.scijava.maven.plugin.dependency.tree.DependencyNode node,
                                                ArtifactFilter filter )
    {
        String versionSelectedFromRange = null;
        if ( node.getVersionSelectedFromRange() != null )
        {
            versionSelectedFromRange = node.getVersionSelectedFromRange().toString();
        }

        DefaultDependencyNode current =
            new DefaultDependencyNode( parent, node.getArtifact(), node.getPremanagedVersion(),
                                       node.getPremanagedScope(), versionSelectedFromRange );

        List<DependencyNode> nodes = new ArrayList<DependencyNode>( node.getChildren().size() );
        for ( org.scijava.maven.plugin.dependency.tree.DependencyNode child : node.getChildren() )
        {
            if ( child.getState() != org.scijava.maven.plugin.dependency.tree.DependencyNode.INCLUDED )
            {
                // only included nodes are supported in the graph API
                continue;
            }

            if ( ( filter == null ) || filter.include( child.getArtifact() ) )
            {
                nodes.add( buildDependencyNode( current, child, filter ) );
            }
        }

        current.setChildren( Collections.unmodifiableList( nodes ) );

        return current;
    }
}
