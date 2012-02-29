/*******************************************************************************
 * Copyright (c) 2011 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Illinois) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.tests.lang;

import static org.eclipse.photran.internal.core.lang.linescanner.FortranLineType.*;
import junit.framework.TestCase;

import org.eclipse.photran.internal.core.lang.linescanner.FortranLineScanner;
import org.eclipse.photran.internal.core.lang.linescanner.FortranLineType;
import org.eclipse.photran.internal.core.lang.linescanner.StringLookaheadLineReader;

/**
 * Unit tests for {@link FortranLineScanner}.
 * 
 * @author Jeff Overbey
 */
public class FortranLineScannerTests extends TestCase
{
    private boolean fixedForm;

    private boolean preprocessed;

    private void freeFormTests()
    {
        //check(COMMENT, "");
        check(COMMENT, " ");
        check(COMMENT, "\t");
        check(COMMENT, "  \t  \r");

        check(COMMENT, "! Comment");
        check(COMMENT, "! Comment\n");
        check(COMMENT, "!");
        check(COMMENT, "  ! Comment");
        check(COMMENT, "  ! Comment\n");
        check(COMMENT, "  !", "  !");
        check(COMMENT, "! This is a comment\n! This is too", "! This is a comment\n");

        check(STATEMENT, "c Comment\n");
        check(STATEMENT, "C Comment\n");
        check(STATEMENT, "* Comment\n");

        check(INCLUDE_LINE, "include 'file.h'");
        check(INCLUDE_LINE, "InClUdE \"file.h\"");
        check(INCLUDE_LINE, "  include 'file.h'");
        check(INCLUDE_LINE, "  InClUdE   \"C:\\WINDOWS\\windows.h\"");
        check(INCLUDE_LINE, "include 'file.h'  ! Comment");
        check(INCLUDE_LINE, "InClUdE \"file.h\"  ! Comment");
        check(INCLUDE_LINE, "  include 'file.h'  ! Comment");
        check(INCLUDE_LINE, "  InClUdE   \"C:\\WINDOWS\\windows.h\"  ! Comment");

        check(STATEMENT, "print *, \"Hello\"");
    }

    public void testFreeFormPreprocessed()
    {
        fixedForm = false;
        preprocessed = true;

        freeFormTests();

        check(PREPROCESSOR_DIRECTIVE, "#error");
        check(PREPROCESSOR_DIRECTIVE, " #  include  \"mpif.h\"");
        check(PREPROCESSOR_DIRECTIVE, "  ??= include  \"mpif.h\"");
    }

    public void testFreeFormUnpreprocessed()
    {
        fixedForm = false;
        preprocessed = false;

        freeFormTests();

        check(STATEMENT, "#error");
        check(STATEMENT, " #  include  \"mpif.h\"");
        check(STATEMENT, "  ??= include  \"mpif.h\"");
    }

    private void fixedFormTests()
    {
        //check(COMMENT, "");
        check(COMMENT, " ");
        check(COMMENT, "\t");
        check(COMMENT, "  \t  \r");

        check(COMMENT, "! Comment");
        check(COMMENT, "! Comment\n");
        check(COMMENT, "!");
        check(COMMENT, "  ! Comment");
        check(COMMENT, "  ! Comment\n");
        check(COMMENT, "  !", "  !");
        check(COMMENT, "! This is a comment\n! This is too", "! This is a comment\n");

        check(COMMENT, "c Comment");
        check(COMMENT, "c Comment\n");
        check(COMMENT, "c");
        check(STATEMENT, "  c Comment");
        check(STATEMENT, "  c Comment\n");
        check(STATEMENT, "  c");
        check(COMMENT, "c This is a comment\nc This is too", "c This is a comment\n");

        check(COMMENT, "C Comment");
        check(COMMENT, "C Comment\n");
        check(COMMENT, "C");
        check(STATEMENT, "  C Comment");
        check(STATEMENT, "  C Comment\n");
        check(STATEMENT, "  C");
        check(COMMENT, "C This is a comment\nC This is too", "C This is a comment\n");

        check(COMMENT, "* Comment");
        check(COMMENT, "* Comment\n");
        check(COMMENT, "*");
        check(STATEMENT, "  * Comment");
        check(STATEMENT, "  * Comment\n");
        check(STATEMENT, "  *");
        check(COMMENT, "* This is a comment\n* This is too", "* This is a comment\n");

        check(INCLUDE_LINE, "include 'file.h'");
        check(INCLUDE_LINE, "InClUdE \"file.h\"");
        check(INCLUDE_LINE, "  include 'file.h'");
        check(INCLUDE_LINE, "  InClUdE   \"C:\\WINDOWS\\windows.h\"");
        check(INCLUDE_LINE, "include 'file.h'  ! Comment");
        check(INCLUDE_LINE, "InClUdE \"file.h\"  ! Comment");
        check(INCLUDE_LINE, "  include 'file.h'  ! Comment");
        check(INCLUDE_LINE, "  InClUdE   \"C:\\WINDOWS\\windows.h\"  ! Comment");
        check(INCLUDE_LINE, "in  clude 'file.h'");
        check(INCLUDE_LINE, "I nCl UdE \"file.h\"");
        check(INCLUDE_LINE, "  i ncl ude 'file.h'");
        check(INCLUDE_LINE, "  InC lU dE   \"C:\\WINDOWS\\windows.h\"");
        check(INCLUDE_LINE, "i n c l u d e 'file.h'  ! Comment");
        check(INCLUDE_LINE, " I  n C l U d \tE \"file.h\"  ! Comment");
        check(INCLUDE_LINE, "\ti\tn\tc\tl\tu\td\te 'file.h'  ! Comment");
        check(INCLUDE_LINE, "InCl\tUdE\"C:\\WINDOWS\\windows.h\"  ! Comment");

        check(STATEMENT, "print *, \"Hello\"");
    }

