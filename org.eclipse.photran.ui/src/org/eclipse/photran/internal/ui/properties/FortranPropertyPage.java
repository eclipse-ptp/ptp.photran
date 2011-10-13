/*******************************************************************************
 * Copyright (c) 2010 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.properties;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * This is a superclass for Fortran project property pages that need to ask the user
 * to close and re-open any Fortran editors to see their changes take effect.
 * <p>
 * This common superclass ensures that the message dialog will only be shown once,
 * no matter how many property pages are contributed.
 * <p>
 * Subclasses should call {@link #setDirty()} to ensure that the dialog is
 * displayed.
 * 
 * @author Jeff Overbey
 */
public abstract class FortranPropertyPage extends PropertyPage
{
    // static to ensure this is shared among all instances
    private static boolean dialogShown;
    
    private boolean shouldNotifyUser;

    public FortranPropertyPage()
    {
        shouldNotifyUser = false;
        dialogShown = false;
    }
    
    // This really ought to be protected, but that causes IllegalAccessErrors
    // to be thrown by the preferences dialog at runtime, so we'll make it public
    public void setDirty()
    {
        shouldNotifyUser = true;
    }
    
    @Override public final boolean performOk()
    {
        boolean result = doPerformOk();
        
        if (shouldNotifyUser && !dialogShown)
        {
            dialogShown = true;
            MessageDialog.openInformation(getShell(),
                UIMessages.FortranPropertyPage_PreferencesChangedTitle,
                UIMessages.FortranPropertyPage_NeedToCloseAndReOpenEditors);
        }
        
        return result;
    }
    
    protected abstract boolean doPerformOk();

    // Utility Method

    protected IProject getProjectFromElement()
    {
        Object element = getElement();
        IProject proj = null;

        if (element instanceof IProject)
        {
            proj = (IProject)getElement();
        }
        else
        {
            if (element instanceof IAdaptable)
            {
                proj = (IProject)((IAdaptable)element).getAdapter(IProject.class);
            }
        }
        return proj;
    }
}
