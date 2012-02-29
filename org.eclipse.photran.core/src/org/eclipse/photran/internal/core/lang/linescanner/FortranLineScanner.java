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

import java.util.regex.Pattern;

/**
 * A {@link FortranLineScanner} reads a line of input and determines its length and
 * {@link FortranLineType} (statement, comment, preprocessor directive, etc.); if it is determined
 * to be a {@link FortranLineType#STATEMENT} which is continued on subsequent lines, the
 * {@link FortranLineScanner} determines the complete statement, including all continuation lines
 * and intervening comment lines.
 * 
 * @author Jeff Overbey
 */
public final class FortranLineScanner
{
    private static final Pattern FIXED_FORM_INCLUDE_LINE = Pattern
        .compile("[ \t]*[Ii][ \t]*[Nn][ \t]*[Cc][ \t]*[Ll][ \t]*[Uu][ \t]*[Dd][ \t]*[Ee][ \t]*[\"']([^\r\n\"]*)[\"'][ \t]*(![^\r\n]*)?[\r\n]*"); //$NON-NLS-1$
    private static final Pattern FREE_FORM_INCLUDE_LINE = Pattern
        .compile("[ \t]*[Ii][Nn][Cc][Ll][Uu][Dd][Ee][ \t]+[\"']([^\r\n\"]*)[\"'][ \t]*(![^\r\n]*)?[\r\n]*"); //$NON-NLS-1$

    @SuppressWarnings("unused")
    private static final int INCLUDE_LINE_CAPTURING_GROUP_OF_FILENAME = 1;

    private static final Pattern COMMENT_DIRECTIVE = Pattern.compile(
        "^[ \\t]*[Cc*!][ \\t]*\\$([Oo][Mm][Pp]|[Aa][Cc][Cc])"); //$NON-NLS-1$

    private final boolean fixedForm;

    private final boolean preprocessed;

    private int lineLength = -1;

    private FortranLineType lineType = null;

    public FortranLineScanner(boolean fixedForm, boolean preprocessed)
    {
        this.fixedForm = fixedForm;
        this.preprocessed = preprocessed;
    }

    /**
     * Note that the {@link FortranLineScanner} may read several lines beyond the end of the
     * statement. This is necessary to detect (the absence of) continuation lines in fixed form
     * source code.
     */
    public final <X extends Throwable> String scan(ILookaheadLineReader<X> reader) throws X
    {
        this.lineLength = -1;
        this.lineType = null;

        String line = reader.readNextLine();
        if (line == null)
        {
            this.lineLength = 0;
            this.lineType = null;
            return null;
        }
        else
        {
            this.lineLength = line.length();
            this.lineType = classify(line);
            this.lineLength += checkForContinuationLines(line, reader);
            return reader.advanceAndRestart(this.lineLength);
        }
    }

    private <X extends Throwable> int checkForContinuationLines(String line, ILookaheadLineReader<X> reader) throws X
    {
        if (this.lineType == FortranLineType.PREPROCESSOR_DIRECTIVE)
            return checkForPreprocessorDirectiveContinuation(line, reader);
        else if (this.lineType == FortranLineType.STATEMENT)
            return fixedForm ? checkForFixedFormContinuation(reader) : checkForFreeFormContinuation(line, reader);
        else
            return 0;
    }

    private <X extends Throwable> int checkForPreprocessorDirectiveContinuation(String line, ILookaheadLineReader<X> reader) throws X
    {
        int numberOfAdditionalCharacters = 0;

        while (findLastNonWhitespaceCharacter(line) == '\\')
        {
            line = reader.readNextLine();
            numberOfAdditionalCharacters += line.length();
        }

        return numberOfAdditionalCharacters;
    }

    private <X extends Throwable> int checkForFreeFormContinuation(String line, ILookaheadLineReader<X> reader) throws X
    {
        int numberOfAdditionalCharacters = 0;

        while (isContinued(line) || findFirstNonWhitespaceCharacter(line) == '!')
        {
            line = reader.readNextLine();
            numberOfAdditionalCharacters += (line == null ? 0 : line.length());
        }

        return numberOfAdditionalCharacters;
    }

