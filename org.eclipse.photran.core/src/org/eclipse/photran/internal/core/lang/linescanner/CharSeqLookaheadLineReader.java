/*******************************************************************************
 * Copyright (c) 2011-2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Illinois) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.lang.linescanner;

/**
 * An implementation of {@link ILookaheadLineReader} that reads lines from a String, char[] or other CharSequence.
 * 
 * @author Jeff Overbey
 */
public final class CharSeqLookaheadLineReader implements ILookaheadLineReader<Error>
{
    private final CharSequence string;

    private int start;

    private int index;

    public CharSeqLookaheadLineReader(CharSequence string)
    {
        this.string = string;
        this.start = 0;
        this.index = 0;
    }

    public CharSeqLookaheadLineReader(char[] chars)
    {
        this.string = new CharArraySequence(chars);
        this.start = 0;
        this.index = 0;
    }

    public CharSequence readNextLine()
    {
        if (index >= string.length())
        {
            return null;
        }
        else
        {
            int nextLF = indexOf('\n', string, index) + 1;
            if (nextLF <= 0) nextLF = string.length();
            CharSequence result = string.subSequence(index, nextLF);
            index = nextLF;
            return result;
        }
    }

    private int indexOf(char c, CharSequence seq, int start)
    {
        for (int i = start, len = seq.length(); i < len; i++)
            if (seq.charAt(i) == c)
                return i;
        return -1;
    }

    public CharSequence advanceAndRestart(int numChars)
    {
        CharSequence result = string.subSequence(start, start + numChars);
        this.start += numChars;
        this.index = start;
        return result;
    }

    public void close()
    {
    }
}
