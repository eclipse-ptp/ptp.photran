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

import java.io.PrintStream;
import java.util.Iterator;

import java.util.List;

import org.eclipse.photran.internal.core.parser.ASTListNode;
import org.eclipse.photran.internal.core.parser.ASTNode;
import org.eclipse.photran.internal.core.parser.ASTNodeWithErrorRecoverySymbols;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.parser.IASTNode;
import org.eclipse.photran.internal.core.parser.IASTVisitor;
import org.eclipse.photran.internal.core.lexer.Token;

import org.eclipse.photran.internal.core.lexer.*;                   import org.eclipse.photran.internal.core.analysis.binding.ScopingNode;                   import org.eclipse.photran.internal.core.SyntaxException;                   import java.io.IOException;

@SuppressWarnings("all")
public class ASTHPStructureDeclNode extends ASTNode implements IBlockDataBodyConstruct, IBodyConstruct, IDeclarationConstruct, IHPField, IModuleBodyConstruct, ISpecificationPartConstruct
{
    org.eclipse.photran.internal.core.lexer.Token label; // in ASTHPStructureDeclNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTStructure; // in ASTHPStructureDeclNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTSlash; // in ASTHPStructureDeclNode
    org.eclipse.photran.internal.core.lexer.Token name; // in ASTHPStructureDeclNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTSlash2; // in ASTHPStructureDeclNode
    IASTListNode<ASTEntityDeclNode> fieldNamelist; // in ASTHPStructureDeclNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTEos; // in ASTHPStructureDeclNode
    IASTListNode<IHPField> fields; // in ASTHPStructureDeclNode
    ASTHPEndStructureStmtNode hiddenHPEndStructureStmt; // in ASTHPStructureDeclNode

    public org.eclipse.photran.internal.core.lexer.Token getLabel()
    {
        return this.label;
    }

    public void setLabel(org.eclipse.photran.internal.core.lexer.Token newValue)
    {
        this.label = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public org.eclipse.photran.internal.core.lexer.Token getName()
    {
        return this.name;
    }

    public void setName(org.eclipse.photran.internal.core.lexer.Token newValue)
    {
        this.name = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTEntityDeclNode> getFieldNamelist()
    {
        return this.fieldNamelist;
    }

    public void setFieldNamelist(IASTListNode<ASTEntityDeclNode> newValue)
    {
        this.fieldNamelist = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<IHPField> getFields()
    {
        return this.fields;
    }

    public void setFields(IASTListNode<IHPField> newValue)
    {
        this.fields = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTHPStructureDeclNode(this);
        visitor.visitIBlockDataBodyConstruct(this);
        visitor.visitIBodyConstruct(this);
        visitor.visitIDeclarationConstruct(this);
        visitor.visitIHPField(this);
        visitor.visitIModuleBodyConstruct(this);
        visitor.visitISpecificationPartConstruct(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 9;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.label;
        case 1:  return this.hiddenTStructure;
        case 2:  return this.hiddenTSlash;
        case 3:  return this.name;
        case 4:  return this.hiddenTSlash2;
        case 5:  return this.fieldNamelist;
        case 6:  return this.hiddenTEos;
        case 7:  return this.fields;
        case 8:  return this.hiddenHPEndStructureStmt;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.label = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenTStructure = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.hiddenTSlash = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 3:  this.name = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenTSlash2 = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 5:  this.fieldNamelist = (IASTListNode<ASTEntityDeclNode>)value; if (value != null) value.setParent(this); return;
        case 6:  this.hiddenTEos = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 7:  this.fields = (IASTListNode<IHPField>)value; if (value != null) value.setParent(this); return;
        case 8:  this.hiddenHPEndStructureStmt = (ASTHPEndStructureStmtNode)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

