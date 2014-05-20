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
public class ASTLoopControlNode extends ASTNode
{
    org.eclipse.photran.internal.core.lexer.Token variableName; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTEquals; // in ASTLoopControlNode
    IExpr lb; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTConcurrent; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTWhile; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTLparen; // in ASTLoopControlNode
    IASTListNode<ASTForallTripletSpecListNode> forallTripletSpecList; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTComma; // in ASTLoopControlNode
    IExpr ub; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTComma2; // in ASTLoopControlNode
    IExpr step; // in ASTLoopControlNode
    IExpr whileExpr; // in ASTLoopControlNode
    ASTScalarMaskExprNode scalarMaskExpr; // in ASTLoopControlNode
    org.eclipse.photran.internal.core.lexer.Token hiddenTRparen; // in ASTLoopControlNode

    public org.eclipse.photran.internal.core.lexer.Token getVariableName()
    {
        return this.variableName;
    }

    public void setVariableName(org.eclipse.photran.internal.core.lexer.Token newValue)
    {
        this.variableName = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IExpr getLb()
    {
        return this.lb;
    }

    public void setLb(IExpr newValue)
    {
        this.lb = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IASTListNode<ASTForallTripletSpecListNode> getForallTripletSpecList()
    {
        return this.forallTripletSpecList;
    }

    public void setForallTripletSpecList(IASTListNode<ASTForallTripletSpecListNode> newValue)
    {
        this.forallTripletSpecList = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IExpr getUb()
    {
        return this.ub;
    }

    public void setUb(IExpr newValue)
    {
        this.ub = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IExpr getStep()
    {
        return this.step;
    }

    public void setStep(IExpr newValue)
    {
        this.step = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IExpr getWhileExpr()
    {
        return this.whileExpr;
    }

    public void setWhileExpr(IExpr newValue)
    {
        this.whileExpr = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTScalarMaskExprNode getScalarMaskExpr()
    {
        return this.scalarMaskExpr;
    }

    public void setScalarMaskExpr(ASTScalarMaskExprNode newValue)
    {
        this.scalarMaskExpr = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    @Override
    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTLoopControlNode(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 14;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.variableName;
        case 1:  return this.hiddenTEquals;
        case 2:  return this.lb;
        case 3:  return this.hiddenTConcurrent;
        case 4:  return this.hiddenTWhile;
        case 5:  return this.hiddenTLparen;
        case 6:  return this.forallTripletSpecList;
        case 7:  return this.hiddenTComma;
        case 8:  return this.ub;
        case 9:  return this.hiddenTComma2;
        case 10: return this.step;
        case 11: return this.whileExpr;
        case 12: return this.scalarMaskExpr;
        case 13: return this.hiddenTRparen;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.variableName = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 1:  this.hiddenTEquals = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 2:  this.lb = (IExpr)value; if (value != null) value.setParent(this); return;
        case 3:  this.hiddenTConcurrent = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 4:  this.hiddenTWhile = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 5:  this.hiddenTLparen = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 6:  this.forallTripletSpecList = (IASTListNode<ASTForallTripletSpecListNode>)value; if (value != null) value.setParent(this); return;
        case 7:  this.hiddenTComma = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 8:  this.ub = (IExpr)value; if (value != null) value.setParent(this); return;
        case 9:  this.hiddenTComma2 = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        case 10: this.step = (IExpr)value; if (value != null) value.setParent(this); return;
        case 11: this.whileExpr = (IExpr)value; if (value != null) value.setParent(this); return;
        case 12: this.scalarMaskExpr = (ASTScalarMaskExprNode)value; if (value != null) value.setParent(this); return;
        case 13: this.hiddenTRparen = (org.eclipse.photran.internal.core.lexer.Token)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

