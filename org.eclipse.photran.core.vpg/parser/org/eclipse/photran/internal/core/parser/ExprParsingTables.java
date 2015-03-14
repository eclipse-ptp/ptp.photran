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

        protected static final int[] rowmap = { 0, 1, 2, 3, 4, 2, 5, 6, 7, 8, 9, 3, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 1, 37, 38, 4, 39, 40, 41, 42, 43, 44, 0, 6, 0, 45, 46, 5, 7, 47, 48, 49, 50, 11, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 7, 66, 67, 68, 69, 8, 9, 70, 71, 72, 73, 74, 75, 76, 77, 78, 10, 79, 12, 16, 80, 24, 81, 26, 82, 83, 84, 0, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 29, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 13, 109, 30, 31, 110, 36, 111, 112, 113, 114, 115, 116, 117, 0, 14, 118, 119, 120, 121, 15, 122, 37, 123, 124, 125, 126, 17, 127, 128, 129, 130, 131, 41, 132, 133, 18, 134, 42, 135, 0, 1, 136, 2, 137, 43, 138, 44, 139, 19, 140, 45, 3, 141, 48, 142, 143, 144, 145, 146, 147, 148, 49, 149, 150, 20, 151, 152, 153, 154, 2, 21, 155, 156, 157, 158, 159, 160, 161, 162, 22, 163, 164, 165, 166, 167, 50, 168, 169, 170, 51, 171, 172, 52, 173, 53, 174, 175, 176, 177, 178, 54, 55 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 3, 0, 0, 0, 0, 4, 0, 0, 5, 6, 0, 0, 0, 7, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 9, 0, 0, 10, 0, 0, 0, 0, 0, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 13, 0, 0, 0, 14, 0, 0, 0, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 16, 17, 18, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 21, 22, 23, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 24, 0, 25, 0, 0, 26, 27, 0, 0, 0, 0, 0, 0, 0, 0, 28, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 29, 30, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 33, 0, 0, 0, 0, 0, 0, 34, 0, 0, 0, 0, 0, 0, 0, 35, 0, 36, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 37, 0, 38, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 40, 0, 0, 0, 0, 0, 41, 0, 0, 0, 0, 0, 0 };

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
            final int cols = 9;
            final int compressedBytes = 566;
            final int uncompressedBytes = 8317;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWctuwjAQHCMO5uaqPfRI/yRtOXLgk6x+WT+tTggNSfyYtR" +
                "eE1C4qSNXgjNe7s7sGFh0MvMUGgz0Br/3nC1x4H/755o4GOxjr" +
                "jcfuG/iEN9hbt8XJwe7PmMMZ8zViPkYMIhifxgBbBAzgwytQCw" +
                "zCJ2z46xxG6zn3iDXnyYh9Uc/qbYesMfu69qFP+JDZF+NnUGd6" +
                "iJ+p8LxIzOBD2/u5jfN0XvN1Ov04RDE2RP6h+JT9c+PYQIN/EM" +
                "1Tw+VpQy6D9iGR75zWHcv+SXM+4dpbJSPWSZ7XXFtW5ip0bDAz" +
                "vPsZSwi1954YUH4u+ufXLjvfVO794sPcOkvLYTLrUGeqnKdZzJ" +
                "KzPH7+MX8bU85TZW1RyAuGM6E/ZG98J13V6lu0MGRdltRc31S7" +
                "l2qX+qaJYjpRLVhyfh442xlngk9V/xObZaZ9La0rVgPxbPVYmK" +
                "r5q3YWJvpwuq9juiKF3k/mZ9x+9sRBZdbj5gu1fSnPsGjSTFoT" +
                "UIgfAqP1rGGd8LIY72TeK7WO0V5qjhPparpeUDOIZeqg0nwquD" +
                "/MzbBx/dHkvJ4Zs7NVRWzEewmd2NDK07b+8LSac5tjleijJHN3" +
                "fp27aZ20Lnu60UxwXmsdAmcnehYTz/p7l9cCcf3S4UOfl2mvg2" +
                "oxT2DU5gL9O72mGNPTVZ1nNfYAMiHTzAtDzw5o6esUz7T+Xh0P" +
                "V3OdkDNXu7X6cEbrFPRQzc+a9/wKPWTD7zJM3amvg8zvFznMDw" +
                "FQtOg=");
            
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
            final int compressedBytes = 2667;
            final int uncompressedBytes = 15037;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrVWmurXVcVPeRDobdpjDG1NbHG2DY9TSuC9EsL6jpnceMb1C" +
                "aFFq1FUkijSR9R/Lr84Acrokhi3iIiVEQEwbZ5v3PvjWlFFMGS" +
                "5iZp7CP6D/zknHucseZce59z7k7Si+RO5h5jzDnW3vvsdfbjnH" +
                "PTyo78pQnJ30guqdQtaaksV/R2ynKR5M/XLO7kv7SgtzPdJHhH" +
                "Wq66ci1Mt8pyGRzxdo00IXlvvJ3aECwdgmYvda2HWlpljvCGRp" +
                "qQ/KvkkoFeOsDVaVGFh6ErvkDyJknZz1yT/TRHfEQj/DM+Ilut" +
                "kFqj0wFLe6HZQ9JVch1VHaON1SsZLIHUhubQTE83XeApz1GFxR" +
                "xVfFGn9pcWpO/5OUrfrc+R38/08Wp7m+PmwZZrSKbLuDnvp3OZ" +
                "k+vE37FO+gRY7xedoX+oc51NV1yokTbGhcqpDcGoyTV1nVbzjv" +
                "CiRtoIlHmuIZkFK7rOem0Q5zXk/Xle6lVqENMnwajJkVb1XEaN" +
                "mff0/eHzLvnguHmPExpybk4opzYEoyaXc3PCRpc8ToRZDXnts7" +
                "L3VWoYglGTI63qeZiNd2rIft4pW6pSwxCMmhxpVc8l1muk38b1" +
                "Ul1f9dYD08NA1OFDr/TSL2t5aLC2Fudm/3w5R/3zfo60W85R+l" +
                "SK6XNpIn1etrSh2t4GIDUw9dKaNBk3pE9Do5e+kL4otUCX8M+k" +
                "z4YrGjJHV+QoV6lBTF8CoyZHWtXzcCXerCFzdLNspUoNQzBqcq" +
                "RVPYey41n1no3PkpVIpsv05bIGHPTXach+rlNObSijv2KaPaRV" +
                "PY/rwjkNOZ7n5IhUqUFMXwWjJkda1fNwLj6mIVvJSQ2s9iDr0m" +
                "tVzzkqHPAYDlBbx/7CgfRjrdKVXihHiHpXQ5FJrZG+BkZdeq3q" +
                "OdSc59HF2nl00d/jtFu71t2iocikBoJRl16reg5Vu0M9H58nK1" +
                "H2ZF2p66601h3Vgx7DQWrrlF4kemlrOSJvf8jxHFTkeE5ONe4d" +
                "7nhqF8eztu0zHsMZRXIZsavuRcKRdpYj5Cg8oaGITI9Bp8eB8N" +
                "BXeq3qOUcNxn4HSyC1oTmaNV+Nt2oopq8DqYFg1Nn7DeuhJs+0" +
                "2RG7GtLtpieA1EAwanKkVT2XNbW4H6U94+Y97ca8p2/l+9GYdf" +
                "a2D38O6W0vPiNsr68z3qGhyKQGglGXXqtWxzM74loN6eakBoJR" +
                "l16reh7XDntm5TNt1ps7Y/7SpsYZ9GdbmlK0Tn+2HNGfRR8O7f" +
                "oR7T4jpINj5/2F+vUzvG5LU4rWSceK/XydCUd173Aj2u3n5PTY" +
                "69J043mpzTpnxj0na3fY56Ph897bMeKzzI7Rqpq1oxqKTGogGH" +
                "XptarnUHJ093kM+6hRSaeKOdrHhKuaIzci+/Z7DPupUUln614k" +
                "XNU63Yh296N0rt37s7afpz2G04rkMuJi3YuEo9pPNyL7pjyGKc" +
                "Uw1dtjHe/t7UEfPbhQKXxHPYajiuFob5d1vLe3C3304EKl8M14" +
                "DDOKYaa31zre29uLPnpwoVL4TngMJxTDid5u63hvbzf66MGFyl" +
                "zzPvq7i3Leq9qQczMc8xiOKZI39vMY03p+xLzu5ymP4ZQieWM/" +
                "TzGt50fk2mGP4bAieWOdh5nW8yNy7bjHcFyRvLHO40zr+RG5dt" +
                "JjOKlI3ljnSab1/IhcO+QxHFIkb6zzENN6fkSuHfEYjiiSN9Z5" +
                "hGk9PyLXpj2GaUXyxjqnmdbzIwTP2tKUYtlx6zzLtJ4fcW3fK7" +
                "Z7z4+/zrddZ+31vOQxvERtndKL9K6mb/As+m0sgdSG5mjWfDW8" +
                "ZktTimXH7edrTOv5EYKv2tKUYtlx63yVaT0/Yn7mPX5TQxEpn+" +
                "MqLZ/jKoSHvtJrVc85al7eSy97DC9TW6f0Ir1riO8Vj+EVauuU" +
                "XqR3eV/8iIZi+gGQGghGTY60queypjafEX7Y5nja57g2n4/6F8" +
                "ZpqvSjPL7Nd6qXat8FXSq+C7o02M+f5PfnhzQU00+B1EAw6uz9" +
                "mfVQk8+b2dE/pSFbG4Fg1MN64GmVd8x9PNv92fGM92koMqmBYN" +
                "Sl16qex/vCmxryrs1JDQSjLr1W9VziTxpSyUkNBKMuvVb1XOKi" +
                "hlRyUgPBqEuvVT2XuKwhlZzUQDDq0mtVz8Plebkmf1hDkUkNBK" +
                "MuvVb1HCq/rzZhGTd5bWiOZq1erd2Rt8QtZCXm71S3lH5DfKca" +
                "LmkoMqmBYNSl16qeh0vvwXdB2xqVa5v3B8fNe2+bhiKTGghGXX" +
                "qt6nlvW3xSQ47wIOX+Xmm5v1dYHf2qxzCvVT2PT/a2ashWclID" +
                "wahLr1U9722NT2nIVgapS2B6GMgeo+6lPz6VHjJVrUF/j6t+qY" +
                "5PR/4KXkNzNGu+Gv6jocikBoJRl16reg6V9xW/6T8TnxlsuYZk" +
                "uqzXfDU+qqHIpAbCQ116rep5fDRc0JA9z0kNBKMuvVb1XOLfGl" +
                "LJSQ0Eoy69VvUcqnZdei4+R1YimS7db5HONeg/rqHIpAbCQ116" +
                "req5xGINqSyWZ5sKqYFg1Nm7w3qoybNNdkzu0+h0RiEY9bBevT" +
                "+5r/kbCjMOntCVpb3+NxTZq/G/oazWkO5qee0VUgPBqLP3l9ZD" +
                "TdaZHa2ePy/Xnj8vF8+flwfPn7+6mu+o293f7dm7/1+N0QhGPa" +
                "xX76Mz5/P8m+M0Vfr1dd7jxj7bzMvxnNYYjWDUw3r1vsRJDanl" +
                "pAaCUZdeq/p+/2Sr55AXr/LTAo7n797T58+VGopybq70GghGnb" +
                "2/tx5qcm5mR6vPsH+4unmfj/dSXKShmP4IpAaCUWdv13qoyWvP" +
                "jsn9GnJNHYFg1MN69f7k/hvl3Gx1XfrXOE2VXrkxrkvtfovMr+" +
                "7dcbrelcppjdEIRj2sV+/3T4d3NOQJKic1EIy69FrVc4nG/2gy" +
                "w+AKp2zNYv+/mtZtjqn+i7PNNeToVZ7v79dQZFIDwahLr1U9l1" +
                "ihIZUV6TiQGghGnb1d63nfIN6nIZWc1EAw6tJrVc+h5np/Tp4Z" +
                "d/dpdv9v59Hb43S923Kd74zT9a5UZjRGIxj1sF6935+JyzRk1p" +
                "alaSA1EIw6e7vWQ03uR9lxo1w/48c0FJnUQDDq0mtVzyWWaEhl" +
                "iRzPCqmBYNTZ27UeanI8syN+VEO6OamBYNSl16qeSyzVkMpSeQ" +
                "arkBoIRp29f7EearKf2RHv15Du/fLaK6QGglFnb9d6qMk6naPF" +
                "efTWOF3vypaWaygyqYFg1KXXqp7H5df//Xz627VcQ+Zw/73x2u" +
                "/SUJQ5ustrIBh19nath5rMUXbED2hINyc1EIy69FrVc4l7NKRy" +
                "j7w/K6QGglFn7z+sh5rsZ3bMy/P8AxqKTGogGHXptarn8YH+lI" +
                "a8c0cgGPWwXr3fn7phrsl3aygyqYFg1KXXqp7Hu+flO4EzGqMR" +
                "jHpYr97vn2n1+f382PP9jWu5hqQLY92zjWvIbRqKcg25zWsgGH" +
                "X2dq2Hmpyb2XHDvD9XaSjKa1/lNRCMOnu71kMtrTJH/KCGdHNS" +
                "A8GoS69VPYeac97f6lz3X2Odb1/d+HivhiKTGghGXXqt6rmo/w" +
                "GCL+e+");
            
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

        protected static final int[] rowmap = { 0, 0, 0, 0, 0, 1, 2, 0, 0, 0, 0, 0, 0, 3, 1, 0, 0, 0, 4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0, 0, 0, 6, 0, 0, 0, 1, 0, 7, 8, 9, 2, 0, 0, 1, 0, 10, 0, 11, 0, 1, 0, 12, 2, 0, 0, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0, 14, 0, 15, 0, 0, 0, 0, 0, 0, 0, 2, 0, 16, 17, 0, 3, 4, 0, 0, 0, 18, 1, 19, 0, 0, 0, 20, 21, 22, 0, 3, 0, 23, 0, 3, 4, 4, 0, 0, 0, 0, 0, 0, 24, 0, 0, 1, 0, 0, 5, 25, 0, 2, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 26, 5, 0, 0, 0, 0, 6, 7, 27, 28, 0, 6, 29, 0, 30, 31, 0, 0, 7, 0, 32, 0, 8, 33, 34, 9, 35, 0, 36, 37, 8, 38, 0, 39, 9, 40, 0, 0, 0, 0, 0, 0, 0, 41, 0, 0, 0, 2, 42, 0, 0, 0, 10, 0, 43, 11, 12, 0, 10, 44, 45, 0, 0, 11, 46, 0, 12, 13, 13, 0, 47, 0, 14, 15, 14, 0, 15, 48, 0, 49, 0, 50, 51, 0, 52, 0, 0, 16, 17, 0, 16, 53, 0, 54, 0, 17, 0, 18, 19, 0, 0, 0 };
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 5, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4, 0, 7, 0, 0, 9, 0, 0, 0, 0, 0, 10, 0, 0, 0, 11, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 13, 0, 0, 14, 0, 0, 15, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 16, 0, 0, 0, 0, 17, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 18, 0, 0, 0, 0, 0, 0, 0, 0, 19, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 21, 22, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 23, 24, 0, 0, 0, 0, 0, 25, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 26, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 27, 0, 0, 0, 0, 0, 0, 28, 0, 29, 0, 0, 9, 0, 0, 4, 0, 0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 31, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 33, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 34, 35, 0, 0, 0, 13, 0, 0, 0, 0, 0, 0 };

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
            final int compressedBytes = 451;
            final int uncompressedBytes = 14785;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtWVtugzAQHK/4QFU/VlUOkKOgnsxHb0KoBClQv9ZeG88PkY" +
                "DAPjwziwkgANbMhxl3GF5+Po/D68Qnw2C8wf5eBjtiwiG2pwht" +
                "45G/Ndb528TO66ue+aOk+R9kojPoOMBb/ei/+rHlkPVTFyYF+V" +
                "uuOTvJahI2pujDjij+XmVwr/8GvPgX8Om/XMsobfx7+jVLyyZ+" +
                "CMevqaVJ8I//6P+Hs/5/O/HcAe5Hz4f/86XzZ05CkIcR7y0H/m" +
                "ER/vGuf0b+y1F/3/jZBsTf/fMV9asjcf0E+eeEf1HM/3WoQU79" +
                "CdF/R/5DKf6LzV9Z/ydr/tzq7++/fDy39v4R15+I+enLZ36zie" +
                "e32ucfktp/0DX/k9b8d1yif4PQDLsb5/o57D9kqx81UHib98UH" +
                "XBNj6ReQ9i99e0upf4nVv737x/T7D7n0o5T+24jCpth/qlb/T8" +
                "Ch+VvqzzXML7H1912/1rV/Ji/+suILJSP/JV9/ivkry/NN9rbo" +
                "qHh+b07/5GdAaf/f7PeHRvizWPyx/kFCv5v4/ifkvwqtfxKOv3" +
                "T/6/YvJfgjZf0Uz1+190/t/Nu/n1xb/6vwb/L63/uvFH4AWLto" +
                "0w==");
            
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
            final int compressedBytes = 588;
            final int uncompressedBytes = 3961;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrNlM9rE0EUx7/BlthTLEVDIAnBpAepBKHEFuJJPKcHQQUPKt" +
                "YfoOCPQmnV/qCK8SCFUqjHUkRBRFq0FdtDDy0GKy0iiL9u2v4f" +
                "6cvs7O7s7swmmwxNA28zww6f/b7ve28QRgFxZNCHCGLIIok8Op" +
                "DAMbzBBZxDCKfRgkM4gU7cwhFEkUY7juIAWnEQKZxBGw7jdrmM" +
                "AkWGIkKRpchTJNBLT86hVYVzEffK1g9d1opxEKZV3OAgRs8kRU" +
                "XPWoVDEaIw9NzEA5ee40jZHPSwvLpZXidNjvU1mzOJERcnp+Sc" +
                "8uFMYVTN4aetvNjOzTlr6GH7KAXn0IpzrNMOTsVnkWP6bHLoed" +
                "7gmD7Tk/uMSyyvK3ZeuOr2h1bOvC7betDPTl8rV/mZeSnfq+r+" +
                "HjdcehY8eq47/RE5ps/44qlXyfAHd2Q+e/Tc5ZxNGcepR8bBfW" +
                "desj705DVQve745ql7Saj7oLvuSs4PNQdDtfUhhqV96NvPeKio" +
                "1z9lvdJV5+KRkNeOnz/0r5gL3zllethurCY9435zyvcqPRO1zV" +
                "fV+QvjKeVV5PP+hO75x7J556fV8/5Mlx75vLt9lup57jOnQTjT" +
                "Igcz9j0fkJMTOC/2gT+zkvtnzs0JWPdG9Lz0zMUrXf7wfn6tPy" +
                "+8DdY/Dj3vGtRTtOuF+Xo4/P2i4NiHPe/DJaU/H5vSh580cZY9" +
                "Pq9o7ufVuv1Z3wf3z2dNPm9o4nwVOFt6/OHf+a657j/rrTt//6" +
                "updf+tqV5/lPfG36bcG/81cbYFzi4Y7BPA");
            
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
    protected static final int[] columnmap = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

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
            final int cols = 9;
            final int compressedBytes = 31;
            final int uncompressedBytes = 8317;
            
            byte[] decoded = new byte[compressedBytes];
            base64Decode(decoded,
                "eNrtwTEBAAAAwqD1T20IX6AAAAAAAAAAAA4DIH0AAQ==");
            
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
