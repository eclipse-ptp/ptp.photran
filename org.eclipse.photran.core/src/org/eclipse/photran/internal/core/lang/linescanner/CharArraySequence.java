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
package org.eclipse.photran.internal.core.lang.linescanner;

/**
 * A {@link CharSequence} that wraps a char[] array.
 * 
 * @author Jeff Overbey
 */
public final class CharArraySequence implements CharSequence
{
    private final char[] chars;

    private final int start;

    private final int end;

    public CharArraySequence(char[] chars)
    {
        this(chars, 0, chars.length);
    }

    public CharArraySequence(char[] chars, int start, int end)
    {
        this.chars = chars;
        this.start = start;
        this.end = end;
    }

    public char charAt(int i)
    {
        return chars[start + i];
    }

    public int length()
    {
        return end - start;
    }

    public CharSequence subSequence(int start, int end)
    {
        return new CharArraySequence(chars, start, end);
    }
}