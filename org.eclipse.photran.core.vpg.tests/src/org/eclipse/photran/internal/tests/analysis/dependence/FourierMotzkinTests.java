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
import java.util.Random;

import org.eclipse.photran.internal.core.analysis.dependence.FourierMotzkinEliminator;
import org.eclipse.photran.internal.core.analysis.dependence.Matrix;

import junit.framework.TestCase;

/**
 * Unit tests for
 * {@link org.eclipse.photran.internal.core.analysis.dependence.FourierMotzkinEliminator}.
 * 
 * @author Susan Chesnut
 * @edit September 27,2014
 */
public class FourierMotzkinTests extends TestCase
{
    private Matrix matrixWithSolution;

    private Matrix noSolutionMatrix;

    private FourierMotzkinEliminator e;

    @Override
    public void setUp()
    {
        this.matrixWithSolution = new Matrix();
        this.e = new FourierMotzkinEliminator();
        double[] row1 = { 1, 1, 1, 10 };
        double[] row2 = { 1, -1, 2, 20 };
        double[] row3 = { 2, -1, -1, -1 };
        double[] row4 = { -1, 1, -1, 5 };
        matrixWithSolution.addRowAtIndex(0, row1);
        matrixWithSolution.addRowAtIndex(1, row2);
        matrixWithSolution.addRowAtIndex(2, row3);
        matrixWithSolution.addRowAtIndex(3, row4);
        matrixWithSolution.trimRowsToSize();

        this.noSolutionMatrix = new Matrix();
        double[] row5 = { 1, 0, 20 };
        double[] row6 = { -1, 0, -10 };
        double[] row7 = { 0, 1, 5 };
        double[] row8 = { 0, -1, 0 };
        double[] row9 = { 1, -1, 4 };
        noSolutionMatrix.addRowAtIndex(0, row5);
        noSolutionMatrix.addRowAtIndex(1, row6);
        noSolutionMatrix.addRowAtIndex(2, row7);
        noSolutionMatrix.addRowAtIndex(3, row8);
        noSolutionMatrix.addRowAtIndex(4, row9);
        noSolutionMatrix.trimRowsToSize();
    }

    @Override
    public void tearDown()
    {
        this.matrixWithSolution = null;
        this.noSolutionMatrix = null;
        this.e = null;
    }

    // ex 4.23
    public void test100_010EliminateForRealSolution()
    {
        assertEquals(true, e.eliminateForRealSolutions(matrixWithSolution));
    }

