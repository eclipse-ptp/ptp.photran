// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <SubroutineArgList> ::= empty :production878
 * <SubroutineArgList> ::= SubroutineArg:<SubroutineArg>  :production879
 * <SubroutineArgList> ::= @:<SubroutineArgList> tcomma:T_COMMA SubroutineArg:<SubroutineArg>  :production880
 */
public class ASTSubroutineArgListNode extends ParseTreeNode
{
    public ASTSubroutineArgListNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public int count()
    {
        ParseTreeNode node = this;
        int count = 1;
        while (node.getChild("@") != null)
        {
            count++;
            node = node.getChild("@");
        }
        return count;
    }

    public ASTSubroutineArgNode getASTSubroutineArg(int index)
    {
        ASTSubroutineArgListNode node = this;
        for (int i = 0; i < index; i++)
            node = (ASTSubroutineArgListNode)node.getChild("@");
        return (ASTSubroutineArgNode)node.getChild("SubroutineArg");
    }

    public Token getASTTcomma(int index)
    {
        ASTSubroutineArgListNode node = this;
        for (int i = 0; i < index; i++)
            node = (ASTSubroutineArgListNode)node.getChild("@");
        return node.getChildToken("tcomma");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTSubroutineArgListNode(this); }
}