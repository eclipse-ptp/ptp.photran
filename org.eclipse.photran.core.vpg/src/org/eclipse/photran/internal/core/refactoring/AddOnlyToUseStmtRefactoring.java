/*******************************************************************************
 * Copyright (c) 2009, 2013 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *    Louis Orenstein (Tech-X Corporation) - fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=399565
 *******************************************************************************/
package org.eclipse.photran.internal.core.refactoring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.RefactoringStatusContext;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.lexer.Terminal;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTEntityDeclNode;
import org.eclipse.photran.internal.core.parser.ASTModuleNode;
import org.eclipse.photran.internal.core.parser.ASTSeparatedListNode;
import org.eclipse.photran.internal.core.parser.ASTUseStmtNode;
import org.eclipse.photran.internal.core.parser.GenericASTVisitor;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranEditorRefactoring;
import org.eclipse.photran.internal.core.vpg.PhotranTokenRef;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;

/**
 * Refactoring to add an ONLY clause to a USE statement.
 *
 * @author Kurt Hendle
 * @author Jeff Overbey - externalized strings
 * @author Louis Orenstein (Tech-X Corporation) - fix for https://bugs.eclipse.org/399565 and https://bugs.eclipse.org/404170
 */
public class AddOnlyToUseStmtRefactoring extends FortranEditorRefactoring
{
    private String moduleName = null;
    private IProject projectInEditor = null;
    private ASTUseStmtNode useNode = null;
    private List<IFile> filesContainingModule = null;

    private List<String> entitiesInProgram = new ArrayList<String>();

    private List<String> entitiesInModule = new ArrayList<String>();
    private Map<String, Definition> moduleEntDefMap = new HashMap<String, Definition>();

    private List<Definition> existingOnlyList = new ArrayList<Definition>();
    private List<Definition> defsToAdd = new ArrayList<Definition>();
    private Set<String> entitiesToAdd = new HashSet<String>();

    private Set<PhotranTokenRef> allReferences = null;
    private boolean onlyListInitiatlized = false;

    public AddOnlyToUseStmtRefactoring()
    {
    }

    public AddOnlyToUseStmtRefactoring(IFile file, ITextSelection selection)
    {
        initialize(file, selection);
    }

    public List<String> getModuleEntityList()
    {
        return entitiesInModule;
    }

    public void addToOnlyList(String name)
    {
        final String id = PhotranVPG.canonicalizeIdentifier(name);
        entitiesToAdd.add(id);
        if (!defsToAdd.contains(moduleEntDefMap.get(id))) {
            defsToAdd.add(moduleEntDefMap.get(id));
        }
    }

    public void removeFromOnlyList(String name)
    {
        entitiesToAdd.remove(name);
        defsToAdd.remove(moduleEntDefMap.get(name));
    }

    public Set<String> getNewOnlyList()
    {
        return entitiesToAdd;
    }
    
