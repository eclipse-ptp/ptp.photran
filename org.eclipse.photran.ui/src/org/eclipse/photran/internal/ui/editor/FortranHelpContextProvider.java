/**********************************************************************
 * Copyright (c) 2004, 2008, 2012 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: 
 *     Intel Corporation - Initial API and implementation
 *     Anton Leherbauer (Wind River Systems)
 *     Jeff Overbey (UIUC) - Modifications for Photran
 **********************************************************************/

package org.eclipse.photran.internal.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.help.IHelpResource;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.Region;
import org.eclipse.photran.internal.ui.ContributedAPIDocs;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Fortran help context provider.
 * <p>
 * Based on org.eclipse.cdt.internal.ui.util.CUIHelp
 * 
 * @author Unknown
 * @author Jeff Overbey - Fortran modifications
 */
public class FortranHelpContextProvider implements IContextProvider
{
    /** The Fortran editor for which context-sensitive help is being provided */
    private final ITextEditor fEditor;

    /**
     * Creates a context provider for the given text editor.
     * @param editor
     */
    public FortranHelpContextProvider(ITextEditor editor)
    {
        fEditor = editor;
    }

    /*
     * @see org.eclipse.help.IContextProvider#getContext(java.lang.Object)
     */
    public IContext getContext(Object target)
    {
        String selected = getSelectedString(fEditor);
        String preceding = getPrecedingString(fEditor);
        // IContext context= HelpSystem.getContext(ICHelpContextIds.CEDITOR_VIEW);
        IContext context = HelpSystem.getContext(FortranEditor.HELP_CONTEXT_ID);
        if (context != null && selected != null && selected.length() > 0)
        {
            try
            {
                context = new FortranHelpDisplayContext(context, fEditor, selected, preceding);
            }
            catch (CoreException exc)
            {
            }
        }
        return context;
    }

    /*
     * @see org.eclipse.help.IContextProvider#getContextChangeMask()
     */
    public int getContextChangeMask()
    {
        return SELECTION;
    }

    /*
     * @see org.eclipse.help.IContextProvider#getSearchExpression(java.lang.Object)
     */
    public String getSearchExpression(Object target)
    {
        return getSelectedString(fEditor);
    }

    public static String getSelectedString(ITextEditor editor)
    {
        String expression = null;
        try
        {
            ITextSelection selection = (ITextSelection)editor.getSite().getSelectionProvider().getSelection();
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            IRegion region = findWord(document, selection.getOffset());
            if (region != null)
                expression = document.get(region.getOffset(), region.getLength());
        }
        catch (Exception e)
        {
        }
        return expression;
    }

    public static String getPrecedingString(ITextEditor editor)
    {
        String expression = ""; //$NON-NLS-1$
        try
        {
            ITextSelection selection = (ITextSelection)editor.getSite().getSelectionProvider().getSelection();
            IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
            IRegion region = findWord(document, selection.getOffset());
            if (region != null)
            {
                int end = region.getOffset();
                IRegion partition = document.getPartition(end);
                int start = partition.getOffset();
                expression = document.get(start, end-start);
            }
        }
        catch (Exception e)
        {
        }
        return expression;
    }

    public static final class FortranHelpDisplayContext implements IContext
    {
        private final IHelpResource[] helpResources;

        private final String text;

        public FortranHelpDisplayContext(IContext context, ITextEditor editor, String selected, String precedingText)
            throws CoreException
        {
            this.text = context.getText();
            this.helpResources = getHelpResources(context, editor, selected, precedingText);
        }

        private IHelpResource[] getHelpResources(IContext context, ITextEditor editor, String selected, String precedingText)
        {
            List<IHelpResource> helpResources = new ArrayList<IHelpResource>();

            IHelpResource[] apiResources = getAPIHelp(editor, selected, precedingText);
            if (apiResources != null) helpResources.addAll(Arrays.asList(apiResources));

            IHelpResource[] relatedResources = context.getRelatedTopics();
            if (relatedResources != null) helpResources.addAll(Arrays.asList(relatedResources));

            return helpResources.toArray(new IHelpResource[helpResources.size()]);
        }

        /** @return a list of help resources provided via the {@value ContributedAPIDocs#API_HELP_PROVIDER_EXTENSION_POINT_ID} extension point */
        private IHelpResource[] getAPIHelp(ITextEditor fortranEditor, String apiName, String precedingText)
        {
            return ContributedAPIDocs.getAPIHelp(fortranEditor, apiName, precedingText);
        }

        public IHelpResource[] getRelatedTopics()
        {
            return helpResources;
        }

        public String getText()
        {
            return text;
        }
    }

    // from CWordFinder#findWord
    public static IRegion findWord(IDocument document, int offset)
    {
        int start = -2;
        int end = -1;

        try
        {
            int pos = offset;
            char c;

            while (--pos >= 0)
            {
                c = document.getChar(pos);
                if (!Character.isJavaIdentifierPart(c))
                {
                    break;
                }
            }

            start = pos;

            pos = offset;
            int length = document.getLength();

            while (pos < length)
            {
                c = document.getChar(pos);
                if (!Character.isJavaIdentifierPart(c)) break;
                ++pos;
            }

            end = pos;
        }
        catch (BadLocationException x)
        {
        }

        if (start >= -1 && end > -1)
        {
            if (start == offset && end == offset)
                return new Region(offset, 0);
            else if (start == offset)
                return new Region(start, end - start);
            else
                return new Region(start + 1, end - start - 1);
        }

        return null;
    }
}
