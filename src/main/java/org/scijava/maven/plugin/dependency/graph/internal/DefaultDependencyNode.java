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

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.scijava.maven.plugin.dependency.graph.DependencyNode;
import org.scijava.maven.plugin.dependency.graph.traversal.DependencyNodeVisitor;

/**
 * Default implementation of a DependencyNode.
 */
public class DefaultDependencyNode implements DependencyNode
{
    private final Artifact artifact;

    private final DependencyNode parent;

    private final String premanagedVersion;

    private final String premanagedScope;

    private final String versionConstraint;

    private List<DependencyNode> children;

    /**
     * Constructs the DefaultDependencyNode.
     *
     * @param parent            Parent node.
     * @param artifact          Artifact associated with this dependency.
     * @param premanagedVersion
     * @param premanagedScope
     * @param versionConstraint
     */
    public DefaultDependencyNode( DependencyNode parent, Artifact artifact, String premanagedVersion,
                                  String premanagedScope, String versionConstraint )
    {
        this.parent = parent;
        this.artifact = artifact;
        this.premanagedVersion = premanagedVersion;
        this.premanagedScope = premanagedScope;
        this.versionConstraint = versionConstraint;
    }

    /**
     * Applies the specified dependency node visitor to this dependency node and its children.
     * 
     * @param visitor the dependency node visitor to use
     * @return the visitor result of ending the visit to this node
     * @since 1.1
     */
    public boolean accept( DependencyNodeVisitor visitor )
    {
        if ( visitor.visit( this ) )
        {
            for ( DependencyNode child : getChildren() )
            {
                if ( !child.accept( visitor ) )
                {
                    break;
                }
            }
        }

        return visitor.endVisit( this );
    }

    /**
     * @return Artifact for this DependencyNode.
     */
    public Artifact getArtifact()
    {
        return artifact;
    }

    /**
     *
     * @param children  List of DependencyNode to set as child nodes.
     */
    public void setChildren( List<DependencyNode> children )
    {
        this.children = children;
    }

    /**
     * @return List of child nodes for this DependencyNode.
     */
    public List<DependencyNode> getChildren()
    {
        return children;
    }

    /**
     * @return Parent of this DependencyNode.
     */
    public DependencyNode getParent()
    {
        return parent;
    }

    public String getPremanagedVersion()
    {
        return premanagedVersion;
    }

    public String getPremanagedScope()
    {
        return premanagedScope;
    }

    public String getVersionConstraint()
    {
        return versionConstraint;
    }

    /**
     * @return Stringified representation of this DependencyNode.
     */
    public String toNodeString()
    {
        StringBuffer buffer = new StringBuffer();

        buffer.append( artifact );

        ItemAppender appender = new ItemAppender( buffer, " (", "; ", ")" );

        if ( getPremanagedVersion() != null )
        {
            appender.append( "version managed from ", getPremanagedVersion() );
        }

        if ( getPremanagedScope() != null )
        {
            appender.append( "scope managed from ", getPremanagedScope() );
        }

        if ( getVersionConstraint() != null )
        {
            appender.append( "version selected from constraint ", getVersionConstraint() );
        }

        appender.flush();

        return buffer.toString();
    }

    /**
     * Utility class to concatenate a number of parameters with separator tokens.
     */
    private static class ItemAppender
    {
        private StringBuffer buffer;

        private String startToken;

        private String separatorToken;

        private String endToken;

        private boolean appended;

        public ItemAppender( StringBuffer buffer, String startToken, String separatorToken, String endToken )
        {
            this.buffer = buffer;
            this.startToken = startToken;
            this.separatorToken = separatorToken;
            this.endToken = endToken;

            appended = false;
        }

        public ItemAppender append( String item1, String item2 )
        {
            appendToken();

            buffer.append( item1 ).append( item2 );

            return this;
        }

        public void flush()
        {
            if ( appended )
            {
                buffer.append( endToken );

                appended = false;
            }
        }

        private void appendToken()
        {
            buffer.append( appended ? separatorToken : startToken );

            appended = true;
        }
    }
}
