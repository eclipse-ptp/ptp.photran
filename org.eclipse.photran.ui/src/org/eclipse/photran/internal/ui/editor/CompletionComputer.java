/*******************************************************************************
 * Copyright (c) 2011 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Base class for a content assist completion computer.
 * <p>
 * Determines the prefix and suffix at the current cursor location.
 * 
 * @author Jeff Overbey
 */
public abstract class CompletionComputer
{
    protected final IDocument document;
    protected final int prefixIndex;
    protected final String prefix;
    protected final int suffixIndex;
    protected final String suffix;
    protected final int replOffset;
    protected final int replLen;

    protected CompletionComputer(IDocument document, int offset) throws BadLocationException
    {
        this.document = document;
        
        this.prefixIndex = CompletionUtil.findPrefix(document, offset);
        this.prefix = document.get(prefixIndex, offset-prefixIndex).toLowerCase();
        
        this.suffixIndex = CompletionUtil.findSuffix(document, offset);
        this.suffix = document.get(offset, suffixIndex-offset).toLowerCase();
        
        this.replOffset = prefixIndex;
        this.replLen = suffixIndex - prefixIndex;
    }
}
