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
package org.eclipse.photran.internal.tests.lexer.preprocessor.fortran_include;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.TreeMap;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.refactoring.infrastructure.SourcePrinter;
import org.eclipse.photran.internal.core.reindenter.Reindenter;
import org.eclipse.photran.internal.core.reindenter.Reindenter.Strategy;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.photran.internal.tests.Activator;
import org.eclipse.photran.internal.tests.PhotranWorkspaceTestCase;

/**
 * 
 * @author Mariano Méndez
 */
public class FortranIncludeFixedFormTestCase extends PhotranWorkspaceTestCase
{
    private static final String DIR = "fixed-form-test-code/fixed-form-lexer/fortran-include-stmt";

    
    protected String  filename = null;
    private int fromLine;
    private int thruLine;

    public FortranIncludeFixedFormTestCase() {;}  // when JUnit invokes a subclass outside a test suite

    public FortranIncludeFixedFormTestCase(String filename)
    {
        this.filename =filename;
        this.setName("test");
    }

    public void test() throws Exception
    {
        if (filename == null) return; // when JUnit invokes this outside a test suite

        final IFile thisFile = importFile(Activator.getDefault(), DIR, filename);

        IFortranAST ast = PhotranVPG.getInstance().acquireTransientAST(thisFile);
        assertNotNull(ast);

        thisFile.setContents(
            new ByteArrayInputStream(SourcePrinter.getSourceCodeFromAST(ast).getBytes()),
            true,
            false,
            new NullProgressMonitor());

        project.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());

       assertEquals(
            readTestFile(filename + ".result"), // expected result
            readWorkspaceFile(filename));      // actual refactored file
    }

    
    protected String readTestFile(String filename) throws IOException, URISyntaxException
    {
        return super.readTestFile(Activator.getDefault(), DIR, filename);
    }
}
