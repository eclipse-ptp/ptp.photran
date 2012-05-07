/*******************************************************************************
 * Copyright (c) 2011 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.lexer.sourceform;
import java.io.IOException;
import java.io.Reader;

import org.eclipse.core.resources.IFile;
import org.eclipse.photran.internal.core.lexer.FixedFormLexerPhase2;
import org.eclipse.photran.internal.core.lexer.ILexer;
import org.eclipse.photran.internal.core.lexer.preprocessor.fortran_include.IncludeLoaderCallback;
import org.eclipse.photran.internal.core.lexer.preprocessor.fortran_include.PreprocessingFixedFormLexerPhase1;
import org.eclipse.photran.internal.core.sourceform.ISourceForm;


/**
 *  * An {@link ISourceForm} for fixed form Fortran source code that may
 * contain INCLUDE lines (but not any other kind of preprocessor directives).
 * 
 * @author Mariano Méndez
 */

public class FixedSourceForm implements ISourceForm
{
    private IncludeLoaderCallback callback = null;
    
    @SuppressWarnings("unchecked")
    public ILexer createLexer(
        Reader in, IFile file, String filename,
        boolean accumulateWhitetext) throws IOException
    {
        return new FixedFormLexerPhase2(
            new PreprocessingFixedFormLexerPhase1(
                in,
                file,
                filename,
                callback,
                accumulateWhitetext));
    }
    
  

    public ISourceForm configuredWith(Object data)
    {
        if (data instanceof IncludeLoaderCallback)
            this.callback = (IncludeLoaderCallback)data;

        return this;
    }

    public boolean isFixedForm()
    {
        return true;
    }

    public boolean isCPreprocessed()
    {
        return false;
    }
}