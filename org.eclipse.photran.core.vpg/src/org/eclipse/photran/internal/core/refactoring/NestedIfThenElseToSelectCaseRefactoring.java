/*******************************************************************************
 * Copyright (c) 2011 UFSM - Universidade Federal de Santa Maria (www.ufsm.br).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.photran.internal.core.refactoring;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.parser.ASTDerivedTypeDefNode;
import org.eclipse.photran.internal.core.parser.ASTElseConstructNode;
import org.eclipse.photran.internal.core.parser.ASTElseIfConstructNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.parser.ASTIfConstructNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranResourceRefactoring;

/**
 * Refactoring that converts nested if/then/else constructs into a SELECT CASE construct.
 * 
 * @author Gustavo Risetti
 */
@SuppressWarnings("nls") // TODO: Externalize strings
public class NestedIfThenElseToSelectCaseRefactoring extends FortranResourceRefactoring{

    List<ASTIfConstructNode> ifNodes = new LinkedList<ASTIfConstructNode>();
    List<ASTIfConstructNode> removeIfNodes = new LinkedList<ASTIfConstructNode>();
    List<String> varNames = new LinkedList<String>();
    List<String> cases = new LinkedList<String>();
    List<String> bodyCases = new LinkedList<String>();

    @Override
    public String getName() {
        return "Nested If-Then-Else To Select Case";
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        ensureProjectHasRefactoringEnabled(status);
        removeFixedFormFilesFrom(this.selectedFiles, status);
        removeCpreprocessedFilesFrom(this.selectedFiles, status);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure{
        try {
            for (IFile file : selectedFiles) {
                IFortranAST ast = vpg.acquirePermanentAST(file);
                if(ast == null) {
                    status.addError("One of the selected files (" + file.getName() +") cannot be parsed.");
                }else {
                    makeChangesTo(file, ast, status, pm);
                    vpg.releaseAST(file);
                }
            }
        }finally {
            vpg.releaseAllASTs();
        }
    }

    @SuppressWarnings("unchecked")
    private void makeChangesTo(IFile file, IFortranAST ast, RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        boolean hasChanged = false;
        List<ScopingNode> scopes = ast.getRoot().getAllContainedScopes();
        for(ScopingNode scope : scopes){
            if (!(scope instanceof ASTExecutableProgramNode) && !(scope instanceof ASTDerivedTypeDefNode)){
                IASTListNode<IASTNode> body = (IASTListNode<IASTNode>)scope.getBody();
                // Find nodes which contains nested if-then-else and add in a node list.
                for(IASTNode node : body){
                    if(node instanceof ASTIfConstructNode){
                        ASTIfConstructNode ifNode = ((ASTIfConstructNode)node);
                        if(ifNode.getElseIfConstruct() != null){
                            ifNodes.add((ASTIfConstructNode)node);
                        }
                    }
                }
            }
        }
        // Checks that appear only allowed comparisons (==, .eq., or)
        for(ASTIfConstructNode ifNode : ifNodes){
            parserStmts(ifNode, null);
        }
        // Remove the nodes that do not satisfy the condition to be a case of a SELECT CASE
        ifNodes.removeAll(removeIfNodes);
        for(ASTIfConstructNode ifNode : ifNodes){
            // Gets the list of values and cases for the new node to be built.
            getPairs(ifNode, null);
            // Checks if the same variable is evaluated in expressions.
            if(hasSameVars()){
                hasChanged = true;
                String[] tabWithComments = ifNode.findFirstToken().getWhiteBefore().toString().split("\n"); //$NON-NLS-1$
                String tab = tabWithComments[tabWithComments.length-1];
                for(int i = 0; i < tab.length(); i ++){
                    if(tab.charAt(i) == ' ' || tab.charAt(i) == '\t'){
                        continue;
                    }else{
                        tab = ""; //$NON-NLS-1$
                        break;
                    }
                }
                // The new node is constructed.
                String selectCaseNode = "SELECT CASE (" + varNames.get(0).trim() + ")\n"; //$NON-NLS-1$ //$NON-NLS-2$
                for(int i = 0; i< cases.size(); i++){
                    String casesUnits = tab + "\tCASE " + cases.get(i).trim() + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
                    String[] body = bodyCases.get(i).split("\n"); //$NON-NLS-1$
                    for(String bodyLine : body){
                        casesUnits += tab + "\t\t" + bodyLine.trim()+"\n"; //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    selectCaseNode += casesUnits;
                }
                selectCaseNode += tab + "END SELECT\n"; //$NON-NLS-1$
                // Replaces the old node with the new node in AST.
                ifNode.replaceWith(selectCaseNode);
                varNames.clear();
                cases.clear();
                bodyCases.clear();
            }else{
                varNames.clear();
                cases.clear();
                bodyCases.clear();
            }
        }
        if (hasChanged){
            addChangeFromModifiedAST(file, pm);
        }
    }

    // Checks if the same variable is evaluated in expressions.
    private boolean hasSameVars(){
        for(int i=0; i<varNames.size()-1; i++){
            if(!(varNames.get(i).equalsIgnoreCase(varNames.get(i+1)))){
                return false;
            }
        }
        return true;
    }

    // Gets the list of values ​​and cases.
    private void getPairs(ASTIfConstructNode ifNode, ASTElseIfConstructNode elseIfNode) {
        if(elseIfNode == null){
            String ifExpression = ifNode.getIfThenStmt().getGuardingExpression().toString().trim();
            ifExpression = ifExpression.replaceAll(".OR.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".Or.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".oR.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".EQ.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".Eq.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".eQ.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            ifExpression = ifExpression.replaceAll(".eq.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            String[] orSplit = ifExpression.split(".or."); //$NON-NLS-1$
            for(String s : orSplit){
                s = s.trim();
                String[] eqSplit = s.split("=="); //$NON-NLS-1$
                for(String seq : eqSplit){
                    seq = seq.trim();
                }
                for(int i=0; i<eqSplit.length; i+=2){
                    varNames.add(eqSplit[i]);
                    cases.add("("+eqSplit[i+1].trim()+")"); //$NON-NLS-1$ //$NON-NLS-2$
                    bodyCases.add(ifNode.getConditionalBody().toString());
                }
            }
            ASTElseIfConstructNode elseIf = ifNode.getElseIfConstruct();
            if(elseIf != null){
                getPairs(ifNode, elseIf);
            }
        }else{
            String elseIfExpression = elseIfNode.getElseIfStmt().getGuardingExpression().toString().trim();
            elseIfExpression = elseIfExpression.replaceAll(".OR.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".Or.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".oR.", ".or."); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".EQ.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".Eq.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".eQ.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            elseIfExpression = elseIfExpression.replaceAll(".eq.", "=="); //$NON-NLS-1$ //$NON-NLS-2$
            String[] orSplit = elseIfExpression.split(".or."); //$NON-NLS-1$
            for(String s : orSplit){
                s = s.trim();
                String[] eqSplit = s.split("=="); //$NON-NLS-1$
                for(String seq : eqSplit){
                    seq = seq.trim();
                }
                for(int i=0; i<eqSplit.length; i+=2){
                    varNames.add(eqSplit[i]);
                    cases.add("("+eqSplit[i+1].trim()+")"); //$NON-NLS-1$ //$NON-NLS-2$
                    bodyCases.add(elseIfNode.getConditionalBody().toString());
                }
            }
            ASTElseIfConstructNode elseIf = elseIfNode.getElseIfConstruct();
            if(elseIf != null){
                getPairs(ifNode, elseIf);
            }else{
                ASTElseConstructNode elseDefault = elseIfNode.getElseConstruct();
                if(elseDefault != null){
                    String elseDefaultExpression = elseDefault.getConditionalBody().toString();
                    cases.add("DEFAULT"); //$NON-NLS-1$
                    bodyCases.add(elseDefaultExpression);
                }
            }
        }
    }

    // Checks that appear only allowed comparisons (==, .eq., or)
    private void parserStmts(ASTIfConstructNode ifNode, ASTElseIfConstructNode elseIfNode) {
        String not_aloowed[] = {">", ".GT.", "<", ".LT.", ">=", ".GE.", "<=", ".LE.", "/=", ".NE.", ".AND.", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$
                                ".EQV.", ".NEQV.", ".gt.", ".lt.", ".ge.", ".le.", ".ne.", ".and.", ".eqv.", ".neqv."}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
        if(elseIfNode == null){
            String ifExpression = ifNode.getIfThenStmt().getGuardingExpression().toString().trim();
            for(String s : not_aloowed){
                if(ifExpression.contains(s)){
                    removeIfNodes.add(ifNode);
                    return;
                }
            }
            ASTElseIfConstructNode elseIf = ifNode.getElseIfConstruct();
            if(elseIf != null){
                parserStmts(ifNode, elseIf);
            }
        }else{
            String elseIfExpression = elseIfNode.getElseIfStmt().getGuardingExpression().toString().trim();
            for(String s : not_aloowed){
                if(elseIfExpression.contains(s)){
                    removeIfNodes.add(ifNode);
                    return;
                }
            }
            ASTElseIfConstructNode elseIf = elseIfNode.getElseIfConstruct();
            if(elseIf != null){
                parserStmts(ifNode, elseIf);
            }
        }
    }

    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        // The change is made in method makeChangesTo(...).
    }
}
