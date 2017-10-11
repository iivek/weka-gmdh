/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Copyright (C) 2010 Ivan Ivek
 */

package wGmdh.jGmdh.oldskul;

/**
 * @author ivek
 */

/*
 * TODO: a situation may occur where we divide by zero. Exception? Or are we
 * satisfied with +Inf, -Inf, and NaNs?
 */
public class LinearEqSystem {

    public double[][] mA;   // Matrix of coefficients
    public double[] mB_tr;  // Transposed right-side values vector
    public double[] mX_tr;  // Transposed vector of unknowns

    public LinearEqSystem() {
        this.mA = null;
        this.mB_tr = null;
        this.mX_tr = null;
    }

    public LinearEqSystem(double[][] mA, double[] mB_tr, double[] mX_tr) {
        this.mA = mA;
        this.mB_tr = mB_tr;
        this.mX_tr = mX_tr;
    }

    public LinearEqSystem(int nrEquations) {
        this.mA = new double[nrEquations][nrEquations];
        this.mB_tr = new double[nrEquations];
        this.mX_tr = new double[nrEquations];
    }

    /**
     * Determines the largest element of mA in pivot column. Then switches
     * the rows making the row containing that largest element a pivot row.
     */
    private int partialPivot(int pivotCol) {
        double largestVal = Math.abs(mA[pivotCol][pivotCol]);
        int largestRow = pivotCol;
        for (int j = pivotCol; j < mA.length; ++j) {
            if (Math.abs(mA[j][pivotCol]) > largestVal) {
                largestVal = mA[j][pivotCol];
                largestRow = j;
            }
        }
        
        if(largestVal == 0) {
            /* mA is singular
             */
        }
        
        /*
         * Switches rows and updates swaps
         */
        if (largestRow != pivotCol) {
            double[] tempRow = new double[mA[pivotCol].length];
            System.arraycopy(mA[pivotCol], 0, tempRow, 0,
                    mA[pivotCol].length);
            System.arraycopy(mA[largestRow], 0, mA[pivotCol], 0,
                    mA[largestRow].length);
            System.arraycopy(tempRow, 0, mA[largestRow], 0, tempRow.length);
            //
            tempRow[0] = mB_tr[pivotCol];
            mB_tr[pivotCol] = mB_tr[largestRow];
            mB_tr[largestRow] = tempRow[0];
        }
        return largestRow;
    }

    /**
     * Eliminates all elements below pivot, without normalizing the current
     * pivot row).
     */
    private void forwardElimination(int pivotCol) {
        double factor;
        for (int i = pivotCol + 1; i < mA.length; ++i) {
            factor = mA[i][pivotCol] / mA[pivotCol][pivotCol];
            for (int j = pivotCol; j < mA[0].length; ++j) {
                mA[i][j] -= factor * mA[pivotCol][j];
            }
            mB_tr[i] -= factor * mB_tr[pivotCol];
        }
    }

    private void backSubstitution() {
        for (int j = mA[0].length - 1; j > -1; j--) {
            mX_tr[j] = mB_tr[j] / mA[j][j];
            for (int i = j; i > -1; i--) {
                mB_tr[i] -= mX_tr[j] * mA[i][j];
            }
        }
    }

    /**
     * Solves system of linear equations by Gauss elimination method, using
     * partial pivoting, forward elimination and back substitution.
     * In-place method, i.e. changes mA, mB_tr and mX_tr.
     */
    public double[] gaussElimination() {
        for (int i = 0; i < mA.length; ++i) {
            partialPivot(i);
            forwardElimination(i);
        }
        backSubstitution();
        return mX_tr;
    }

    public void uglyOutMatrices() {
        for (int i = 0; i < mA.length; ++i) {
            for (int j = 0; j < mA[0].length; ++j) {
                System.out.print(mA[i][j] + "  ");
            }
            System.out.print("\n");
        }
        System.out.print("\n");
        //
        for (int i = 0; i < mB_tr.length; ++i) {
            System.out.println(mB_tr[i]);
        }
        System.out.print("\n");
        //
        for (int i = 0; i < mX_tr.length; ++i) {
            System.out.println(mX_tr[i]);
        }
        System.out.print("\n");
    }
}
