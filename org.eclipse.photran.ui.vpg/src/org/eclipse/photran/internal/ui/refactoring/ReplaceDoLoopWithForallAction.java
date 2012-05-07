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
import org.eclipse.photran.internal.core.refactoring.ReplaceDoLoopWithForallRefactoring;
import org.eclipse.photran.internal.core.vpg.PhotranVPG;
import org.eclipse.rephraserengine.core.vpg.refactoring.VPGRefactoring;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * User interface/action handler for {@link ReplaceDoLoopWithForallRefactoring}.
 * 
 * @author Gustavo Risetti
 */
public class ReplaceDoLoopWithForallAction
    extends AbstractFortranRefactoringActionDelegate
    implements IWorkbenchWindowActionDelegate, IEditorActionDelegate
{
    public ReplaceDoLoopWithForallAction()
    {
        super(ReplaceDoLoopWithForallRefactoring.class, FortranReplaceDoLoopByForallRefactoringWizard.class);
    }

    @Override
    protected VPGRefactoring<IFortranAST, Token, PhotranVPG> getRefactoring(List<IFile> files)
    {
        ReplaceDoLoopWithForallRefactoring r = new ReplaceDoLoopWithForallRefactoring();
        r.initialize(
            getFortranEditor().getIFile(),
            getFortranEditor().getSelection());
        return r;
    }

    public static class FortranReplaceDoLoopByForallRefactoringWizard extends AbstractFortranRefactoringWizard
    {
        protected ReplaceDoLoopWithForallRefactoring replaceDoLoopByForallRefactoring;

        public FortranReplaceDoLoopByForallRefactoringWizard(ReplaceDoLoopWithForallRefactoring r)
        {
            super(r);
            this.replaceDoLoopByForallRefactoring = r;
        }

        @Override
        protected void doAddUserInputPages()
        {
            addPage(new UserInputWizardPage(replaceDoLoopByForallRefactoring.getName())
            {
                public void createControl(Composite parent)
                {
                    Composite top = new Composite(parent, SWT.NONE);
                    initializeDialogUnits(top);
                    setControl(top);

                    top.setLayout(new GridLayout(1, false));

                    Composite group = top;
                    Label lbl = new Label(group, SWT.NONE);
                    lbl.setText(Messages.ReplaceDoLoopWithForallAction_messageText);
                }
            });
        }
    }
}
