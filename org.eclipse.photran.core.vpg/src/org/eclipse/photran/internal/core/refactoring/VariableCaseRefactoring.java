/*******************************************************************************
 * Copyright (c) 2010 Tombazzi Juan, Aquino German, Mariano Mendez and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Tombazzi Juan - Initial API and implementation
 * Aquino German - Initial API and implementation
 * Mariano Mendez - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.refactoring;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTCommonBlockObjectNode;
import org.eclipse.photran.internal.core.parser.ASTFunctionParNode;
import org.eclipse.photran.internal.core.parser.ASTFunctionSubprogramNode;
import org.eclipse.photran.internal.core.parser.ASTNameNode;
import org.eclipse.photran.internal.core.parser.ASTNamedConstantDefNode;
import org.eclipse.photran.internal.core.parser.ASTObjectNameNode;
import org.eclipse.photran.internal.core.parser.ASTSubroutineParNode;
import org.eclipse.photran.internal.core.parser.ASTVarOrFnRefNode;
import org.eclipse.photran.internal.core.parser.GenericASTVisitor;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranResourceRefactoring;

/**
 * Change Variable Case: refactoring to unify case of all variable names in Fortran files.
 * 
 * @author German Aquino
 * @author Federico Tombazzi
 */
public class VariableCaseRefactoring extends FortranResourceRefactoring
{
    private boolean lowerCase = true;   //true for lower case, false for upper case

    @Override
    public String getName()
    {
        return Messages.VariableCaseRefactoring_Name;
    }

    public void setLowerCase(boolean value)
    {
        this.lowerCase = value;
    }

    /** from RepObsOpersRefactoring.java */
    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure
    {
        ensureProjectHasRefactoringEnabled(status);
        removeFixedFormFilesFrom(this.selectedFiles, status);
        removeCpreprocessedFilesFrom(this.selectedFiles, status);
    }

    /** from RepObsOpersRefactoring.java */
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure
    {
        try
        {
            for (IFile file : selectedFiles)
            {
                IFortranAST ast = vpg.acquirePermanentAST(file);
                if (ast == null)
                    status.addError(Messages.bind(Messages.VariableCaseRefactoring_SelectedFileCannotBeParsed, file.getName()));
                makeChangesTo(file, ast, status, pm);
                vpg.releaseAST(file);
            }
        }
        finally
        {
            vpg.releaseAllASTs();
        }
    }

    /** modeled after RepObsOpersRefactoring.java */
    private void makeChangesTo(IFile file, IFortranAST ast, RefactoringStatus status, IProgressMonitor pm) throws Error
    {
        try
        {
            if (ast == null) return;

            CaseChangingVisitor replacer = new CaseChangingVisitor();
            replacer.lowerCase = this.lowerCase;
            ast.accept(replacer);
            if (replacer.changedAST) // Do not include the file in changes unless actually changed
                addChangeFromModifiedAST(file, pm);
        }
        catch (Exception e)
        {
            throw new Error(e);
        }
    }

    /** from RepObsOpersRefactoring.java */
    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException, OperationCanceledException
    {
    }

    private static final class CaseChangingVisitor extends GenericASTVisitor
    {
        private boolean changedAST = false;
        private boolean lowerCase;

        @Override
        public void visitASTNameNode(ASTNameNode node) {
            checkNode(node);
        }

        @Override
        public void visitASTObjectNameNode(ASTObjectNameNode node) {
            checkNode(node); 
        }

        private void checkNode(IASTNode node){
            if(!(identifierIsInFunctionCall(node) || identifierIsInFunctionReturn(node))){
                changeCaseOf(node.findFirstToken());         
            }
        }        

        private boolean identifierIsInFunctionCall(IASTNode node){
            IASTNode parent = node.getParent();
            /*if the parent node represents a function name in a function call,
            it should not be modified.*/
            if (parent instanceof ASTVarOrFnRefNode){
                ASTVarOrFnRefNode varOfFnNode = (ASTVarOrFnRefNode)parent;
                if(varOfFnNode.getPrimarySectionSubscriptList() != null){
                    return true;
                }                
            }
            return false;
        }

        private boolean identifierIsInFunctionReturn(IASTNode node){
            IASTNode parent = node.getParent();
            //if the node name matches the enclosing function name, it should not be modified.
            while (parent != null && ! (parent instanceof ASTFunctionSubprogramNode)){
                parent = parent.getParent();
            }
            if (parent != null){
                ASTFunctionSubprogramNode functionNode = (ASTFunctionSubprogramNode)parent;
                String functionName = functionNode.getFunctionStmt().getFunctionName().getFunctionName().getText();
                if (functionName.equals(node.findFirstToken().getText())){
                    return true;
                }
            }
            return false;
        }
 
        @Override
        public void visitASTNamedConstantDefNode(ASTNamedConstantDefNode node) {
            changeCaseOf(node.findFirstToken());  
        }
        
        @Override
        public void visitASTCommonBlockObjectNode(ASTCommonBlockObjectNode node) {
            changeCaseOf(node.findFirstToken());  
        }

        @Override
        public void visitASTSubroutineParNode(ASTSubroutineParNode node) {
            changeCaseOf(node.findFirstToken());  
        }

        @Override
        public void visitASTFunctionParNode(ASTFunctionParNode node) {
            changeCaseOf(node.findFirstToken());  
        }


        private void changeCaseOf(Token node)
        {
            if(lowerCase)
                node.findFirstToken().setText(node.getText().toLowerCase());
            else
                node.findFirstToken().setText(node.getText().toUpperCase());

            changedAST = true;
        }
    }
}

