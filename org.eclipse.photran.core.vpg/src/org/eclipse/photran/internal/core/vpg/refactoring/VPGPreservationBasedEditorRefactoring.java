package org.eclipse.photran.internal.core.vpg.refactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.photran.internal.core.preservation.PreservationAnalysis;
import org.eclipse.photran.internal.core.preservation.PreservationRuleset;
import org.eclipse.photran.internal.core.vpg.IVPGNode;
import org.eclipse.photran.internal.core.vpg.eclipse.EclipseVPG;

public abstract class VPGPreservationBasedEditorRefactoring<A, T, V extends EclipseVPG<A, T, ? extends IVPGNode<T>>>
    extends VPGEditorRefactoring<A, T, V>
{
    protected PreservationAnalysis preservation = null;

    @Override
    protected final void doCheckFinalConditions(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure
    {
        try
        {
            pm.beginTask(Messages.VPGPreservationBasedEditorRefactoring_CheckingFinalPreconditions, 40);

            // If the user used the Back button in the refactoring wizard dialog,
            // the AST pointed to by astOfFileInEditor may have been released, so we
            // should re-acquire the current AST to make sure (1) we're not using
            // a modified AST, and (2) we're using an AST that the VPG is currently
            // aware of (i.e., not a stale AST no longer in its cache).
            this.astOfFileInEditor = vpg.acquireTransientAST(fileInEditor);

            doValidateUserInput(status, new SubProgressMonitor(pm, 5));
            if (!status.hasFatalError())
            {
                vpg.acquirePermanentAST(fileInEditor);

                preservation = new PreservationAnalysis(getVPG(), pm, 10,
                    fileInEditor,
                    getEdgesToPreserve());

                doTransform(status, new SubProgressMonitor(pm, 5));

                vpg.commitChangesFromInMemoryASTs(pm, 20, fileInEditor);
                preservation.checkForPreservation(status, pm, 0);

                this.addChangeFromModifiedAST(this.fileInEditor, pm);
            }

            pm.done();
        }
        finally
        {
            vpg.releaseAllASTs();
        }
    }

    @Override
    protected final void doCreateChange(IProgressMonitor pm) throws CoreException, OperationCanceledException
    {
    }

    protected abstract void doValidateUserInput(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure;

    protected abstract PreservationRuleset getEdgesToPreserve();

    protected abstract void doTransform(RefactoringStatus status, IProgressMonitor pm) throws PreconditionFailure;
}
