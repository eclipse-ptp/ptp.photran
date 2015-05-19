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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of Fourier-Motzkin elimination with Pugh's dark shadow improvement.
 * 
 * @author Susan Chesnut
 * @edit October 15, 2014 Initial API and implementation
 * 
 */
/*
 * We assume that every inequality in the matrix is of the form (Ax <= b) Determines whether a real
 * solution to a system of linear inequalities exists. Solves the system by projecting it onto a
 * reduced number of unknowns, eliminating one unknown at a time.
 */
public class FourierMotzkinEliminator
{

    /**
     * The Fourier-Motzkin Elimination algorithm
     * @param matrixIn system of linear inequalities on which to find whether a real solution exists
     *            or not
     * @return whether a solution exists or not
     */
    public boolean eliminateForRealSolutions(Matrix matrixIn)
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("eliminateForRealSolutions - matrixIn is empty"); //$NON-NLS-1$

        Matrix unconstrainedMatrix = new Matrix();
        while (unconstrainedMatrix.getNumRows() != matrixIn.getNumRows())
        {
            unconstrainedMatrix = matrixIn.cloneMatrix();
            matrixIn = deleteAllUnconstrainedVariables(matrixIn);
            if (matrixIn.getNumRows() == 0) return true;
        }

        unconstrainedMatrix = null;

        matrixIn = deleteAllUnconstrainedVariables(matrixIn);
        matrixIn = deleteRowsOfAllZeroes(matrixIn);
        if (matrixIn.getNumRows() == 0) return true;
        if (containsInconsistentInequalities(matrixIn)) return false;

        // if 4 columns, eliminatedVar is 3 (subtract 1)
        // but Matrix index starts at 0, so subtract 2 to get correct index
        int eliminatedVar = matrixIn.getNumColumns() - 2;

        // Continue until we have eliminated every variable
        // or we have eliminated every row
        while (eliminatedVar >= 0 && matrixIn.getNumRows() > 0)
        {
            matrixIn = realProjection(matrixIn, eliminatedVar);
            matrixIn = deleteRowsOfAllZeroes(matrixIn);
            if (containsInconsistentInequalities(matrixIn)) return false;
            eliminatedVar--;
        }

        // all variables have been eliminated - a solution exists
        if (matrixIn.getNumRows() == 0)
        {
            return true;
        }

