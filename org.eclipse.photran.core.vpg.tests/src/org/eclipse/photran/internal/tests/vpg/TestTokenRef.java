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
package org.eclipse.photran.internal.tests.vpg;

import java.io.Serializable;

import org.eclipse.photran.internal.core.vpg.IVPGNode;
import org.eclipse.photran.internal.core.vpg.NodeRef;
import org.eclipse.photran.internal.core.vpg.VPG;
import org.eclipse.photran.internal.core.vpg.VPGDB;

public class TestTokenRef extends NodeRef<Object> implements Serializable
{
    private static final long serialVersionUID = 1L;

    private transient final VPGDB<?,?,TestTokenRef> db;

    public TestTokenRef(VPGDB<?,?,TestTokenRef> db, String filename, int offset, int length)
    {
        super(filename, offset, length);
        this.db = db;
    }

    @Override public Object getASTNode() { throw new UnsupportedOperationException(); }

    @Override protected <R extends IVPGNode<Object>> VPG< ? , Object, R> getVPG() { throw new UnsupportedOperationException(); }

    @SuppressWarnings("unchecked")
    @Override protected VPGDB<?,?,TestTokenRef> getDB() { return db; }
}