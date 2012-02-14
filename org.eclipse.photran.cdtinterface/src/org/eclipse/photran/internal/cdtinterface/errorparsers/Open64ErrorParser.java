/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 *     Jeff Overbey (Illinois/NCSA) - Design and implementation
 ******************************************************************************/
package org.eclipse.photran.internal.cdtinterface.errorparsers;

import static org.eclipse.cdt.core.IMarkerGenerator.SEVERITY_ERROR_RESOURCE;
import static org.eclipse.cdt.core.IMarkerGenerator.SEVERITY_WARNING;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.core.resources.IFile;

/**
 * Error parser for Open64 Fortran (tested with Open64 Fortran version 4.5.1).
 * <p>
 * This error parser recognizes error messages such as the following from openf90:
 * <pre>
 * 
 *   undefined_variable = 6
 *   ^                      
 * openf95-113 openf90: ERROR ERROR, File = error.f90, Line = 5, Column = 3 
 *   IMPLICIT NONE is specified in the local scope, therefore an explicit type must be specified for data object "UNDEFINED_VARIABLE".
 * 
 *   n = one_arg(1, 2, 3)
 *       ^                
 * openf95-287 openf90: WARNING ONE_ARG, File = error.f90, Line = 6, Column = 7 
 *   The result of function name "ONE_ARG" in the function subprogram is not defined.
 * 
 * openf90 ERROR: file does not exist:  dne.f90
 * </pre>
 * 
 * @author Jeff Overbey
 */
public class Open64ErrorParser implements IErrorParser {

    private static final Pattern F_ERROR_WARNING_LINE = Pattern.compile( //
        // Capture Group ----1------2-------3------------------------4------------5-------6-----------7
        "^openf..-[0-9]+ open(f..): ([A-Z]+)( [A-Za-z0-9$_]+, File = (.*), Line = ([0-9]+)(, Column = ([0-9]+))?|.*)?[ \t]*$"); //$NON-NLS-1$
    // Capture groups in the above regexes
    private static final int SEVERITY_GROUP = 2;
    private static final int FILENAME_GROUP = 4;
    private static final int LINE_NUMBER_GROUP = 5;

    private static final Pattern F_ALTERNATE_ERROR_LINE = Pattern.compile( //
        "openf90 ERROR: (.*)[ \t]*$"); //$NON-NLS-1$
    // Capture groups in the above regexes
    private static final int ALT_DESCRIPTION_GROUP = 1;

    @Override
    public boolean processLine(String currentLine, ErrorParserManager eoParser)
    {
        Matcher matcher = matchErrorWarningLine(eoParser.getPreviousLine());
        if (matcher != null)
        {
            final int severity = matcher.group(SEVERITY_GROUP).equals("WARNING") ? SEVERITY_WARNING : SEVERITY_ERROR_RESOURCE; //$NON-NLS-1$
            final String filename = matcher.group(FILENAME_GROUP);
            final IFile file = filename == null ? null : eoParser.findFileName(filename);
            final int lineNumber = atoi(matcher.group(LINE_NUMBER_GROUP));
            final String description = currentLine.trim();

            eoParser.generateMarker(file, lineNumber, description, severity, null);

            return true;
        }
        else
        {
            matcher = matchAlternateErrorLine(currentLine);
            if (matcher != null)
            {
                eoParser.generateMarker(null, 0, matcher.group(ALT_DESCRIPTION_GROUP),
                    SEVERITY_ERROR_RESOURCE, null);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    private Matcher matchErrorWarningLine(String previousLine)
    {
        if (previousLine != null)
        {
            Matcher m = F_ERROR_WARNING_LINE.matcher(previousLine);
            if (m.matches()) { return m; }
        }

        return null;
    }

    private Matcher matchAlternateErrorLine(String currentLine)
    {
        if (currentLine != null)
        {
            Matcher m = F_ALTERNATE_ERROR_LINE.matcher(currentLine);
            if (m.matches()) { return m; }
        }

        return null;
    }

    private int atoi(String string)
    {
        if (string == null)
        {
            return 0;
        }
        else
        {
            try
            {
                return Integer.parseInt(string);
            }
            catch (final NumberFormatException e)
            {
                return 0;
            }
        }
    }
}
