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
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTCallStmtNode;
import org.eclipse.photran.internal.core.parser.ASTDerivedTypeDefNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.parser.ASTMainProgramNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranResourceRefactoring;

/**
 * Refactoring that adds a call tree in a comment preceding each subprogram.
 * 
 * @author Gustavo Risetti
 */
//@SuppressWarnings("nls") // TODO: Externalize strings
public class IntroduceCallTreeRefactoring extends FortranResourceRefactoring {
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        try {
            for (IFile file : selectedFiles) {
                IFortranAST ast = vpg.acquirePermanentAST(file);
                if (ast == null) {
                    status.addError(Messages.bind(Messages.IntroduceCallTreeRefactoring_CannotParse, file.getName()));
                } else {
                    makeChangesTo(file, ast, status, pm);
                    vpg.releaseAST(file);
                }
            }
        } finally {
            vpg.releaseAllASTs();
        }
    }

    @SuppressWarnings("unchecked")
    private void makeChangesTo(IFile file, IFortranAST ast, RefactoringStatus status, IProgressMonitor pm) {
        List<ScopingNode> scopes = ast.getRoot().getAllContainedScopes();
        List<Character> blank_characters = new LinkedList<Character>();
        List<String> callStmtsOfScope = new LinkedList<String>();
        Integer line_int = new Integer(0);
        ScopingNode scopeChanged = null;
        boolean hasCall = false;
        // Get the number of calls within the main program
        int mainProgramCalls = getMainProgramCalls(scopes);
        int scopeCalls = 0;
        for (ScopingNode scope : scopes){
            // Updates number of calls in scope.
            scopeCalls = getScopeCalls(scopeCalls, scope);
            callStmtsOfScope.clear();
            blank_characters.clear();
            scopeChanged = null;
            hasCall = false;
            if (!(scope instanceof ASTExecutableProgramNode) && !(scope instanceof ASTDerivedTypeDefNode)){
                IASTListNode<IASTNode> body = (IASTListNode<IASTNode>)scope.getBody();
                for (IASTNode node : body){
                    if (node instanceof ASTCallStmtNode){
                        hasCall = true;
                        scopeChanged = scope;
                        // Information of the line number where are the call.
                        if(scope instanceof ASTMainProgramNode){
                            line_int = node.findFirstToken().getLine() + mainProgramCalls;
                        }else{
                            line_int = node.findFirstToken().getLine() + scopeCalls + mainProgramCalls;
                        }
                        String line = line_int.toString();
                        // Adds entry to the calls buffer of the scope.
                        callStmtsOfScope.add(Messages.bind(Messages.IntroduceCallTreeRefactoring_OnLine, ((ASTCallStmtNode)node).getSubroutineName().getText() , line));
                    }
                }
                if((hasCall== true) && (scopeChanged != null)){
                    // Get the declaration of the subprogram.
                    Token firstToken = scopeChanged.findFirstToken();
                    String firstTokenText = firstToken.getText();
                    // Get indentation
                    String tab = ""; //$NON-NLS-1$
                    String headerStmt = scopeChanged.getHeaderStmt().toString();
                    String[] headerStmtWithoutComments = headerStmt.split("\n"); //$NON-NLS-1$
                    headerStmt = headerStmtWithoutComments[headerStmtWithoutComments.length - 1];
                    tab = getBlankCharacters(blank_characters, tab, headerStmt);
                    String name = ""; //$NON-NLS-1$
                    if(scopeChanged.isMainProgram()){
                        name = " in program "; //$NON-NLS-1$
                    }if(scopeChanged.isModule()){
                        name = " in module "; //$NON-NLS-1$
                    }if(scopeChanged.isSubprogram()){
                        name = " in subroutine "; //$NON-NLS-1$
                    }
                    // Prints the call tree of each subroutine.
                    firstToken.setText("! " + "Calls" + name + scopeChanged.getName().toUpperCase()+": \n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    for(int i=0; i<callStmtsOfScope.size(); i++){
                        String arrow = "="; //$NON-NLS-1$
                        for(int j=0; j<i; j++){
                            arrow += "="; //$NON-NLS-1$
                        }
                        arrow += ">"; //$NON-NLS-1$
                        firstToken.setText(firstToken.getText()+ tab + "! " + arrow + " " + callStmtsOfScope.get(i).toString() + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                    }
                    firstToken.setText(firstToken.getText()+tab + firstTokenText.trim());
                }
            }
        }
        addChangeFromModifiedAST(file, pm);
    }

    // Get correct indentation.
    private String getBlankCharacters(List<Character> blank_characters, String tab, String headerStmt) {
        boolean start = false;
        for(int i=0; i<headerStmt.length(); i++){
            char c = headerStmt.charAt(i);
            if((c != '\t') && (c != ' ')){
                start = true;
            }
            if((c == '\t' || c == ' ') && !start){
                blank_characters.add(headerStmt.charAt(i));
            }
        }
        for(int i=0; i<blank_characters.size();i++){
            tab+=blank_characters.get(i);
        }
        return tab;
    }

    // Get the calls of the scope.
    @SuppressWarnings("unchecked")
    private int getScopeCalls(int scopeCalls, ScopingNode scope) {
        if (!(scope instanceof ASTExecutableProgramNode) && !(scope instanceof ASTDerivedTypeDefNode)){
            IASTListNode<IASTNode> body = (IASTListNode<IASTNode>)scope.getBody();
            int n = 0;
            for (IASTNode node : body){
                if (node instanceof ASTCallStmtNode){
                    n++;
                }
            }
            if(n > 0){
                n++;
            }
            scopeCalls+=n;
        }
        return scopeCalls;
    }

    // Get the calls of the main program.
    @SuppressWarnings("unchecked")
    private int getMainProgramCalls(List<ScopingNode> scopes){
        int mainProgramCalls = 0;
        for (ScopingNode scope : scopes){
            if(scope instanceof ASTMainProgramNode){
                IASTListNode<IASTNode> body = (IASTListNode<IASTNode>)scope.getBody();
                for (IASTNode node : body){
                    if (node instanceof ASTCallStmtNode){
                        mainProgramCalls ++;
                    }
                }
                if(mainProgramCalls > 0){
                    mainProgramCalls ++;
                }
            }
        }
        return mainProgramCalls;
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        ensureProjectHasRefactoringEnabled(status);
        removeFixedFormFilesFrom(this.selectedFiles, status);
        removeCpreprocessedFilesFrom(this.selectedFiles, status);
    }

    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        // The change is made in method makeChangesTo(...).
    }

    @Override
    public String getName() {
        return Messages.IntroduceCallTreeRefactoring_Name;
    }
}
