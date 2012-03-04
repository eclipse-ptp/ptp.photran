/*******************************************************************************
 * Copyright (c) 2007-2012 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.views.declaration;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.ITokenScanner;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.analysis.binding.Definition;
import org.eclipse.photran.internal.core.lexer.TokenList;
import org.eclipse.photran.internal.core.parser.ASTExecutableProgramNode;
import org.eclipse.photran.internal.core.properties.SearchPathProperties;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.photran.internal.ui.ContributedAPIDocs;
import org.eclipse.photran.internal.ui.editor.FortranEditor;
import org.eclipse.photran.internal.ui.editor.FortranHelpContextProvider;
import org.eclipse.photran.internal.ui.editor.FortranKeywordRuleBasedScanner;
import org.eclipse.photran.internal.ui.editor.FortranStmtPartitionScanner;
import org.eclipse.photran.internal.ui.editor_vpg.DefinitionMap;
import org.eclipse.photran.internal.ui.editor_vpg.FortranEditorTasks;
import org.eclipse.photran.internal.ui.editor_vpg.IFortranEditorASTTask;
import org.eclipse.photran.internal.ui.editor_vpg.IFortranEditorVPGTask;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

/**
 * Implements Photran's Declaration view
 *
 * @author Jeff Overbey - modified to use MVC pattern, SourceViewer, caret listener, DefinitionMap; based on code by...
 * @author John Goode, Abe Hassan, Sean Kim
 *  Group: Fennel-Garlic
 *  University of Illinois at Urbana-Champaign
 *  CS 427 Fall 2007
 */
