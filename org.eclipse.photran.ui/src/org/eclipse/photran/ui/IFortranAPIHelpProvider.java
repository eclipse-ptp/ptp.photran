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
package org.eclipse.photran.ui;

import org.eclipse.help.IHelpResource;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A provider of help resources for a particular Fortran API.
 * <p>
 * This interface is implemented by clients that extend the FIXME extension point.
 * 
 * @author Jeff Overbey
 * 
 * @since 8.0
 */
public interface IFortranAPIHelpProvider
{
    /**
     * Returns a list of help resources for the given Fortran editor with the given selected text.
     * <p>
     * If no help resources are available, this method may either return <code>null</code> or an
     * empty array.
     * <p>
     * The returned array must not contain <code>null</code> elements.
     * 
     * @param fortranEditor the active Fortran editor, or <code>null</code> if no editor is active
     *            (e.g., if this method is being invoked by the Fortran Declaration view)
     * @param identifier the word under the caret in the Fortran editor; or, if the editor selection is
     *            nonempty, the text in the editor selection; or, if this method is being invoked
     *            from a source other than an editor (e.g., by the Fortran Declaration view), the
     *            name of an API type, procedure, or constant (nonempty, non- <code>null</code>)
     * @param precedingText the text on the current editor line preceding <code>apiName</code>, or
     *            the empty string (non-<code>null</code>)
     * 
     * @return a list of help resources for the given Fortran editor with the given selected text
     *         (possibly <code>null</code>)
     */
    IHelpResource[] getHelpResources(ITextEditor fortranEditor, String identifier, String precedingText);
}
