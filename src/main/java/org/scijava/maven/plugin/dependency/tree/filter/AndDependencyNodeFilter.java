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
package org.scijava.maven.plugin.dependency.tree.filter;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.scijava.maven.plugin.dependency.tree.DependencyNode;

/**
 * A dependency node filter that logically ANDs together a number of other dependency node filters.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: AndDependencyNodeFilter.java 1595642 2014-05-18 17:32:08Z jvanzyl $
 * @since 1.1
 */
public class AndDependencyNodeFilter
    implements DependencyNodeFilter
{
    // fields -----------------------------------------------------------------

    /**
     * The dependency node filters that this filter ANDs together.
     */
    private final List<DependencyNodeFilter> filters;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node filter that logically ANDs together the two specified dependency node filters.
     * 
     * @param filter1 the first dependency node filter to logically AND together
     * @param filter2 the second dependency node filter to logically AND together
     */
    public AndDependencyNodeFilter( DependencyNodeFilter filter1, DependencyNodeFilter filter2 )
    {
        this( Arrays.asList( new DependencyNodeFilter[] { filter1, filter2 } ) );
    }

    /**
     * Creates a dependency node filter that logically ANDs together the specified dependency node filters.
     * 
     * @param filters the list of dependency node filters to logically AND together
     */
    public AndDependencyNodeFilter( List<DependencyNodeFilter> filters )
    {
        this.filters = Collections.unmodifiableList( filters );
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean accept( DependencyNode node )
    {
        for ( DependencyNodeFilter filter : filters )
        {
            if ( !filter.accept( node ) )
            {
                return false;
            }
        }

        return true;
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the list of dependency node filters that this filter ANDs together.
     * 
     * @return the dependency node filters that this filter ANDs together
     */
    public List<DependencyNodeFilter> getDependencyNodeFilters()
    {
        return filters;
    }
}