public class DeclarationView extends ViewPart
    implements ISelectionListener,
               ISelectionChangedListener,
               IFortranEditorVPGTask,
               IFortranEditorASTTask
{
    private static enum ContentType { HTML, FORTRAN_SOURCE };

    private FortranEditor activeEditor = null;
    private HashMap<String, ASTExecutableProgramNode> activeAST = new HashMap<String, ASTExecutableProgramNode>();
    private HashMap<String, TokenList> activeTokenList = new HashMap<String, TokenList>();
    private HashMap<String, DefinitionMap<String>> activeDefinitions = new HashMap<String, DefinitionMap<String>>();

    private Composite composite = null;
    private StackLayout stackLayout = null;

    private SourceViewer viewer = null;
    private Document document = new Document();

    private Browser browser = null;

    private Color LIGHT_YELLOW = new Color(null, new RGB(255, 255, 191));

    /*
     * The content provider class is responsible for
     * providing objects to the view. It can wrap
     * existing objects in adapters or simply return
     * objects as-is. These objects may be sensitive
     * to the current input of the view, or ignore
     * it and always show the same content
     * (like Task List, for example).
     */

    /**
     * This is a callback that will allow us
     * to create the viewer and initialize it.
     */
    @Override public void createPartControl(Composite parent)
    {
        this.composite = new Composite(parent, SWT.NONE);
        this.stackLayout = new StackLayout();
        composite.setLayout(this.stackLayout);

        this.viewer = createFortranSourceViewer(composite);
        this.browser = createBrowser(composite);

        stackLayout.topControl = viewer.getControl();
        composite.layout();

        // Add this view as a selection listener to the workbench page
        getSite().getPage().addSelectionListener(this);

        // Update the selection immediately
        try
        {
            IWorkbenchPage activePage = getSite().getWorkbenchWindow().getActivePage();
            if (activePage != null)
                selectionChanged(activePage.getActivePart(),
                                 activePage.getSelection());
        }
        catch (Throwable e) // NullPointerException, etc.
        {
            ;
        }
    }

    private SourceViewer createFortranSourceViewer(Composite parent)
    {
        final SourceViewer viewer = new SourceViewer(parent, null, SWT.V_SCROLL); //TextViewer(parent, SWT.NONE);
        viewer.configure(new FortranEditor.FortranSourceViewerConfiguration(null)
        {
            @Override protected ITokenScanner getStatementTokenScanner()
            {
                // Copied from FortranEditor#getTokenScanner
                return new FortranKeywordRuleBasedScanner(false, viewer);
            }
        });
        viewer.setDocument(document);
        IDocumentPartitioner partitioner = new FastPartitioner(new FortranStmtPartitionScanner(), FortranStmtPartitionScanner.PARTITION_TYPES);
        partitioner.connect(document);
        document.setDocumentPartitioner(partitioner);

        viewer.getControl().setBackground(LIGHT_YELLOW);
        viewer.setEditable(false);
        viewer.getTextWidget().setFont(JFaceResources.getTextFont());

        return viewer;
    }

    private Browser createBrowser(Composite parent)
    {
        try {
            return new Browser(parent, SWT.NONE);
        } catch (SWTError e) {
            return null;
        }
    }

    /**
     * Update document by displaying the new text
     */
    public void update(ContentType contentType, String str)
    {
        if (str.length() > 0)
            str = trimBlankLines(str);

        switch (contentType)
        {
            case HTML:
                if (browser != null)
                {
                    browser.setText(str);
                    stackLayout.topControl = browser;
                    composite.layout();
                }
                else
                {
                    document.set(""); //$NON-NLS-1$
                }
                break;
                
            case FORTRAN_SOURCE:
                document.set(str);
                viewer.refresh();
                stackLayout.topControl = viewer.getControl();
                composite.layout();
                break;
        }
    }

    private static Pattern blankLine = Pattern.compile("(([ \\t]*[\\r\\n]+)+)[^\\00]*"); //$NON-NLS-1$
    private String trimBlankLines(String str)
    {
        Matcher m = blankLine.matcher(str);
        while (str.length() > 0 && m.matches())
        {
            str = str.substring(m.end(1));
            m = blankLine.matcher(str);
        }
        return str;
    }

    /**
     * Passing the focus request to the viewer's control.
     */
    @Override public void setFocus()
    {
        viewer.getControl().setFocus();
    }

    /**
     * ISelectionListener - Callback notifying the view that a new workbench part has been selected.
     */
    public synchronized void selectionChanged(IWorkbenchPart part, ISelection selection)
    {
        if (part instanceof FortranEditor)
        {
            if (activeEditor != part)
            {
                // Observe new editor
                stopObserving(activeEditor);
                activeEditor = startObserving((FortranEditor)part);
            }
            else
            {
                // Leave everything as-is
            }
        }
        else
        {
            // Observe nothing
            stopObserving(activeEditor);
            activeEditor = null;
        }
    }

    /**
     * Registers this view to receive notifications of caret movement in <code>editor</code>
     *
     * See http://dev.eclipse.org/mhonarc/newsLists/news.eclipse.platform/msg44602.html
     */
    private FortranEditor startObserving(final FortranEditor editor)
    {
        if (editor != null)
        {
            String declViewEnabledProperty = new SearchPathProperties().getProperty(
                editor.getIFile(),
                SearchPathProperties.ENABLE_DECL_VIEW_PROPERTY_NAME);
            if (declViewEnabledProperty != null && declViewEnabledProperty.equals("true")) //$NON-NLS-1$
            {
                addCaretMovementListenerTo(editor);
                FortranEditorTasks tasks = FortranEditorTasks.instance(editor);
                tasks.addASTTask(this);
                tasks.addVPGTask(this);

                ((IPartService)getSite().getService(IPartService.class)).addPartListener(new IPartListener2()
                {
                    public void partActivated(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partBroughtToTop(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partClosed(IWorkbenchPartReference partRef)
                    {
                        if (partRef.getPart(false) == editor)
                        {
                            FortranEditorTasks tasks = FortranEditorTasks.instance(editor);
                            tasks.removeASTTask(DeclarationView.this);
                            tasks.removeVPGTask(DeclarationView.this);

                            IFile ifile = editor.getIFile();
                            if (ifile != null)
                            {
                                String path = ifile.getFullPath().toPortableString();
                                activeAST.remove(path);
                                activeDefinitions.remove(path);
                                activeTokenList.remove(path);
                            }
                        }
                        ((IPartService)getSite().getService(IPartService.class)).removePartListener(this);
                    }

                    public void partDeactivated(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partHidden(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partInputChanged(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partOpened(IWorkbenchPartReference partRef)
                    {
                    }

                    public void partVisible(IWorkbenchPartReference partRef)
                    {
                    }
                });

                tasks.getRunner().runTasks(true);
                return editor;
            }
        }

        return null;
    }

    private void addCaretMovementListenerTo(FortranEditor editor)
    {
        TextViewer sourceViewer = (TextViewer)editor.getSourceViewerx();
        if (sourceViewer != null)
            sourceViewer.addPostSelectionChangedListener(this);
    }

    /**
     * Unregisters this view to receive notifications of caret movement in <code>editor</code>
     */
    private void stopObserving(FortranEditor editor)
    {
        update(ContentType.FORTRAN_SOURCE, ""); //$NON-NLS-1$
        if (editor != null)
            removeCaretMovementListenerFrom(editor);
    }

    private void removeCaretMovementListenerFrom(FortranEditor editor)
    {
        TextViewer sourceViewer = (TextViewer)editor.getSourceViewerx();
        if (sourceViewer != null)
            sourceViewer.removePostSelectionChangedListener(this);
    }

    /**
     * IFortranEditorVPGTask - Callback run when the VPG is more-or-less up-to-date.
     * This method is run <i>outside</i> the UI thread.
     */
    public void handle(IFile file, IFortranAST ast, DefinitionMap<Definition> defMap)
    {
        if (defMap == null) return;

        long start = System.currentTimeMillis();

        DefinitionMap<String> newDefMap = new DefinitionMap<String>(defMap)
        {
            @Override protected String map(String qualifiedName, Definition def)
            {
                return def.describe();
            }
        };

        synchronized (this)
        {
            activeDefinitions.put(file.getFullPath().toPortableString(), newDefMap);
        }

        PhotranVPG.getInstance().debug("        Decl view IEditorVPGTask handler:\t" + (System.currentTimeMillis()-start) + " ms", PhotranVPG.getFilenameForIFile(file)); //$NON-NLS-1$ //$NON-NLS-2$
    }

    /**
     * IFortranEditorASTTask - Callback run when a fresh AST for the file in the editor
     * is available.  May be newer than the information available in the VPG.
     * This method is run <i>outside</i> the UI thread.
     */
    public synchronized boolean handle(ASTExecutableProgramNode ast, TokenList tokenList, DefinitionMap<Definition> defMap)
    {
        if (activeEditor != null)
        {
            long start = System.currentTimeMillis();
            
            String path = activeEditor.getIFile().getFullPath().toPortableString();
            activeAST.put(path, ast);
            activeTokenList.put(path, tokenList);
            
            PhotranVPG.getInstance().debug("        Decl view IEditorASTTask handler:\t" + (System.currentTimeMillis()-start) + " ms", null); //$NON-NLS-1$ //$NON-NLS-2$
        }
        return true;
    }

    /**
     * ISelectionChangedListener - Callback notifying the view that the editor's caret has moved
     */
    public synchronized void selectionChanged(SelectionChangedEvent event)
    {
        if (activeEditor == null) return;

        if (event.getSelection() instanceof TextSelection)
        {
            String contributedHelp = ContributedAPIDocs.getAPIHelpAsHTML(FortranHelpContextProvider.getSelectedString(activeEditor));
            if (contributedHelp != null)
            {
                // Use third-party HTML documentation if it is present
                update(ContentType.HTML, contributedHelp);
            }
            else
            {
                // Otherwise, extract the declaration from the Fortran source code, if possible
                String description = getDeclarationTextFromDefMap((TextSelection)event.getSelection());
                update(ContentType.FORTRAN_SOURCE, description);
            }
        }
    }

    private String getDeclarationTextFromDefMap(TextSelection selection)
    {
        String path = activeEditor.getIFile().getFullPath().toPortableString();
        TokenList tokenList = activeTokenList.get(path);
        DefinitionMap<String> defMap = activeDefinitions.get(path);
        String description;
        if (tokenList != null && defMap != null)
        {
            description = defMap.lookup(selection, tokenList);
        }
        else
        {
            description = ""; //$NON-NLS-1$
        }
        return description == null ? "" : description; //$NON-NLS-1$
    }
}