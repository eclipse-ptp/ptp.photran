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
 * An object which reads lines from a file (or other source of input) so that they can be processed by a {@link FortranLineScanner}.
 * 
 * @author Jeff Overbey
 * 
 * @see LookaheadLineReader
 * @see StringLookaheadLineReader
 * @see FortranLineScanner#scan(ILookaheadLineReader)
 */
public interface ILookaheadLineReader<X extends Throwable>
{
    /**
     * 
     * @return or <code>null</code>
     * @throws X
     */
    String readNextLine() throws X;

    String advanceAndRestart(int numChars);

    void close() throws X;
}
