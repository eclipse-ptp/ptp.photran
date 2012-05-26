/*******************************************************************************
 * Copyright (c) 2011 Mariano Mendez and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Mariano Mendez - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.refactoring;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.loops.ASTProperLoopConstructNode;
import org.eclipse.photran.internal.core.analysis.loops.ASTVisitorWithLoops;
import org.eclipse.photran.internal.core.analysis.loops.LoopReplacer;
import org.eclipse.photran.internal.core.lexer.Terminal;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTAssignmentStmtNode;
import org.eclipse.photran.internal.core.parser.ASTBinaryExprNode;
import org.eclipse.photran.internal.core.parser.ASTNameNode;
import org.eclipse.photran.internal.core.parser.ASTNode;
import org.eclipse.photran.internal.core.parser.ASTVarOrFnRefNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.parser.IActionStmt;
import org.eclipse.photran.internal.core.parser.IExecutableConstruct;
import org.eclipse.photran.internal.core.parser.IExecutionPartConstruct;
import org.eclipse.photran.internal.core.parser.IExpr;
import org.eclipse.photran.internal.core.parser.IObsoleteActionStmt;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranEditorRefactoring;
import org.eclipse.photran.internal.core.reindenter.Reindenter;
import org.eclipse.photran.internal.core.reindenter.Reindenter.Strategy;

/**
 * Change to Vector Form Refactoring: Change a loop that works only with an array
 * into a vector notation statement.
 * 
 * @author Mariano Mendez
 */
public class ChangeToVectorNotationRefactoring extends FortranEditorRefactoring
{
   
    private ASTProperLoopConstructNode DoLoopNode =null;
    @Override
    public String getName()
    {
        return "Change to Vector Notation"; //$NON-NLS-1$
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        ensureProjectHasRefactoringEnabled(status);
        removeFixedFormFilesFrom(this.selectedFiles, status);
        removeCpreprocessedFilesFrom(this.selectedFiles, status);
        LoopReplacer.replaceAllLoopsIn(this.astOfFileInEditor.getRoot());
        ensureDoLoopHasBeenSelected();
        checkIfCanBeChanged();
    }

    private void checkIfCanBeChanged()
    throws PreconditionFailure
    {
       Token index = this.DoLoopNode.getIndexVariable();

       DependencyFinderVisitor dependencyFinder= new DependencyFinderVisitor(index.getText());
       VectorNotationVisitor changer= new VectorNotationVisitor(index.getText());

       this.DoLoopNode.accept(dependencyFinder);
       this.DoLoopNode.getBody().accept(changer);

       if ((!changer.canBeChanged() || dependencyFinder.getHasDependencies() )  )
       {
           if (!changer.canBeChanged())
           {
             fail(
                 Messages.bind(
                     Messages.ChangeToVectorNotation_CanNotBeChanged,
                     changer.failMessage)
                 );  
           }
           else 
               fail(Messages.ChangeToVectorNotation_CanNotBeChangedToVectorNotation);
       }
    }

    private void ensureDoLoopHasBeenSelected()
    throws PreconditionFailure
    {
        ASTNode oldNode = getNode(this.astOfFileInEditor, this.selectedRegionInEditor, ASTProperLoopConstructNode.class);
        if (oldNode == null)
            fail(Messages.ChangeToVectorNotation_PleaseSelectDoLoopNode);
        else
        {
            if (!isOldStyleDoLoop((ASTProperLoopConstructNode)oldNode))
                DoLoopNode = (ASTProperLoopConstructNode)oldNode;
            else
                fail(Messages.ChangeToVectorNotation_PleaseSelectNewStyleDoLoopNode);
        }
    }

