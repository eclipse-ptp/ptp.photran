/*******************************************************************************
 * Copyright (c) 2009 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *******************************************************************************/
package org.eclipse.rephraserengine.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.MultiRule;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.rephraserengine.core.vpg.eclipse.EclipseVPG;
import org.eclipse.rephraserengine.core.vpg.eclipse.VPGSchedulingRule;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

/**
 * Abstract class for an {@link IWorkbenchWindowActionDelegate} that requires access to a VPG.
 * <p>
 * The user interface will allow this action to be run on any VPG contributed to the <i>vpg</i>
 * extension point.  If there is only one VPG, it will run on that; if multiple VPGs are available,
 * the user will be asked to select one.
 * <p>
 * This class schedules itself to run after it can successfully lock all of the resources in the
 * workspace; this guarantees that only one such action will be accessing the VPG at a time.
 *
 * @author Jeff Overbey
 */
@SuppressWarnings("rawtypes")
public abstract class VPGWindowActionDelegate
           implements IWorkbenchWindowActionDelegate,
                      IRunnableWithProgress
{
    private EclipseVPG vpg = null;

    /** The active workbench window; may be <code>null</code> */
    protected IWorkbenchWindow activeWindow = null;

    /** The active shell; may be <code>null</code> */
    protected Shell activeShell = null;

    public final void init(IWorkbenchWindow window)
    {
        activeWindow = window;
        if (activeWindow != null)
            activeShell = activeWindow.getShell();
    }

    public void dispose() {;}
    public void selectionChanged(IAction action, ISelection selection) {;}

    public final void run(IAction action)
    {
        vpg = PhotranVPG.getInstance();
        scheduleThisUsingVPGSchedulingRule();
    }

    private void scheduleThisUsingVPGSchedulingRule()
    {
        IProgressService context = PlatformUI.getWorkbench().getProgressService();

        ISchedulingRule lockEntireWorkspace = ResourcesPlugin.getWorkspace().getRoot();
        ISchedulingRule vpgSched = VPGSchedulingRule.getInstance();
        ISchedulingRule schedulingRule = MultiRule.combine(lockEntireWorkspace, vpgSched);

        try
        {
            context.runInUI(context, this, schedulingRule);
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
            MessageDialog.openError(
                    activeShell,
                    Messages.VPGWindowActionDelegate_UnhandledExceptionTitle,
                    e.getMessage());
        }
        catch (InterruptedException e)
        {
            // Do nothing
        }
    }

    public final void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException
    {
        try
        {
            run(vpg, progressMonitor);
        }
        catch (Throwable e)
        {
            throw new InvocationTargetException(e);
        }
        finally
        {
            progressMonitor.done();
        }
    }

    /**
     * Subclasses must override this method; this is where the action-specific VPG work is defined.
     *
     * @param vpg an {@link EclipseVPG} contributed to the <i>vpg</i> extension point; if only one
     *            has been contributed, it will be that; otherwise, the user will have been prompted
     *            to select a VPG, and this will be the VPG selected by the user
     * @param progressMonitor an {@link IProgressMonitor} for displaying status information to the
     *            user (if the operation is long-running)
     *
     * @throws Exception
     */
    protected abstract void run(EclipseVPG vpg, IProgressMonitor progressMonitor) throws Exception;

    ///////////////////////////////////////////////////////////////////////////
    // Utility Methods for Subclasses
    ///////////////////////////////////////////////////////////////////////////

    /** @return the active shell, or <code>null</code> if no shell is active */
    protected Shell getShell()
    {
        return activeShell;
    }
}