    private <X extends Throwable> int checkForFixedFormContinuation(ILookaheadLineReader<X> reader) throws X
    {
        int numberOfAdditionalCharacters = 0;

        boolean moreContinuationLinesPossible = true;

        while (moreContinuationLinesPossible)
        {
            // Skip comment lines
            int lengthOfComments = 0;
            String line = reader.readNextLine();
            while (line != null
                && classify(line) == FortranLineType.COMMENT
                && findFirstNonWhitespaceCharacter(line) != '\0')
            {
                lengthOfComments += line.length();
                line = reader.readNextLine();
            }

            // Then check for continuation line
            if (line != null
                && line.length() >= 7
                && line.startsWith("     ") && line.charAt(5) != ' ' && line.charAt(5) != '0') //$NON-NLS-1$
            {
                numberOfAdditionalCharacters += lengthOfComments + line.length();
                line = reader.readNextLine();
                moreContinuationLinesPossible = true;
            }
            else
            {
                moreContinuationLinesPossible = false;
            }
        }

        return numberOfAdditionalCharacters;
    }

    private FortranLineType classify(String line)
    {
        final char firstChar = line.isEmpty() ? '\0' : line.charAt(0);
        final char firstNonSpaceChar = findFirstNonWhitespaceCharacter(line);

        if (firstNonSpaceChar == '\0') // Line contains only blanks
        {
            return FortranLineType.COMMENT;
        }
        else if (fixedForm && (firstChar == 'C' || firstChar == 'c' || firstChar == '*'))
        {
            if (line.startsWith("      C") || line.startsWith("      c") || line.startsWith("      *")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                return FortranLineType.STATEMENT; // Actually a continuation of a previous line
            else
                return isCommentDirective(line) ? FortranLineType.COMMENT_DIRECTIVE : FortranLineType.COMMENT;
        }
        else if (firstNonSpaceChar == '!')
        {
            if (fixedForm && line.startsWith("      !")) //$NON-NLS-1$
                return FortranLineType.STATEMENT; // Actually a continuation of a previous line
            else
                return isCommentDirective(line) ? FortranLineType.COMMENT_DIRECTIVE : FortranLineType.COMMENT;
        }
        else if (preprocessed
            && (firstNonSpaceChar == '#' || (firstNonSpaceChar == '?' && line.trim().startsWith(
                "??=")))) //$NON-NLS-1$
        {
            return FortranLineType.PREPROCESSOR_DIRECTIVE;
        }
        else
        {
            if ((firstNonSpaceChar == 'I' || firstNonSpaceChar == 'i') && isIncludeLine(line))
                return FortranLineType.INCLUDE_LINE;
            else
                return FortranLineType.STATEMENT;
        }
    }

    private boolean isIncludeLine(String line)
    {
        if (fixedForm)
            return FIXED_FORM_INCLUDE_LINE.matcher(line).matches();
        else
            return FREE_FORM_INCLUDE_LINE.matcher(line).matches();
    }

    private static final char findFirstNonWhitespaceCharacter(String string)
    {
        if (string != null)
        {
            for (int i = 0, len = string.length(); i < len; i++)
            {
                char ch = string.charAt(i);
                if (!Character.isWhitespace(ch)) { return ch; }
            }
        }
        return '\0';
    }

    private boolean isCommentDirective(String line)
    {
        return COMMENT_DIRECTIVE.matcher(line).find();
    }

    private static final char findLastNonWhitespaceCharacter(String string)
    {
        if (string != null)
        {
            for (int i = string.length() - 1; i >= 0; i--)
            {
                char ch = string.charAt(i);
                if (!Character.isWhitespace(ch)) { return ch; }
            }
        }
        return '\0';
    }

    private boolean isContinued(String line)
    {
        if (line == null) return false;

        boolean inString = false;
        char stringStart = '\0';
        char lastNonSpaceChar = '\0';

        for (int i = 0, length = line.length(); i < length; i++)
        {
            char thisChar = line.charAt(i);
            char nextChar = i + 1 < length ? line.charAt(i + 1) : '\0';

            if ((thisChar == '\"' || thisChar == '\'') && !inString)
            {
                stringStart = thisChar;
                inString = true;
            }
            else if (thisChar == stringStart && inString)
            {
                if (nextChar != thisChar)
                {
                    inString = false;
                    stringStart = '\0';
                }
            }
            else if (thisChar == '!' && !inString)
            {
                return lastNonSpaceChar == '&';
            }

            if (!Character.isWhitespace(thisChar)) lastNonSpaceChar = thisChar;
        }

        return lastNonSpaceChar == '&';
    }

    /**
     * @return
     */
    public final int getLineLength()
    {
        if (lineLength < 0)
            throw new IllegalStateException("Must call #readNextLine before calling #getLineLength"); //$NON-NLS-1$
        else
            return lineLength;
    }

    public final FortranLineType getLineType()
    {
        if (lineType == null)
            throw new IllegalStateException("Must call #readNextLine before calling #getLineType"); //$NON-NLS-1$
        else
            return lineType;
    }
}
