/*******************************************************************************
 * Copyright (c) 2009 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.tests.parser;

import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

import org.eclipse.photran.internal.core.analysis.loops.LoopReplacer;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTErrorConstructNode;
import org.eclipse.photran.internal.core.parser.ASTErrorProgramUnitNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.parser.ASTFormatStmtNode;
import org.eclipse.photran.internal.core.parser.ASTNodeWithErrorRecoverySymbols;
import org.eclipse.photran.internal.core.parser.GenericASTVisitor;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.refactoring.infrastructure.SourcePrinter;
import org.eclipse.photran.internal.tests.PhotranASTTestCase;
import org.eclipse.photran.internal.tests.PhotranTestSuiteFromFiles;
import org.junit.Assert;

import junit.framework.TestSuite;

/**
 * An aggregate test suite that tests the parser, source reproduction, and loop replacement.
 * 
 * @author Jeff Overbey
 */
public abstract class MultiTestSuite extends TestSuite
{
    public MultiTestSuite(String directorySuffix, boolean isFixedForm, boolean mustExist) throws FileNotFoundException, IOException
    {
        setName("Running multiple tests on " + directorySuffix);
        
        if (isFixedForm)
            addTest(new ParserTestSuite(directorySuffix, isFixedForm, mustExist) {});
        else
            //addTest(new SourceReproductionTestSuite(directorySuffix, isFixedForm, mustExist) {});
            addTest(new LoopReplacerSRTestSuite(directorySuffix, isFixedForm, mustExist) {});
    }
    
    public MultiTestSuite() {;} // to keep JUnit quiet
    public void test() {} // to keep JUnit quiet

    private static void checkCorrectParenting(final ASTExecutableProgramNode ast)
    {
        ast.accept(new GenericASTVisitor()
        {
            private IASTNode expectedParent = null;
            
            @Override public void visitASTNode(IASTNode node)
            {
                if (node != ast && node.getParent() == null)
                    System.err.println("!"); // Set breakpoint here
                
                if (node == ast)
                    Assert.assertNull(node.getParent());
                else
                    Assert.assertNotNull(node.getParent());
                
                Assert.assertEquals(expectedParent, node.getParent());
                IASTNode myParent = expectedParent;
                
                expectedParent = node;
                traverseChildren(node);
                expectedParent = myParent;
            }
            
            @Override public void visitToken(Token token)
            {
                if (token.getParent() == null)
                    System.err.println("!"); // Set breakpoint here

                Assert.assertEquals(expectedParent, token.getParent());
            }
        });
    }

    private static void checkNoErrorNodes(final ASTExecutableProgramNode ast)
    {
        ast.accept(new GenericASTVisitor()
        {
            @Override
            public void visitASTNode(IASTNode node)
            {
                if (node instanceof ASTNodeWithErrorRecoverySymbols
                    && !(node instanceof ASTFormatStmtNode)) // Lots of problems with FORMAT; ignore for now
                {
                    ASTNodeWithErrorRecoverySymbols errorNode = (ASTNodeWithErrorRecoverySymbols)node;
                    if (errorNode.getErrorToken() != null)
                        fail(node.getClass().getSimpleName() + ": Error on " + errorNode.getErrorToken() + "; discarded " + errorNode.getSymbolsDiscardedDuringErrorRecovery());
                }
                super.visitASTNode(node);
            }

            @Override
            public void visitASTErrorConstructNode(ASTErrorConstructNode node)
            {
                fail("ErrorConstructNode: " + node);
            }

            @Override
            public void visitASTErrorProgramUnitNode(ASTErrorProgramUnitNode node)
            {
                fail("ErrorProgramUnitNode: " + node);
            }
        });
    }

    public static abstract class ParserTestSuite extends PhotranTestSuiteFromFiles
    {
        public ParserTestSuite(String directorySuffix, boolean isFixedForm, boolean mustExist) throws FileNotFoundException, IOException
        {
            super("Parse", directorySuffix, isFixedForm, mustExist);
        }

        @Override
        protected PhotranASTTestCase createTestFor(File file, boolean isFixedForm, String fileDescription)
        {
            return new ParserTestCase(file, isFixedForm, fileDescription);
        }
    }
    
    /**
     * This is a special instance of {@link ParserTestSuite} which attempts to parse only files listed
     * in a PHOTRAN-PARSER-ERRORS.txt file in the project's directory.
     * 
     * @author joverbey
     */
    public static abstract class FailingParserTestSuite extends ParserTestSuite
    {
        public FailingParserTestSuite(String directorySuffix, boolean isFixedForm, boolean mustExist) throws FileNotFoundException, IOException
        {
            super(directorySuffix, isFixedForm, mustExist);
        }

