/*******************************************************************************
 * Copyright (c) 2009 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.vpg;

import java.io.Serializable;

/**
 * Standard implementation of {@link IVPGNode}.
 * 
 * @author Jeff Overbey
 * 
 * @param <T> node/token type
 */
public abstract class NodeRef<T> implements IVPGNode<T>
{
	private static final long serialVersionUID = 1L;

	protected final String filename;
	protected final int offset;
	protected final int length;

	/** Constructor.  Creates a TokenRef referring to the token at
	 *  the given position in the given file. */
	public NodeRef(String filename, int offset, int length)
	{
		this.filename = filename;
		this.offset = offset;
		this.length = length;
	}

    /** Copy constructor. */
	public NodeRef(NodeRef<T> copyFrom)
	{
		this.filename = copyFrom.filename;
		this.offset = copyFrom.offset;
		this.length = copyFrom.length;
	}

    ///////////////////////////////////////////////////////////////////////////
	// Accessors
    ///////////////////////////////////////////////////////////////////////////

	public String getFilename()
	{
		return filename;
	}

	public int getOffset()
	{
		return offset;
	}

	public int getLength()
	{
		return length;
	}

    ///////////////////////////////////////////////////////////////////////////
    // AST Mapping
    ///////////////////////////////////////////////////////////////////////////

    public abstract T getASTNode();

    ///////////////////////////////////////////////////////////////////////////
    // AST Mapping
    ///////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    public <R extends IVPGNode<T>> Iterable<R> followOutgoing(int edgeType)
    {
        return this.<R>getDB().getOutgoingEdgeTargets((R)this, edgeType);
    }

    @SuppressWarnings("unchecked")
    public <R extends IVPGNode<T>> Iterable<R> followIncoming(int edgeType)
    {
        return this.<R>getDB().getIncomingEdgeSources((R)this, edgeType);
    }

    @SuppressWarnings("unchecked")
    public <R extends Serializable> R getAnnotation(int annotationID)
    {
        return (R)getDB().getAnnotation(this, annotationID);
    }

    ///////////////////////////////////////////////////////////////////////////
	// Utility Methods
    ///////////////////////////////////////////////////////////////////////////

    protected abstract <R extends IVPGNode<T>> VPG<?, T, R> getVPG();
    
    protected <R extends IVPGNode<T>> VPGDB<?, T, R> getDB()
    {
        return this.<R>getVPG().getDB();
    }
    
    public int getEndOffset()
    {
        return offset + length;
    }

    ///////////////////////////////////////////////////////////////////////////

    @Override public String toString()
    {
        return "(Offset " + offset + ", length " + length + " in " + filename + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }

    @Override public boolean equals(Object other)
    {
        if (other == null || !(other instanceof IVPGNode<?>)) return false;

        IVPGNode<?> o = (IVPGNode<?>)other;
        return filename.equals(o.getFilename())
            && offset == o.getOffset()
            && length == o.getLength();
    }

    @Override public int hashCode()
    {
        return offset + length + (filename == null ? 0 : filename.hashCode());
    }

    public int compareTo(IVPGNode<?> that)
    {
        int result = 0;
        
        if (this.filename != null && that.getFilename() == null)
            result = -1;
        else if (this.filename == null && that.getFilename() != null)
            result = 1;
        else if (this.filename != null && that.getFilename() != null)
            result = this.filename.compareTo(that.getFilename());
        if (result != 0) return result;
        
        result = this.offset - that.getOffset();
        if (result != 0) return result;
        
        result = this.length - that.getLength();
        return result;
    }
}
