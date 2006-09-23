// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <FmtSpec> ::= Formatedit:<Formatedit>  :production794
 * <FmtSpec> ::= Formatsep:<Formatsep>  :production795
 * <FmtSpec> ::= Formatsep:<Formatsep> Formatedit:<Formatedit>  :production796
 * <FmtSpec> ::= FmtSpec:<FmtSpec> Formatsep:<Formatsep>  :production797
 * <FmtSpec> ::= FmtSpec:<FmtSpec> Formatsep:<Formatsep> Formatedit:<Formatedit>  :production798
 * <FmtSpec> ::= FmtSpec:<FmtSpec> tcomma:T_COMMA Formatedit:<Formatedit>  :production799
 * <FmtSpec> ::= FmtSpec:<FmtSpec> tcomma:T_COMMA Formatsep:<Formatsep>  :production800
 * <FmtSpec> ::= FmtSpec:<FmtSpec> tcomma:T_COMMA Formatsep:<Formatsep> Formatedit:<Formatedit>  :production801
 */
public class ASTFmtSpecNode extends ParseTreeNode
{
    public ASTFmtSpecNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public ASTFormateditNode getASTFormatedit()
    {
        return (ASTFormateditNode)this.getChild("Formatedit");
    }

    public ASTFormatsepNode getASTFormatsep()
    {
        return (ASTFormatsepNode)this.getChild("Formatsep");
    }

    public ASTFmtSpecNode getASTFmtSpec()
    {
        return (ASTFmtSpecNode)this.getChild("FmtSpec");
    }

    public Token getASTTcomma()
    {
        return this.getChildToken("tcomma");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTFmtSpecNode(this); }
}