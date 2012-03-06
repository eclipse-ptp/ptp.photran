/*******************************************************************************
 * Copyright (c) 2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (UIUC) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IHelpResource;
import org.eclipse.photran.internal.ui.editor.FortranEditor;
import org.eclipse.photran.ui.IFortranAPIHelpProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Provides access to the {@value #API_HELP_PROVIDER_EXTENSION_POINT_ID} extension point, which
 * allows third parties to contribute context-sensitive help for Fortran APIs.
 * 
 * @author Jeff Overbey
 */
public final class ContributedAPIDocs
{
    /** Extension point ID for third parties to contribute context-sensitive help for Fortran APIs */
    public static final String API_HELP_PROVIDER_EXTENSION_POINT_ID = "org.eclipse.photran.ui.apiHelpProvider"; //$NON-NLS-1$

    /**
     * @return a list of help resources provided via the
     *         {@value FortranUIHelp#API_HELP_PROVIDER_EXTENSION_POINT_ID} extension point
     */
    public static String getAPIHelpAsHTML(ITextEditor editor, String apiName, String precedingText)
    {
        IHelpResource[] resources = getAPIHelp(editor, apiName, precedingText);
        if (resources.length == 0)
        {
            return null;
        }
        else
        {
            try
            {
                // This forces the help provider to load. Without it, HelpSystem.getHelpContent()
                // below may return null until the user manually activates it (e.g., by clicking
                // Help > Dynamic Help)
                HelpSystem.getContext(FortranEditor.HELP_CONTEXT_ID);

                String href = resources[0].getHref();
                return read(HelpSystem.getHelpContent(href));
            }
            catch (NullPointerException e)
            {
                return ""; //$NON-NLS-1$
            }
        }
    }

    private static String read(InputStream in)
    {
        if (in == null) return ""; //$NON-NLS-1$

        try
        {
            final StringBuilder sb = new StringBuilder();
            for (int ch = in.read(); ch >= 0; ch = in.read())
            {
                sb.append((char)ch);
            }
            in.close();
            return sb.toString();
        }
        catch (IOException e)
        {
            return ""; //$NON-NLS-1$
        }
    }

    /**
     * @return a list of help resources provided via the
     *         {@value FortranUIHelp#API_HELP_PROVIDER_EXTENSION_POINT_ID} extension point
     */
    public static IHelpResource[] getAPIHelp(ITextEditor fortranEditor, String apiName, String precedingText)
    {
        final List<IHelpResource> helpResources = new ArrayList<IHelpResource>();

        for (IConfigurationElement config : Platform.getExtensionRegistry().getConfigurationElementsFor(API_HELP_PROVIDER_EXTENSION_POINT_ID))
        {
            try
            {
                IFortranAPIHelpProvider helpProvider = (IFortranAPIHelpProvider)config.createExecutableExtension("class"); //$NON-NLS-1$
                IHelpResource[] resources = helpProvider.getHelpResources(fortranEditor, apiName, precedingText);
                if (resources != null)
                    for (IHelpResource resource : resources)
                        if (resource != null)
                            helpResources.add(resource);
            }
            catch (CoreException e)
            {
                FortranUIPlugin.log(e);
            }
        }

        return helpResources.toArray(new IHelpResource[helpResources.size()]);
    }

    private ContributedAPIDocs()
    {
        ;
    }
}
