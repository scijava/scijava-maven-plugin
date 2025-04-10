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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;

/**
 * Represents a Maven project's dependency tree.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTree.java 1100703 2011-05-08 08:27:33Z hboutemy $
 * @deprecated As of 1.1, replaced by the dependency tree root {@link DependencyNode}
 */
public class DependencyTree
{
    // fields -----------------------------------------------------------------

    private final DependencyNode rootNode;

    private final Collection<DependencyNode> nodes;

    // constructors -----------------------------------------------------------

    /**
     * Create a tree initialized to the arguments
     * 
     * @param rootNode
     * @param nodes
     */
    public DependencyTree( DependencyNode rootNode, Collection<DependencyNode> nodes )
    {
        this.rootNode = rootNode;
        this.nodes = nodes;
    }

    // public methods ---------------------------------------------------------

    public DependencyNode getRootNode()
    {
        return rootNode;
    }

    public Collection<DependencyNode> getNodes()
    {
        return nodes;
    }

    public List<Artifact> getArtifacts()
    {
        List<Artifact> artifacts = new ArrayList<Artifact>();

        for ( DependencyNode node : getNodes() )
        {
            artifacts.add( node.getArtifact() );
        }

        return artifacts;
    }

    public String toString()
    {
        return getRootNode().toString();
    }

    /**
     * @see DependencyNode#iterator()
     */
    public Iterator<DependencyNode> iterator()
    {
        return getRootNode().iterator();
    }

    /**
     * @see DependencyNode#preorderIterator()
     */
    public Iterator<DependencyNode> preorderIterator()
    {
        return getRootNode().preorderIterator();
    }

    /**
     * @see DependencyNode#inverseIterator()
     */
    public Iterator<DependencyNode> inverseIterator()
    {
        return getRootNode().inverseIterator();
    }
}
