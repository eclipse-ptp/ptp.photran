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
 * An implementation of {@link ILookaheadLineReader} which reads lines from a String.
 * 
 * @author Jeff Overbey
 */
public final class StringLookaheadLineReader implements ILookaheadLineReader<Error>
{
    private final String string;

    private int start;

    private int index;

    public StringLookaheadLineReader(String string)
    {
        this.string = string;
        this.start = 0;
        this.index = 0;
    }

    public String readNextLine()
    {
        if (index >= string.length())
        {
            return null;
        }
        else
        {
            int nextLF = string.indexOf('\n', index) + 1;
            if (nextLF <= 0) nextLF = string.length();
            String result = string.substring(index, nextLF);
            index = nextLF;
            return result;
        }
    }

    public String advanceAndRestart(int numChars)
    {
        String result = string.substring(start, numChars);
        this.start += numChars;
        this.index = start;
        return result;
    }

    public void close()
    {
    }
}
