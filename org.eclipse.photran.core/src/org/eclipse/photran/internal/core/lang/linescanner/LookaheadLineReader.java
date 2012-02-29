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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

/**
 * An implementation of {@link ILookaheadLineReader} which reads lines from a {@link Reader}.
 * 
 * @author Jeff Overbey
 */
public final class LookaheadLineReader implements ILookaheadLineReader<IOException>
{
    private final BufferedReader input;

    private final LinkedList<String> lineBuffer;

    private int currentLine;

    public LookaheadLineReader(Reader input)
    {
        if (input instanceof BufferedReader)
            this.input = (BufferedReader)input;
        else
            this.input = new BufferedReader(input);

        this.lineBuffer = new LinkedList<String>();

        this.currentLine = 0;
    }

    public String readNextLine() throws IOException
    {
        if (currentLine == lineBuffer.size()) lineBuffer.add(input.readLine());

        String result = lineBuffer.get(currentLine);
        if (result == null)
        {
            return null;
        }
        else
        {
            currentLine++;
            return result;
        }
    }

    public String advanceAndRestart(int numChars)
    {
        this.currentLine = 0;

        StringBuilder result = new StringBuilder(numChars);

        String line = lineBuffer.isEmpty() ? null : lineBuffer.get(0);
        while (line != null && numChars >= line.length())
        {
            result.append(lineBuffer.remove(0));
            numChars -= line.length();
            line = lineBuffer.isEmpty() ? null : lineBuffer.get(0);
        }

        if (line != null && numChars > 0 && numChars < line.length())
        {
            result.append(line.substring(0, numChars));
            lineBuffer.set(0, line.substring(numChars));
        }

        return result.toString();
    }

    public void close() throws IOException
    {
        input.close();
    }
}
