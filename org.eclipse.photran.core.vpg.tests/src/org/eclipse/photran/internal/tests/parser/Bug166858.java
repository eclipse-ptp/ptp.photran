/*******************************************************************************
 * Copyright (c) 2008 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.tests.parser;

import java.io.StringReader;

import junit.framework.TestCase;

import org.eclipse.photran.internal.core.SyntaxException;
import org.eclipse.photran.internal.core.lexer.ASTLexerFactory;
import org.eclipse.photran.internal.core.lexer.IAccumulatingLexer;
import org.eclipse.photran.internal.core.parser.Parser;

/**
 * Bug 166858:
 * <p>
 * The rules for part_ref and procedure_designator are ambiguous and allow invalid code to be
 * accepted for a call statement. For example, look at the following code:
 * 
 * <pre>
 * call hello(i)(j,k,z)
 * end
 * </pre>
 * 
 * This is currently accepted by the parser even though it is invalid. The problem is that in
 * matching a data_ref for procedure_designator, the (..) is matched as part of the data_ref
 * (actually, it's matched in part_ref, which is called by data_ref, to be more accurate). Then,
 * upon returning to procedure_designator, the parser is still looking for an optional (..). This
 * appears to be an ambiguity in the grammar that could be fixed by only allowing data_ref to match
 * the (..) since procedure_designator only has one alternative, which is a data_ref.
 * 
 * @author Jeff Overbey
 */
public class Bug166858 extends TestCase
{
    private String program = "call hello(i)(j,k,z); end";

    public void testBug166858() throws Exception
    {
        IAccumulatingLexer lexer = new ASTLexerFactory().createLexer(new StringReader(program),
            null, "<stdin>");
        try {
            new Parser().parse(lexer);
            fail("Parser should fail");
        } catch (SyntaxException e) {
            // OK
        }
    }
}
