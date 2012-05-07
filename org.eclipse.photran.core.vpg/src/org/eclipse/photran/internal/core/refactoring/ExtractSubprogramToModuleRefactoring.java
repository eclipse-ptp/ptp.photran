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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTAssignmentStmtNode;
import org.eclipse.photran.internal.core.parser.ASTCallStmtNode;
import org.eclipse.photran.internal.core.parser.ASTDerivedTypeDefNode;
import org.eclipse.photran.internal.core.parser.ASTEntityDeclNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.parser.ASTFunctionSubprogramNode;
import org.eclipse.photran.internal.core.parser.ASTModuleNode;
import org.eclipse.photran.internal.core.parser.ASTNameNode;
import org.eclipse.photran.internal.core.parser.ASTObjectNameNode;
import org.eclipse.photran.internal.core.parser.ASTSubroutineSubprogramNode;
import org.eclipse.photran.internal.core.parser.ASTTypeDeclarationStmtNode;
import org.eclipse.photran.internal.core.parser.ASTUseStmtNode;
import org.eclipse.photran.internal.core.parser.ASTVarOrFnRefNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.parser.IInternalSubprogram;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranEditorRefactoring;

/**
 * Refactoring that extracts a subprogram to a module.
 * 
 * @author Gustavo Risetti
 */
@SuppressWarnings("nls") // TODO: Externalize strings
public class ExtractSubprogramToModuleRefactoring extends FortranEditorRefactoring {

    IASTNode selectedFunctionOrSubroutine = null;
    List<ASTModuleNode> fileModules = new LinkedList<ASTModuleNode>();
    private String moduleName;
    ScopingNode originalScope = null;
    List<Definition> parameters = new LinkedList<Definition>();

    @Override
    public String getName() {
        return "Extract Subroutine Or Function To Module";
    }