    @Override
    protected void preCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        // no-op
    }

    /* (non-Javadoc)
     * @see org.eclipse.photran.internal.core.refactoring.infrastructure.AbstractFortranRefactoring#doCheckInitialConditions(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        ensureProjectHasRefactoringEnabled(status);

        moduleName = this.selectedRegionInEditor.getText();
        if(moduleName == null || moduleName.equals("")) //$NON-NLS-1$
            fail(Messages.AddOnlyToUseStmtRefactoring_NoModuleNameSelected);

        findUseStmtNode();
        checkIfModuleExistsInProject();
        getModuleDeclaredEntities();
        getProgramDeclaredEntities();
        readExistingOnlyList();
    }

    private void findUseStmtNode() throws PreconditionFailure
    {
      //get the use statement node in case we need to add to the only list
        Token token = findEnclosingToken();
        if(token == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_SelectModuleName);

        useNode = token.findNearestAncestor(ASTUseStmtNode.class);
        if(useNode == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_SelectModuleName);
    }

    private void checkIfModuleExistsInProject() throws PreconditionFailure
    {
      //Check to see if the module exists in the project
        projectInEditor = this.fileInEditor.getProject();  //current project
        filesContainingModule = vpg.findFilesThatExportModule(moduleName);

        if(filesContainingModule.isEmpty() || filesContainingModule == null)
            fail(Messages.bind(Messages.AddOnlyToUseStmtRefactoring_NoFilesContainModule, moduleName));
        else if(filesContainingModule.size() > 1)
            filterFileList();

       //check again after the filtering happens
        if(filesContainingModule.isEmpty() || filesContainingModule == null)
            fail(Messages.bind(Messages.AddOnlyToUseStmtRefactoring_NoFilesContainModule, moduleName));

        if(filesContainingModule.size() > 1)
            fail(Messages.bind(Messages.AddOnlyToUseStmtRefactoring_MultipleDefinitionsOfModule, moduleName));
    }

    //same method used in CommonVarNamesRefactoring.java
    private void filterFileList() throws PreconditionFailure
    {
        if(projectInEditor == null) fail(Messages.AddOnlyToUseStmtRefactoring_ProjectDoesNotExist);

        //filter out files not in the project
        int i = 0;
        while(i < filesContainingModule.size())
        {
            if(filesContainingModule.get(i) == null
                || filesContainingModule.get(i).getProject() != projectInEditor)
                filesContainingModule.remove(i);   //shifts all elements left, don't increment i
            else
                i++;
        }
    }

    //modified from RenameRefactoring.java
    private Token findEnclosingToken() throws PreconditionFailure
    {
        Token selectedToken = findEnclosingToken(this.astOfFileInEditor, this.selectedRegionInEditor);
        if (selectedToken == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_PleaseSelectModuleName);
        return selectedToken;
    }

    private void getProgramDeclaredEntities() throws PreconditionFailure
    {
        IFortranAST ast = vpg.acquirePermanentAST(this.fileInEditor);
        if(ast == null) return;

        DeclarationVisitor visitor = new DeclarationVisitor();
        ast.accept(visitor);
        vpg.releaseAST(this.fileInEditor);
    }

    private void getModuleDeclaredEntities() throws PreconditionFailure
    {
        //get module declaration and check if it has declared entities
        PhotranTokenRef moduleTokenRef = vpg.getModuleTokenRef(moduleName);
        if(moduleTokenRef == null)
            fail(Messages.bind(Messages.AddOnlyToUseStmtRefactoring_NoModuleNamed, moduleName));

        Token moduleToken = moduleTokenRef.findTokenOrReturnNull();
        if(moduleToken == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_ModuleTokenNotFound);

        ASTModuleNode moduleNode = moduleToken.findNearestAncestor(ASTModuleNode.class);
        if(moduleNode == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_ModuleNodeNodeFound);

        List<Definition> moduleDefs = moduleNode.getAllPublicDefinitions();        
        if (moduleDefs.isEmpty()) {
            fail(Messages.AddOnlyToUseStmtRefactoring_NoDeclarationsInModule);
        } else {
            for(Definition def : moduleDefs) {
                moduleEntDefMap.put(def.getCanonicalizedName(), def);
                entitiesInModule.add(def.getCanonicalizedName());
            }
        }
    }

    public void readExistingOnlyList()
    {
        @SuppressWarnings("rawtypes")
        ASTSeparatedListNode existingOnlys = (ASTSeparatedListNode)useNode.getOnlyList();
        if(existingOnlys != null)
        {
            for(Object existingOnly : existingOnlys) {
                final String id = PhotranVPG.canonicalizeIdentifier(existingOnly.toString().trim());
                entitiesToAdd.add(id);
                existingOnlyList.add(moduleEntDefMap.get(id));
            }
        }

        if (!onlyListInitiatlized) {
            IFortranAST ast = vpg.acquirePermanentAST(fileInEditor);
            if(ast == null) return;
    
            TokenVisitor visitor = new TokenVisitor();
            ast.accept(visitor);
            vpg.releaseAST(fileInEditor);
            onlyListInitiatlized = true;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.photran.internal.core.refactoring.infrastructure.AbstractFortranRefactoring#doCreateChange(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        IFortranAST ast = vpg.acquirePermanentAST(this.fileInEditor);
        if(ast == null) return;
        
        pm.subTask(Messages.AddOnlyToUseStmtRefactoring_InsertingUseStmt);
        createAndInsertUseStmt(ast);
  
        pm.subTask(Messages.AddOnlyToUseStmtRefactoring_CreatingChangeObject);
        addChangeFromModifiedAST(this.fileInEditor, pm);

        vpg.releaseAST(this.fileInEditor);

        pm.done();
    }

    /* (non-Javadoc)
     * @see org.eclipse.photran.internal.core.refactoring.infrastructure.AbstractFortranRefactoring#doCheckFinalConditions(org.eclipse.ltk.core.refactoring.RefactoringStatus, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        pm.beginTask(Messages.AddOnlyToUseStmtRefactoring_Analyzing, IProgressMonitor.UNKNOWN);

        if(useNode == null)
            fail(Messages.AddOnlyToUseStmtRefactoring_ModuleNameInUseStmtNotSelected);

        pm.subTask(Messages.AddOnlyToUseStmtRefactoring_Parsing + fileInEditor.getName());
        IFortranAST ast = vpg.acquirePermanentAST(fileInEditor);
        if(ast == null) return;

        pm.subTask(Messages.AddOnlyToUseStmtRefactoring_CheckingForConflicts);
        checkConflictingBindings(ast, pm, status); //find conflicts
        
        vpg.releaseAST(fileInEditor);

        pm.done();
    }
    
    /*
     * This method assumes that any existing only list is OK. Only checks for conflicting
     * bindings with NEW additions to only list.
     */
    private void checkConflictingBindings(IFortranAST ast, IProgressMonitor pm, RefactoringStatus status)
    {
        pm.subTask(Messages.AddOnlyToUseStmtRefactoring_FindingReferences);

        // find names that are the same as entities in the module that will be added to the "only" list
        // find references to those names, and make sure those references don't change        
        // find references to the current file and make sure that referenced names don't change
        
        for(String entityToAdd : entitiesToAdd) {
            
            final Set<PhotranTokenRef> newRefs = findModuleEntityRefs(ast, Arrays.asList(entityToAdd));
            allReferences = newRefs;

            Definition def = moduleEntDefMap.get(entityToAdd);
            
            checkForConflictingBindings(pm,
                new ConflictingBindingErrorHandler(status),
                def,
                (ScopingNode)ast.getRoot().getProgramUnitList().get(0),
                allReferences,
                def.getCanonicalizedName());
        }
    }
    
    private Set<PhotranTokenRef> findModuleEntityRefs(IFortranAST ast, final Collection<String> defNames) 
    {
        final Set<PhotranTokenRef> result = new HashSet<PhotranTokenRef>();
        
        ast.accept(new GenericASTVisitor() {
            @Override
            public void visitToken(Token token) {
                if (token.getTerminal() == Terminal.T_IDENT) {
                    for (Definition def : token.resolveBinding()) {
                        if (defNames.contains(def.getCanonicalizedName())) {
                            result.add(token.getTokenRef());
                        }
                    }
                }
            }
        });
        
        return result;
    }

    //remove module refs outside project and in the original module definition
    // NOTE: not used for now, filtering conflicts working for now.
    @SuppressWarnings("unused")
    private void removeOriginalModuleRefs()
    {
        if(allReferences != null && allReferences.size() > 0)
        {
            HashSet<PhotranTokenRef> referencesToRemove = new HashSet<PhotranTokenRef>();

            for(PhotranTokenRef ref : allReferences)
            {
                IFile file = ref.getFile();
                IProject project = file.getProject();
                if(!projectInEditor.equals(project))   //ref is in a file not in the project
                    referencesToRemove.add(ref);
                //else if(filesContainingModule.contains(file))  //ref is the module file itself
                //    referencesToRemove.add(ref);
            }

            allReferences.removeAll(referencesToRemove);
        }
    }

    private void createAndInsertUseStmt(IFortranAST ast)
    {
    	// find the use node in the ast
        Token useToken = ast.findTokenByStreamOffsetLength(
        		useNode.getUseToken().getFileOffset(), useNode.getUseToken().getLength());
        ASTUseStmtNode useToModify = (ASTUseStmtNode)useToken.getParent();
        Collection<String> varNames = new TreeSet<String>(entitiesToAdd); // JO -- Sort names

        //create the new only selection
        String newOnlyAdditions = " "; //$NON-NLS-1$
        Iterator<String> iter = varNames.iterator();
        int counter = 0;

        while(iter.hasNext())
        {
            newOnlyAdditions += iter.next();
            if(counter < varNames.size()-1)
                newOnlyAdditions += ", "; //$NON-NLS-1$
            counter++;
        }

        //construct the new USE node and replace the old one in the ast
        ASTUseStmtNode newStmtNode;
        if(entitiesToAdd.size() > 0)// && entitiesToAdd.size() < moduleEntities.size())
            newStmtNode = (ASTUseStmtNode)parseLiteralStatement(useToken.getWhiteBefore() + "use " + //$NON-NLS-1$
                useToModify.getName().getText()+", only:" + newOnlyAdditions //$NON-NLS-1$
                + System.getProperty("line.separator")); //$NON-NLS-1$
        else
            newStmtNode = (ASTUseStmtNode)parseLiteralStatement(useToken.getWhiteBefore() + "use " + //$NON-NLS-1$
                useToModify.getName().getText() + System.getProperty("line.separator")); //$NON-NLS-1$

        useToModify.replaceWith(newStmtNode);
    }

    /* (non-Javadoc)
     * @see org.eclipse.ltk.core.refactoring.Refactoring#getName()
     */
    @Override
    public String getName()
    {
        return Messages.AddOnlyToUseStmtRefactoring_Name;
    }


    private final class DeclarationVisitor extends GenericASTVisitor
    {
        @Override public void visitASTEntityDeclNode(ASTEntityDeclNode node)
        {
            String name = node.getObjectName().getObjectName().getText();
            if(!entitiesInProgram.contains(name))
            {
                entitiesInProgram.add(name);
            }
        }
    }

    private final class TokenVisitor extends GenericASTVisitor
    {
        @Override public void visitToken(Token node)
        {
            String name = node.getText();
            if(moduleEntDefMap.containsKey(name))
                addToOnlyList(name);
        }
    }

    //borrowed (slightly modified) from RenameRefactoring.java
    private final class ConflictingBindingErrorHandler implements IConflictingBindingCallback
    {
        private final RefactoringStatus status;

        private ConflictingBindingErrorHandler(RefactoringStatus status) { this.status = status; }

        public void addConflictError(List<Conflict> conflictingDef)
        {
            PhotranVPG vpg = getVPG(); // to prevent Bug https://bugs.eclipse.org/404170
            for(Conflict conflict : conflictingDef)
            {
                //remove conflicts with the module itself
                IFile file = conflict.tokenRef.getFile();
                if(!filesContainingModule.contains(file) && file.getProject().equals(projectInEditor))
                {
                    Definition def = vpg.getDefinitionFor(conflict.tokenRef);
                    Token token = def.getTokenRef().findToken();
                    String msg =
                        Messages.bind(
                            Messages.AddOnlyToUseStmtRefactoring_NameConflicts,
                            new Object[] {
                                          conflict.name,
                                          def.getCanonicalizedName(),
                                          def.getClassification(),
                                          token.getLine(),
                                          conflict.tokenRef.getFilename()
                            });
                    RefactoringStatusContext context = createContext(conflict.tokenRef); // Highlights problematic definition
                    status.addError(msg, context);
                }
            }
        }

        public void addConflictWarning(List<Conflict> conflictingDef)
        {
            for(Conflict conflict : conflictingDef)
            {
                //remove conflicts with the module itself
                IFile file = conflict.tokenRef.getFile();
                if(!filesContainingModule.contains(file) && file.getProject().equals(projectInEditor))
                {
                    String msg =
                        Messages.bind(
                            Messages.AddOnlyToUseStmtRefactoring_NameMightConflict,
                            conflict.name);
                    RefactoringStatusContext context = createContext(conflict.tokenRef); // Highlights problematic definition
                    status.addWarning(msg, context);
                }
            }
        }

        public void addReferenceWillChangeError(String newName, Token reference)
        {
            //add error for names being added conflicting with declared names in program
            if(entitiesInProgram.contains(newName))
            {
                // The entity with the new name will shadow the definition to which this binding resolves
                String msg = Messages.bind(
                    Messages.AddOnlyToUseStmtRefactoring_AddingWouldChangeMeaningOf,
                    new Object[] { newName,
                                   reference.getText(),
                                   reference.getLine(),
                                   reference.getTokenRef().getFilename() 
                    });
                RefactoringStatusContext context = createContext(reference); // Highlight problematic reference
                status.addError(msg, context);
            }
        }
    }
}
