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
package org.scijava.maven.plugin.dependency.tree.traversal;

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

import java.util.Stack;

import org.scijava.maven.plugin.dependency.tree.DependencyNode;

/**
 * A dependency node visitor that clones visited nodes into a new dependency tree. This can be used in conjunction with
 * a dependency node filter to construct subtrees.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: BuildingDependencyNodeVisitor.java 1595642 2014-05-18 17:32:08Z jvanzyl $
 * @since 1.1
 */
public class BuildingDependencyNodeVisitor
    implements DependencyNodeVisitor
{
    // fields -----------------------------------------------------------------

    /**
     * The dependency node visitor to apply on the resultant dependency tree, or <code>null</code> for none.
     */
    private final DependencyNodeVisitor visitor;

    /**
     * The resultant tree parent nodes for the currently visited node.
     */
    private final Stack<DependencyNode> parentNodes;

    /**
     * The root node of the resultant tree.
     */
    private DependencyNode rootNode;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node visitor that clones visited nodes into a new dependency tree.
     */
    public BuildingDependencyNodeVisitor()
    {
        this( null );
    }

    /**
     * Creates a dependency node visitor that clones visited nodes into a new dependency tree, and then applies the
     * specified dependency node visitor on the resultant dependency tree.
     * 
     * @param visitor the dependency node visitor to apply on the resultant dependency tree, or <code>null</code> for
     *            none
     */
    public BuildingDependencyNodeVisitor( DependencyNodeVisitor visitor )
    {
        this.visitor = visitor;

        parentNodes = new Stack<DependencyNode>();
    }

    // DependencyNodeVisitor methods ------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean visit( DependencyNode node )
    {
        // clone the node
        DependencyNode newNode = new DependencyNode( node.getArtifact(), node.getState(), node.getRelatedArtifact() );
        newNode.setOriginalScope( node.getOriginalScope() );
        newNode.setFailedUpdateScope( node.getFailedUpdateScope() );
        newNode.setPremanagedVersion( node.getPremanagedVersion() );
        newNode.setPremanagedScope( node.getPremanagedScope() );

        if ( parentNodes.empty() )
        {
            rootNode = newNode;
        }
        else
        {
            DependencyNode parentNode = parentNodes.peek();
            parentNode.addChild( newNode );
        }

        parentNodes.push( newNode );

        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean endVisit( DependencyNode node )
    {
        parentNodes.pop();

        // apply the visitor to the resultant tree on the last visit
        if ( parentNodes.empty() && visitor != null )
        {
            rootNode.accept( visitor );
        }

        return true;
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the dependency node visitor that this visitor applies on the resultant dependency tree.
     * 
     * @return the dependency node visitor, or <code>null</code> for none
     */
    public DependencyNodeVisitor getDependencyNodeVisitor()
    {
        return visitor;
    }

    /**
     * Gets the root node of the resultant dependency tree constructed by this visitor.
     * 
     * @return the root node, or <code>null</code> if the source tree has not yet been visited
     */
    public DependencyNode getDependencyTree()
    {
        return rootNode;
    }
}
