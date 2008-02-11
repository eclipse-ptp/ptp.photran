/*******************************************************************************
 * Copyright (c) 2007 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.parser;

import org.eclipse.photran.internal.core.lexer.*;                   import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;

import org.eclipse.photran.internal.core.parser.Parser.*;
import java.util.List;

public class ASTInterfaceBodyNode extends InteriorNode implements IInterfaceSpecification
{
    ASTInterfaceBodyNode(Production production, List<CSTNode> childNodes, List<CSTNode> discardedSymbols)
    {
         super(production);
         
         for (Object o : childNodes)
             addChild((CSTNode)o);
         constructionFinished();
    }
        
    @Override public InteriorNode getASTParent()
    {
        InteriorNode actualParent = super.getParent();
        
        // If a node has been pulled up in an ACST, its physical parent in
        // the CST is not its logical parent in the ACST
        if (actualParent != null && actualParent.childIsPulledUp(actualParent.findChild(this)))
            return actualParent.getParent();
        else 
            return actualParent;
    }
    
    @Override protected void visitThisNodeUsing(ASTVisitor visitor)
    {
        visitor.visitIInterfaceSpecification(this);
        visitor.visitASTInterfaceBodyNode(this);
    }

    public ASTFunctionStmtNode getFunctionStmt()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_939)
            return (ASTFunctionStmtNode)getChild(0);
        else
            return null;
    }

    public ASTSubroutineStmtNode getSubroutineStmt()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_940)
            return (ASTSubroutineStmtNode)getChild(0);
        else
            return null;
    }

    public ASTSubprogramInterfaceBodyNode getSubprogramInterfaceBody()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_939)
            return (ASTSubprogramInterfaceBodyNode)((ASTFunctionInterfaceRangeNode)getChild(1)).getSubprogramInterfaceBody();
        else if (getProduction() == Production.INTERFACE_BODY_940)
            return (ASTSubprogramInterfaceBodyNode)((ASTSubroutineInterfaceRangeNode)getChild(1)).getSubprogramInterfaceBody();
        else
            return null;
    }

    public boolean hasSubprogramInterfaceBody()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_939)
            return ((ASTFunctionInterfaceRangeNode)getChild(1)).hasSubprogramInterfaceBody();
        else if (getProduction() == Production.INTERFACE_BODY_940)
            return ((ASTSubroutineInterfaceRangeNode)getChild(1)).hasSubprogramInterfaceBody();
        else
            return false;
    }

    public ASTEndFunctionStmtNode getEndFunctionStmt()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_939)
            return (ASTEndFunctionStmtNode)((ASTFunctionInterfaceRangeNode)getChild(1)).getEndFunctionStmt();
        else
            return null;
    }

    public ASTEndSubroutineStmtNode getEndSubroutineStmt()
    {
        if (treeHasBeenModified()) throw new IllegalStateException("Accessor methods cannot be called on the nodes of a CST after it has been modified");

        if (getProduction() == Production.INTERFACE_BODY_940)
            return (ASTEndSubroutineStmtNode)((ASTSubroutineInterfaceRangeNode)getChild(1)).getEndSubroutineStmt();
        else
            return null;
    }

    @Override protected boolean childIsPulledUp(int index)
    {
        if (getProduction() == Production.INTERFACE_BODY_939 && index == 1)
            return true;
        else if (getProduction() == Production.INTERFACE_BODY_940 && index == 1)
            return true;
        else
            return false;
    }
}
