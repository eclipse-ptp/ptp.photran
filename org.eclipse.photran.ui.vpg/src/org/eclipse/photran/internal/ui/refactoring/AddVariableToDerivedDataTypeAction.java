/*******************************************************************************
 * Copyright (c) 2011 UFSM - Universidade Federal de Santa Maria (www.ufsm.br).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.photran.internal.ui.refactoring;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.ltk.ui.refactoring.UserInputWizardPage;
import org.eclipse.photran.core.IFortranAST;
import org.eclipse.photran.internal.core.lexer.Token;
import org.eclipse.photran.internal.core.refactoring.AddVariableToDerivedDataTypeRefactoring;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.photran.internal.core.vpg.refactoring.VPGRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * User interface/action handler for {@link AddVariableToDerivedDataTypeRefactoring}.
 * 
 * @author Gustavo Risetti
 */
public class AddVariableToDerivedDataTypeAction
    extends AbstractFortranRefactoringActionDelegate
    implements IWorkbenchWindowActionDelegate, IEditorActionDelegate
{
    public AddVariableToDerivedDataTypeAction()
    {
        super(AddVariableToDerivedDataTypeRefactoring.class, FortranAddVariableToDerivedDataTypeRefactoringWizard.class);
    }

    @Override
    protected VPGRefactoring<IFortranAST, Token, PhotranVPG> getRefactoring(List<IFile> files)
    {
        AddVariableToDerivedDataTypeRefactoring r = new AddVariableToDerivedDataTypeRefactoring();
        r.initialize(
            getFortranEditor().getIFile(),
            getFortranEditor().getSelection());
        return r;
    }

    public static class FortranAddVariableToDerivedDataTypeRefactoringWizard extends AbstractFortranRefactoringWizard
    {
        protected AddVariableToDerivedDataTypeRefactoring addVariableToDerivedDataTypeRefactoring;

        public FortranAddVariableToDerivedDataTypeRefactoringWizard(AddVariableToDerivedDataTypeRefactoring r)
        {
            super(r);
            this.addVariableToDerivedDataTypeRefactoring = r;
        }

        @Override
        protected void doAddUserInputPages()
        {
            addPage(new UserInputWizardPage(addVariableToDerivedDataTypeRefactoring.getName())
            {
                protected Text derivedDataTypeName;
                protected Text variableName;

                public void createControl(Composite parent)
                {
                    Composite top = new Composite(parent, SWT.NONE);
                    initializeDialogUnits(top);
                    setControl(top);

                    top.setLayout(new GridLayout(2, false));

                    Composite group = top;
                    Label lbl = new Label(group, SWT.NONE);
                    lbl.setText(Messages.AddVariableToDerivedDataTypeAction_label1Text);
                    
                    derivedDataTypeName = new Text(group, SWT.BORDER);
                    derivedDataTypeName.setText(""); //$NON-NLS-1$
                    derivedDataTypeName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    derivedDataTypeName.selectAll();
                    derivedDataTypeName.addModifyListener(new ModifyListener()
                    {
                        public void modifyText(ModifyEvent e)
                        {
                            addVariableToDerivedDataTypeRefactoring.setDerivedTypeName(derivedDataTypeName.getText());
                        }
                    });
                    
                    Label lbl2 = new Label(group, SWT.NONE);
                    lbl2.setText(Messages.AddVariableToDerivedDataTypeAction_label2Text);

                    variableName = new Text(group, SWT.BORDER);
                    variableName.setText(""); //$NON-NLS-1$
                    variableName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    variableName.selectAll();
                    variableName.addModifyListener(new ModifyListener()
                    {
                        public void modifyText(ModifyEvent e)
                        {
                            addVariableToDerivedDataTypeRefactoring.setDerivedTypeVariableName(variableName.getText());
                        }
                    });                    

                    // Call once for sure, just in case the user doesn't modify the text
                    addVariableToDerivedDataTypeRefactoring.setDerivedTypeName(derivedDataTypeName.getText());
                    addVariableToDerivedDataTypeRefactoring.setDerivedTypeVariableName(derivedDataTypeName.getText());

                    derivedDataTypeName.setFocus();
                }
            });
        }
    }
}