        // simple inequalities exist - all coefficients should be 0
        else
        {
            int numColumns = matrixIn.getNumColumns() - 1;
            for (int i = 0; i < matrixIn.getNumRows(); i++)
            {
                // for all rows left, if the last column is less than 0
                // there cannot be a solution since the matrix is in the form
                // Ax <= b ... b must be 0 or greater
                if (matrixIn.getValueAtMatrixIndex(i, numColumns) < 0) { return false; }
            }
            return true;
        }
    }

    /**
     * The Fourier-Motzkin Elimination integer algorithm
     * @param matrixIn system of linear inequalities on which to find whether a integer solution
     *            exists or not
     * @return whether a solution exists or not
     */
    public boolean eliminateForIntegerSolutions(Matrix matrixIn)
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("eliminateForIntegerSolutions - matrixIn is empty"); //$NON-NLS-1$
        Matrix unconstrainedMatrix = new Matrix();

        boolean inexact = false;
        boolean darkEmpty = false;
        int eliminatedVar = 0;

        // real shadow ... if no real solution, real shadow is empty,
        // and thus there are no integer solutions. At a point,
        // this becomes too time consuming though.
        if (matrixIn.getNumColumns() < 8)
        {
            if (!eliminateForRealSolutions(matrixIn.cloneMatrix())) return false;
        }

        while ((eliminatedVar < (matrixIn.getNumColumns() - 1)) && matrixIn.getNumRows() > 0)
        {
            while (unconstrainedMatrix.getNumRows() != matrixIn.getNumRows())
            {
                unconstrainedMatrix = matrixIn.cloneMatrix();
                matrixIn = deleteAllUnconstrainedVariables(matrixIn);
                if (matrixIn.getNumRows() == 0) return true;
            }

            matrixIn = deleteRedundantInequalities(matrixIn);
            matrixIn = deleteRowsOfAllZeroes(matrixIn);
            if (matrixIn.getNumRows() == 0) return true;
            if (containsInconsistentInequalities(matrixIn)) return false;

            if (isInexactProjection(matrixIn, eliminatedVar)) inexact = true;

            unconstrainedMatrix = matrixIn.cloneMatrix();

            // will use this for next round if !inexact or darkEmpty
            matrixIn = inexactIntegerProjection(matrixIn, eliminatedVar);

            if (inexact && !darkEmpty)
            {
                // will use this for next round
                // don't want to change unconstrainedMatrix here -- would mess up check in next
                // round
                matrixIn = darkShadowIntegerProjection(unconstrainedMatrix.cloneMatrix(),
                    eliminatedVar);

                if (eliminatedVar + 1 < (matrixIn.getNumColumns() - 1))
                {
                    if (isSolutionSetEmpty(getRowsWithCertainVariable(matrixIn, eliminatedVar + 1),
                        eliminatedVar + 1))
                        darkEmpty = true;
                    else
                    { // solution may exist...unlikely to get here
                        if (eliminatedVar + 1 == matrixIn.getNumColumns() - 2) return true;
                    }
                }
            }
            eliminatedVar++;
        }

        // all variables have been eliminated - a solution exists
        if (matrixIn.getNumRows() == 0)
        {
            return true;
        }

        // should only be simple inequalities - all coefficients should be 0
        else
        {
            int numColumns = matrixIn.getNumColumns() - 1;
            for (int i = 0; i < matrixIn.getNumRows(); i++)
            {
                // for all rows left, if the last column is less than 0
                // there cannot be a solution since the matrix is in the form
                // Ax <= b ... b must be 0 or greater
                if (matrixIn.getValueAtMatrixIndex(i, numColumns) < 0 && matrixIn
                    .getValueAtMatrixIndex(i, eliminatedVar - 1) == 0.0) { return false; }
            }
        }

        // Otherwise, there are values to check for a solution
        if (isSolutionSetEmpty(matrixIn, eliminatedVar - 1)) return false;

        // dark shadow is nonempty so an integer solution exists
        if (darkEmpty == false)
            return true;
        else
            return false;
    }

    /**
     * @param matrixIn the matrix being projected on
     * @param eliminatingVar the variable being eliminated
     * @return the matrix produced after doing a single real projection on matrixIn
     */
    public Matrix realProjection(Matrix matrixIn, int eliminatingVar)
    {
        // obtain lower and upper bounding sets - may not encompass all rows
        // sets contain the row numbers of the matrix
        List<Integer> lowerBoundSet = calculateLowerBoundSet(matrixIn, eliminatingVar);
        List<Integer> upperBoundSet = calculateUpperBoundSet(matrixIn, eliminatingVar);

        // if either bounding set is empty, delete anything with the variable
        // being eliminated and start eliminating a new variable
        // i.e., this variable is unconstrained so eliminate another
        if (lowerBoundSet.isEmpty() || upperBoundSet.isEmpty())
        {
            matrixIn = deleteRowsInBoundingSet(matrixIn,
                combineBoundingSets(lowerBoundSet, upperBoundSet));
            return matrixIn;
        }

        // for all rows in the bounding sets, divide the row by
        // the absolute value of that row's eliminating variable's value
        // i.e. divide row A_i by |a_ij|
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            matrixIn.divideRowByColIndex(lowerBoundSet.get(i), eliminatingVar);
        }

        for (int i = 0; i < upperBoundSet.size(); i++)
        {
            matrixIn.divideRowByColIndex(upperBoundSet.get(i), eliminatingVar);
        }

        // compare bounds to derive a new inequality that no longer
        // contains the variable being eliminated - (this variable should become 0.0)
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            for (int k = 0; k < upperBoundSet.size(); k++)
            {
                double[] newRow = matrixIn.addTwoRowsToCreateNewRow(
                    matrixIn.getSingleRow(lowerBoundSet.get(i)),
                    matrixIn.getSingleRow(upperBoundSet.get(k)));
                matrixIn.addRowAtIndex(matrixIn.getNumRows(), newRow);

            }
        }

        // delete rows contained in the bounding sets
        // all inequalities involving eliminatedVar are deleted
        matrixIn = deleteRowsInBoundingSet(matrixIn,
            combineBoundingSets(lowerBoundSet, upperBoundSet));
        matrixIn.trimRowsToSize(); // helps when growth of inequalities tends toward exponential
        return matrixIn;
    }

    /**
     * @param matrixIn the matrix being projected on
     * @param eliminatingVar the variable being eliminated
     * @return the matrix produced after doing a single inexact integer projection on matrixIn
     */
    public Matrix inexactIntegerProjection(Matrix matrixIn, int eliminatingVar)
    {
        // obtain lower and upper bounding sets - may not encompass all rows
        // sets contain the row numbers of the matrix
        List<Integer> lowerBoundSet = calculateLowerBoundSet(matrixIn, eliminatingVar);
        List<Integer> upperBoundSet = calculateUpperBoundSet(matrixIn, eliminatingVar);

        // if either bounding set is empty, delete anything with the variable
        // being eliminated and start eliminating a new variable
        // i.e., this variable is unconstrained so eliminate another
        if (lowerBoundSet.isEmpty() || upperBoundSet.isEmpty())
        {
            matrixIn = deleteRowsInBoundingSet(matrixIn,
                combineBoundingSets(lowerBoundSet, upperBoundSet));
            return matrixIn;
        }

        // for all rows in the bounding sets, divide the row by
        // the absolute value of that row's eliminating variable's value
        // i.e. divide row A_i by |a_ij|
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            double gcd = gcd(matrixIn.getSingleRow(lowerBoundSet.get(i)));
            matrixIn = divideRowForIntegerProjection(matrixIn, lowerBoundSet.get(i), gcd);
        }
        for (int i = 0; i < upperBoundSet.size(); i++)
        {
            double gcd = gcd(matrixIn.getSingleRow(upperBoundSet.get(i)));
            matrixIn = divideRowForIntegerProjection(matrixIn, upperBoundSet.get(i), gcd);
        }

        double akj = 0;
        double aij = 0;
        double[] akjAi = new double[matrixIn.getNumColumns()];
        double[] aijAk = new double[matrixIn.getNumColumns()];

        double[] newRow = new double[matrixIn.getNumColumns()];
        // compare bounds to derive a new inequality that no longer
        // contains the variable being eliminated - (this variable should become 0.0)
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            for (int k = 0; k < upperBoundSet.size(); k++)
            {

                akj = (matrixIn.getSingleRow(upperBoundSet.get(k)))[eliminatingVar];
                aij = (matrixIn.getSingleRow(lowerBoundSet.get(i)))[eliminatingVar];
                akjAi = multiplyArrayByNumber(matrixIn.getSingleRow(lowerBoundSet.get(i)), akj);
                aijAk = multiplyArrayByNumber(matrixIn.getSingleRow(upperBoundSet.get(k)), aij);

                newRow = arraySubtraction(akjAi, aijAk);
                matrixIn.addRowAtIndex(matrixIn.getNumRows(), newRow);
            }
        }

        // delete rows contained in the bounding sets
        // all inequalities involving eliminatedVar are deleted
        matrixIn = deleteRowsInBoundingSet(matrixIn,
            combineBoundingSets(lowerBoundSet, upperBoundSet));
        matrixIn.trimRowsToSize(); // helps when growth of inequalities tends toward exponential
        return matrixIn;
    }

    /**
     * @param matrixIn the matrix being projected on
     * @param eliminatingVar the variable being eliminated
     * @return the matrix produced after doing a single dark shadow projection on matrixIn
     */
    public Matrix darkShadowIntegerProjection(Matrix matrixIn, int eliminatingVar)
    {
        // obtain lower and upper bounding sets - may not encompass all rows
        // sets contain the row numbers of the matrix
        List<Integer> lowerBoundSet = calculateLowerBoundSet(matrixIn, eliminatingVar);
        List<Integer> upperBoundSet = calculateUpperBoundSet(matrixIn, eliminatingVar);

        // if either bounding set is empty, delete anything with the variable
        // being eliminated and start eliminating a new variable
        // i.e., this variable is unconstrained so eliminate another
        if (lowerBoundSet.isEmpty() || upperBoundSet.isEmpty())
        {
            matrixIn = deleteRowsInBoundingSet(matrixIn,
                combineBoundingSets(lowerBoundSet, upperBoundSet));
            return matrixIn;
        }

        // for all rows in the bounding sets, divide the row by
        // the absolute value of that row's eliminating variable's value
        // i.e. divide row A_i by |a_ij|
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            double gcd = gcd(matrixIn.getSingleRow(lowerBoundSet.get(i)));
            matrixIn = divideRowForIntegerProjection(matrixIn, lowerBoundSet.get(i), gcd);
        }
        for (int i = 0; i < upperBoundSet.size(); i++)
        {
            double gcd = gcd(matrixIn.getSingleRow(upperBoundSet.get(i)));
            matrixIn = divideRowForIntegerProjection(matrixIn, upperBoundSet.get(i), gcd);
        }

        // compare bounds to derive a new inequality that no longer
        // contains the variable being eliminated - (this variable should become 0.0)
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            for (int k = 0; k < upperBoundSet.size(); k++)
            {
                double[] newRow = calculateDarkShadow(matrixIn, lowerBoundSet.get(i),
                    upperBoundSet.get(k), eliminatingVar);
                matrixIn.addRowAtIndex(matrixIn.getNumRows(), newRow);
            }
        }

        // delete rows contained in the bounding sets
        // all inequalities involving eliminatedVar are deleted
        matrixIn = deleteRowsInBoundingSet(matrixIn,
            combineBoundingSets(lowerBoundSet, upperBoundSet));
        matrixIn.trimRowsToSize(); // helps when growth of inequalities tends toward exponential
        return matrixIn;
    }

    /**
     * @param matrixIn matrix to be changed
     * @return matrixIn minus any redundant rows redundant meaning they are the same inequality
     */
    public Matrix deleteRedundantInequalities(Matrix matrixIn)
    {
        List<Integer> redundants = new ArrayList<Integer>();
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            for (int k = i + 1; k < matrixIn.getNumRows(); k++)
            {
                if (Arrays.equals(matrixIn.getSingleRow(i), matrixIn.getSingleRow(k))
                    && !redundants.contains(k))
                {
                    redundants.add(k);
                }
            }
        }
        matrixIn = deleteRowsInBoundingSet(matrixIn, redundants);
        return matrixIn;
    }

    /**
     * @param matrixIn matrix to be changed
     * @return matrixIn minus any inconsistent inequalities inconsistent meaning Aj <= bj and Ai <=
     *         bk where Aj = -Ak and -bk > bj
     */
    public Matrix deleteInconsistentInequalities(Matrix matrixIn)
    {
        List<Integer> inconsistent = new ArrayList<Integer>();
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            for (int k = i + 1; k < matrixIn.getNumRows(); k++)
            {
                if (isInconsistentInequality(matrixIn.getSingleRow(i), matrixIn.getSingleRow(k))
                    && !inconsistent.contains(i) && !inconsistent.contains(k))
                {
                    inconsistent.add(i);
                    inconsistent.add(k);
                }
            }
        }
        matrixIn = deleteRowsInBoundingSet(matrixIn, inconsistent);
        return matrixIn;
    }

    /**
     * @param matrixIn
     * @return whether the matrix contains inconsistent inequalities or not inconsistent meaning Aj
     *         <= bj and Ai <= bk where Aj = -Ak and -bk > bj
     */
    public boolean containsInconsistentInequalities(Matrix matrixIn)
    {
        boolean inconsistent = false;
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            for (int k = i + 1; k < matrixIn.getNumRows(); k++)
            {
                if (isInconsistentInequality(matrixIn.getSingleRow(i),
                    matrixIn.getSingleRow(k))) { return true; }
            }
        }
        return inconsistent;
    }

    /**
     * @param row1 Aj - the first row to compare
     * @param row2 Ak - the second row to compare
     * @return whether the two arrays create an inconsistency inconsistent meaning Aj <= bj and Ai
     *         <= bk where Aj = -Ak and -bk > bj
     */
    public boolean isInconsistentInequality(double[] row1, double[] row2)
    {
        for (int i = 0; i < row1.length - 1; i++)
        {
            if (row1[i] != -row2[i]) return false;
        }
        // if it gets here, Aj = -Ak, check if -bk > bj
        if (-row2[row1.length - 1] > row1[row1.length - 1])
            return true;
        else
            return false;
    }

    /**
     * @param matrixIn matrix to be changed
     * @return matrixIn minus any unconstrained variables
     */
    public Matrix deleteAllUnconstrainedVariables(Matrix matrixIn)
    {
        if (matrixIn.getNumRows() == 0) throw new IndexOutOfBoundsException(
            "deleteAllUnconstrainedVariables - matrixIn is empty"); //$NON-NLS-1$

        boolean[] constrainedVars = determineUnconstrainedVariables(matrixIn);
        for (int i = 0; i < constrainedVars.length; i++)
        {
            if (constrainedVars[i])
            {
                matrixIn = deleteUnconstrainedVariable(matrixIn, i);
            }
        }
        matrixIn.trimRowsToSize();
        return matrixIn;
    }

    /**
     * TODO don't need Sorts matrixIn based on the contents of the bounding sets Puts any rows
     * belonging to either of the sets at the top of the matrix
     * @param matrixIn matrix to be sorted
     * @param lowerBoundSet
     * @param upperBoundSet
     * @return the sorted matrix
     */
    public Matrix sortMatrixByBoundingSetContents(Matrix matrixIn, List<Integer> lowerBoundSet,
        List<Integer> upperBoundSet)
    {
        if (matrixIn.getNumRows() == 0) throw new IndexOutOfBoundsException(
            "sortMatrixByBoundingSetContents - matrixIn is empty"); //$NON-NLS-1$

        Matrix sortedMatrix = new Matrix();
        for (Integer i = 0; i < matrixIn.getNumRows(); i++)
        {
            // if the bounding sets contain the value, put it at the top of the matrix
            // and push every row down
            if (lowerBoundSet.contains(i) || upperBoundSet.contains(i))
            {
                // will push every other entry down
                sortedMatrix.addRowAtIndex(0, matrixIn.getSingleRow(i));
            }
            // otherwise, put it at the bottom of the matrix
            else
                sortedMatrix.addRowAtIndex(sortedMatrix.getNumRows(), matrixIn.getSingleRow(i));
        }
        return sortedMatrix;
    }

    /**
     * TODO don't need Deletes n=size rows from the top of the matrix. Use this with
     * sortMatrixByBoundingSetContents to delete bounding sets U and L
     * @param matrixIn
     * @param size number of rows to delete
     * @return this matrix minus the top n=size rows
     * @throws IndexOutOfBoundsException
     */
    public Matrix deleteRowsFromSortedMatrix(Matrix matrixIn, int size)
        throws IndexOutOfBoundsException
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("deleteRowsFromSortedMatrix - matrixIn is empty"); //$NON-NLS-1$

        if (size > matrixIn.getNumRows() || size < 0)
            throw new IndexOutOfBoundsException("deleteRowsFromSortedMatrix - size invalid"); //$NON-NLS-1$

        // delete rows from the top of the matrix
        for (int i = 0; i < size; i++)
        {
            matrixIn.deleteRow(0);
        }
        return matrixIn;
    }

    /**
     * @param matrixIn
     * @param colIndex the variable we are eliminating from matrixIn
     * @return a list of integers that contain the row indices for a lower bounding set L = { i |
     *         a_ij < 0 }
     */
    public List<Integer> calculateLowerBoundSet(Matrix matrixIn, int colIndex)
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("calculateLowerBoundSet - matrixIn is empty"); //$NON-NLS-1$

        if (colIndex < 0 || colIndex >= matrixIn.getNumColumns())
            throw new IndexOutOfBoundsException("calculateLowerBoundSet - colIndex out of bounds"); //$NON-NLS-1$

        List<Integer> lowerBound = new ArrayList<Integer>();
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            if (matrixIn.getValueAtMatrixIndex(i, colIndex) < 0)
            {
                lowerBound.add(i);
            }
        }
        return lowerBound;
    }

    /**
     * colIndex is the variable we are eliminating from matrixIn
     * @param matrixIn
     * @param colIndex
     * @return a list of integers that contain the row indices for an upper bounding set U = { i |
     *         a_ij > 0 }
     */
    public List<Integer> calculateUpperBoundSet(Matrix matrixIn, int colIndex)
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("calculateUpperBoundSet - matrixIn is empty"); //$NON-NLS-1$

        if (colIndex < 0 || colIndex >= matrixIn.getNumColumns())
            throw new IndexOutOfBoundsException("calculateUpperBoundSet - colIndex out of bounds"); //$NON-NLS-1$

        List<Integer> upperBound = new ArrayList<Integer>();
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            if (matrixIn.getValueAtMatrixIndex(i, colIndex) > 0)
            {
                upperBound.add(i);
            }
        }
        return upperBound;
    }

    /**
     * @param matrixIn matrix to be changed
     * @return unconstrained an array describing whether a variable is constrained
     */
    public boolean[] determineUnconstrainedVariables(Matrix matrixIn)
    {
        if (matrixIn.getNumRows() == 0) return null;

        int numOfVariables = matrixIn.getNumColumns() - 1;
        boolean[] unconstrained = new boolean[numOfVariables];

        for (int i = 0; i < numOfVariables; i++)
        {
            if (calculateLowerBoundSet(matrixIn, i).isEmpty()
                || calculateUpperBoundSet(matrixIn, i).isEmpty()) unconstrained[i] = true;
        }
        return unconstrained;
    }

    /**
     * @param matrixIn matrix to be changed
     * @param unconstrainedIndex the column index of an unconstrained variable
     * @return matrixIn minus any row containing a non-zero value in the column "unconstrainedIndex"
     */
    public Matrix deleteUnconstrainedVariable(Matrix matrixIn, int unconstrainedIndex)
    {
        if (matrixIn.getNumRows() == 0) return matrixIn;

        if (unconstrainedIndex < 0 || unconstrainedIndex >= matrixIn.getNumColumns())
            throw new IndexOutOfBoundsException(
                "deleteUnconstrainedVariable - unconstrainedIndex out of bounds"); //$NON-NLS-1$

        List<Integer> lowerBoundSet = null;
        List<Integer> upperBoundSet = null;

        lowerBoundSet = calculateLowerBoundSet(matrixIn, unconstrainedIndex);
        upperBoundSet = calculateUpperBoundSet(matrixIn, unconstrainedIndex);

        matrixIn = deleteRowsInBoundingSet(matrixIn,
            combineBoundingSets(lowerBoundSet, upperBoundSet));

        return matrixIn;
    }

    /**
     * @param a
     * @param b
     * @return the gcd of a and b
     */
    public double gcd(double a, double b)
    {
        while (b > 0)
        {
            double temp = b;
            b = a % b; // % is remainder
            a = temp;
        }
        return a;
    }

    /**
     * @param input
     * @return the gcd of all the elements in input
     */
    public double gcd(double[] input)
    {
        double result = Math.abs(input[0]);
        for (int i = 1; i < input.length - 1; i++)
            result = gcd(result, Math.abs(input[i]));
        return Math.abs(result);
    }

    /**
     * Multiply the values in the array by the multiplier
     * @param arrayIn
     * @param multiplier
     * @return arrayIn the array with values multiplied
     */
    public double[] multiplyArrayByNumber(double[] arrayIn, double multiplier)
    {
        double[] multArray = arrayIn.clone();
        for (int i = 0; i < multArray.length; i++)
        {
            multArray[i] = multArray[i] * multiplier;
            if (multArray[i] == Double.POSITIVE_INFINITY || multArray[i] == Double.NEGATIVE_INFINITY
                || multArray[i] == Double.NaN) { throw new ArithmeticException(
                    "multiplyArrayByNumber - overflow or NaN result"); } //$NON-NLS-1$
        }
        return multArray;
    }

    /**
     * Subtracts the second array's values from the first array
     * @param biggerArray first operand in subtraction
     * @param smallerArray second operand in subtraction
     * @return resulting array
     */
    public double[] arraySubtraction(double[] biggerArray, double[] smallerArray)
    {
        for (int i = 0; i < biggerArray.length; i++)
        {
            biggerArray[i] = biggerArray[i] - smallerArray[i]; // possible over/underflow here.
                                                               // unlikely
        }
        return biggerArray;
    }

    /**
     *
     * @param solutionSpace
     * @param elimVar
     * @return whether a given variable in solutionSpace potentially has a solution. Only returns
     *         true if it definitely does not have a solution. False does not mean that no solutions
     *         exist. Other variables could affect it and are not taken into account here.
     */
    public boolean isSolutionSetEmpty(Matrix solutionSpace, int elimVar)
    {
        if (solutionSpace.getNumRows() == 0) return true;
        int numberOfEquations = solutionSpace.getNumRows();
        ArrayList<Double> upperBoundValues = new ArrayList<Double>();
        ArrayList<Double> lowerBoundValues = new ArrayList<Double>();
        double[] equation;
        double lastNum = 0.0;
        for (int i = 0; i < numberOfEquations; i++)
        {
            equation = solutionSpace.getSingleRow(i);
            lastNum = equation[equation.length - 1]; // last number in equation
            if (multipleNonZeroValuesExistInArrayExcludingLastPlace(equation))
                return false;

            else
            {
                if (equation[elimVar] != 0) lastNum = lastNum / equation[elimVar];

                if (lastNum <= 0 && equation[elimVar] < 0)
                    lowerBoundValues.add(lastNum);
                else if (lastNum > 0 && equation[elimVar] < 0)
                    lowerBoundValues.add(lastNum);
                else
                    upperBoundValues.add(lastNum);
            }
        }
        Collections.sort(upperBoundValues);
        Collections.sort(lowerBoundValues);
        if (upperBoundValues.size() > 0 && lowerBoundValues.size() > 0)
        {
            double upperBound = upperBoundValues.get(0);
            double lowerBound = lowerBoundValues.get(lowerBoundValues.size() - 1);
            if ((upperBound - lowerBound) >= 1)
                return false;
            else
                return true;
        }
        else
            return false;
    }

    /**
     * @param arrayIn
     * @return whether arrayIn contains multiple non-zero values (exluding last column)
     */
    private boolean multipleNonZeroValuesExistInArrayExcludingLastPlace(double[] arrayIn)
    {
        int numNonZeroValues = 0;
        for (int i = 0; i < arrayIn.length - 1; i++)
        {
            if (arrayIn[i] != 0) numNonZeroValues++;
        }
        if (numNonZeroValues > 1)
            return true;
        else
            return false;
    }

    /**
     * @param matrixIn
     * @param colNum
     * @return rowsWithVar a matrix with all rows from matrixIn where the value at colNum was
     *         non-zero
     */
    private Matrix getRowsWithCertainVariable(Matrix matrixIn, int colNum)
    {
        Matrix rowsWithVar = new Matrix();
        for (int i = 0; i < matrixIn.getNumRows() - 1; i++)
        {
            if (matrixIn.getSingleRow(i)[colNum] != 0)
                rowsWithVar.addRowAtIndex(rowsWithVar.getNumRows(), matrixIn.getSingleRow(i));
        }
        return rowsWithVar;
    }

    /**
     * @param matrixIn
     * @param elimVar the variable to determine if inexact projection
     * @return whether the projection on elimVar is inexact or not
     */
    private boolean isInexactProjection(Matrix matrixIn, int elimVar)
    {
        double inexactCond = 0;
        for (int i = 0; i < matrixIn.getNumRows() - 1; i++)
        {
            for (int k = i + 1; k < matrixIn.getNumRows() - 1; k++)
            {
                inexactCond = matrixIn.getSingleRow(i)[elimVar] * matrixIn.getSingleRow(k)[elimVar];
                if (Math.abs(inexactCond) > 1) return true;
            }
        }
        return false;
    }

    /**
     * @param matrixIn
     * @param rowIndex
     * @param divisor
     * @return matrixIn a matrix with the designated row's value divided by divisor
     */
    public Matrix divideRowForIntegerProjection(Matrix matrixIn, int rowIndex, double divisor)
    {
        int lastColIndex = matrixIn.getSingleRow(rowIndex).length - 1;

        // divide row A_i by divisor (won't get last column)
        for (int i = 0; i < lastColIndex; i++)
        {
            if (matrixIn.getSingleRow(rowIndex)[i] != 0 && (matrixIn.getSingleRow(rowIndex)[i]
                / divisor) == 0.0) { throw new ArithmeticException(
                    "divideRowForIntegerProjection - underflow"); } //$NON-NLS-1$
            matrixIn.getSingleRow(rowIndex)[i] = matrixIn.getSingleRow(rowIndex)[i] / divisor;
            if (matrixIn.getSingleRow(rowIndex)[i] == Double.POSITIVE_INFINITY
                || matrixIn.getSingleRow(rowIndex)[i] == Double.NEGATIVE_INFINITY
                || matrixIn
                    .getSingleRow(rowIndex)[i] == Double.NaN) { throw new ArithmeticException(
                        "divideRowForIntegerProjection - overflow or NaN result"); } //$NON-NLS-1$
        }

        // let b_i = floor( b_i / divisor )
        matrixIn.getSingleRow(rowIndex)[lastColIndex] = Math
            .floor(matrixIn.getSingleRow(rowIndex)[lastColIndex] / divisor);

        return matrixIn;
    }

    /**
     * Determines the dark shadow on two rows in regards to the eliminating variable
     * @param matrixIn
     * @param lowerBoundSetVal a row index in matrixIn
     * @param upperBoundSetVal a row index in matrixIn
     * @param elimVar
     * @return newRow the dark shadow inequality
     */
    public double[] calculateDarkShadow(Matrix matrixIn, int lowerBoundSetVal, int upperBoundSetVal,
        int elimVar)
    {
        double akj = (matrixIn.getSingleRow(upperBoundSetVal))[elimVar];
        double aij = (matrixIn.getSingleRow(lowerBoundSetVal))[elimVar];
        double[] akjAi = multiplyArrayByNumber(matrixIn.getSingleRow(lowerBoundSetVal).clone(),
            akj);
        double[] aijAk = multiplyArrayByNumber(matrixIn.getSingleRow(upperBoundSetVal).clone(),
            aij);
        double akjbi = akj * matrixIn.getSingleRow(lowerBoundSetVal)[matrixIn.getNumColumns() - 1];
        if (akjbi == Double.POSITIVE_INFINITY || akjbi == Double.NEGATIVE_INFINITY
            || akjbi == Double.NaN) { throw new ArithmeticException(
                "calculateDarkShadow - overflow or NaN result for akjbi"); } //$NON-NLS-1$
        double aijbk = aij * matrixIn.getSingleRow(upperBoundSetVal)[matrixIn.getNumColumns() - 1];
        if (aijbk == Double.POSITIVE_INFINITY || aijbk == Double.NEGATIVE_INFINITY
            || aijbk == Double.NaN) { throw new ArithmeticException(
                "calculateDarkShadow - overflow or NaN result for aijbk"); } //$NON-NLS-1$
        double akjaij = akj * aij;
        if (akjaij == Double.POSITIVE_INFINITY || akjaij == Double.NEGATIVE_INFINITY
            || akjaij == Double.NaN) { throw new ArithmeticException(
                "calculateDarkShadow - overflow or NaN result for akjaij"); } //$NON-NLS-1$

        double newB = akjbi - aijbk + akjaij + akj - aij - 1;
        double[] newRow = arraySubtraction(akjAi, aijAk);
        newRow[matrixIn.getNumColumns() - 1] = newB;
        return newRow;
    }

    /**
     * @param matrixIn
     * @param combinedSet the combined set of the lowerbound and upperbound sets (comes from
     *            combineBoundingSets)
     * @return matrixIn minus the rows with row indexes in combinedSet
     */
    public Matrix deleteRowsInBoundingSet(Matrix matrixIn, List<Integer> combinedSet)
    {
        if (matrixIn.getNumRows() == 0)
            throw new IndexOutOfBoundsException("deleteRowsInBoundingSet - matrixIn is empty"); //$NON-NLS-1$

        int numDeleted = 0;
        for (int i = 0; i < combinedSet.size(); i++)
        {
            matrixIn.deleteRow(combinedSet.get(i) - numDeleted);
            numDeleted++;
        }
        return matrixIn;
    }

    /**
     * @param matrixIn
     * @return matrixIn minus any rows that were full of zeroes
     */
    public Matrix deleteRowsOfAllZeroes(Matrix matrixIn)
    {
        int numDeleted = 0;
        for (int i = 0; i < matrixIn.getNumRows(); i++)
        {
            if (matrixIn.isRowFullOfZeroes(matrixIn.getSingleRow(i)))
            {
                matrixIn.deleteRow(i - numDeleted);
                numDeleted++;
            }
        }
        return matrixIn;
    }

    /**
     * Combines the values in two List<Integer> objects
     * @param lowerBoundSet
     * @param upperBoundSet
     * @return a List containing the values from both lowerBoundSet and uppserBoundSet in sorted
     *         order
     */
    public List<Integer> combineBoundingSets(List<Integer> lowerBoundSet,
        List<Integer> upperBoundSet)
    {
        List<Integer> combinedList = new ArrayList<Integer>();
        for (int i = 0; i < lowerBoundSet.size(); i++)
        {
            combinedList.add(lowerBoundSet.get(i));
        }

        for (int i = 0; i < upperBoundSet.size(); i++)
        {
            combinedList.add(upperBoundSet.get(i));
        }
        Collections.sort(combinedList);
        return combinedList;
    }

}