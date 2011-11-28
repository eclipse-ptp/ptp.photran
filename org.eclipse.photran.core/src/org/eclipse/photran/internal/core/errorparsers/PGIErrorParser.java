/*******************************************************************************
 * Copyright (c) 2007, 2011 Los Alamos National Laboratory and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Craig Rasmussen (LANL) - Initial API and implementation
 *     Jeff Overbey (Illinois/NCSA) - Adjusted regexes to accept more errors
 *******************************************************************************/
package org.eclipse.photran.internal.core.errorparsers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.core.resources.IFile;

/**
* PGI Fortran Error Parser -- An error parser for the Portland Group compiler
*
* This error parser matches compiler errors such as
* <pre>PGFTN-S-0034-Syntax error at or near :: (life_f.f: 19)</pre>
* <pre>PGF90-S-0034-Syntax error at or near &lt;quoted string&gt; (sample.f90: 2)</pre>
*
* @author Craig Rasmussen
* @author Jeff Overbey
*/
@SuppressWarnings("deprecation")
final public class PGIErrorParser implements IErrorParser
{
    /*
     * Regex notes:
     *     \S matches any non-whitespace character
     *     \d matches [0-9]
     *     \w matches [A-Za-z_0-9]
     *     .  matches any character except (possibly) line terminators
     *     $  matches end-of-input
     *     Parentheses define a capturing group
     */
    private Pattern lineError = Pattern.compile("PGF\\w\\w-S-\\d+-(.+):* [(](\\S+): (\\d+)[)]$"); //$NON-NLS-1$
    private Pattern nolineError = Pattern.compile("PGF\\w\\w-S-\\d+-(.+)[(](\\S+)[)]$"); //$NON-NLS-1$

    public boolean processLine(String thisLine, ErrorParserManager eoParser)
    {
        //String test = "PGFTN-S-0034-Syntax error at or near :: (life_f.f: 19)"
        //String test = "PGF90-S-0038-Symbol, junk, has not been explicitly declared (life_f.f90)";
        //boolean match = Pattern.matches("PGF\\w\\w-S-\\d+-([a-zA-Z, ]+)[(](\\S+)[)]", test);
        
        String errorMessage = null, filename = null;
        boolean errorFound = false;
        int lineNum = 0;
        
        Matcher m = lineError.matcher(thisLine);
        if (m.matches()) {
            errorMessage = m.group(1);
            filename = m.group(2);
            lineNum = Integer.parseInt(m.group(3));
            errorFound = true;
        } else {
            m = nolineError.matcher(thisLine);
            if (m.matches()) {
                errorMessage = m.group(1);
                filename = m.group(2);
                lineNum = 1;            //TODO - should be end of file?
                errorFound = true;
            }
        }
        
        if (errorFound) {
            IFile file = eoParser.findFilePath(filename);
            eoParser.generateMarker(file, lineNum, errorMessage, IMarkerGenerator.SEVERITY_ERROR_RESOURCE, null);
        }

        return errorFound;
    }
}