    public void testFixedFormPreprocessed()
    {
        fixedForm = true;
        preprocessed = true;

        fixedFormTests();

        check(PREPROCESSOR_DIRECTIVE, "#error");
        check(PREPROCESSOR_DIRECTIVE, " #  include  \"mpif.h\"");
        check(PREPROCESSOR_DIRECTIVE, "  ??= include  \"mpif.h\"");
    }

    public void testFixedFormUnpreprocessed()
    {
        fixedForm = true;
        preprocessed = false;

        fixedFormTests();

        check(STATEMENT, "#error");
        check(STATEMENT, " #  include  \"mpif.h\"");
        check(STATEMENT, "  ??= include  \"mpif.h\"");
    }

    public void testPreprocessorContinuations()
    {
        fixedForm = false;
        preprocessed = true;

        check(PREPROCESSOR_DIRECTIVE,
            "#define MACRO \\\na < b \\\n  ? a \\ \n  : b  \n! That was a directive\n",
            "#define MACRO \\\na < b \\\n  ? a \\ \n  : b  \n");
        check(STATEMENT,
            "print \"Hel\\\nprint *, \"That was just an incomplete statement\"\n",
            "print \"Hel\\\n");
    }

    public void testFreeFormContinuations()
    {
        fixedForm = false;
        preprocessed = false;

        check(STATEMENT,
            "if (a < b) &\n  then\n  a = b\nend if\n",
            "if (a < b) &\n  then\n");
        check(STATEMENT,
            "if (a < b) &\n  & then\n  a = b\nend if\n",
            "if (a < b) &\n  & then\n");
        check(STATEMENT,
            "if (a < b) & !!!\n  then\n  a = b\nend if\n",
            "if (a < b) & !!!\n  then\n");
        check(STATEMENT,
            "if (a < b) & !!!\n  & then\n  a = b\nend if\n",
            "if (a < b) & !!!\n  & then\n");
        check(STATEMENT,
            "print *, & \n ! Comment \n \"Hello\"\n stop",
            "print *, & \n ! Comment \n \"Hello\"\n");
        check(STATEMENT,
            "print *, & \n ! Comment \n ! Comment \n ! Comment \n \"Hello\"\n stop",
            "print *, & \n ! Comment \n ! Comment \n ! Comment \n \"Hello\"\n");

        // Continuation of string literals
        check(STATEMENT,
            "if (t .ne. \"This&\n  &thng\") then\n  stop\nendif\n",
            "if (t .ne. \"This&\n  &thng\") then\n");
        check(STATEMENT,
            "if (t .ne. \"This&\n  ! Wow\n  &thng\") then\n  stop\nendif\n",
            "if (t .ne. \"This&\n  ! Wow\n  &thng\") then\n");

        check(STATEMENT, "print *, & \n"); // No continuation line available
    }

    public void testFixedFormContinuations()
    {
        fixedForm = true;
        preprocessed = false;

        check(STATEMENT,
            "      if (a < b)\n     * then\n        a = b\n      end if\n",
            "      if (a < b)\n     * then\n");
        check(STATEMENT,
            "      if (a < b)\n     + then\n        a = b\n      end if\n",
            "      if (a < b)\n     + then\n");
        check(STATEMENT,
            "      if (a < b)\n     0 then\n        a = b\n      end if\n",
            "      if (a < b)\n");
        check(STATEMENT,
            "      if (a < b)\nc Comment\nc Comment\n     * then\n        a = b\n      end if\n",
            "      if (a < b)\nc Comment\nc Comment\n     * then\n");
        check(STATEMENT,
            "      if (a < b)\nc Comment\n",
            "      if (a < b)\n"); // No continuation after comment
    }

    private void check(FortranLineType expectedType, String string)
    {
        check(expectedType, string, string);
    }

    private void check(FortranLineType expectedType, String string, String expectedStmt)
    {
        FortranLineScanner scanner = createScanner();
        String actualStmt = scanner.scan(new StringLookaheadLineReader(string));
        assertEquals(expectedType, scanner.getLineType());
        assertEquals(expectedStmt, actualStmt);
    }

    private FortranLineScanner createScanner()
    {
        return new FortranLineScanner(fixedForm, preprocessed);
    }
}
