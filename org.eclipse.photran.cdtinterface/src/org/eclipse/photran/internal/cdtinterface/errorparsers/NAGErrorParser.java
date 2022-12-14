/*******************************************************************************
 * Copyright (c) 2007 Walt Brainerd and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     WB - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.cdtinterface.errorparsers;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IErrorParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.core.resources.IFile;
 
/**
 * NAG Error Parser -- An error parser for NAG Fortran (and F)
 *
 * This error parser matches compiler errors of the following form:
 * <pre>Error: f.f95, line 44: Oops, you blew it!</pre>
 *
 * @author Walt Brainerd
 */
@SuppressWarnings("deprecation")
public class NAGErrorParser implements IErrorParser
{
     private String fileNameString;
     private int lineNumber;

     private boolean processLineLocal(String line, ErrorParserManager epm)
     {
          if (line.startsWith("Info:") ||  //$NON-NLS-1$
              line.startsWith("Warning:") || //$NON-NLS-1$
              line.startsWith("Extension:") || //$NON-NLS-1$
              line.startsWith("Obsolescent:") || //$NON-NLS-1$
              line.startsWith("Option error:") || //$NON-NLS-1$
              line.startsWith("Option warning:") || //$NON-NLS-1$
              line.startsWith("Questionable:") || //$NON-NLS-1$
              line.startsWith("Fatal:") || //$NON-NLS-1$
              line.startsWith("Fatal Error:") || //$NON-NLS-1$
              line.startsWith("Sequence Error:") || //$NON-NLS-1$
              line.startsWith("Deleted feature used:") || //$NON-NLS-1$
              line.startsWith("Error:") || //$NON-NLS-1$
              line.startsWith("Panic:")) // shouldn't happen, but ... //$NON-NLS-1$
          {
              String[] tokens = line.split(" "); //$NON-NLS-1$
    		  fileNameString = tokens[1].substring(0,tokens[1].length()-1);
    		  lineNumber = Integer.parseInt(
    				  tokens[3].substring(0,tokens[3].length()-1));
/*
 * In case the text of the diagnostic message is ever needed:
    		  message = line.substring(line.indexOf(":")+1,
    								   line.length());
    		  message = message.substring(message.indexOf(":")+1,
    				 				      message.length());
 */

               IFile file = epm.findFilePath(fileNameString);
               int severity = (line.startsWith("Error:") ||  //$NON-NLS-1$
                               line.startsWith("Fatal:") || //$NON-NLS-1$
                               line.startsWith("Fatal Error:") || //$NON-NLS-1$
                               line.startsWith("Sequence Error:") || //$NON-NLS-1$
                               line.startsWith("Panic:")) //$NON-NLS-1$
                    ? IMarkerGenerator.SEVERITY_ERROR_RESOURCE
                    : IMarkerGenerator.SEVERITY_WARNING;
               //Generate and plant a marker for the message...
               epm.generateMarker(file, lineNumber, line, severity, null);

               //Shows up in the console of the debugging session...
               //System.out.println("Hello? " + line);
          }
          return false;
     }
 
     public boolean processLine(String line, ErrorParserManager epm)
     {
          //All we do is pass the buck to the local version
          //of process line. It's handy to put a catch-all here.
          try
          {
               return processLineLocal(line, epm);
          }
          catch (Throwable e)
          {
               //Eat whatever is thrown at us...
          }
          return false;
     }
}
