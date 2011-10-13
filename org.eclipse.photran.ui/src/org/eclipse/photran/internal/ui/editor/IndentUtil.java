/*******************************************************************************
 * Copyright (c) 2005, 2010, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin, Google
 *     Anton Leherbauer (Wind River Systems)
 *     Jeff Overbey (Actilon Consulting) - Trimmed to minimum for Photran
 *******************************************************************************/
package org.eclipse.photran.internal.ui.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

/**
 * Utility that indents a number of lines in a document.
 */
public final class IndentUtil {
	
	private static final String SLASHES= "//"; //$NON-NLS-1$

	/**
	 * Returns the visual length of a given <code>CharSequence</code> taking into
	 * account the visual tabulator length.
	 *
	 * @param seq the string to measure
	 * @return the visual length of <code>seq</code>
	 */
	public static int computeVisualLength(CharSequence seq, int tablen) {
		int size= 0;

		for (int i= 0; i < seq.length(); i++) {
			char ch= seq.charAt(i);
			if (ch == '\t') {
				if (tablen != 0)
					size += tablen - size % tablen;
				// else: size stays the same
			} else {
				size++;
			}
		}
		return size;
	}

	/**
	 * Returns the indentation of the line <code>line</code> in <code>document</code>.
	 * The returned string may contain pairs of leading slashes that are considered
	 * part of the indentation.
	 *
	 * @param document the document
	 * @param line the line
	 * @param indentInsideLineComments  option whether to indent inside line comments starting at column 0
	 * @return the indentation of <code>line</code> in <code>document</code>
	 * @throws BadLocationException if the document is changed concurrently
	 */
	public static String getCurrentIndent(IDocument document, int line, boolean indentInsideLineComments) throws BadLocationException {
		IRegion region= document.getLineInformation(line);
		int from= region.getOffset();
		int endOffset= region.getOffset() + region.getLength();

		int to= from;
		if (indentInsideLineComments) {
			// go behind line comments
			while (to < endOffset - 2 && document.get(to, 2).equals(SLASHES))
				to += 2;
		}
		
		while (to < endOffset) {
			char ch= document.getChar(to);
			if (!Character.isWhitespace(ch))
				break;
			to++;
		}

		return document.get(from, to - from);
	}

	/**
	 * Extends the string with whitespace to match displayed width.
	 * @param prefix  add to this string
	 * @param displayedWidth  the desired display width
	 * @param tabWidth  the configured tab width
	 * @param useSpaces  whether to use spaces only
	 */
	public static String changePrefix(String prefix, int displayedWidth, int tabWidth, boolean useSpaces) {
		int column = computeVisualLength(prefix, tabWidth);
		if (column > displayedWidth) {
			return prefix;
		}
		final StringBuilder buffer = new StringBuilder(prefix);
		appendIndent(buffer, displayedWidth, tabWidth, useSpaces, column);
		return buffer.toString();
	}

	/**
	 * Appends whitespace to given buffer such that its visual length equals the given width.
	 * @param buffer  the buffer to add whitespace to
	 * @param width  the desired visual indent width
	 * @param tabWidth  the configured tab width
	 * @param useSpaces  whether tabs should be substituted by spaces
	 * @param startColumn  the column where to start measurement
	 * @return StringBuilder
	 */
	private static StringBuilder appendIndent(StringBuilder buffer, int width, int tabWidth, boolean useSpaces, int startColumn) {
		assert tabWidth > 0;
		int tabStop = startColumn - startColumn % tabWidth;
		int tabs = useSpaces ? 0 : (width-tabStop) / tabWidth;
		for (int i = 0; i < tabs; ++i) {
			buffer.append('\t');
			tabStop += tabWidth;
			startColumn = tabStop;
		}
		int spaces = width - startColumn;
		for (int i = 0; i < spaces; ++i) {
			buffer.append(' ');
		}
		return buffer;
	}

}