        @Override protected boolean shouldSkip(File file, Set<String> filenamesToSkip)
        {
            return !super.shouldSkip(file, filenamesToSkip);
        }

    }

    /**
     * A test case which runs the parser over a file, expecting a successful parse.
     * Created by {@link ParserTestSuite}.
     * 
     * @author joverbey
     */
    public static final class ParserTestCase extends PhotranASTTestCase
    {
        public ParserTestCase(File file, boolean isFixedForm, String testCaseDescription)
        {
            super(file, isFixedForm, testCaseDescription);
        }

        @Override
        protected void handleAST(ASTExecutableProgramNode ast)
        {
            checkCorrectParenting(ast);
            checkNoErrorNodes(ast);
        }

        public ParserTestCase() { super(null, false, ""); } // to keep JUnit quiet
    }

    public static abstract class SourceReproductionTestSuite extends PhotranTestSuiteFromFiles
    {
        public SourceReproductionTestSuite(String directorySuffix, boolean isFixedForm, boolean mustExist) throws FileNotFoundException, IOException
        {
            super("Reproduce source code for", directorySuffix, isFixedForm, mustExist);
        }

        @Override
        protected PhotranASTTestCase createTestFor(File file, boolean isFixedForm, String fileDescription)
        {
            return new SourceReproductionTestCase(file, isFixedForm, fileDescription);
        }
    }
    
    public static class SourceReproductionTestCase extends PhotranASTTestCase
    {
        public SourceReproductionTestCase(File file, boolean isFixedForm, String testCaseDescription)
        {
            super(file, isFixedForm, testCaseDescription);
        }

        @Override
        protected void handleAST(ASTExecutableProgramNode ast) throws IOException
        {
            checkCorrectParenting(ast);
            // FIXME Add this back in: checkNoErrorNodes(ast);
            
            String originalSourceCode = getSourceCodeFromFile(file).replaceAll("\r", "");
            transform(ast);
            String reproducedSourceCode = getSourceCodeFromAST(ast).replaceAll("\r", "");
            assertEquals(originalSourceCode, reproducedSourceCode);
        }

        protected void transform(ASTExecutableProgramNode ast)
        {
            // Subclass and override to transform AST first
        }

        private String getSourceCodeFromFile(File file) throws IOException
        {
            StringBuffer sb = new StringBuffer(4096);
            BufferedReader in = new BufferedReader(new FileReader(file));
            for (int ch = in.read(); ch >= 0; ch = in.read())
                sb.append((char)ch);
            in.close();
            return sb.toString();
        }
        
        private String getSourceCodeFromAST(ASTExecutableProgramNode ast)
        {
            return SourcePrinter.getSourceCodeFromAST(ast);
        }

        public SourceReproductionTestCase() { super(null, false, ""); } // to keep JUnit quiet
    }
    
    public static abstract class LoopReplacerSRTestSuite extends PhotranTestSuiteFromFiles
    {
        public LoopReplacerSRTestSuite(String directorySuffix, boolean isFixedForm, boolean mustExist) throws FileNotFoundException, IOException
        {
            super("Replace loops and reproduce source for", directorySuffix, isFixedForm, mustExist);
        }

        @Override
        protected PhotranASTTestCase createTestFor(File file, boolean isFixedForm, String fileDescription)
        {
            return new LoopReplacerSRTestCase(file, isFixedForm, fileDescription);
        }
    }
    
    public static class LoopReplacerSRTestCase extends SourceReproductionTestCase
    {
        private long elapsedTime = 0L;
        
        public LoopReplacerSRTestCase(File file, boolean isFixedForm, String testCaseDescription)
        {
            super(file, isFixedForm, testCaseDescription);
        }

        @Override protected void transform(ASTExecutableProgramNode ast)
        {
            long start = System.currentTimeMillis();
            
            LoopReplacer.replaceAllLoopsIn(ast);
            
            elapsedTime = System.currentTimeMillis() - start;
        }
        
        public void testPerformance()
        {
            assertTrue("Loop replacer must complete in less than 5 seconds ("
                + (this.file == null ? "???" : this.file.getName())
                + " took "
                + ((elapsedTime/1000) + (elapsedTime%1000 > 500 ? 1 : 0))
                + " seconds)",
                elapsedTime < 5000);
        }

        public LoopReplacerSRTestCase() { super(null, false, ""); } // to keep JUnit quiet
    }
}
