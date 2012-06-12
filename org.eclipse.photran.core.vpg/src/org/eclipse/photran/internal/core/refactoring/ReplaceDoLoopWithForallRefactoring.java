/*******************************************************************************
 * Copyright (c) 2011 UFSM - Universidade Federal de Santa Maria (www.ufsm.br).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.photran.internal.core.refactoring;

import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.internal.core.analysis.loops.ASTProperLoopConstructNode;
import org.eclipse.photran.internal.core.analysis.loops.LoopReplacer;
import org.eclipse.photran.internal.core.parser.ASTAssignmentStmtNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.parser.IExecutionPartConstruct;
import org.eclipse.photran.internal.core.parser.IExpr;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranEditorRefactoring;

/**
 * Refactoring that replaces a DO loop with a FORALL loop.
 * 
 * @author Gustavo Risetti
 */
@SuppressWarnings("nls") // TODO: Externalize strings
public class ReplaceDoLoopWithForallRefactoring extends FortranEditorRefactoring {

    ASTProperLoopConstructNode selected_do_loop = null;
    LinkedList<ASTProperLoopConstructNode> nested_selected_do_loop = new LinkedList<ASTProperLoopConstructNode>();

    @Override
    public String getName() {
        return "Replace Do Loop With Forall (Unchecked)";
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        ensureProjectHasRefactoringEnabled(status);
        LoopReplacer.replaceAllLoopsIn(this.astOfFileInEditor.getRoot());
        IASTNode do_loop_node = findEnclosingNode(astOfFileInEditor, selectedRegionInEditor);
        if(!(do_loop_node instanceof ASTProperLoopConstructNode)){
            fail("Please, select a Do Loop.");
        }
        // Find the selected loop.
        selected_do_loop = findSelectedDoLoop();
        // Checks if in the loops are made only assignments.
        if(selected_do_loop != null){
            IASTListNode<IExecutionPartConstruct> body_selected_loop = selected_do_loop.getBody();
            for(IExecutionPartConstruct i : body_selected_loop){
                if(!getBody(i)){
                    fail("Sorry, this refactoring can be applied only in Do Loops that contains just variable assignments.");
                }
            }
        }
        findNestedSelectedDoLoop();
    }

    private boolean getBody(IExecutionPartConstruct node){
        if(node instanceof ASTProperLoopConstructNode){
            IASTListNode<IExecutionPartConstruct> body = ((ASTProperLoopConstructNode)node).getBody();
            for(IExecutionPartConstruct i : body){
                if(i instanceof ASTProperLoopConstructNode){
                    return getBody(i);
                }else{
                    if(!(i instanceof ASTAssignmentStmtNode)){
                        return false;
                    }
                }
            }
        }else if(!(node instanceof ASTAssignmentStmtNode)){
            return false;
        }
        return true;
    }

    private ASTProperLoopConstructNode findSelectedDoLoop() {
        return getLoopNode(this.astOfFileInEditor, this.selectedRegionInEditor);
    }

    private void findNestedSelectedDoLoop(){
        if(selected_do_loop != null){
            IASTListNode<IExecutionPartConstruct> body_selected_loop = selected_do_loop.getBody();
            for(IExecutionPartConstruct i : body_selected_loop){
                addNestedSelectedDoLoop(i);
            }
        }
    }

    private void addNestedSelectedDoLoop(IExecutionPartConstruct node){
        if(node instanceof ASTProperLoopConstructNode){
            nested_selected_do_loop.add((ASTProperLoopConstructNode)node);
            IASTListNode<IExecutionPartConstruct> body = ((ASTProperLoopConstructNode)node).getBody();
            for(IExecutionPartConstruct i : body){
                if(i instanceof ASTProperLoopConstructNode){
                    addNestedSelectedDoLoop(i);
                }
            }
        }
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        // There is no final conditions to check...
    }

    @Override
    protected void doCreateChange(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
        for(int i=nested_selected_do_loop.size()-1; i>=0; i--){
            ASTProperLoopConstructNode node = nested_selected_do_loop.get(i);
            // Constructs a new FORALL node with the contents of the DO node and replaces in the AST.
            String forall = constructForallNode(node);
            node.replaceWith(parseLiteralStatementSequence(forall));
        }
        String forall = constructForallNode(selected_do_loop);
        selected_do_loop.replaceWith(parseLiteralStatementSequence(forall));
        addChangeFromModifiedAST(fileInEditor, progressMonitor);
        vpg.releaseAST(fileInEditor);
    }

    // Constructs a new FORALL node.
    private String constructForallNode(ASTProperLoopConstructNode node){
        String variable = node.getLoopHeader().getLoopControl().getVariableName().getText();
        String initial_value = node.getLoopHeader().getLoopControl().getLb().toString().trim();
        String final_value = node.getLoopHeader().getLoopControl().getUb().toString().trim();
        String tab_init = node.findFirstToken().getWhiteBefore();
        String tab_end = node.getEndDoStmt().findFirstToken().getWhiteBefore();
        String step = null;
        IExpr step_expr = node.getLoopHeader().getLoopControl().getStep();
        if(step_expr != null){
            step = step_expr.toString();
        }
        String forall = tab_init + "FORALL (" + variable + "=" + initial_value + ":" + final_value; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if(step != null){
            forall += ":" + step + ")"; //$NON-NLS-1$ //$NON-NLS-2$
        }else{
            forall += ")"; //$NON-NLS-1$
        }
        String body = node.getBody().toString();
        forall += "\n" + body + tab_end + "END FORALL"; //$NON-NLS-1$ //$NON-NLS-2$
        return forall;
    }
}
