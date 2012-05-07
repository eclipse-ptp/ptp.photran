/*******************************************************************************
 * Copyright (c) 2010 Tombazzi Juan, Aquino German, Mariano Mendez and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Tombazzi Juan - Initial API and implementation
 * Aquino German - Initial API and implementation
 * Mariano Mendez - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.ui.refactoring;

import org.eclipse.photran.internal.core.refactoring.VariableCaseRefactoring;
import org.eclipse.rephraserengine.ui.refactoring.CustomUserInputPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * User input wizard page for the Change Variable Case refactoring
 *
 * @author German Aquino, Federico Tombazzi
 */
public class VariableCaseInputPage extends CustomUserInputPage<VariableCaseRefactoring>
{
    protected Button radioLowerCase;
    protected Button radioUpperCase;

    @Override
    public void createControl(Composite parent)
    {
        Composite top = new Composite(parent, SWT.NONE);
        initializeDialogUnits(top);
        setControl(top);

        top.setLayout(new GridLayout(1, false));

        Composite group = top;
        Label instr = new Label(group, SWT.NONE);
        instr.setText(Messages.VariableCaseInputPage_ChangeKeywordsToLabel);

        radioLowerCase = new Button(group, SWT.RADIO);
        radioLowerCase.setText(Messages.VariableCaseInputPage_LowerCaseLabel);
        radioLowerCase.setSelection(true);
        radioLowerCase.addSelectionListener(new SelectionListener()
        {
            public void widgetDefaultSelected(SelectionEvent e)
            {
                widgetSelected(e);
            }

            public void widgetSelected(SelectionEvent e)
            {
                boolean isChecked = radioLowerCase.getSelection();
                getRefactoring().setLowerCase(isChecked);
            }
        });

        radioUpperCase = new Button(group, SWT.RADIO);
        radioUpperCase.setText(Messages.VariableCaseInputPage_UpperCaseLabel);

        Label lbl = new Label(group, SWT.NONE);
        lbl.setText(Messages.VariableCaseInputPage_ClickOKMessage);
    }
}
