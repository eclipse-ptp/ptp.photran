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

import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;
import org.eclipse.photran.internal.core.parser.ASTCommonBlockNode;
import org.eclipse.photran.internal.core.parser.ASTCommonBlockObjectNode;
import org.eclipse.photran.internal.core.parser.ASTCommonStmtNode;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.refactoring.infrastructure.FortranResourceRefactoring;

/**
 * Refactoring that removes all unused variables of common blocks, making it more readable.
 * 
 * @author Federico Tombazzi
 * @author German Aquino
 */
public class RemoveUnusedCommonBlocksRefactoring extends FortranResourceRefactoring
{
    @Override
    protected void doCheckInitialConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        ensureProjectHasRefactoringEnabled(status);
        removeFixedFormFilesFrom(this.selectedFiles, status);
        removeCpreprocessedFilesFrom(this.selectedFiles, status);

        ensureImplicitNoneAndVariablesDeclared(status);
    }

    @Override
    protected void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm)
        throws PreconditionFailure
    {
        try
        {
            for (IFile file : selectedFiles)
            {
                IFortranAST ast = vpg.acquirePermanentAST(file);
                if (ast != null)
                {
                    makeChangesTo(file, ast, status, pm);
                    addChangeFromModifiedAST(file, pm);
                }

                vpg.releaseAST(file);
            }
        }
        finally
        {
            vpg.releaseAllASTs();
        }
    }

    private void makeChangesTo(IFile file, IFortranAST ast, RefactoringStatus status,
        IProgressMonitor pm) throws PreconditionFailure
    {
        for (ASTCommonStmtNode node : ast.getRoot().findAll(ASTCommonStmtNode.class))
        {
            processCommon(node);
        }
    }

    @Override
    protected void doCreateChange(IProgressMonitor pm) throws CoreException,
        OperationCanceledException
    {
        // Change created in #doCheckFinalConditions
    }

    @Override
    public String getName()
    {
        return Messages.RemoveUnusedCommonBlocksRefactoring_Name;
    }

    private void esureAllCommonVariablesAreDeclared(IFile file, IFortranAST ast) throws PreconditionFailure
    {
        for (ASTCommonStmtNode common : ast.getRoot().findAll(ASTCommonStmtNode.class))
        {
            ensureAllVariblesAreDeclaredInCommon(common);
        }
    }

    private void ensureAllVariblesAreDeclaredInCommon(ASTCommonStmtNode node) throws PreconditionFailure
    {
        for (ASTCommonBlockNode commonBlockNode : node.getCommonBlockList()) {
            for (ASTCommonBlockObjectNode commonBlockObject : commonBlockNode.getCommonBlockObjectList())
            {
               List<Definition> definition = commonBlockObject.getVariableName().resolveBinding();

               if (definition.size() == 0)
               {
                   fail(Messages.bind(Messages.RemoveUnusedCommonBlocksRefactoring_NoDeclarationFoundFor, commonBlockObject.getVariableName()));
               }
               else if (definition.size() > 1)
               {
                   fail(Messages.bind(Messages.RemoveUnusedCommonBlocksRefactoring_MultipleDeclarationsFoundFor, commonBlockObject.getVariableName()));
               }
            }
        }
    }

   /**
    * From {@link RemoveUnusedVariablesRefactoring}
    */
   private void ensureAllScopesAreImplicitNone(IFile file, IFortranAST ast)
       throws PreconditionFailure
   {
       for (ScopingNode scope : ast.getRoot().getAllContainedScopes())
           if (!(scope instanceof ASTExecutableProgramNode))
               if (!scope.isImplicitNone())
                   fail(Messages.bind(Messages.RemoveUnusedCommonBlocksRefactoring_SelectedFilesMustBeImplicitNone, file.getName()));
   }

   private void processCommon(ASTCommonStmtNode node) throws PreconditionFailure
   {
       boolean remove;
       int declaredCommons = node.getCommonBlockList().size();

       for (ASTCommonBlockNode commonBlockNode : node.getCommonBlockList()) {
           remove = true;
           for (ASTCommonBlockObjectNode commonBlockObject : commonBlockNode.getCommonBlockObjectList())
           {
              List<Definition> definition = commonBlockObject.getVariableName().resolveBinding();

              if (definition.get(0).findAllReferences(true).size() > 1)
              {
                 remove = false;
                 break;
              }

          }

           if (remove) {
               commonBlockNode.removeFromTree();
               declaredCommons--;
           }
       }

       if (declaredCommons == 0) {
           node.removeFromTree();
       }
   }

   private void ensureImplicitNoneAndVariablesDeclared(RefactoringStatus status) throws PreconditionFailure{
       try
       {
           for (IFile file : selectedFiles)
           {
               IFortranAST ast = vpg.acquirePermanentAST(file);
               ensureAllScopesAreImplicitNone(file, ast);
               esureAllCommonVariablesAreDeclared(file, ast);
               vpg.releaseAST(file);
           }
       }
       finally
       {
           vpg.releaseAllASTs();
       }
   }
}
