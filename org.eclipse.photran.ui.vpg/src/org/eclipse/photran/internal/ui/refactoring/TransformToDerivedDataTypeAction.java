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
import org.eclipse.photran.internal.core.refactoring.TransformToDerivedDataTypeRefactoring;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.rephraserengine.core.vpg.refactoring.VPGRefactoring;
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
 * User interface/action handler for {@link TransformToDerivedDataTypeRefactoring}.
 * 
 * @author Gustavo Risetti
 */
public class TransformToDerivedDataTypeAction
    extends AbstractFortranRefactoringActionDelegate
    implements IWorkbenchWindowActionDelegate, IEditorActionDelegate
{
    public TransformToDerivedDataTypeAction()
    {
        super(TransformToDerivedDataTypeRefactoring.class, FortranTransformToDerivedDataTypeRefactoringWizard.class);
    }

    @Override
    protected VPGRefactoring<IFortranAST, Token, PhotranVPG> getRefactoring(List<IFile> files)
    {
        TransformToDerivedDataTypeRefactoring r = new TransformToDerivedDataTypeRefactoring();
        r.initialize(
            getFortranEditor().getIFile(),
            getFortranEditor().getSelection());
        return r;
    }

    public static class FortranTransformToDerivedDataTypeRefactoringWizard extends AbstractFortranRefactoringWizard
    {
        protected TransformToDerivedDataTypeRefactoring transformToDerivedDataTypeRefactoring;

        public FortranTransformToDerivedDataTypeRefactoringWizard(TransformToDerivedDataTypeRefactoring r)
        {
            super(r);
            this.transformToDerivedDataTypeRefactoring = r;
        }

        @Override
        protected void doAddUserInputPages()
        {
            addPage(new UserInputWizardPage(transformToDerivedDataTypeRefactoring.getName())
            {
                protected Text newNameField;
                protected Text newVariableName;

                public void createControl(Composite parent)
                {
                    Composite top = new Composite(parent, SWT.NONE);
                    initializeDialogUnits(top);
                    setControl(top);

                    top.setLayout(new GridLayout(2, false));

                    Composite group = top;
                    Label lbl = new Label(group, SWT.NONE);
                    lbl.setText(Messages.TransformToDerivedDataTypeAction_label1Text);
                    
                    newNameField = new Text(group, SWT.BORDER);
                    newNameField.setText(""); //$NON-NLS-1$
                    newNameField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    newNameField.selectAll();
                    newNameField.addModifyListener(new ModifyListener()
                    {
                        public void modifyText(ModifyEvent e)
                        {
                            transformToDerivedDataTypeRefactoring.setDerivedTypeName(newNameField.getText());
                        }
                    });
                    
                    Label lbl2 = new Label(group, SWT.NONE);
                    lbl2.setText(Messages.TransformToDerivedDataTypeAction_label2Text);

                    newVariableName = new Text(group, SWT.BORDER);
                    newVariableName.setText(""); //$NON-NLS-1$
                    newVariableName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
                    newVariableName.selectAll();
                    newVariableName.addModifyListener(new ModifyListener()
                    {
                        public void modifyText(ModifyEvent e)
                        {
                            transformToDerivedDataTypeRefactoring.setDerivedTypeVariableName(newVariableName.getText());
                        }
                    });                    

                    // Call once for sure, just in case the user doesn't modify the text
                    transformToDerivedDataTypeRefactoring.setDerivedTypeName(newNameField.getText());
                    transformToDerivedDataTypeRefactoring.setDerivedTypeVariableName(newNameField.getText());

                    newNameField.setFocus();
                }
            });
        }
    }
}

