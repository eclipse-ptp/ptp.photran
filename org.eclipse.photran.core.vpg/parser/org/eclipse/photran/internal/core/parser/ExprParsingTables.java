/*******************************************************************************
 * Copyright (c) 2007 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.parser;

import org.eclipse.photran.internal.core.lexer.*;                   import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;                   import org.eclipse.photran.internal.core.SyntaxException;                   import java.io.IOException;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


import java.util.zip.Inflater;

import org.eclipse.photran.internal.core.parser.Parser.Nonterminal;
import org.eclipse.photran.internal.core.parser.Parser.Production;


@SuppressWarnings("all")
final class ExprParsingTables extends ParsingTables
{
    private static ExprParsingTables instance = null;

    public static ExprParsingTables getInstance()
    {
        if (instance == null)
            instance = new ExprParsingTables();
        return instance;
    }

    @Override
    public int getActionCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
    {
        return ActionTable.getActionCode(state, lookahead);
    }

    @Override
    public int getActionCode(int state, int lookaheadTokenIndex)
    {
        return ActionTable.get(state, lookaheadTokenIndex);
    }

    @Override
    public int getGoTo(int state, Nonterminal nonterminal)
    {
        return GoToTable.getGoTo(state, nonterminal);
    }

    @Override
    public int getRecoveryCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
    {
        return RecoveryTable.getRecoveryCode(state, lookahead);
    }

    /**
     * The ACTION table.
     * <p>
     * The ACTION table maps a state and an input symbol to one of four
     * actions: shift, reduce, accept, or error.
     */
    protected static final class ActionTable
    {
        /**
         * Returns the action the parser should take if it is in the given state
         * and has the given symbol as its lookahead.
         * <p>
         * The result value should be interpreted as follows:
         * <ul>
         *   <li> If <code>result & ACTION_MASK == SHIFT_ACTION</code>,
         *        shift the terminal and go to state number
         *        <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == REDUCE_ACTION</code>,
         *        reduce by production number <code>result & VALUE_MASK</code>.
         *   <li> If <code>result & ACTION_MASK == ACCEPT_ACTION</code>,
         *        parsing has completed successfully.
         *   <li> Otherwise, a syntax error has been found.
         * </ul>
         *
         * @return a code for the action to take (see above)
         */
        protected static int getActionCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert lookahead != null;

            Integer index = Parser.terminalIndices.get(lookahead.getTerminal());
            if (index == null)
                return 0;
            else
                return get(state, index);
        }

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 2, 5, 6, 7, 8, 9, 3, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 1, 37, 38, 4, 39, 40, 41, 42, 43, 44, 0, 6, 45, 0, 46, 5, 7, 47, 48, 49, 50, 7, 11, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 8, 9, 70, 71, 72, 73, 74, 75, 76, 77, 78, 10, 79, 12, 17, 80, 24, 81, 26, 82, 83, 84, 0, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 28, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 13, 109, 30, 31, 110, 36, 111, 112, 113, 114, 115, 116, 117, 0, 14, 118, 119, 120, 121, 15, 122, 37, 123, 124, 125, 126, 16, 127, 128, 129, 130, 131, 41, 132, 133, 18, 134, 42, 135, 0, 1, 136, 2, 137, 43, 138, 44, 139, 19, 140, 45, 3, 141, 48, 142, 143, 144, 145, 146, 147, 148, 49, 149, 150, 20, 151, 152, 153, 154, 2, 21, 155, 156, 157, 158, 159, 160, 161, 162, 22, 163, 164, 165, 166, 167, 50, 168, 169, 170, 51, 171, 172, 52, 173, 53, 174, 175, 176, 177, 178, 54, 55 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 3, 0, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 0, 7, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 13, 0, 0, 0, 14, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 16, 17, 18, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 21, 22, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 25, 0, 0, 26, 27, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 30, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 0, 34, 0, 0, 0, 0, 0, 0, 0, 35, 0, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 38, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 231;
            final int cols = 8;
            final int compressedBytes = 549;
            final int uncompressedBytes = 7393;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWEFuwyAQHFNaoZ4cv8C95Rk++JhH+SF9TJ5W46SNawyzsE" +
                "aRqlIpkToBhl12dhc4DDANWlj4YQe8+e93uPnT4KO9NOhgTngB" +
                "PoH22mNw1hmcHVw/4+MNt3d8uuHY4tjH4fftgGb+WyhM9/+hdw" +
                "shz89jAb/7IPzp+n50iA7Gf2WfZs8+jD+zH6h/GL9x139S/8z4" +
                "Yp+TP0Nq/XL+D//8mt8L7w+S/pXbR3e+Ov7l/Nn+oX0QxpcpjS" +
                "9Q+7H44/pyEZ0/5He+xb9DepD5Mfuv4ns7bI5+LMPL1KIRO0sw" +
                "fauNg9oveX7p+j+nn8Il/M+S+HpE8Nh8dXzp8bT9tvyz7sc//r" +
                "dxeXxXu5/0/grqu4r6RfNT5fgW5C9hfnoty29bBYnoa4j3Mn3d" +
                "8rt6fvbBj+yfnf+39TViaapPKqi8vn8urq3/pf1PrD4U1S9MAh" +
                "T1zWH1e7l/kv0PMBL7WmF/Uoe/vD9qivRJFJ9I+J/g2vWX+Qam" +
                "/e7Ph0x9YfpG+wepfkX0l9bPLrc+z+x/hO8/sf5oN/6P4CfrH7" +
                "L9G+ZXnX+18VNc/6zyr+p+kfpBMt8k51fVl4z81cgKqB1+gb5g" +
                "5udk67P7d9j58vRVrv+6/UX2N+X5Q30/Ca6uXw97nym7H3r90q" +
                "1fnh+FAnLE/TWS+hZF9csB/il6v8TT85OT8uP5TVs/Mn1R6I/a" +
                "fux9tHJ9VPZ+zfQ7P3+w998o/gVH6h8Z");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 179;
            final int cols = 21;
            final int compressedBytes = 2683;
            final int uncompressedBytes = 15037;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVWkuMHFcV7TiJE8Z2LCUrQEJiE0ZIKBJRyMcWvK6nscGOwX" +
                "biBQhYsMMSrEAxzu+xYEEQCkIggU0QIIJ/CAmS+G/PjMdjx7aE" +
                "EAgisPNbJCFKCOD8CATMvXX6vHtfVXdP22aE3Fe3zjn3nve6pq" +
                "q6Pt2T3tuRVxqT/KnkdbValK6V5Xu698hyqeS3J8528ist6N6T" +
                "Fgq+M71bde1anJbI8l1wVG9ppDHJ91VvURuCpYPQ7KVx66GWrj" +
                "dH+JNGGpP8jeR1PX1tD9+fltZ4CLrmCyQXSsp65pqspzniTRrh" +
                "D/GmTgdIrdHpgKWHoNlD0lVyHVVvo426jLdjCaQ2NIdm+nzbBZ" +
                "7yPqqx2Ec1X9ppvNKC9GW/j9KXmvvIr2f6QP1+a+Pa3js3kEyX" +
                "cW1eT+cyJ+fEa6qTbgDrfqXT94U652y7qlc00sbqFeXUhmDU5J" +
                "o6p9W8IzyskTYCZT83kMyCFZ2zWevFaQ05Pk9LvU4NYvogGDU5" +
                "0qqey6gh+z1t6r/fJW8ctt/DGQ1ZzzPKqQ3BqMmRVvU8nKle1p" +
                "DP+8uylevUMASjJpfPe+55Xy/Oacic56Rap4YhGDU50qqeV+fi" +
                "Co20Pa6QI3ZFfeyuAKbbgKjDh17ppV9muRUqvKAh2/MF2SJ1ah" +
                "DTcjBqcqRVPZdRI3zeq8lyv1eTfr9rt9zv6SMppo+lsbRK1n5V" +
                "/TesAlIDU5VWphVxVfowNHppdbpdal26hIf00eolDdlHL8k71q" +
                "lhCEZNjrSq51D2t9fvtz6uJyuRTJdpTVkD9vo3a6SxeLNyakMZ" +
                "vdY0e0irei6xTEPmXCbVOjUMa3fW7CGt6nlcFv6oIUdCTmqNtA" +
                "6MuvRa1XMo0Xs9hr3U1rFX2Ju+oVW60gPlCFHPaygyqTXSejDq" +
                "0mtVz6HmPOaPNI75I/4ap93GfchfNBSZ1EAw6tJrVc+hZL33eQ" +
                "z7qK3jttY+JnrpO+WIfq94Z7yTrEQZv6HUTVdynX7bs1eR7Tmx" +
                "vXXtcNtTu9iejb/nuMdwXJFcRmxpepFwpO+XI2SNg4YiMn0SOn" +
                "0KCA99pdeqnnNUb+zHsQRSG5qjXfPV6q8aiunTQGogGHX2fsZ6" +
                "qMk9bXbEMQ15h7H0WSA1EIyaHGlVz2WmEa4d6QfD9nvaiv2ePp" +
                "evHUPm7G7ufx/S3Vw8I2xuzln9U0ORSQ0Eoy69Vq23Z3bED2nI" +
                "1shJDQSjLr1W9Ryq9dfd0NBfHPZZTl9ofYIet6UpRetUU+WIag" +
                "p9OLTrR4z2jJAODN3vD7TuFZ+wpSlF66RyPZ9gwlFfO9yI0dZz" +
                "YsfQ89KO1r3NKHPuHHafrN1+z0f993v37gHPMncPVvVe+7GGIp" +
                "MaCEZdeq3qOZRs3d0ew25qVNLRYh/tZsJV7yM3Ivv2eAx7qFFJ" +
                "p5peJFz1nG7EaNejdHq047Oxnkc9hqOK5DLimaYXCUe9nm5E9s" +
                "14DDOKYaZ7v3W8t3s/+ujBhUrhO+QxHFIMh7r3Wsd7u/eijx5c" +
                "qBS+Yx7DMcVwrJus473dhD56cKFS+KY8hinFMNW9zzre270Pff" +
                "TgQqXwHfYYDiuSt+Y8zLSeHzHsWBr8fUh5LNW1ft+HzMOc4YjH" +
                "cESRvPW3H2Faz4/ItQMewwFF8tacB5jW8yNybdJjmFQkb805yb" +
                "SeH5Fr0x7DtCJ5a85ppvX8iFzb7zHsVyRvzbmfaT0/ItcOegwH" +
                "Fclbcx5kWs+PyLVZj2FWkbw15yzTen6E4AlbmlIsO27OE0zr+R" +
                "EX9r3ihX+O7Dw/6pyNv+cRj+ERauuUXqR3tX29u/w1WMY1Xhua" +
                "o13z1XDKlqYUy45bz1NM6/kRgidtaUqx7Lg5TzKt50fMz36PXQ" +
                "1FpDzH1Vqe42qEh77Sa1XPOWpejqVHPYZHqa1TepHe1cf3mMfw" +
                "GLV1Si/Su7wv1qGYvhoLDewUmrzjerHB42jPCF8bZXvac9woz0" +
                "fV9DBNlb6ex4/y/edM47ugmeK7oJneen4zP2/+S0MxPQikBoJR" +
                "Z++3rIeaPG9mR/UzDekOQDDqfj1wmdM55t6eo71se8ZFGopMai" +
                "AYdem1qudxUXhGQ47anNRAMOrSa1XPJX6lIZWc1EAw6tJrVc8l" +
                "ntKQSk5qIBh16bWq5xLPakglJzUQjLr0WtXz8Ox8nJOr/2goMq" +
                "mBYNSl16qeQ+Xj6hNYAqkNzdGuNauNK/KGuIGsxPz954bSb4jv" +
                "P8PTGopMaiAYdem1qudQF/ld0HdblQvb7zcO2+/dTRqKTGogGH" +
                "Xptarn3U2x0pAt3Eu5vtdaru811lu/7jHMa1XPYxVe1JAtnJMa" +
                "CEZdeq3qeXixe5eGrHlOaiAYdem1qufdu+JKDVnzXuoSmG4Dss" +
                "doeumPK9OtpuoZ9Ne01XVvNZDa0BztWrOa58Vv+uviup6rgWS6" +
                "bNZ8Nd6iocikBsJDXXqt6nm8JTypIXstJzUQjLr0WtVziT9rSC" +
                "UnNRCMuvRa1XOoxnnpjngHWYlkunS/RTpXr79cQ5FJDYSHuvRa" +
                "1fO4vDqrIWfps3JvUyM1EIw6e79nPdTkPiQ7JrZodDqDEIy6X6" +
                "/Zn9jS/g2FiV9JwNJD/jcUWavhv6Es1pCtsTg9CKQGglFn7w+t" +
                "h1q63hwj3X/ONu4/Z4v7z9ne/eePzuc76tGu7+43lDMagxGMul" +
                "+v2Udnzvv5o8M0VfrJRV7jht7bzMv23KExGMGo+/WafYmHNaSW" +
                "kxoIRl16rer76Mx5H7LtPJ8WsD13/k+/E7hcQ1E+m5d7DQSjzt" +
                "6fWw81+Wxmx0jPsL84v/0+L8fS3zQU0y+B1EAw6uwdtx5qcq7L" +
                "jomtGnJOHYBg1P16zf7E1kvlsznSeenYME2V9lwa56XRfovMf9" +
                "3JYbpqfUtYbdMYjGDU/XrNfrUtPKchd1A5qYFg1KXXqp5LtP5H" +
                "kxl650FlE2f9/2patz2m/i/OUc4hk+f5eX9VQ5FJDQSjLr1W9b" +
                "x6NV6mIWfCy9I0kBoIRp2949bzPkT1dw15l5zUQDDq0mtVz6Hm" +
                "Oj4ndg27+rS7/7fP0ePDdLM74pwnhumq9ctNtVNjMIJR9+s1+x" +
                "Jva0jt7XQcSA0Eo87eceuhJtej7LhUzp/xCg1FJjUQjLr0WtXz" +
                "eEX1moZsmddke9ZIDQSjzt5x66Em2zM74gINeZec1EAw6tJrVc" +
                "/jguoNDXnHN+TZsEZqIBh19v7aeqjJemZHXKIh77IkHQdSA8Go" +
                "s3fceqjJfZ1zjPA5Oj5MN7tS+beGIpMaCEZdeq3qOdTFfT+ffn" +
                "sh55A53L9rfVNxpYai7KMrvQaCUWfvuPVQk32UHdXrGrI1clID" +
                "wahLr1U9r16PV2nIO14lzwg1UgPBqLP399ZDTdYzO+bj/jNeo6" +
                "HIpAaCUZdeq3oer6m2a8jWGIBg1P16zX61/ZI5Jy/UUGRSA8Go" +
                "S69VPY8L5+U5bpfGYASj7tdr9qtdIz2/Pzn0837mQs4h6emh7q" +
                "da5883NRTlGvem10Aw6uwdtx5qcu3Ijkvm+LxaQ1HOn1d7DQSj" +
                "zt5x66Em56XsqP6hIVsmJzUQjLr0WtVzqDn3+3Odi3615nz+/M" +
                "bHd2goMqmBYNSl16qei/ovhcx2uA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        valueInit();
    }
    }

    /**
     * The GOTO table.
     * <p>
     * The GOTO table maps a state and a nonterminal to a new state.
     * It is used when the parser reduces.  Suppose, for example, the parser
     * is reducing by the production <code>A ::= B C D</code>.  Then it
     * will pop three symbols from the <code>stateStack</code> and three symbols
     * from the <code>valueStack</code>.  It will look at the value now on top
     * of the state stack (call it <i>n</i>), and look up the entry for
     * <i>n</i> and <code>A</code> in the GOTO table to determine what state
     * it should transition to.
     */
    protected static final class GoToTable
    {
        /**
         * Returns the state the parser should transition to if the given
         * state is on top of the <code>stateStack</code> after popping
         * symbols corresponding to the right-hand side of the given production.
         *
         * @return the state to transition to (0 <= result < Parser.NUM_STATES)
         */
        protected static int getGoTo(int state, Nonterminal nonterminal)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert nonterminal != null;

            return get(state, nonterminal.getIndex());
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 3, 0, 1, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 6, 0, 0, 0, 1, 0, 7, 8, 9, 2, 0, 0, 1, 0, 10, 0, 11, 0, 1, 12, 0, 2, 0, 0, 0, 0, 0, 13, 2, 0, 0, 0, 0, 0, 0, 0, 14, 15, 0, 0, 0, 0, 0, 0, 0, 0, 16, 17, 0, 3, 4, 0, 0, 0, 18, 1, 19, 0, 0, 0, 20, 21, 22, 0, 3, 0, 23, 0, 3, 4, 4, 0, 0, 0, 0, 0, 0, 24, 0, 0, 1, 0, 0, 5, 25, 0, 2, 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 26, 5, 0, 0, 0, 0, 6, 7, 27, 28, 0, 6, 29, 0, 30, 31, 0, 0, 7, 0, 32, 0, 8, 33, 34, 9, 35, 0, 36, 37, 8, 38, 0, 39, 9, 40, 0, 0, 0, 0, 0, 0, 0, 41, 0, 0, 0, 2, 42, 0, 0, 0, 10, 0, 43, 11, 12, 0, 10, 44, 45, 0, 0, 11, 46, 0, 12, 13, 13, 0, 47, 0, 14, 15, 14, 0, 15, 48, 0, 49, 0, 50, 51, 0, 52, 0, 0, 16, 17, 0, 16, 53, 0, 54, 0, 17, 0, 18, 19, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 7, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 14, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 28, 0, 29, 0, 0, 9, 0, 0, 4, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 34, 35, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return -1;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 231;
            final int cols = 16;
            final int compressedBytes = 453;
            final int uncompressedBytes = 14785;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtW0uOwyAMNRELNKss5gD0Jj5a1BPMkUd0fkSTtPxsA/FbRI" +
                "0qGvDnPRtSC7ACWHDg4Qselu+PJlzwcWPuAJt9A4QfoP0dcQCz" +
                "v0WYG8F+EWL7xVZy+zH+cW1o/4VmdR4U556P/be88J9Dl5Y/tT" +
                "CSNtlmsb8r/jIT9fy4aipW8vdfNBzET4jpwL8APPnLTAH79R/p" +
                "V8jp3fqBev2uI1siYeT90//3VP2/xb9SSBg19ceNzX5elPOI83" +
                "xN4Z87Ef9k+l+I/879nzd/hxLz1/qZV7/b65cs/yi6iJ8n/PeE" +
                "v5PGK3T/oEb/0/gPWOv3lvw5L/8m+r+g/nJZs+g7fhi8UN4/fa" +
                "T3b9i8fxu9frU05w+d9f/Yrf0VV4jfEphpfOST/ff6/IDPf25Y" +
                "e29SJljgokDh51PXLwiKPuuXWv07GL+2P3/g0g8p/bc1IxucP4" +
                "2r/7U5c+5/HKJ/qfV/Xv5iavxkvj9mmXiQhf+a51/P/MXxfE/I" +
                "n4rp+vfp9I+++e97/3LM+mMk/pRbf239QKDfM+z/EdVfUvmPxO" +
                "uXjv++6xcB/ujs/yd0/dfo8TM6/+r+ybX1f4T6jV7/Nf7E8Ali" +
                "nnPW");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 55;
            final int cols = 18;
            final int compressedBytes = 589;
            final int uncompressedBytes = 3961;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNlM9rE0EUx7/BlthLsFBNDgkJJj1IJXhobCF402t6EFTwoG" +
                "L9AQr+KLRU7Q+qGA9SKIV6LEUURKRFW7E99NBisNIigvjrpu3/" +
                "kb7Mzu7O7s5sssnQNPA2M+zw2e/7vvcGYRQQRwZ9iCCGLJLIow" +
                "MJHMMbXMA5hHAaLTiEE+jELRxBFGm04ygOoBUHkcIZtOEwbpfL" +
                "KFBkKCIUWYo8RQK99OQcWlU4F3GvbP3QZa0YB2FaxQ0OYvRMUl" +
                "T0rFU4FCEKQ89NDLn0HEfK5qCH5dXN8jppcqyv2ZxJjLg4OSXn" +
                "lA9nCqNqDj9t5cV2bs5ZQw/bRyk4h1acY512cCo+ixzTZ5NDz/" +
                "MGx/SZntxnXGJ5XbHzwlW3P7Ry5nXZ1oN+dvpaucrPzEv5XlX3" +
                "97jh0rPg0XPd6Y/IMX3GF0+9SoY/uCPz2aPnLudsyjhOPTIO7j" +
                "vzkvWhJ6+B6nXHN0/dS0LdB911V3J+qDkYrq0P8UDah779jIeK" +
                "ev1T1itddS4eCXnt+PlD/4q58J1TpoftxmrSM+43p3yv0jNR23" +
                "xVnb8wnlJeRT7vT+iefyybd35aPe/PdOmRz7vbZ6me5z5zGoQz" +
                "LXIwY9/zATk5gfNiH/gzK7l/5tycgHVvRM9Lz1y80uUP7+fX+v" +
                "PC22D949DzrkE9RbtemK+Hw98vCo592PM+XFL687EpffhJE2fZ" +
                "4/OK5n5erduf9X1w/3zW5POGJs5XgbOlxx/+ne+a6/6z3rrz97" +
                "+aWvffmur1R3lv/G3KvfFfE2db4OwCwEsTuA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        valueInit();
    }
    }

    /**
     * The error recovery table.
     * <p>
     * See {@link #attemptToRecoverFromSyntaxError()} for a description of the
     * error recovery algorithm.
     * <p>
     * This table takes the state on top of the stack and the current lookahead
     * symbol and returns what action should be taken.  The result value should
     * be interpreted as follows:
     * <ul>
     *   <li> If <code>result & ACTION_MASK == DISCARD_STATE_ACTION</code>,
     *        pop a symbol from the parser stacks; a &quot;known&quot; sequence
     *        of symbols has not been found.
     *   <li> If <code>result & ACTION_MASK == DISCARD_TERMINAL_ACTION</code>,
     *        a &quot;known&quot; sequence of symbols has been found, and we
     *        are looking for the error lookahead symbol.  Shift the terminal.
     *   <li> If <code>result & ACTION_MASK == RECOVER_ACTION</code>, we have
     *        matched the error recovery production
     *        <code>Production.values[result & VALUE_MASK]</code>, so reduce
     *        by that production (including the lookahead symbol), and then
     *        continue with normal parsing.
     * </ul>
     * If it is not possible to recover from a syntax error, either the state
     * stack will be emptied or the end of input will be reached before a
     * RECOVER_ACTION is found.
     *
     * @return a code for the action to take (see above)
     */
    protected static final class RecoveryTable
    {
        protected static int getRecoveryCode(int state, org.eclipse.photran.internal.core.lexer.Token lookahead)
        {
            assert 0 <= state && state < Parser.NUM_STATES;
            assert lookahead != null;

            Integer index = Parser.terminalIndices.get(lookahead.getTerminal());
            if (index == null)
                return 0;
            else
                return get(state, index);
        }

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    public static int get(int row, int col)
    {
        if (isErrorEntry(row, col))
            return 0;
        else if (columnmap[col] % 2 == 0)
            return lookupValue(rowmap[row], columnmap[col]/2) >>> 16;
        else
            return lookupValue(rowmap[row], columnmap[col]/2) & 0xFFFF;
    }

    protected static boolean isErrorEntry(int row, int col)
    {
        final int INT_BITS = 32;
        int sigmapRow = row;

        int sigmapCol = col / INT_BITS;
        int bitNumberFromLeft = col % INT_BITS;
        int sigmapMask = 0x1 << (INT_BITS - bitNumberFromLeft - 1);

        return (lookupSigmap(sigmapRow, sigmapCol) & sigmapMask) == 0;
    }

    protected static int[][] sigmap = null;

    protected static void sigmapInit()
    {
        try
        {
            final int rows = 231;
            final int cols = 8;
            final int compressedBytes = 30;
            final int uncompressedBytes = 7393;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtwQENAAAAwqD3T20ON6AAAAAAAAAATgwc4QAB");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            sigmap = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                sigmap[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupSigmap(int row, int col)
    {
        return sigmap[row][col];
    }

    protected static int[][] value = null;

    protected static void valueInit()
    {
        try
        {
            final int rows = 1;
            final int cols = 1;
            final int compressedBytes = 7;
            final int uncompressedBytes = 5;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNpjYAACAA==");
            
            byte[] buffer = new byte[uncompressedBytes];
            Inflater inflater = new Inflater();
            inflater.setInput(decoded, 0, compressedBytes);
            inflater.inflate(buffer);
            inflater.end();
            
            value = new int[rows][cols];
            for (int index = 0; index < uncompressedBytes-1; index += 4)
            {
                int byte1 = 0x000000FF & (int)buffer[index + 0];
                int byte2 = 0x000000FF & (int)buffer[index + 1];
                int byte3 = 0x000000FF & (int)buffer[index + 2];
                int byte4 = 0x000000FF & (int)buffer[index + 3];
                
                int element = index / 4;
                int row = element / cols;
                int col = element % cols;
                value[row][col] = byte1 << 24 | byte2 << 16 | byte3 << 8 | byte4;
            }
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    protected static int lookupValue(int row, int col)
    {
        return value[row][col];
    }

    static
    {
        sigmapInit();
        valueInit();
    }
    }

}
