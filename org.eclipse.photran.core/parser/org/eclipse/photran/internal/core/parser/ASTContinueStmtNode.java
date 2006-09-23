// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <ContinueStmt> ::= LblDef:<LblDef> tcontinue:T_CONTINUE teos:T_EOS  :production660
 */
public class ASTContinueStmtNode extends ParseTreeNode
{
    public ASTContinueStmtNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public ASTLblDefNode getASTLblDef()
    {
        return (ASTLblDefNode)this.getChild("LblDef");
    }

    public Token getASTTcontinue()
    {
        return this.getChildToken("tcontinue");
    }

    public Token getASTTeos()
    {
        return this.getChildToken("teos");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTContinueStmtNode(this); }
}