    // ex 4.24
    public void test100_020EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -4, 2 };
        double[] row2 = { 1, 5, 7 };
        double[] row3 = { -1, 0, -3 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.22
    public void test100_030EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 0, 0, 10 };
        double[] row2 = { -1, 0, 0, 0 };
        double[] row3 = { 1, -1, 0, 25 };
        double[] row4 = { 0, 1, 1, 15 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.5.1
    public void test100_040EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 6 };
        double[] row2 = { 1, 1, 9 };
        double[] row3 = { 1, -1, 5 };
        double[] row4 = { -2, -1, -7 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.25
    public void test100_050EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    public void test100_060EliminateForRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 2, -1, 3 };
        double[] row2 = { 0, -1, 3 };
        double[] row3 = { -0.8, -1, -2.5 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForRealSolutions(m));
    }

    // ex 4.21
    public void test100_010EliminateForNoRealSolution()
    {
        assertEquals(false, e.eliminateForRealSolutions(noSolutionMatrix));
    }

    public void test100_020EliminateForNoRealSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -1, -2 };
        double[] row2 = { -1, 1, -2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        assertEquals(false, e.eliminateForRealSolutions(m));
    }

    public void test100_900EliminateForNoRealSolutionFail()
    {
        Throwable thrown = null;
        Matrix m = new Matrix();
        try
        {
            e.eliminateForRealSolutions(m);
        }
        catch (IndexOutOfBoundsException realThrown)
        {
            thrown = realThrown;
        }
        assertTrue(thrown instanceof IndexOutOfBoundsException);
    }

    public void test200_010CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(3);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 0), testSet);
    }

    public void test200_020CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(1);
        testSet.add(2);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 1), testSet);
    }

    public void test200_030CalculateLowerBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(2);
        testSet.add(3);
        assertEquals(e.calculateLowerBoundSet(matrixWithSolution, 2), testSet);
    }

    public void test300_010CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(1);
        testSet.add(2);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 0), testSet);
    }

    public void test300_020CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(3);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 1), testSet);
    }

    public void test300_030CalculateUpperBoundSet()
    {
        List<Integer> testSet = new ArrayList<Integer>();
        testSet.add(0);
        testSet.add(1);
        assertEquals(e.calculateUpperBoundSet(matrixWithSolution, 2), testSet);
    }

    public void test400_010SortMatrixBoundingSetsAtTop()
    {
        Matrix m = new Matrix();
        List<Integer> temp1 = new ArrayList<Integer>();
        List<Integer> temp2 = new ArrayList<Integer>();
        temp1.add(1);
        temp2.add(2);
        m = e.sortMatrixByBoundingSetContents(noSolutionMatrix, temp1, temp2);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 5.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), -10.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 20.0);
        assertEquals(m.getValueAtMatrixIndex(3, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(3, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(4, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(4, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(4, 2), 4.0);
    }

    public void test400_020SortMatrixBoundingSetsAtTop()
    {
        Matrix m = new Matrix();
        List<Integer> temp1 = new ArrayList<Integer>();
        List<Integer> temp2 = new ArrayList<Integer>();
        temp1.add(0);
        temp2.add(2);
        temp2.add(4);
        m = e.sortMatrixByBoundingSetContents(noSolutionMatrix, temp1, temp2);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 4.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 5.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 20.0);
        assertEquals(m.getValueAtMatrixIndex(3, 0), -1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(3, 2), -10.0);
        assertEquals(m.getValueAtMatrixIndex(4, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(4, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(4, 2), 0.0);
    }

    public void test400_030SortMatrixBoundingSetsAtTop()
    {
        Matrix m = new Matrix();
        List<Integer> low = new ArrayList<Integer>();
        List<Integer> up = new ArrayList<Integer>();
        low.add(2);
        low.add(3);
        up.add(0);
        up.add(1);
        m = e.sortMatrixByBoundingSetContents(matrixWithSolution, low, up);
        assertEquals(m.getValueAtMatrixIndex(0, 0), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), -1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(m.getValueAtMatrixIndex(3, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 2), 1.0);
    }

    public void test400_040SortMatrixBoundingSetsAtTop()
    {
        Matrix m = new Matrix();
        List<Integer> low = new ArrayList<Integer>();
        List<Integer> up = new ArrayList<Integer>();
        low.add(2);
        low.add(3);
        up.add(0);
        up.add(1);
        double[] newRow = { 9, 9, 9, 9 };
        matrixWithSolution.addRowAtIndex(4, newRow);
        m = e.sortMatrixByBoundingSetContents(matrixWithSolution, low, up);
        assertEquals(m.getValueAtMatrixIndex(0, 0), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), -1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 2.0);
        assertEquals(m.getValueAtMatrixIndex(3, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 2), 1.0);
        assertEquals(m.getValueAtMatrixIndex(4, 0), 9.0);
        assertEquals(m.getValueAtMatrixIndex(4, 1), 9.0);
        assertEquals(m.getValueAtMatrixIndex(4, 2), 9.0);
    }

    public void test500_010DeleteFromSortedMatrix()
    {
        Matrix m = new Matrix();
        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 2);
        assertEquals(m.getNumRows(), 3);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 5.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 4.0);
    }

    public void test500_020DeleteFromSortedMatrix()
    {
        Matrix m = new Matrix();
        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 4);
        assertEquals(m.getNumRows(), 1);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 4.0);
    }

    public void test500_030DeleteFromSortedMatrix()
    {
        Matrix m = new Matrix();
        m = e.deleteRowsFromSortedMatrix(noSolutionMatrix, 5);
        assertEquals(m.getNumRows(), 0);
    }

    public void test600_010DeleteAllUnconstrainedVariables()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 2, 3, 20 };
        double[] row2 = { 0, -1, 2, 0, 30 };
        double[] row3 = { 1, 0, 0, 1, 40 };
        double[] row4 = { 2, 1, 3, 1, 50 };
        double[] row5 = { -1, 1, 1, 1, 60 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m = e.deleteAllUnconstrainedVariables(m);
        assertEquals(0, m.getNumRows());
    }

    public void test700_010DeleteRedundantInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 2, 3, 20 };
        double[] row2 = { 0, -1, 2, 0, 30 };
        double[] row3 = { 1, 0, 0, 1, 40 };
        double[] row4 = { 2, 1, 3, 1, 50 };
        double[] row5 = { 1, 0, 0, 1, 40 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m = e.deleteRedundantInequalities(m);
        assertEquals(4, m.getNumRows());
        assertEquals(m.getValueAtMatrixIndex(0, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 3), 3.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 2.0);
        assertEquals(m.getValueAtMatrixIndex(1, 3), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 3), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(3, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(3, 2), 3.0);
        assertEquals(m.getValueAtMatrixIndex(3, 3), 1.0);
    }

    // ex 4.22
    public void test800_010EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 0, 0, 10 };
        double[] row2 = { -1, 0, 0, 0 };
        double[] row3 = { 1, -1, 0, 25 };
        double[] row4 = { 0, 1, 1, 15 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // ex 4.21
    public void test800_020EliminateForIntSolution()
    {
        assertEquals(false, e.eliminateForIntegerSolutions(noSolutionMatrix));
    }

    // ex 4.25
    public void test800_030EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // ex 4.24
    public void test800_040EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -4, 2 };
        double[] row2 = { 1, 5, 7 };
        double[] row3 = { -1, 0, -3 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        assertEquals(false, e.eliminateForIntegerSolutions(m));
    }

    // ex 4.23
    public void test800_050EliminateForIntSolution()
    {
        assertEquals(true, e.eliminateForIntegerSolutions(matrixWithSolution));
    }

    // 3 variable ex
    public void test800_060EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 1, 1, 20 };
        double[] row6 = { 1, -1, 1, 4 };
        double[] row7 = { 1, 0, 1, 8 };
        double[] row8 = { 0, 1, 1, 12 };
        double[] row9 = { -1, 0, -1, 2 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 4 variable ex
    public void test800_070EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 1, 1, 1, 60 };
        double[] row6 = { 0, -1, 0, -1, -40 };
        double[] row7 = { -1, 0, -1, 0, -40 };
        double[] row8 = { -1, 0, 0, 0, -20 };
        double[] row9 = { 0, 0, 0, -1, -20 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        assertEquals(false, e.eliminateForIntegerSolutions(m));
    }

    // 4 variable ex - boundary
    public void test800_080EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 1, 1, 1, 79 };
        double[] row6 = { 0, -1, 0, -1, -40 };
        double[] row7 = { -1, 0, -1, 0, -40 };
        double[] row8 = { -1, 0, 0, 0, -20 };
        double[] row9 = { 0, 0, 0, -1, -20 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        assertEquals(false, e.eliminateForIntegerSolutions(m));
    }

    // 4 variable ex - boundary
    public void test800_090EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 1, 1, 1, 80 };
        double[] row6 = { 0, -1, 0, -1, -40 };
        double[] row7 = { -1, 0, -1, 0, -40 };
        double[] row8 = { -1, 0, 0, 0, -20 };
        double[] row9 = { 0, 0, 0, -1, -20 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 4 variable ex - boundary
    public void test800_100EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, -1, 1, -1, 5 };
        double[] row6 = { -1, 1, -1, 1, 5 };
        double[] row7 = { -1, 0, 0, 0, -1 };
        double[] row8 = { 0, -1, 0, 0, -2 };
        double[] row9 = { 0, 0, -1, 0, -1 };
        double[] row10 = { 0, 0, 0, -1, -2 };
        double[] row11 = { -1, 0, -1, 0, -4 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        m.addRowAtIndex(5, row10);
        m.addRowAtIndex(6, row11);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 2 variable ex - boundary
    public void test800_110EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 0, 1 };
        double[] row6 = { -1, 0, -1 };
        double[] row7 = { 0, 1, -5 };
        double[] row8 = { 1, -1, 2 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        assertEquals(false, e.eliminateForIntegerSolutions(m));
    }

    // 2 variable ex - boundary
    public void test800_120EliminateForIntSolution()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 0, 1 };
        double[] row6 = { -1, 0, -1 };
        double[] row7 = { 0, 1, -5 };
        double[] row8 = { 1, -1, 6 };
        double[] row9 = { 0, -1, 5 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // ex 4.25
    public void test900_010DarkShadowProjection()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        Matrix solutionSpace = new Matrix();
        double[] new2 = { 0.0, 1.0, 2.0 };
        double[] new1 = { 0.0, -5.0, 1.0 };
        solutionSpace.addRowAtIndex(0, new2);
        solutionSpace.addRowAtIndex(1, new1);
        assertEquals(solutionSpace.toString(), e.darkShadowIntegerProjection(m, 0).toString());
    }

    // ex 4.25
    public void test1000_010InexactIntegerProjection()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        Matrix solutionSpace = new Matrix();
        double[] new2 = { 0.0, 1.0, 2.0 };
        double[] new1 = { 0.0, -5.0, 3.0 };
        solutionSpace.addRowAtIndex(0, new2);
        solutionSpace.addRowAtIndex(1, new1);
        assertEquals(solutionSpace.toString(), e.inexactIntegerProjection(m, 0).toString());
    }

    // ex 4.23
    public void test1010_010RealProjection()
    {
        Matrix solutionSpace = new Matrix();
        double[] new1 = { 3.0, 0.0, 0.0, 9.0 };
        double[] new2 = { 2.5, -1.5, 0.0, 9.0 };
        double[] new3 = { 0.0, 2.0, 0.0, 15.0 };
        double[] new4 = { -0.5, 0.5, 0.0, 15.0 };
        solutionSpace.addRowAtIndex(0, new1);
        solutionSpace.addRowAtIndex(1, new2);
        solutionSpace.addRowAtIndex(2, new3);
        solutionSpace.addRowAtIndex(3, new4);
        assertEquals(solutionSpace.toString(), e.realProjection(matrixWithSolution, 2).toString());
    }

    public void test1020_010IsSolutionSetEmpty()
    {
        Matrix solutionSpace = new Matrix();
        double[] row1 = { 0.0, -5.0, 1.0 };
        double[] row2 = { 0.0, 1.0, 2.0 };
        solutionSpace.addRowAtIndex(0, row1);
        solutionSpace.addRowAtIndex(1, row2);
        assertEquals(false, e.isSolutionSetEmpty(solutionSpace.cloneMatrix(), 1));
    }

    public void test1020_020IsSolutionSetEmpty()
    {
        Matrix solutionSpace = new Matrix();
        double[] row1 = { 1.0, 0.0, 4.0 };
        double[] row2 = { -1.0, 0.0, -3.0 };
        solutionSpace.addRowAtIndex(0, row1);
        solutionSpace.addRowAtIndex(1, row2);
        assertEquals(false, e.isSolutionSetEmpty(solutionSpace.cloneMatrix(), 0));
    }

    public void test1020_030IsSolutionSetEmpty()
    {
        Matrix solutionSpace = new Matrix();
        double[] row1 = { 0.0, 1.0, 1.0 };
        double[] row2 = { 0.0, -1.0, -2.0 };
        solutionSpace.addRowAtIndex(0, row1);
        solutionSpace.addRowAtIndex(1, row2);
        assertEquals(true, e.isSolutionSetEmpty(solutionSpace.cloneMatrix(), 1));
    }

    public void test1020_040IsSolutionSetEmpty()
    {
        Matrix solutionSpace = new Matrix();
        double[] row1 = { 0.0, 1.0, 20.0 };
        double[] row2 = { 0.0, -1.0, -30.0 };
        solutionSpace.addRowAtIndex(0, row1);
        solutionSpace.addRowAtIndex(1, row2);
        assertEquals(true, e.isSolutionSetEmpty(solutionSpace.cloneMatrix(), 1));
    }

    public void test1020_050IsSolutionSetEmpty()
    {
        Matrix solutionSpace = new Matrix();
        double[] row1 = { 1.0, 1.0, 20.0 };
        double[] row2 = { 0.0, -1.0, -30.0 };
        solutionSpace.addRowAtIndex(0, row1);
        solutionSpace.addRowAtIndex(1, row2);
        assertEquals(false, e.isSolutionSetEmpty(solutionSpace.cloneMatrix(), 0));
    }

    public void test1030_010DeterminelUnconstrainedVariables()
    {
        Matrix m = new Matrix();
        double[] row1 = { 0, 1, 2, 3, 20 };
        double[] row2 = { 0, -1, 2, 0, 30 };
        double[] row3 = { 1, 0, 0, 1, 40 };
        double[] row4 = { 2, 1, 3, 1, 50 };
        double[] row5 = { -1, 1, 1, 1, 60 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        boolean[] sol = { false, false, true, true };
        assertTrue(Arrays.equals(sol, e.determineUnconstrainedVariables(m)));
    }

    public void test1040_010DeleteUnconstrainedVariable()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 0, 20 };
        double[] row6 = { -1, 0, -10 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        assertEquals(m.toString(), e.deleteUnconstrainedVariable(noSolutionMatrix, 1).toString());
    }

    // ex 4.25
    public void test1050_010CalculateDarkShadowProjection()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 2, -3, 1 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        double[] darkShadow = { 0, -5, 1 };
        assertTrue(Arrays.equals(darkShadow, e.calculateDarkShadow(m, 0, 1, 0)));
    }

    // ex 4.23 done 25xs
    public void testEliminateForIntSolution25xs()
    {
        for (int i = 0; i < 25; i++)
        {
            Matrix m = new Matrix(matrixWithSolution);
            assertEquals(true, e.eliminateForIntegerSolutions(m));
        }
    }

    // 4 variable ex - boundary - done 25xs
    public void testEliminateForIntSolution4VariableTrue25xs()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, -1, 1, -1, 5 };
        double[] row6 = { -1, 1, -1, 1, 5 };
        double[] row7 = { -1, 0, 0, 0, -1 };
        double[] row8 = { 0, -1, 0, 0, -2 };
        double[] row9 = { 0, 0, -1, 0, -1 };
        double[] row10 = { 0, 0, 0, -1, -2 };
        double[] row11 = { -1, 0, -1, 0, -4 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        m.addRowAtIndex(5, row10);
        m.addRowAtIndex(6, row11);
        for (int i = 0; i < 25; i++)
        {
            Matrix m2 = new Matrix(m);
            assertEquals(true, e.eliminateForIntegerSolutions(m2));
        }
    }

    // 4 variable ex - boundary - done 25xs
    public void testEliminateForIntSolution3VariableTrue25xs()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 1, 1, 20 };
        double[] row6 = { 1, -1, 1, 4 };
        double[] row7 = { 1, 0, 1, 8 };
        double[] row8 = { 0, 1, 1, 12 };
        double[] row9 = { -1, 0, -1, 2 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        m.addRowAtIndex(4, row9);
        for (int i = 0; i < 25; i++)
        {
            Matrix m2 = new Matrix(m);
            assertEquals(true, e.eliminateForIntegerSolutions(m2));
        }
    }

    // 2 variable ex - boundary
    public void testEliminateForIntSolution2VariableFalse25xs()
    {
        Matrix m = new Matrix();
        double[] row5 = { 1, 0, 1 };
        double[] row6 = { -1, 0, -1 };
        double[] row7 = { 0, 1, -5 };
        double[] row8 = { 1, -1, 2 };
        m.addRowAtIndex(0, row5);
        m.addRowAtIndex(1, row6);
        m.addRowAtIndex(2, row7);
        m.addRowAtIndex(3, row8);
        for (int i = 0; i < 25; i++)
        {
            Matrix m2 = new Matrix(m);
            assertEquals(false, e.eliminateForIntegerSolutions(m2));
        }
    }

    // 5 rows, 10 variables
    public void testBigMatrix5x10()
    {
        Matrix m = new Matrix();
        Random randGen = new Random();
        for (int i = 0; i < 5; i++)
        {
            double[] row = new double[10];
            for (int j = 0; j < 10; j++)
            {
                row[j] = randGen.nextInt(65536) - 32768;
            }
            m.addRowAtIndex(i, row);
        }
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 10 rows, 10 variables - eliminates first 4 variables before it gets all unconstrained
    public void testBigMatrix1Premade10x10()
    {
        Matrix m = new Matrix();
        double[] row1 = { 5, 1, 8, 13, 35, 10, -13, -23, 12, -2, 24 };
        double[] row2 = { -1, -91, -1, 42, 38, 123, 874, 743, -13, 2, -12 };
        double[] row3 = { -231, 1, -5, -123.5, 824.423, 0, -989, 145, 746, 2, 823 };
        double[] row4 = { 1752, -1, 212, -123, 354.31, 893.412, -811, 9.44444, 134, -2, 23 };
        double[] row5 = { 523, 541, 0, 1235, -438, 15, -13, 654, 234, 2, -658 };
        double[] row6 = { 0, 531, -134, -423, 0, 124, -12.254, 15.2323, 721, -2, 71 };
        double[] row7 = { 24, 0.12342412, -14, -0.1454, 19, -14, -142, 152, 5, 2, 18 };
        double[] row8 = { 13, 15, 17, -92, -637, 204, 38, -26, -17, -2, -16 };
        double[] row9 = { 5, 1, 8, 23, 34, 10, 13, 48, 93, 2, 0 };
        double[] row10 = { -431, 0, -1, 147, -14, -75, -32, 22, -73, -2, 0 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m.addRowAtIndex(5, row6);
        m.addRowAtIndex(6, row7);
        m.addRowAtIndex(7, row8);
        m.addRowAtIndex(8, row9);
        m.addRowAtIndex(9, row10);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 10 rows, 10 variables - eliminates first 2-3 variables before it gets all unconstrained
    public void testBigMatrix2Premade10x10()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, -1, 2, -2, 3, -3, 4, -4, 5, 1, -5 };
        double[] row2 = { -1, 1, -2, 2, -3, 3, -4, 4, -5, 1, 5 };
        double[] row3 = { 1, 0, 3, 4, -5, 6, 7, 8, -1, 1, 1 };
        double[] row4 = { 9, 0, -7, -6, 5, -4, 3, -2, -1, 1, -1 };
        double[] row5 = { 4, 1, 3, 7, -2, 9, -1, 6, 2, 1, 4 };
        double[] row6 = { 0, 0, 0, 6, 4, -9, 0, 2, 1, 1, -6 };
        double[] row7 = { 1, 2, 9, 7, 3, -8, -4, 7, -5, 1, 1 };
        double[] row8 = { -3, -1, -8, -4, 1, 0, 1, 2, 9, 1, 0 };
        double[] row9 = { -4, 8, 1, -3, -1, 1, 3, 0, 5, 1, -7 };
        double[] row10 = { 1, 0, 7, 0, 2, 9, -3, 2, 0, 1, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m.addRowAtIndex(5, row6);
        m.addRowAtIndex(6, row7);
        m.addRowAtIndex(7, row8);
        m.addRowAtIndex(8, row9);
        m.addRowAtIndex(9, row10);
        assertEquals(true, e.eliminateForIntegerSolutions(m));
    }

    // 10 rows, 10 variables - eliminates first 4 variables before it gets all unconstrained
    public void testBigMatrix3Premade10x10()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
        double[] row2 = { -1, -2, -3, -4, -5, -6, -7, -8, -9, -10, -11 };
        double[] row3 = { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1 };
        double[] row4 = { -2, -3, -4, -5, -6, -7, -8, -9, -10, -11, -1 };
        double[] row5 = { 3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 2 };
        double[] row6 = { -3, -4, -5, -6, -7, -8, -9, -10, -11, -1, -2 };
        double[] row7 = { 4, 5, 6, 7, 8, 9, 10, 11, 1, 2, 3 };
        double[] row8 = { -4, -5, -6, -7, -8, -9, -10, -11, -1, -2, -3 };
        double[] row9 = { 5, 6, 7, 8, 9, 10, 11, 1, 2, 3, 4 };
        double[] row10 = { -5, -6, -7, -8, -9, -10, -11, -1, -2, -3, -4 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m.addRowAtIndex(5, row6);
        m.addRowAtIndex(6, row7);
        m.addRowAtIndex(7, row8);
        m.addRowAtIndex(8, row9);
        m.addRowAtIndex(9, row10);
        assertEquals(false, e.eliminateForIntegerSolutions(m));
    }

    // // 10 rows, 10 variables
    // public void testBigMatrix10x10() {
    // Matrix m = new Matrix();
    // Random randGen = new Random();
    // for (int i = 0; i < 10; i++) {
    // double[] row = new double[10];
    // for (int j = 0; j < 10; j++) {
    // row[j] = Math.round((-100 + (100 - (-100)) * randGen.nextDouble())*100)/100.0d;
    // }
    // m.addRowAtIndex(i, row);
    // }
    // assertEquals(true, e.eliminateForIntegerSolutions(m));
    // }

    public void testOverflowMultiplyArrayByNumber()
    {
        Matrix m = new Matrix();
        Throwable thrown = null;
        double[] row1 = { 1e308, 0.0 };
        m.addRowAtIndex(0, row1);

        try
        {
            m.setSingleRow(0, e.multiplyArrayByNumber(row1, 10));
            e.eliminateForIntegerSolutions(m);
        }
        catch (ArithmeticException realThrown)
        {
            thrown = realThrown;
        }
        assertTrue(thrown instanceof ArithmeticException);
    }

    public void testUnderflowDivideRowForIntegerProjection()
    {
        Matrix m = new Matrix();
        Throwable thrown = null;
        double[] row1 = { 3.142E-320, 0.0 };
        m.addRowAtIndex(0, row1);

        try
        {
            e.divideRowForIntegerProjection(m, 0, 100000);
        }
        catch (ArithmeticException realThrown)
        {
            thrown = realThrown;
        }
        assertTrue(thrown instanceof ArithmeticException);
    }

    public void testCombineBoundingSets()
    {
        List<Integer> upper = new ArrayList<Integer>();
        List<Integer> lower = new ArrayList<Integer>();
        upper.add(1);
        upper.add(6);
        lower.add(2);
        List<Integer> combined = new ArrayList<Integer>();
        combined.add(1);
        combined.add(2);
        combined.add(6);
        assertTrue(combined.equals(e.combineBoundingSets(upper, lower)));
    }

    public void testDeleteRowsInBoundingSet()
    {
        Matrix m = new Matrix();
        List<Integer> combined = new ArrayList<Integer>();
        combined.add(0);
        combined.add(1);
        combined.add(3);
        m = e.deleteRowsInBoundingSet(matrixWithSolution, combined);
        assertEquals(m.getValueAtMatrixIndex(0, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), -1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), -1.0);
    }

    public void testDeleteRowsOfAllZeroes()
    {
        Matrix m = new Matrix();
        double[] row1 = { -3, 2, 0 };
        double[] row2 = { 0, 0, 0 };
        double[] row3 = { 0, 1, 2 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m = e.deleteRowsOfAllZeroes(m);
        assertEquals(2, m.getNumRows());
        assertEquals(m.getValueAtMatrixIndex(0, 0), -3.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 2.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 0.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 2.0);
    }

    public void testArraySubtraction()
    {
        double[] array1 = { 5, 4, 9, 2 };
        double[] array2 = { 8, 3, 5, 0 };
        double[] array3 = { -3.0, 1.0, 4.0, 2.0 };
        assertTrue(Arrays.equals(array3, e.arraySubtraction(array1, array2)));
    }

    public void testMultiplyArrayByNumber()
    {
        double[] array = { 5, 2, 7, 1 };
        double[] result = { 10.0, 4.0, 14.0, 2.0 };
        assertTrue(Arrays.equals(result, e.multiplyArrayByNumber(array, 2)));
    }

    public void testGCDRecursive()
    {
        double[] test = { 1, 2 };
        assertEquals(1.0, e.gcd(test));
    }

    public void testGCD()
    {
        assertEquals(1.0, e.gcd(1, 2));
    }

    public void testIsInconsistent()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -1, -2, -3, -5 };
        assertEquals(true, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent2()
    {
        double[] ar1 = { 3, -1, 3, -6 };
        double[] ar2 = { -3, 1, -3, -5 };
        assertEquals(true, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent3()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -1, -2, -3, -4 };
        assertEquals(false, e.isInconsistentInequality(ar1, ar2));
    }

    public void testIsInconsistent4()
    {
        double[] ar1 = { 1, 2, 3, 4 };
        double[] ar2 = { -3, -1, -3, -5 };
        assertEquals(false, e.isInconsistentInequality(ar1, ar2));
    }

    public void test1060_010DeleteInconsistentInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { -1, -2, -3, -5 };
        double[] row3 = { 1, 0, 0, 1 };
        double[] row4 = { 2, 1, 3, 1 };
        double[] row5 = { 1, 0, 0, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        m = e.deleteInconsistentInequalities(m);
        assertEquals(3, m.getNumRows());
        assertEquals(m.getValueAtMatrixIndex(0, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(0, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(0, 3), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 0), 2.0);
        assertEquals(m.getValueAtMatrixIndex(1, 1), 1.0);
        assertEquals(m.getValueAtMatrixIndex(1, 2), 3.0);
        assertEquals(m.getValueAtMatrixIndex(1, 3), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 0), 1.0);
        assertEquals(m.getValueAtMatrixIndex(2, 1), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 2), 0.0);
        assertEquals(m.getValueAtMatrixIndex(2, 3), 1.0);
    }

    public void test1070_010ContainsInconsistentInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { -1, -2, -3, -5 };
        double[] row3 = { 1, 0, 0, 1 };
        double[] row4 = { 2, 1, 3, 1 };
        double[] row5 = { 1, 0, 0, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        assertEquals(true, e.containsInconsistentInequalities(m));
    }

    public void test1070_020ContainsInconsistentInequalities()
    {
        Matrix m = new Matrix();
        double[] row1 = { 1, 2, 3, 4 };
        double[] row2 = { -1, -2, -3, -4 };
        double[] row3 = { 1, 0, 0, 1 };
        double[] row4 = { 2, 1, 3, 1 };
        double[] row5 = { 1, 0, 0, 1 };
        m.addRowAtIndex(0, row1);
        m.addRowAtIndex(1, row2);
        m.addRowAtIndex(2, row3);
        m.addRowAtIndex(3, row4);
        m.addRowAtIndex(4, row5);
        assertEquals(false, e.containsInconsistentInequalities(m));
    }
}
