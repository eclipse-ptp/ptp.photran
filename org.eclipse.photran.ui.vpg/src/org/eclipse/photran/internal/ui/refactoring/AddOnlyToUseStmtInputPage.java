/*******************************************************************************
 * Copyright (c) 2009 University of Illinois at Urbana-Champaign and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    UIUC - Initial API and implementation
 *    Louis Orenstein (Tech-X Corporation) - fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=399314
 *******************************************************************************/
package org.eclipse.photran.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.photran.internal.core.refactoring.AddOnlyToUseStmtRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 *
 * @author Kurt Hendle
 * @author Louis Orenstein (Tech-X Corporation) - fix for bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=399314
 */
public class AddOnlyToUseStmtInputPage extends CustomUserInputPage<AddOnlyToUseStmtRefactoring>
{
    protected List<Button> checkList;

    @Override
    public IWizardPage getNextPage()
    {
        for(Button checkBox : checkList) {
            boolean isChecked = checkBox.getSelection();
            final String buttonText = checkBox.getText();
            if (isChecked) {
                getRefactoring().addToOnlyList(buttonText);
            } else {
                getRefactoring().removeFromOnlyList(buttonText);
            }
        }
        
        return super.getNextPage();
    }

    @Override public void createControl(Composite parent)
    {
        getRefactoring().readExistingOnlyList();
        List<String> entities = getRefactoring().getModuleEntityList();
        Set<String> newOnlyList = getRefactoring().getNewOnlyList();
        checkList = new ArrayList<Button>();

        // using a ScrolledComposite so the list is scrollable
        ScrolledComposite container = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        initializeDialogUnits(container);
        setControl(container);
        
        Composite top = new Composite(container, SWT.NONE);
        top.setLayout(new GridLayout(1, false));
        Composite group = top;

        Label lbl = new Label(group, SWT.NONE);
        lbl.setText(Messages.AddOnlyToUseStmtInputPage_SelectModuleEntitiesLabel);

        for(String entityName : entities) 
        {
            Button check = new Button(group, SWT.CHECK);
            check.setText(entityName);

            if(newOnlyList.contains(entityName)){
                check.setSelection(true);
            }

            checkList.add(check);
        }

        Label instruct = new Label(top, SWT.NONE);
        instruct.setText(Messages.AddOnlyToUseStmtInputPage_ClickOKMessage);
        
        container.setExpandHorizontal(true);
        container.setExpandVertical(true);
        container.setContent(top);
        container.setMinSize(top.computeSize(SWT.DEFAULT, SWT.DEFAULT));
    }
}
