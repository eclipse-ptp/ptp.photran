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

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * 
 * @author Mariano Méndez
 */

public class FortranIncludeFixedFormTestSuite extends TestSuite
{
    public static Test suite() throws Exception
    {
       TestSuite suite = new TestSuite();
        suite.addTest(getSuiteFor("no-include.f77"));
        suite.addTest(getSuiteFor("basic-include-main.f77"));
        suite.addTest(getSuiteFor("test1-two-includes-main.f77"));
        suite.addTest(getSuiteFor("test2-lot-includes-main-file.f77"));        
   
        
        return suite;
    }
    
    private static TestSuite getSuiteFor(String baseFilename)
    {
        TestSuite subSuite = new TestSuite(baseFilename);
        subSuite.addTest(new FortranIncludeFixedFormTestCase(baseFilename));
        return subSuite;
    }
}