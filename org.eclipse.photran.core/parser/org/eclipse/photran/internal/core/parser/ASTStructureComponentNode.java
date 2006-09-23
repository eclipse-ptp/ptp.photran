// Generated by Ludwig version 1.0 alpha 6

package org.eclipse.photran.internal.core.parser; import org.eclipse.photran.internal.core.lexer.*;


/**
 * <StructureComponent> ::= VariableName:<VariableName> FieldSelector:<FieldSelector>  :production413
 * <StructureComponent> ::= StructureComponent:<StructureComponent> FieldSelector:<FieldSelector>  :production414
 */
public class ASTStructureComponentNode extends ParseTreeNode
{
    public ASTStructureComponentNode(Nonterminal nonterminal, Production production)
    {
        super(nonterminal, production);
    }

    public ASTVariableNameNode getASTVariableName()
    {
        return (ASTVariableNameNode)this.getChild("VariableName");
    }

    public ASTFieldSelectorNode getASTFieldSelector()
    {
        return (ASTFieldSelectorNode)this.getChild("FieldSelector");
    }

    public ASTStructureComponentNode getASTStructureComponent()
    {
        return (ASTStructureComponentNode)this.getChild("StructureComponent");
    }

    protected void visitThisNodeUsing(ASTVisitor visitor) { visitor.visitASTStructureComponentNode(this); }
}