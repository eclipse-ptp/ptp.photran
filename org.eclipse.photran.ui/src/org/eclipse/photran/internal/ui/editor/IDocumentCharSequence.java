/*******************************************************************************
 * Copyright (c) Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Auburn University) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * A {@link CharSequence} that provides an interface to an {@link IDocument}.
 * 
 * @author Jeff Overbey
 */
public final class IDocumentCharSequence implements CharSequence
{
    private final IDocument document;

    private final int start;

    private final int end;

    public IDocumentCharSequence(IDocument document)
    {
        this(document, 0, document.getLength());
    }

    public IDocumentCharSequence(IDocument document, int start, int end)
    {
        this.document = document;
        this.start = start;
        this.end = end;
    }

    public char charAt(int i)
    {
        try
        {
            return document.getChar(start + i);
        }
        catch (BadLocationException e)
        {
            throw new Error(e);
        }
    }

    public int length()
    {
        return end - start;
    }

    public CharSequence subSequence(int start, int end)
    {
        return new IDocumentCharSequence(document, start, end);
    }
}