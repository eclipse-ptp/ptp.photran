/*******************************************************************************
 * Copyright (c) 2011 University of Illinois and others.  All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 *     Jeff Overbey (Illinois/NCSA) - Design and implementation
 ******************************************************************************/
package org.eclipse.photran.internal.cdtinterface.errorparsers;

import static org.eclipse.cdt.core.IMarkerGenerator.SEVERITY_ERROR_RESOURCE;
import static org.eclipse.cdt.core.IMarkerGenerator.SEVERITY_INFO;
import static org.eclipse.cdt.core.IMarkerGenerator.SEVERITY_WARNING;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.core.resources.IFile;

/**
 * Error parser for Cray Fortran (tested with Cray Fortran version 7.3.4).
 * <p>
 * This error parser recognizes error messages such as the following from crayftn:
 * <pre>
 *   undefined_variable = 6
 *   ^                      
 * ftn-113 crayftn: ERROR MAIN, File = errors.f90, Line = 11, Column = 3 
 *   IMPLICIT NONE is specified in the local scope, therefore an explicit type must be specified for data object "UNDEFINED_VARIABLE".
 * 
 * ftn-967 crayftn: LIMIT $MAIN, File = file_does_not_exist.f90, Line = 1 
 *   The compiler is unable to open file "file_does_not_exist.f90".
 * </pre>
 * <p>
 * It can also recognize informational messages from loopmark listings, such as the following:
 * <pre>
 * ftn-6005 ftn: SCALAR File = test1.f90, Line = 7 
 *   A loop starting at line 7 was unrolled 4 times.
 * 
 * ftn-6204 ftn: VECTOR File = test1.f90, Line = 7 
 *   A loop starting at line 7 was vectorized.
 * </pre>
 * 
 * @author Jeff Overbey
 */
public class CrayErrorParser implements IErrorParser {

	private static final Pattern F_ERROR_WARNING_LINE = Pattern.compile( //
			// Group ----1------2------3-------4--------------------------5------------6-------7-----------8
			"^ftn-[0-9]+ (cray)?(ftn): ([A-Z]+)( ?[A-Za-z0-9$_]*,? File = (.*), Line = ([0-9]+)(, Column = ([0-9]+))?|.*)?[ \t]*$"); //$NON-NLS-1$

	// Capture groups in the above regexes
	private static final int SEVERITY_GROUP = 3;
	private static final int FILENAME_GROUP = 5;
	private static final int LINE_NUMBER_GROUP = 6;

	private String previousLine = ""; //$NON-NLS-1$

	@Override
	public boolean processLine(String currentLine, ErrorParserManager eoParser) {
		final Matcher matcher = matchErrorWarningLine(previousLine); // eoParser.getPreviousLine() is broken?
		previousLine = currentLine;
		if (matcher != null) {
			final int severity = determineSeverity(matcher.group(SEVERITY_GROUP));
			final String filename = matcher.group(FILENAME_GROUP);
			final IFile file = filename == null ? null : eoParser.findFileName(filename);
			final int lineNumber = atoi(matcher.group(LINE_NUMBER_GROUP));
			final String description = currentLine.trim();

			eoParser.generateMarker(file, lineNumber, description, severity, null);

			return true;
		} else {
			return false;
		}
	}

    private Matcher matchErrorWarningLine(String previousLine) {
		if (previousLine != null) {
	        Matcher m = F_ERROR_WARNING_LINE.matcher(previousLine);
	        if (m.matches()) {
	            return m;
	        }
		}

		return null;
	}

    private int determineSeverity(String text) {
        if (text.equals("WARNING")) { //$NON-NLS-1$
            return SEVERITY_WARNING;
        } else if (text.equals("SCALAR") || text.equals("VECTOR") || text.equals("IPA") || text.equals("THREAD") || text.equals("ACCEL")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
            return SEVERITY_INFO;
        } else {
            return SEVERITY_ERROR_RESOURCE;
        }
    }

	private int atoi(String string) {
		if (string == null) {
			return 0;
		} else {
			try {
				return Integer.parseInt(string);
			} catch (final NumberFormatException e) {
				return 0;
			}
		}
	}
}
