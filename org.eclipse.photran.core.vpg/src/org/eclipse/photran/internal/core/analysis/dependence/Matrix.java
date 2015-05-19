/*******************************************************************************
 * Copyright (c) 2015 Auburn University and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Susan Chesnut - Initial API and implementation
 *******************************************************************************/
package org.eclipse.photran.internal.core.analysis.dependence;

import java.util.ArrayList;
import java.util.List;

/**
 * A data type for M-by-N matrices.
 * <p>
 * Developed for use in conjunction with Fourier-Motzkin Elimination
 * 
 * @author Susan Chesnut
 * @edit October 15, 2014
 */

public final class Matrix
{
    private ArrayList<double[]> rows;

    private int numColumns;

    // rows = { [1, 2, 3, 4], [5, 6, 7, 8] }
    // matrix = 1.0 2.0 3.0 | 4.0
    // 5.0 6.0 7.0 | 8.0

    /**
     * default constructor - creates an empty matrix
     */
    public Matrix()
    {
        // Collections.unmodifiableList(Arrays.asList(new int[] { 3, 4, 5 }));
        this.rows = new ArrayList<double[]>();
        setNumColumns(0);
    }

    /**
     * create matrix based on list of arrays. creates matrix based on given rows.
     * @param rowsIn rows for the created matrix
     */
    public Matrix(List<double[]> rowsIn)
    {
        setRows(rowsIn);
        setNumColumns(this.getSingleRow(0).length);
    }

    /**
     * clones the matrix calling this method
     * @return copied matrix m
     */
    public Matrix cloneMatrix()
    {
        Matrix m = new Matrix();
        m.setRows(this.rows);
        m.setNumColumns(this.numColumns);
        return m;
    }

    /**
     * 
     * @param rowsIn
     * @param numColumns
     */
    public Matrix(List<double[]> rowsIn, int numColumns)
    {
        this.setRows(rowsIn);
        this.setNumColumns(numColumns);
    }

    /**
     * 
     * @param aMatrix
     */
    public Matrix(Matrix aMatrix)
    {
        this(aMatrix.getRows(), aMatrix.getNumColumns());
    }

    /**
     * setter for the member variable rows
     * @param rowsIn rows for the matrix
     */
    public void setRows(List<double[]> rowsIn)
    {
        this.rows = new ArrayList<double[]>();
        for (int i = 0; i < rowsIn.size(); i++)
            this.rows.add(rowsIn.get(i).clone());
        this.setNumColumns(rows.get(0).length);
        trimRowsToSize();
    }

    public void trimRowsToSize()
    {
        this.rows.trimToSize();
    }

    /**
     * sets a single row (given by rowIndex) to be rowIn replaces the element at rowIndex with
     * rowIn.
     * @param rowIndex a single row index for the matrix being changed
     * @param rowIn replacement row
     * @throws IndexOutOfBoundsException when rowIn is not suited for the matrix or the rowIndex is
     *             invalid
     */
    public void setSingleRow(int rowIndex, double[] rowIn) throws IndexOutOfBoundsException
    {
        // if set up Matrix with default constructor,
        // rows will be empty and numColumns = 0
        // will want to set numColumns to equal the number of columns for this first row
        if (rows.isEmpty())
        {
            setNumColumns(rowIn.length);
            addRowAtIndex(0, rowIn);
        }

        if (rowIn.length != numColumns || rowIndex < 0 || rowIndex >= getNumRows())
            throw new IndexOutOfBoundsException("setSingleRow - index out of bounds"); //$NON-NLS-1$

        this.rows.set(rowIndex, rowIn.clone());
    }

    /**
     * setter for the member variable numColumns
     * @param numOfCol number of columns
     */
    public void setNumColumns(int numOfCol)
    {
        if (numOfCol < 0)
            throw new IndexOutOfBoundsException("setNumColumns - numOfCol must be greater than 0"); //$NON-NLS-1$
        this.numColumns = numOfCol;
    }

    /**
     * @return the rows for this matrix
     */
    public List<double[]> getRows()
    {
        return this.rows;
    }

    /**
     * Returns the row, at rowIndex, of the matrix
     * @param rowIndex index of returned ros
     * @return a row from the matrix
     * @throws IndexOutOfBoundsException when the index is invalid
     */
    public double[] getSingleRow(int rowIndex) throws IndexOutOfBoundsException
    {
        if (rowIndex < 0 || rowIndex >= getNumRows())
            throw new IndexOutOfBoundsException("getSingleRow - index out of bounds"); //$NON-NLS-1$
        return rows.get(rowIndex);
    }

    /**
     * @return the number of columns for this matrix
     */
    public int getNumColumns()
    {
        return this.numColumns;
    }

    /**
     * @return the number of rows for this matrix
     */
    public int getNumRows()
    {
        return this.rows.size();
    }

