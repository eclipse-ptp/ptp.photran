// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <SubroutineName> ::= tident:T_IDENT  :production947
 */
public class ASTSubroutineNameNode extends ParseTreeNode
{
    public ASTSubroutineNameNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public Token getASTTident()
    {
        return this.getChildToken("tident");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTSubroutineNameNode(this); }
}