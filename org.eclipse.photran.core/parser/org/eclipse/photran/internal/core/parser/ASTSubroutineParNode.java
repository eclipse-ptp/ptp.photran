// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <SubroutinePar> ::= DummyArgName:<DummyArgName>  :production915
 * <SubroutinePar> ::= tasterisk:T_ASTERISK  :production916
 */
public class ASTSubroutineParNode extends ParseTreeNode
{
    public ASTSubroutineParNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public ASTDummyArgNameNode getASTDummyArgName()
    {
        return (ASTDummyArgNameNode)this.getChild("DummyArgName");
    }

    public Token getASTTasterisk()
    {
        return this.getChildToken("tasterisk");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTSubroutineParNode(this); }
}