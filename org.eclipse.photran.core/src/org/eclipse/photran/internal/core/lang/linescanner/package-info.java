/**
 * This package contains a &quot;line scanner,&quot; which reads a line of input and determines its length and
 * {@link FortranLineType} (statement, comment, preprocessor directive, etc.); if it is determined
 * to be a {@link FortranLineType#STATEMENT} which is continued on subsequent lines, the
 * line scanner determines the complete statement, including all continuation lines
 * and intervening comment lines.
 * <p>
 * @see org.eclipse.photran.internal.core.lang.linescanner.FortranLineScanner
 */
package org.eclipse.photran.internal.core.lang.linescanner;