    private boolean isOldStyleDoLoop(ASTProperLoopConstructNode node)
    {
        return (node.getEndDoStmt()==null
               && node.getLoopHeader().getLblRef()!=null);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        // No final preconditions
    }

    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        // Do something with
        IASTNode newNode = getNewCode(this.DoLoopNode);
        this.DoLoopNode.replaceWith(newNode.toString().trim()+"\n"); //$NON-NLS-1$
        Reindenter.reindent(this.DoLoopNode, this.astOfFileInEditor, Strategy.REINDENT_EACH_LINE );
        this.addChangeFromModifiedAST(this.fileInEditor, pm);
        vpg.releaseAST(this.fileInEditor);
    }



    @SuppressWarnings("rawtypes")
    private IASTNode getNewCode(ASTProperLoopConstructNode node)
    {
        IExpr lowerBound = this.DoLoopNode.getLowerBoundIExpr();
        IExpr upperBound = this.DoLoopNode.getUpperBoundIExpr();
        final String newIndex=(lowerBound.toString()+":"+upperBound.toString()); //$NON-NLS-1$
        final String indexVariable = this.DoLoopNode.getIndexVariable().getText();

        IASTListNode newBody = ((IASTListNode)(node.getBody().clone()));

        newBody.accept(new ASTVisitorWithLoops()
        {
            @Override
            public void visitToken(Token token)
            {
                if (token.getTerminal() == Terminal.T_IDENT && (token.getText()).equals(indexVariable))
                {
                    String s2 = token.getText();
                    s2 = newIndex;
                    token.replaceWith(s2);
                }
            }
        });
        return newBody;
    }




    public class DependencyFinderVisitor extends ASTVisitorWithLoops
    {
        private boolean hasDependencies= false;
        private String indexVarName;

        public DependencyFinderVisitor(String indexVarName)
        {
            super();
            this.indexVarName=indexVarName;
        }

        public boolean getHasDependencies()
        {
            return hasDependencies;
        }
        @Override
        public void visitASTBinaryExprNode(ASTBinaryExprNode node)
        {
            IExpr lhsExpr; // in ASTBinaryExprNode
            IExpr rhsExpr; // in ASTBinaryExprNode

            lhsExpr=node.getLhsExpr();
            rhsExpr=node.getRhsExpr();


            checkExpr(lhsExpr);
            checkExpr(rhsExpr);
            traverseChildren(node);
        }

        private void checkExpr(IExpr Expr)
        {
            if (IsVariable(Expr))
            {
                ASTNameNode nameNode=((ASTVarOrFnRefNode)Expr).getName();
                if (nameNode !=null)
                {
                    if (this.indexVarName.equals(nameNode.getName().getText())) this.hasDependencies=true;
                }
            }

        }

        private boolean IsVariable(IExpr Expr)
        {
            return (Expr instanceof ASTVarOrFnRefNode );
        }
    }

    public class VectorNotationVisitor extends ASTVisitorWithLoops
    {
        private boolean CanChangeVectorNotation=true;
        private String indexVarName;
        private String failMessage=""; //$NON-NLS-1$
        
        public VectorNotationVisitor(String indexVarName)
        {
            super();
            this.indexVarName=indexVarName;
        }

        public boolean canBeChanged()
        {
            return CanChangeVectorNotation;
        }
        
        public String getFailMessage()
        {
            return this.failMessage;
        }

        @Override public void visitIExecutionPartConstruct(IExecutionPartConstruct node)
        {
            if (! (node  instanceof ASTAssignmentStmtNode ) )
                this.CanChangeVectorNotation=false;
            else CheckAsignmentNode((ASTAssignmentStmtNode)node);

        }


        private void CheckAsignmentNode(ASTAssignmentStmtNode  node)
        {
            Token lhsVariable= node.getLhsVariable().getName();
            IExpr rhs = node.getRhs();

            if (! leftHandCanBeChanged(lhsVariable) ) this.CanChangeVectorNotation=false;
            CheckRightHandExpr(rhs);
        }

        private void CheckRightHandExpr(IExpr rhs)
        {
            if (rhs instanceof ASTBinaryExprNode){
                CheckRightHandExpr(((ASTBinaryExprNode)rhs).getLhsExpr());
                CheckRightHandExpr(((ASTBinaryExprNode)rhs).getRhsExpr());
            }

            if (rhs instanceof ASTVarOrFnRefNode){
                ASTNameNode nameNode=((ASTVarOrFnRefNode)rhs).getName();
                if (nameNode!=null){
                        Token varName= nameNode.getName();
                        if ( varName!=null && varName.getText().toString().equals(this.indexVarName) )
                            this.CanChangeVectorNotation=false;
                }
            }
        }

        boolean leftHandCanBeChanged(Token lhsVariable)
        {
            if (lhsVariable.isIdentifier()) {

                List<Definition> definitions = lhsVariable.resolveBinding();

                if (!definitions.isEmpty() && definitions.size()==1)
                {
                    Definition symbol = definitions.get(0);
                    if (!symbol.isArray()) 
                        {
                            this.failMessage= "Variable " + lhsVariable.getText() + " must be an explicitly defined array"; //$NON-NLS-1$ //$NON-NLS-2$
                            return false; //if lhsVariable is not an array it can not be changed
                        }
                }
                else
                {
                    this.failMessage= "Variable " + lhsVariable.getText() + " must be an explicitly defined array"; //$NON-NLS-1$ //$NON-NLS-2$
                    return false;
                }

            }
            else
                return false ;

            return true ;
        }

        @Override public void visitIExecutableConstruct(IExecutableConstruct node)
        {
            visitIExecutionPartConstruct(node);
        }

       @Override public void visitIActionStmt(IActionStmt node)
        {
            visitIExecutionPartConstruct(node);
        }

        @Override public void visitIObsoleteActionStmt(IObsoleteActionStmt node)
        {
            visitIExecutionPartConstruct(node);
        }

    }
    
   
    
}