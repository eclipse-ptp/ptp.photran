/*******************************************************************************
 * Copyright (c) 2010 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.preservation;

import java.util.Collection;

import org.eclipse.photran.internal.core.vpg.IVPGNode;
import org.eclipse.photran.internal.core.vpg.VPGEdge;

/**
 * 
 * @author Jeff Overbey
 */
final class MergePreservationAnalyzer<A, T, R extends IVPGNode<T>> extends PreservationAnalyzer<A, T, R>
{
    public MergePreservationAnalyzer(
        Collection<VPGEdge<A,T,R>> initialEdges,
        Collection<VPGEdge<A,T,R>> finalEdges,
        PreservationRuleset ruleset)
    {
        super(initialEdges, finalEdges, ruleset);
    }
}
