/*******************************************************************************
 * Copyright (c) 2011-2012 University of Illinois and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeff Overbey (Illinois) - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.lang.linescanner;

/**
 * The type of a line read by a {@link FortranLineScanner}.
 * <p>
 * ISO/IEC 1539-1:2010, Section 3.3: &quot;A Fortran program unit is a sequence of one or more
 * lines, organized as Fortran statements, comments, and INCLUDE lines.&quot; (Note that we have
 * also included OpenMP, OpenACC, and C preprocessor lines, which are not described in the Fortran
 * language specification.)
 * 
 * @author Jeff Overbey
 * 
 * @see FortranLineScanner#getLineType()
 */
public enum FortranLineType {
    /**
     * A Fortran comment.
     * <p>
     * Comments usually start with !, although in fixed form they may be indicated by a C, c, or *
     * in column 1.
     */
    COMMENT,

    /** An OpenMP or OpenACC directive, which starts with !$omp, !$acc, C$omp, c$acc, etc. */
    COMMENT_DIRECTIVE,

    /** A Fortran statement: a line which is not a comment, directive, or INCLUDE line */
    STATEMENT,

    /**
     * A Fortran INCLUDE line. (This is distinct from a C preprocessor #include directive, which is
     * treated as a {@link #PREPROCESSOR_DIRECTIVE}.)
     */
    INCLUDE_LINE,

    /** A C preprocessor directive: #include, #define, #line, #error, etc. */
    PREPROCESSOR_DIRECTIVE;
}
