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
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import java.util.Collection;
import java.util.Collections;

/**
 * Default dependency graph builder that detects current Maven version to delegate to either Maven 2 or Maven 3 specific
 * code.
 *
 * @see Maven2DependencyGraphBuilder
 * @see Maven3DependencyGraphBuilder
 * @author Hervé Boutemy
 * @since 2.0
 */
@Component( role = DependencyGraphBuilder.class )
public class DefaultDependencyGraphBuilder
    extends AbstractLogEnabled
    implements DependencyGraphBuilder, Contextualizable
{
    protected PlexusContainer container;

    /**
     * Builds a dependency graph.
     *
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @return DependencyNode containing the dependency graph.
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    public DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter )
        throws DependencyGraphBuilderException
    {
        return buildDependencyGraph( project, filter, null );
    }

    /**
     * Builds a dependency graph.
     *
     * @param project the project
     * @param filter artifact filter (can be <code>null</code>)
     * @param reactorProjects Collection of those projects contained in the reactor (can be <code>null</code>)
     * @return DependencyNode containing the dependency graph.
     * @throws DependencyGraphBuilderException if some of the dependencies could not be resolved.
     */
    public DependencyNode buildDependencyGraph( MavenProject project, ArtifactFilter filter,
                                                Collection<MavenProject> reactorProjects )
        throws DependencyGraphBuilderException
    {
        try
        {
            String hint = isMaven31() ? "maven31" : isMaven2x() ? "maven2" : "maven3";

            DependencyGraphBuilder effectiveGraphBuilder =
                (DependencyGraphBuilder) container.lookup( DependencyGraphBuilder.class.getCanonicalName(), hint );
            getLogger().debug( "building " + hint + " dependency graph for " + project.getId() + " with "
                                   + effectiveGraphBuilder.getClass().getSimpleName() );

            return effectiveGraphBuilder.buildDependencyGraph( project, filter, reactorProjects );
        }
        catch ( ComponentLookupException e )
        {
            throw new DependencyGraphBuilderException( e.getMessage(), e );
        }
    }

    /**
     * @return true if the current Maven is Maven 2.x.
     */
    protected static boolean isMaven2x()
    {
        return !canFindCoreClass( "org.apache.maven.project.DependencyResolutionRequest" ); // Maven 3 specific
    }

    /**
     * @return true if the current Maven version is Maven 3.1.
     */
    protected static boolean isMaven31()
    {
        return canFindCoreClass( "org.eclipse.aether.artifact.Artifact" ); // Maven 3.1 specific
    }

    private static boolean canFindCoreClass( String className )
    {
        try
        {
            Thread.currentThread().getContextClassLoader().loadClass( className );

            return true;
        }
        catch ( ClassNotFoundException e )
        {
            return false;
        }
    }

    /**
     * Injects the Plexus content.
     *
     * @param context   Plexus context to inject.
     * @throws ContextException if the PlexusContainer could not be located.
     */
    public void contextualize( Context context )
        throws ContextException
    {
        container = (PlexusContainer) context.get( PlexusConstants.PLEXUS_KEY );
    }
}
