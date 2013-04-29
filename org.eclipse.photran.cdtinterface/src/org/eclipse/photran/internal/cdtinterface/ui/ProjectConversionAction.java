/*******************************************************************************
 * Copyright (c) 2009, 2011 Eclipse Engineering LLC and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Eclipse Engineering LLC (Matt Scarpino) - Initial API and implementation
 *   University of Illinois (Jeff Overbey) - Updated for Projects View
 *   Louis Orenstein (Tech-X Corporation) - fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=379854
 *******************************************************************************/
package org.eclipse.photran.internal.cdtinterface.ui;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.photran.internal.cdtinterface.CDTInterfacePlugin;
import org.eclipse.photran.internal.core.FProjectNature;
import org.eclipse.photran.internal.core.FortranCorePlugin;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.navigator.CommonNavigator;

/**
 * Implements the "Convert to Fortran Project" action, which can be invoked by right-clicking on a
 * C/C++ Project in the Fortran Projects view.
 * 
 * @author Matt Scarpino
 * @author Jeff Overbey
 * @author Louis Orenstein (Tech-X Corporation) - fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=379854
 */
public class ProjectConversionAction implements IViewActionDelegate
{
    private IViewPart view = null;

    private Set<IProject> projects = new HashSet<IProject>();

    /**
     * Callback invoked to initialize this action.
     */
    public void init(IViewPart v)
    {
        view = v;
    }

    /**
     * Callback invoked when the workbench selection changes.
     * <p>
     * Determines if any of the selected resources are C/C++ projects, and, if so, adds them to
     * {@link #projects}.
     */
    public void selectionChanged(IAction action, ISelection selection)
    {
        projects.clear();
        if (selection instanceof IStructuredSelection)
        {
            IStructuredSelection structuredSelection = (IStructuredSelection)selection;
            Iterator e = structuredSelection.iterator();
            while (e.hasNext())
            {
                Object element = e.next();
                if (element instanceof IProject)
                {
                    try
                    {
                        IProject project = (IProject)element;
                        if (project.isOpen() && project.hasNature(CProjectNature.C_NATURE_ID))
                        {
                            projects.add(project);
                        }
                    }
                    catch (CoreException ex)
                    {
                        FortranCorePlugin.log(ex);
                    }
                }
                else if (element instanceof ICProject)
                {
                    projects.add(((ICProject)element).getProject());
                }
            }
        }
    }

    /**
     * Callback invoked to run this action.
     * <p>
     * Adds the Fortran nature to selected C/C++ projects, and refreshes the view to display the new
     * nature image (i.e., to make sure the project is displayed with an &quot;F&quot; icon).
     */
    public void run(IAction action)
    {
        if (!projects.isEmpty())
        {
            addFortranNatureToSelectedProjects();
            refreshView();
            projects.clear();
        }
    }

    private void addFortranNatureToSelectedProjects()
    {
        for (IProject project : projects)
        {
            addFortranNatureTo(project);
        }
    }

    private void addFortranNatureTo(IProject project)
    {
        try
        {
            if (!project.hasNature(FProjectNature.F_NATURE_ID))
            {
                IProjectDescription description = project.getDescription();
                String[] natures = description.getNatureIds();
                String[] newNatures = new String[natures.length + 1];
                System.arraycopy(natures, 0, newNatures, 1, natures.length);
                newNatures[0] = FProjectNature.F_NATURE_ID;
                description.setNatureIds(newNatures);
                project.setDescription(description, null);
            }
        }
        catch (CoreException e)
        {
            CDTInterfacePlugin.log(e);
            e.printStackTrace();
        }
    }

    @SuppressWarnings("restriction")
    private void refreshView()
    {
        if (view instanceof FortranView)
        {
            ((FortranView)view).getViewer().refresh();
        }
        else if (view instanceof CommonNavigator)
        {
            ((CommonNavigator)view).getCommonViewer().refresh();
        }
    }
}