    /**
     * returns the value at the matrix index (rowIndex, colIndex)
     * @param rowIndex index for the row
     * @param colIndex index for the column
     * @return the value at the matrix index
     * @throws IndexOutOfBoundsException when either of the indexes is invalid
     */
    public double getValueAtMatrixIndex(int rowIndex, int colIndex) throws IndexOutOfBoundsException
    {
        if (rowIndex < 0 || rowIndex >= getNumRows() || colIndex < 0 || colIndex >= getNumColumns())
            throw new IndexOutOfBoundsException("getValueAtMatrixIndex - index out of bounds"); //$NON-NLS-1$
        return getSingleRow(rowIndex)[colIndex];
    }

    /**
     * adds a row to a specific index of the matrix
     * @param rowIn row to be added to the matrix
     * @param index - where to add the row in the matrix
     * @throws IllegalArgumentException when either of the indexes is invalid
     */
    public void addRowAtIndex(int index, double[] rowIn) throws IllegalArgumentException
    {
        // if set up Matrix with default constructor,
        // rows will be empty and numColumns = 0
        // will want to set numColumns to equal the number of columns for this first row
        if (rows.isEmpty()) setNumColumns(rowIn.length);

        // trying to add a row with the incorrect number of columns
        // or adding at an invalid index
        if (rowIn.length != numColumns || index < 0 || index > getNumRows() + 1)
            throw new IllegalArgumentException("addRowAtIndex - invalid column or index number"); //$NON-NLS-1$

        // shifts everything down
        rows.add(index, rowIn.clone());
    }

    /**
     * deletes a row, based on rowIndex, of the matrix
     * @param rowIndex the row to delete from the matrix
     * @return whether the delete was successful or not
     * @throws IndexOutOfBoundsException
     */
    public boolean deleteRow(int rowIndex) throws IndexOutOfBoundsException
    {
        if (rowIndex < 0 || rowIndex >= getNumRows()) { throw new IndexOutOfBoundsException(
            "deleteRow - index out of bounds"); } //$NON-NLS-1$
        rows.remove(rowIndex);
        return true;
    }

    /**
     * divides row at rowIndex by the value at (rowIndex, colIndex) or, divides row A_i by |a_ij|
     * @param rowIndex row to change
     * @param colIndex column index to divide row by
     */
    public void divideRowByColIndex(int rowIndex, int colIndex)
    {
        double value = Math.abs(getValueAtMatrixIndex(rowIndex, colIndex));
        if (value == 0) return;
        for (int i = 0; i < getSingleRow(rowIndex).length; i++)
        {
            getSingleRow(rowIndex)[i] = getSingleRow(rowIndex)[i] / value;
        }

    }

    /**
     * given two row indices, sum the two rows and return the newly summed row or, returns a new
     * row: A_i + A_j
     * @param firstRow the first row used in sum
     * @param secondRow the second row used in sum
     * @return a new row created by adding the rows at firstRowIndex and secondRowIndex by indices
     * @throws IndexOutOfBoundsException when either index is invalid
     */
    public double[] addTwoRowsToCreateNewRow(double[] firstRow, double[] secondRow)
        throws IndexOutOfBoundsException
    {
        // firstRowIndex or secondRowIndex as row indices are invalid
        if (firstRow.length != secondRow.length)
            throw new IndexOutOfBoundsException("addTwoRowsToCreateNewRow - invalid rows"); //$NON-NLS-1$

        double[] newRow = new double[firstRow.length];
        for (int i = 0; i < firstRow.length; i++)
        {
            newRow[i] = firstRow[i] + secondRow[i];
        }
        return newRow;
    }

    /**
     * 
     * @param row
     * @return whether row is all zeroes or not
     */
    public boolean isRowFullOfZeroes(double[] row)
    {
        boolean isAllZeroes = true;
        for (int j = 0; j < getNumColumns(); j++)
        {
            if (row[j] != 0.0) return false;
        }
        return isAllZeroes;
    }

    /**
     * @return a string of the matrix in the form:
     * 
     *         <pre>
     * 1.0 2.0 3.0 | 4.0 
     * 5.0 6.0 7.0 | 8.0 
     * 9.0 9.0 9.0 | 9.0
     *         </pre>
     * 
     *         rows = { [1, 2, 3, 4], [5, 6, 7, 8], [9, 9, 9, 9] }
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        for (int numRow = 0; numRow < getNumRows(); numRow++)
        {
            for (int numCol = 0; numCol < getNumColumns(); numCol++)
            {
                if (numCol == getNumColumns() - 2)
                {
                    builder.append(getValueAtMatrixIndex(numRow, numCol) + " | "); //$NON-NLS-1$
                }
                else
                    builder.append(getValueAtMatrixIndex(numRow, numCol) + " "); //$NON-NLS-1$
            }
            builder.append("\n"); //$NON-NLS-1$
        }
        return builder.toString();
    }

}