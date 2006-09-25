package org.eclipse.photran.core;

import org.eclipse.photran.internal.core.parser.ASTVisitor;
import org.eclipse.photran.internal.core.parser.GenericParseTreeVisitor;
import org.eclipse.photran.internal.core.parser.ParseTreeVisitor;

public interface IFortranAST
{
    ///////////////////////////////////////////////////////////////////////////
    // Visitor Support
    ///////////////////////////////////////////////////////////////////////////

    public void visitTopDownUsing(ASTVisitor visitor);
    public void visitBottomUpUsing(ASTVisitor visitor);
    public void visitOnlyThisNodeUsing(ASTVisitor visitor);
    public void visitUsing(ParseTreeVisitor visitor);
    public void visitUsing(GenericParseTreeVisitor visitor);
}
