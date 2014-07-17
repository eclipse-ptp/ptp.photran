/*******************************************************************************
 * Copyright (c) 2014 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Auburn University) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.tests.lang;

import java.io.IOException;
import java.util.Random;

import junit.framework.TestCase;

import org.eclipse.photran.internal.core.lang.linescanner.CharArraySequence;
import org.eclipse.photran.internal.core.lang.linescanner.CharSeqLookaheadLineReader;
import org.eclipse.photran.internal.core.lang.linescanner.FortranLineScanner;

/**
 * Benchmark for the {@link FortranLineScanner}.
 * 
 * @author Jeff Overbey
 */
public class FortranLineScannerBenchmark extends TestCase
{
    private static final int N = 20;
    private static final int LINES = 500000;

    private static final char[][] lines = new char[][] {
        "! This is an example of a line that is a comment\n".toCharArray(),
        "                      ! This is also a comment  \n".toCharArray(),
        "        357  print *, 3  ! This is a statement  \n".toCharArray(),
        "    include \"windows.h\"                         \n".toCharArray()
    };

    public void testBenchmark() throws IOException
    {
        int maxLineLen = maxLen(lines);
        Random rand = new Random();
        char[] string = new char[LINES*maxLineLen];
        int idx = 0;
        for (int i = 0; i < LINES; i++) {
            char[] line = lines[rand.nextInt(lines.length)];
            System.arraycopy(line, 0, string, idx, line.length);
            idx += line.length;
        }

        long start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            CharSeqLookaheadLineReader reader = new CharSeqLookaheadLineReader(string);
            FortranLineScanner scanner = new FortranLineScanner(false, true);
            int len = 0;
            for (int j = 0; j < LINES; j++) {
                len += scanner.scan(reader).length();
            }
            assertEquals(string.length, len);
        }
        long end = System.currentTimeMillis();
        long elapsedTimeMS = (end-start);
        System.out.printf("%s: %d ms/%d KLOC\n", getClass().getSimpleName(), elapsedTimeMS, N*LINES);

        start = System.currentTimeMillis();
        CharSequence charSeq = new CharArraySequence(string);
        for (int i = 0; i < N; i++) {
            idx = 0;
            for (int j = 0; j < LINES; j++) {
                //while (charSeq.charAt(idx++) != '\n') {
                //}
                while (charSeq.charAt(idx) != '\n') {
                    switch (charSeq.charAt(idx)) {
                        case '!':
                            idx++;
                        case '#':
                            idx++;
                        default:
                            idx++;
                    }
                }
                idx++;
            }
            assertEquals(string.length, idx);
        }
        end = System.currentTimeMillis();
        elapsedTimeMS = (end-start);
        System.out.printf("    Line reading via CharSequence: %d ms/%d KLOC\n", elapsedTimeMS, N*LINES);

        start = System.currentTimeMillis();
        for (int i = 0; i < N; i++) {
            idx = 0;
            for (int j = 0; j < LINES; j++) {
//                while (chars[idx++] != '\n') {
//                }
                while (string[idx] != '\n') {
                    switch (string[idx]) {
                        case '!':
                            idx++;
                        case '#':
                            idx++;
                        default:
                            idx++;
                    }
                }
                idx++;
            }
            assertEquals(string.length, idx);
        }
        end = System.currentTimeMillis();
        elapsedTimeMS = (end-start);
        System.out.printf("    Line reading via char[]: %d ms/%d KLOC\n", elapsedTimeMS, N*LINES);
    }

    private int maxLen(char[][] lines)
    {
        int max = 0;
        for (char[] line : lines) {
            max = Math.max(max, line.length);
        }
        return max;
    }
}
