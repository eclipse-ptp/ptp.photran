/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (UIUC) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Utility methods for finding prefixes and suffixes around the cursor when computing content assist proposals.
 * 
 * @author Jeff Overbey
 */
final class CompletionUtil
{
    public static int findPrefix(IDocument document, int offset) throws BadLocationException
    {
        if (offset-1 >= 0 && document.getChar(offset-1) == '!') // Immediately after !: allow !$omp or !$acc
            return offset - 1;

        for (offset--; offset >= 0; offset--)
        {
            char c = document.getChar(offset);
            if (c == '$' && isCommentStart(document, offset-1)) // Directive: !$omp or !$acc
                return offset - 1;
            else if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_') // Identifier or keyword
                return offset + 1;
        }
        return 0;
    }

    private static boolean isCommentStart(IDocument document, int offset) throws BadLocationException
    {
        if (offset < 0 || offset >= document.getLength())
            return false;

        char ch = document.getChar(offset);
        if (ch == '!')
            return true;
        else if (ch == 'C' || ch == 'c' || ch == '*')
            return isInColumn1(document, offset);
        else
            return false;
    }

    private static boolean isInColumn1(IDocument document, int offset) throws BadLocationException
    {
        return document.getLineInformationOfOffset(offset).getOffset() == offset;
    }

    public static int findSuffix(IDocument s, int offset) throws BadLocationException
    {
        int length = s.getLength();
        for (; offset < length; offset++)
        {
            char c = s.getChar(offset);
            if (!Character.isLetter(c) && !Character.isDigit(c) && c != '_')
                return offset;
        }
        return length;
    }

    private CompletionUtil() {;}
}