    public void setModuleName(String name){
        this.moduleName = name;
    }

    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        ensureProjectHasRefactoringEnabled(status);
        // Finds the selected node and checks if it is a subroutine or a function.
        IASTNode selectedNode = findEnclosingNode(astOfFileInEditor, selectedRegionInEditor);
        if(selectedNode instanceof ASTSubroutineSubprogramNode || selectedNode instanceof ASTFunctionSubprogramNode){
            selectedFunctionOrSubroutine = selectedNode;
        }else{
            fail("Please, select a Subroutine or a Function statement.");
        }
        // Stores all the modules of the file, to verify if the user will
        // enter a name of an existing module.
        for (ScopingNode scope : astOfFileInEditor.getRoot().getAllContainedScopes()){
            if(scope instanceof ASTModuleNode){
                fileModules.add((ASTModuleNode)scope);
            }
        }
        // Stores a reference to the scope of where the subroutine or function will be extracted.
        originalScope = ((ScopingNode)selectedFunctionOrSubroutine).getEnclosingScope();
    }

    ASTModuleNode moduleExists(String moduleName){
        for(ASTModuleNode module : fileModules){
            if(module.getName().equalsIgnoreCase(moduleName)){
                return module;
            }
        }
        return null;
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure {
        final String VALID_NAMES_WARNING = "Fill in the fields with valid values.";
        final String SPACE_TD_WARNING = "The module name can not contain spaces and exclamation points.";
        final String NUMERIC_DIGITS_WARNING = "The module name can not start with numeric digits.";
        final Character[] numeric_digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

        if(moduleName.length() < 1){
            fail(VALID_NAMES_WARNING);
        }
        for(int i = 0; i< moduleName.length(); i++){
            if(moduleName.charAt(i) == ' ' || moduleName.charAt(i) == '!' || moduleName.charAt(i) == '\t'){
                fail(SPACE_TD_WARNING);
            }
        }
        for(int i=0; i<numeric_digits.length; i++){
            if(moduleName.charAt(0) == numeric_digits[i]){
                fail(NUMERIC_DIGITS_WARNING);
            }
        }
        ASTModuleNode module = moduleExists(moduleName);
        if(module != null){
            fail("The module " + moduleName.toUpperCase() + " already exists. Choose another name for the module to be created.");
        }
    }

    @Override
    protected void doCreateChange(IProgressMonitor progressMonitor) throws CoreException, OperationCanceledException {
        List<ScopingNode> scopes = new LinkedList<ScopingNode>();
        // Located where the module will be added in the AST.
        Token insertModule = astOfFileInEditor.getRoot().findFirstToken();
        // Checks for a PARAMETER variable used only in the selected code.
        boolean moveParameter;
        for(Definition def : originalScope.getAllDefinitions()){
            if(def.isParameter()){
                if(!(hasReference(def.getDeclaredName(), originalScope)) && hasReference(def.getDeclaredName(), selectedFunctionOrSubroutine)){
                    moveParameter = true;
                    for(IInternalSubprogram internal : originalScope.getInternalSubprograms()){
                        if(internal instanceof ASTSubroutineSubprogramNode){
                            if(selectedFunctionOrSubroutine instanceof ASTSubroutineSubprogramNode){
                                if(((ASTSubroutineSubprogramNode)internal) != ((ASTSubroutineSubprogramNode)selectedFunctionOrSubroutine)){
                                    if(hasReference(def.getDeclaredName(), ((ASTSubroutineSubprogramNode)internal))){
                                        moveParameter = false;
                                    }
                                }
                            }else{
                                if(hasReference(def.getDeclaredName(), ((ASTSubroutineSubprogramNode)internal))){
                                    moveParameter = false;
                                }
                            }
                        }
                        if(internal instanceof ASTFunctionSubprogramNode){
                            if(selectedFunctionOrSubroutine instanceof ASTFunctionSubprogramNode){
                                if(((ASTFunctionSubprogramNode)internal) != ((ASTFunctionSubprogramNode)selectedFunctionOrSubroutine)){
                                    if(hasReference(def.getDeclaredName(), ((ASTFunctionSubprogramNode)internal))){
                                        moveParameter = false;
                                    }
                                }
                            }else{
                                if(hasReference(def.getDeclaredName(), ((ASTFunctionSubprogramNode)internal))){
                                    moveParameter = false;
                                }
                            }
                        }
                    }
                    if(moveParameter){
                        // If there is a parameter used only in the subroutine or function, it is stored
                        // in a list to be added in the module created.
                        parameters.add(def);
                        try{
                            // The original statement is removed from original scope.
                            removeVariableDeclFor(def);
                        }catch (PreconditionFailure e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Construction of the module.
        String newModuleNode = "MODULE " + moduleName + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
        // Adds parameters.
        for(Definition def : parameters){
            ASTTypeDeclarationStmtNode declarationNode = getTypeDeclarationStmtNode(def.getTokenRef().findToken().getParent());
            newModuleNode += "\t" + declarationNode.toString(); //$NON-NLS-1$
        }
        newModuleNode+="CONTAINS\n"; //$NON-NLS-1$
        // Adds the subroutine or function in the module body.
        newModuleNode+=selectedFunctionOrSubroutine.toString();
        newModuleNode+="END MODULE " + moduleName + "\n\n"; //$NON-NLS-1$ //$NON-NLS-2$
        insertModule.setText(newModuleNode + insertModule.getText());
        // Adds the USE statement in scopes where the extracted subroutine or function is used.
        addUseInScope(scopes);
        selectedFunctionOrSubroutine.removeFromTree();
        if(originalScope.getInternalSubprograms().size() == 1){
            originalScope.getContainsStmt().removeFromTree();
        }
        addChangeFromModifiedAST(fileInEditor, progressMonitor);
        vpg.releaseAST(fileInEditor);
    }

    private void removeVariableDeclFor(Definition def) throws PreconditionFailure {
        ASTTypeDeclarationStmtNode declarationNode = getTypeDeclarationStmtNode(def.getTokenRef().findToken().getParent());
        IASTListNode<ASTEntityDeclNode> entityDeclList = declarationNode.getEntityDeclList();
        if (entityDeclList.size() == 1) {
            declarationNode.findFirstToken().setWhiteBefore(""); //$NON-NLS-1$
            declarationNode.replaceWith(""); //$NON-NLS-1$
        }else {
            removeVariableDeclFromList(def, entityDeclList);
        }
    }

    private void removeVariableDeclFromList(Definition def, IASTListNode<ASTEntityDeclNode> entityDeclList) throws PreconditionFailure {
        for (ASTEntityDeclNode decl : entityDeclList) {
            ASTObjectNameNode objectName = decl.getObjectName();
            String declName = objectName.getObjectName().getText();
            if (declName.equals(def.getDeclaredName())) {
                if (!entityDeclList.remove(decl)) {
                    fail("The operation could not be completed.");
                }
                break;
            }
        }
        entityDeclList.findFirstToken().setWhiteBefore(" "); //$NON-NLS-1$
    }

    private ASTTypeDeclarationStmtNode getTypeDeclarationStmtNode(IASTNode node) {
        if (node == null){
            return null;
        }else if (node instanceof ASTTypeDeclarationStmtNode){
            return (ASTTypeDeclarationStmtNode)node;
        }else{
            return getTypeDeclarationStmtNode(node.getParent());
        }
    }

    private boolean hasReference(String name, IASTNode scope){
        boolean r = false;
        if(scope instanceof ASTSubroutineSubprogramNode){
            for (int i=0; i<((ASTSubroutineSubprogramNode)scope).getBody().size(); i++){
                r = isReferenced(((ASTSubroutineSubprogramNode)scope).getBody().get(i), name);
                if (r) break;
            }
        }
        if(scope instanceof ASTFunctionSubprogramNode){
            for (int i=0; i<((ASTFunctionSubprogramNode)scope).getBody().size(); i++){
                r = isReferenced(((ASTFunctionSubprogramNode)scope).getBody().get(i), name);
                if (r) break;
            }
        }
        return r;
    }

    private boolean hasReference(String name, ScopingNode scope){
        boolean r = false;
        for (int i=0; i<scope.getBody().size(); i++){
            r = isReferenced(scope.getBody().get(i), name);
            if (r) break;
        }
        return r;
    }

    private boolean isReferenced(IASTNode node, String name){
        boolean r = false;
        if (node instanceof ASTVarOrFnRefNode) {
            r = existsReferenceForVariable(node, name);
        } else {
            for (IASTNode child : node.getChildren()) {
                if (! r ) {
                    r = isReferenced(child, name);
                } else {
                    break;
                }
            }
        }
        return r;
    }

    private boolean existsReferenceForVariable(IASTNode node, String name){
        boolean r = false;
        if (node instanceof ASTNameNode) {
            if ( ((ASTNameNode)node).getName().getText().equalsIgnoreCase(name) ) {
                r = true;
            }
        } else {
            for (IASTNode child : node.getChildren()) {
                if (! r ) {
                    r = existsReferenceForVariable(child, name);
                } else {
                    break;
                }
            }
        }
        return r;
    }

    private void addUseInScope(List<ScopingNode> scopes) {
        String name = null;
        for (ScopingNode scope : astOfFileInEditor.getRoot().getAllContainedScopes()){
            if (!(scope instanceof ASTExecutableProgramNode) && !(scope instanceof ASTDerivedTypeDefNode)){
                for(IASTNode node : scope.getBody()){
                    if(node instanceof ASTCallStmtNode){
                        if(selectedFunctionOrSubroutine instanceof ASTSubroutineSubprogramNode){
                            name = ((ASTSubroutineSubprogramNode)selectedFunctionOrSubroutine).getName();
                        }else{
                            name = ((ASTFunctionSubprogramNode)selectedFunctionOrSubroutine).getName();
                        }
                        if(((ASTCallStmtNode)node).getSubroutineName().getText().equalsIgnoreCase(name)){
                            if(!scopes.contains(scope)){
                                scopes.add(scope);
                            }
                        }
                    }if(node instanceof ASTAssignmentStmtNode){
                        if(selectedFunctionOrSubroutine instanceof ASTFunctionSubprogramNode){
                            name = ((ASTFunctionSubprogramNode)selectedFunctionOrSubroutine).getName();
                            String funcName = null;
                            if(((ASTAssignmentStmtNode)node).getRhs() instanceof ASTVarOrFnRefNode){
                                funcName = ((ASTVarOrFnRefNode)((ASTAssignmentStmtNode)node).getRhs()).getName().getName().getText();
                                if(name.equalsIgnoreCase(funcName)){
                                    if(!scopes.contains(scope)){
                                        scopes.add(scope);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean hasUse = false;
        for(ScopingNode scope : scopes){
            hasUse = false;
            for(IASTNode node : scope.getBody()){
                if(node instanceof ASTUseStmtNode){
                    if(((ASTUseStmtNode)node).getName().getText().equalsIgnoreCase(moduleName)){
                        hasUse = true;
                        break;
                    }
                }
            }
            if(!hasUse){
                String lastToken = scope.getHeaderStmt().findLastToken().getText();
                scope.getHeaderStmt().findLastToken().setText(lastToken+"\tUSE " + moduleName + "\n"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }
}
