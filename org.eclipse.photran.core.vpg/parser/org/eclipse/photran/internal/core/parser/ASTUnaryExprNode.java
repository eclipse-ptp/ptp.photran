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
public class ASTUnaryExprNode extends ASTNode implements IExpr, ISelector
{
    ASTOperatorNode operator; // in ASTUnaryExprNode
    ASTSignNode sign; // in ASTUnaryExprNode
    IExpr operand; // in ASTUnaryExprNode

    public ASTOperatorNode getOperator()
    {
        return this.operator;
    }

    public void setOperator(ASTOperatorNode newValue)
    {
        this.operator = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public ASTSignNode getSign()
    {
        return this.sign;
    }

    public void setSign(ASTSignNode newValue)
    {
        this.sign = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public IExpr getOperand()
    {
        return this.operand;
    }

    public void setOperand(IExpr newValue)
    {
        this.operand = newValue;
        if (newValue != null) newValue.setParent(this);
    }


    public void accept(IASTVisitor visitor)
    {
        visitor.visitASTUnaryExprNode(this);
        visitor.visitIExpr(this);
        visitor.visitISelector(this);
        visitor.visitASTNode(this);
    }

    @Override protected int getNumASTFields()
    {
        return 3;
    }

    @Override protected IASTNode getASTField(int index)
    {
        switch (index)
        {
        case 0:  return this.operator;
        case 1:  return this.sign;
        case 2:  return this.operand;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }

    @Override protected void setASTField(int index, IASTNode value)
    {
        switch (index)
        {
        case 0:  this.operator = (ASTOperatorNode)value; if (value != null) value.setParent(this); return;
        case 1:  this.sign = (ASTSignNode)value; if (value != null) value.setParent(this); return;
        case 2:  this.operand = (IExpr)value; if (value != null) value.setParent(this); return;
        default: throw new IllegalArgumentException("Invalid index");
        }
    }
}

