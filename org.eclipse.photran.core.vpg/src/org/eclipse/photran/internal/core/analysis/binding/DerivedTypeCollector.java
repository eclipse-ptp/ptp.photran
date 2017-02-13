/*******************************************************************************
 * Copyright (c) 2014 Chris Hansen and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Chris Hansen (U Washington) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.analysis.binding;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.parser.ASTBindingAttrNode;
import org.eclipse.photran.internal.core.parser.ASTProcComponentDefStmtNode;
import org.eclipse.photran.internal.core.parser.ASTProcDeclNode;
import org.eclipse.photran.internal.core.parser.ASTSpecificBindingNode;
import org.eclipse.photran.internal.core.parser.IASTListNode;
import org.eclipse.photran.internal.core.vpg.AnnotationType;
import org.eclipse.photran.internal.core.vpg.PhotranTokenRef;

/**
 * Phase 6 of name-binding analysis.
 * <p>
 * Visits derived type declarations, setting children in the VPG.
 *
 * @author Chris Hansen
 * @see Binder
 */
class DerivedTypeCollector extends BindingCollector
{
    @Override public void visitASTProcComponentDefStmtNode(ASTProcComponentDefStmtNode node)
    {
        super.traverseChildren(node);
        IASTListNode<ASTProcDeclNode> decls = node.getProcDeclList();
        PhotranTokenRef procRef = decls.get(0).getProcedureEntityName().getTokenRef();
        
        Token bindingInterface = node.getProcInterface().getInterfaceName();
        String compText = null;
        //
        ArrayList<Definition> intDefs = vpg.findAllDeclarationsInInterfacesForExternalSubprogram(bindingInterface.getText());
        if (!intDefs.isEmpty()) {
            Definition subDef = intDefs.get(0);
            compText = subDef.getCompletionText();
        }
        //
        if (compText!=null) { 
            int argStart = compText.indexOf('(');
            if (argStart>=0) {
                compText = compText.substring(argStart);
            } else {
                compText = "()"; //$NON-NLS-1$
            }
        } else {
            compText = "()"; //$NON-NLS-1$
        }
        
        Definition def = vpg.getDefinitionFor(procRef);
        compText = def.getDeclaredName() + compText;
        def.setCompletionText(compText.toString());
        vpgProvider.setDefinitionFor(procRef, def);
    }

    @Override public void visitASTSpecificBindingNode(ASTSpecificBindingNode node)
    {
        super.traverseChildren(node);
        
        boolean skipFirst = true;
        PhotranTokenRef procRef = node.getBindingName().getTokenRef();
        IASTListNode<ASTBindingAttrNode> bindAttrs = node.getBindingAttrList();
        if (bindAttrs != null) {
            for (ASTBindingAttrNode bindAttr: bindAttrs) {
                if (bindAttr.isNoPass())
                    skipFirst = false;
            }
        }
        Token bindingInterface = node.getInterfaceName();
        PhotranTokenRef tokenRef = null;
        if (bindingInterface != null) {
            ScopingNode enclosingScope = bindingInterface.getEnclosingScope().getGlobalScope();
            List<PhotranTokenRef> possParents = enclosingScope.manuallyResolve(bindingInterface);
            tokenRef = possParents.get(0);
        } else {
            Token proToken = node.getProcedureName();
            if (proToken == null)
                return;
            ScopingNode enclosingScope = proToken.getEnclosingScope();
            List<PhotranTokenRef> possParents = enclosingScope.manuallyResolve(proToken);
            tokenRef = possParents.get(0);
        }
        Definition subDef = tokenRef.getAnnotation(AnnotationType.DEFINITION_ANNOTATION_TYPE);
        String compText = subDef.getCompletionText();
        if (compText!=null) { 
            int argStart = compText.indexOf('(');
            if (argStart>=0) {
                compText = compText.substring(argStart);
            } else {
                compText = "()"; //$NON-NLS-1$
            }
            if (skipFirst) {
                argStart = compText.indexOf(',');
                if (argStart>=0) {
                    compText = compText.substring(argStart+1);
                } else {
                    compText = ")"; //$NON-NLS-1$
                }
                compText = "(" + compText; //$NON-NLS-1$
            }
        } else {
            compText = "()"; //$NON-NLS-1$
        }
        
        Definition def = vpg.getDefinitionFor(procRef);
        compText = def.getDeclaredName() + compText;
        def.setCompletionText(compText.toString());
        vpgProvider.setDefinitionFor(procRef, def);

    }
}
