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
package org.eclipse.photran.internal.tests.analysis.dependence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.photran.internal.core.analysis.dependence.Matrix;

import junit.framework.TestCase;

public class MatrixTests extends TestCase {

    private Matrix matrixWithSolution;

    private Matrix noSolutionMatrix;

    @Override
    public void setUp() {
        this.matrixWithSolution = new Matrix();
        double[] row1 = { 1, 1, 1, 10 };
        double[] row2 = { 1, -1, 2, 20 };
        double[] row3 = { 2, -1, -1, -1 };
        double[] row4 = { -1, 1, -1, 5 };
        matrixWithSolution.addRowAtIndex(0, row1);
        matrixWithSolution.addRowAtIndex(1, row2);
        matrixWithSolution.addRowAtIndex(2, row3);
        matrixWithSolution.addRowAtIndex(3, row4);

        this.noSolutionMatrix = new Matrix();
        double[] row5 = { 1, 0, 20 };
        double[] row6 = { -1, 0, 10 };
        double[] row7 = { 0, 1, 5 };
        double[] row8 = { 0, -1, 0 };
        double[] row9 = { 1, -1, 4 };
        noSolutionMatrix.addRowAtIndex(0, row5);
        noSolutionMatrix.addRowAtIndex(1, row6);
        noSolutionMatrix.addRowAtIndex(2, row7);
        noSolutionMatrix.addRowAtIndex(3, row8);
        noSolutionMatrix.addRowAtIndex(4, row9);
    }

    @Override
    public void tearDown() {
        this.matrixWithSolution = null;
        this.noSolutionMatrix = null;
    }

    public void test100_010DefaultConstructorRowsEmpty() {
        Matrix m = new Matrix();
        assertTrue(m.getRows().isEmpty());
    }

    public void test100_020DefaultConstructorColsZero() {
        Matrix m = new Matrix();
        assertEquals(m.getNumColumns(), 0);
    }

    public void test200_030GivenRowsConstructorRowsCorrect() {
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { 5, 6, 7, 8 };
        List<double[]> rows = new ArrayList<double[]>();
        rows.add(row1);
        rows.add(row2);
        Matrix m = new Matrix(rows);
        assertTrue(Arrays.equals(m.getSingleRow(0), row1));
        assertTrue(Arrays.equals(m.getSingleRow(1), row2));
    }

    public void test200_030GivenRowsConstructorNumColCorrect() {
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { 5, 6, 7, 8 };
        List<double[]> rows = new ArrayList<double[]>();
        rows.add(row1);
        rows.add(row2);
        Matrix m = new Matrix(rows);
        assertEquals(m.getNumColumns(), 4);
    }

    public void test400_010AddRowAtIndex() {
        double[] row1 = { 0.0, 0.0, 0.0, 0.0 };
        matrixWithSolution.addRowAtIndex(1, row1);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), -1.0);
    }

    public void test400_020AddRowAtIndex() {
        double[] row1 = { 0.0, 0.0, 0.0, 0.0 };
        matrixWithSolution.addRowAtIndex(0, row1);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), -1.0);
    }

    public void test400_030AddRowAtIndex() {
        double[] row1 = { 0.0, 0.0, 0.0, 0.0 };
        matrixWithSolution.addRowAtIndex(4, row1);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 1), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 2), 0.0);
    }

    public void test400_040AddRowAtIndex() {
        double[] row1 = { 0.0, 0.0, 0.0, 0.0 };
        matrixWithSolution.addRowAtIndex(3, row1);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 0), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 2), -1.0);
    }
    
    public void test400_050AddRowAtIndexBoundary() {
        double[] row1 = { 0.0, 2.0, 0.0, 2.0 };
        matrixWithSolution.addRowAtIndex(0, row1);
        matrixWithSolution.addRowAtIndex(3, row1);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 2), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 0), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 2), -1.0);        
        matrixWithSolution.divideRowByColIndex(0, 3);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 0), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 1), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(3, 2), 0.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 0), 2.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 1), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(4, 2), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 0), -1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 1), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(5, 2), -1.0); 
    }

    public void test500_010DeleteFirstRow() {
        Matrix m = new Matrix();
        double[] row1 = { 0.0, 0.0 };
        double[] row2 = { 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.deleteRow(0);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 2.0);
    }

    public void test500_020DeleteLastRow() {
        Matrix m = new Matrix();
        double[] row1 = { 0.0, 0.0 };
        double[] row2 = { 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.deleteRow(1);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 0.0);
    }

    public void test600_010GetSingleRow() {
        Matrix m = new Matrix();
        double[] row1 = { 0.0, 0.0 };
        double[] row2 = { 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        assertTrue(Arrays.equals(m.getSingleRow(0), row1));
        assertTrue(Arrays.equals(m.getSingleRow(1), row2));
    }

    public void test700_010GetValueAtMatrixIndex() {
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(0, 2), 1.0);
        assertEquals(matrixWithSolution.getValueAtMatrixIndex(1, 1), -1.0);
    }

    public void test800_010ToString() {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3 };
        double[] row2 = { 4.5, 6.7, 8.9 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        assertEquals(m.toString(), "1.0 2.0 | 3.0 \n4.5 6.7 | 8.9 \n");
    }

    public void test900_010DivideRowByColIndex() {
        Matrix m = new Matrix();
        double[] row1 = { 6, 4, 2 };
        double[] row2 = { 4.5, 6.7, 8.9 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.divideRowByColIndex(0, 2);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 3.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 4.5);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 6.7);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 8.9);
    }

    public void test900_020DivideRowByColIndex() {
        Matrix m = new Matrix();
        double[] row1 = { 6, 4, 2 };
        double[] row2 = { -4.5, 6.7, 8.9 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.divideRowByColIndex(1, 0);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 6.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 4.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 2.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.488888888888889);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 1.9777777777777779);
    }

    public void test900_030DivideRowByColIndexWhereValueIsZero() {
        Matrix m = new Matrix();
        double[] row1 = { 6, 4, 0 };
        double[] row2 = { 4.5, 6.7, 8.9 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.divideRowByColIndex(0, 2);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 6.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 4.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 4.5);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 6.7);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 8.9);
    }

    public void test1000_040AddTwoRowsTogether() {
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { 5, 6, 7, 8 };
        List<double[]> rows = new ArrayList<double[]>();
        rows.add(row1);
        rows.add(row2);
        Matrix m = new Matrix(rows);
        double[] newRow = m.addTwoRowsToCreateNewRow(row1, row2);
        assertEquals(newRow[0], 6.0);
        assertEquals(newRow[1], 8.0);
        assertEquals(newRow[2], 10.0);
        assertEquals(newRow[3], 12.0);
    }
    
    public void test1010_010SetSingleRow()   {
        Matrix m = new Matrix();
        double[] row = {5, 6};
        m.setSingleRow(0, row);
    }
    
    public void test1020_010AddSingleRow()   {
        Matrix m = new Matrix();
        double[] row = {5, 6};
        m.addRowAtIndex(0, row);
    }
    
    public void test1030_010SetRows()   {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { 5, 6, 7, 8 };
        List<double[]> rows = new ArrayList<double[]>();
        rows.add(row1);
        rows.add(row2);
        m.setRows(rows);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 3.0);
        assertEquals(m.getValueAtMatrixIndex(0, 3), 4.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 5.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 6.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 7.0);
        assertEquals(m.getValueAtMatrixIndex(1, 3), 8.0);
    }
    
    public void test1040_010SetNumCols()   {
        Matrix m = new Matrix();
        m.setNumColumns(5);
        assertEquals(5, m.getNumColumns());
    }
    
    public void test1050_010GetRows()   {
        List<double[]> rows = matrixWithSolution.getRows();
        double[] row1 = { 1, 1, 1, 10 };
        double[] row2 = { 1, -1, 2, 20 };
        double[] row3 = { 2, -1, -1, -1 };
        double[] row4 = { -1, 1, -1, 5 };
        assertTrue(Arrays.equals(rows.get(0), row1));
        assertTrue(Arrays.equals(rows.get(1), row2));
        assertTrue(Arrays.equals(rows.get(2), row3));
        assertTrue(Arrays.equals(rows.get(3), row4));
    }
    
    public void test1060_010IsRowFullOfZeroes()   {
        double[] row = {0, 0, 0, 0};
        assertTrue(matrixWithSolution.isRowFullOfZeroes(row));
    }
    
    public void test1060_020IsRowFullOfZeroes()   {
        double[] row = {0, 0, 1, 0};
        assertEquals(false, matrixWithSolution.isRowFullOfZeroes(row));
    }
    
    public void test1060_030IsRowFullOfZeroes()   {
        double[] row = {0, 0, 0, -1};
        assertEquals(false, matrixWithSolution.isRowFullOfZeroes(row));
    }
}
