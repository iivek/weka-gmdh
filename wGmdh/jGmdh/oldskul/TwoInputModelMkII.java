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

import Jama.Matrix;
import wGmdh.jGmdh.oldskul.measures.Performance;
import wGmdh.jGmdh.exceptions.*;
import java.util.ArrayList;
import java.util.Iterator;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import weka.core.Instances;

/**
 * Polynomial GMDH node with two inputs, by L2 fitting. Output function is of
 * the form a1*x1*x2 + a2*x1 + a3*x2 + a4.
 *
 * @author ivek
 */
public class TwoInputModelMkII extends TwoInputModel {

    private static final long serialVersionUID = 2055852643346433698L;

    private TwoInputModelMkII() {
    }
    ;

    /**
     *
     * @param links
     */
    public TwoInputModelMkII(Node... links) {
        super(links);
    }

    /**
     *
     * @param regressionGoal
     * @param selectionCriterion
     * @param errorMeasure
     * @param inputL
     * @param inputR
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    public TwoInputModelMkII(ArrayList<double[]> regressionGoal, Performance selectionCriterion,
            Performance errorMeasure, Node inputL, Node inputR) throws TooBig, ExpressionEqualToZero, TooSmall {

        super(regressionGoal, selectionCriterion, errorMeasure, inputL, inputR);
    }

    /**
     *
     * @param regressionGoals
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    public TwoInputModelMkII(double[] regressionGoals,
            Performance selectionCriterion,
            Performance errorMeasure,
            Node... links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        /*this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = new ArrayList<Node>(Arrays.asList(links));

        coeffs.add(0, coeffsFromData(regressionGoals, this.collectTrainingData(0)));
        this.trainSetOutput.add(localOuputOnArray(this.collectTrainingData(0), coeffs.get(0)));

        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
        this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
         */
        super(regressionGoals, selectionCriterion, errorMeasure, links);
    }

    /**
     * Sets up performance criteria and links. Does not determine coefficients
     * and measure quality of fit.
     *
     * @param selectionCriterion
     * @param errorMeasure
     * @param inputs
     * @throws wGmdh.jGmdh.exceptions.TooBig
     * @throws wGmdh.jGmdh.exceptions.ExpressionEqualToZero
     * @throws wGmdh.jGmdh.exceptions.TooSmall
     */
    public TwoInputModelMkII(Performance selectionCriterion,
            Performance errorMeasure, Node... inputs) throws TooBig, ExpressionEqualToZero, TooSmall {

        // delegate parameters upward
        super(selectionCriterion, errorMeasure, inputs);
    }

    /**
     * Least squares fitting of basic GMDH building-block, a polynomial
     * P(trainingData[0],trainingData[1]) of form a1*x1*x2 + a2*x1 + a3*x2 + a4.
     * Coefficients of the polynomial are treated as unknowns.
     *
     * @param regressTo       values we are fitting to
     * @param trainingData
     * @return coefficients of polynomial
     */
    @Override
    protected double[] coeffsFromData(
            double[] regressTo, double[][] trainingData) throws TooBig {
        if (trainingData.length != 2) {
            throw new TooBig("Number of variables different than 2");
        }

        if (trainingData[0].length != trainingData[1].length) {
            throw new TooBig("trainingData[0].length != trainingData[1].length");
        }

        int numPoints = trainingData[0].length;
        /*
         * mA    Matrix of equation coefficients that multiply unknowns
         * mB_tr Transposed vector of right-side regressTos
         */
        double[][] mA = new double[4][4];
        double[] mB_tr = new double[4];
        /*
         * Transposed vector of coefficients of the polynomial
         */
        double[] mX_tr = new double[4];

        mA[0][0] = numPoints;
        for (int i = 0; i < numPoints; ++i) {
            // double x = trainingData[0][i];
            // double y = trainingData[1][i];
            double xx = trainingData[0][i] * trainingData[0][i];
            double xy = trainingData[0][i] * trainingData[1][i];
            double yy = trainingData[1][i] * trainingData[1][i];

            mB_tr[0] += regressTo[i];
            mB_tr[1] += regressTo[i] * trainingData[0][i];
            mB_tr[2] += regressTo[i] * trainingData[1][i];
            mB_tr[3] += regressTo[i] * xy;

            mA[0][1] += trainingData[0][i];
            mA[0][2] += trainingData[1][i];
            mA[0][3] += xy;
            mA[1][1] += xx;
            mA[1][3] += xx * trainingData[1][i];
            mA[2][2] += yy;
            mA[2][3] += trainingData[0][i] * yy;
            mA[3][3] += xx * yy;
        }
        mA[1][2] = mA[0][3];

        mA[1][0] = mA[0][1];
        mA[2][1] = mA[1][2];
        mA[2][0] = mA[0][2];
        mA[2][1] = mA[1][2];
        mA[3][0] = mA[0][3];
        mA[3][1] = mA[1][3];
        mA[3][2] = mA[2][3];

        /*
         * Now we have a system of linear equations to solve
         */
        /*                LinearEqSystem regressionSystem =
        new LinearEqSystem(mA, mB_tr, mX_tr);
        //regressionSystem.uglyOutMatrices();

        return regressionSystem.gaussElimination();
         */

        /* let's use JAMA instead
         */
        Matrix mA_matrix = new Matrix(mA);
        double[][] mB_tr_2D = new double[4][1];
        mB_tr_2D[0][0] = mB_tr[0];
        mB_tr_2D[1][0] = mB_tr[1];
        mB_tr_2D[2][0] = mB_tr[2];
        mB_tr_2D[3][0] = mB_tr[3];
        Matrix mB_tr_matrix = new Matrix(mB_tr_2D);
        Matrix mX_tr_matrix = null;
        try {
            mX_tr_matrix = mA_matrix.solve(mB_tr_matrix);
        } catch (RuntimeException ex) {
            this.setIllConditioned(true);
//            System.out.println("ID: " + getIdentifier() +
//                    " matrix ill-conditioned");
        }
        if (!isIllConditioned()) {
            double[][] solution = mX_tr_matrix.getArray();
            mX_tr[0] = solution[0][0];
            mX_tr[1] = solution[1][0];
            mX_tr[2] = solution[2][0];
            mX_tr[3] = solution[3][0];
        } else {
            // mark the coeffs as NaNs
            mX_tr[0] = Double.NaN;
            mX_tr[1] = Double.NaN;
            mX_tr[2] = Double.NaN;
            mX_tr[3] = Double.NaN;
        }

        return mX_tr;
    }

    /**
     * Calculates the GMDH model output given an array of immediate inputs
     *
     * @param inputs   array of length of polynomial inputs (number of
     *                 inputs is 2)
     * @param coefficients
     * @return         coefficients[0] +
     *                 coefficients[1] * inputs[0][i] +
     *                 coefficients[2] * inputs[1][i] +
     *                 coefficients[3] * inputs[0][i] * inputs[1][i] +
     *                 i ranges from 0 to trainingData[0].length-1 == x2.length-1
     */
    @Override
    public double[] localOuputOnArray(double[][] inputs, double[] coefficients)
            throws TooBig {

        /*        if (coeffs.length != 4) {
        throw new TooBig("coeffs.length != 4);
        }
        if (inputs.length != 2) {
        throw new TooBig("Number of variables different than 2");
        }
         */
        int length = inputs[0].length;
        /*        if (length != inputs[1].length) {
        throw new TooBig("inputs[0].length != inputs[1].length");
        }
         */
        double[] polyValues = new double[length];
        for (int i = 0; i < length; ++i) {
            polyValues[i] = coefficients[0] +
                    coefficients[1] * inputs[0][i] +
                    coefficients[2] * inputs[1][i] +
                    coefficients[3] * inputs[0][i] * inputs[1][i];
        }
        return polyValues;
    }

    /**
     * Calculates this polynomial value given an array of immediate inputs
     * @param inputs    array of two doubles, one for each input
     * @param coefficients
     * @return         coefficients[0] +
     *                 coefficients[1] * inputs[0] +
     *                 coefficients[2] * inputs[1] +
     *                 coefficients[3] * inputs[0] * inputs[0] +
     *                 coefficients[4] * inputs[0] * inputs[1] +
     *                 coefficients[5] * inputs[1] * inputs[1];
     */
    protected double localOutput(double[] inputs, double[] coefficients) {

        /*if (coeffs.length != 4) {
        throw new TooBig("coeffs.length != 4");
        }

        if (inputs.length != 2) {
        throw new TooBig("need exactly 2 inputs");
        }
         */

        double polyValue = coefficients[0] +
                coefficients[1] * inputs[0] +
                coefficients[2] * inputs[1] +
                coefficients[3] * inputs[0] * inputs[1];

        return polyValue;
    }

    @Override
    public String toString(int fold) {
        String output = new String();
        double[] coeffsToShow = this.getCoeffs().get(fold);

        output = output.concat(
                "P" + getIdentifier() + " = ");
        output = output.concat("" + coeffsToShow[0]);
        output = output.concat(" + " + coeffsToShow[1] + "*P" +
                links.get(0).getIdentifier());
        output = output.concat(" + " + coeffsToShow[2] + "*P" +
                links.get(1).getIdentifier());
        output = output.concat(" + " + coeffsToShow[3] + "*P" +
                links.get(0).getIdentifier() +
                "*P" + links.get(1).getIdentifier());
        output = output.concat("\n");
        return output;
    }

    @Override
    public String toHTML(int fold) {
        String output = new String();
        double[] coeffsToShow = this.getCoeffs().get(fold);

        output = output.concat(
                "<html><body><font size = 2><b>P<sub<html>" + getIdentifier() +
                "</sub></b> = ");
        output = output.concat("" + coeffsToShow[0]);
        output = output.concat(" + " + coeffsToShow[1] + "*P<sub>" +
                links.get(0).getIdentifier() + "</sub>");
        output = output.concat(" + " + coeffsToShow[2] + "*P<sub>" +
                links.get(1).getIdentifier() + "</sub>");
        output = output.concat(" + " + coeffsToShow[3] + "*P<sub>" +
                links.get(0).getIdentifier() + "</sub>" +
                "*P<sub>" + links.get(1).getIdentifier() + "</sub>");
        output = output.concat("</font><br>");
        return output;

    }

    public void calculateOutputs(DatasetSupervised handler) {
        // we'll go instance by instance
        Iterator<Instances> learningSets = handler.getLearningSets();
        Iterator<Instances> validationSets = handler.getValidationSets();

        int fold = 0;
        // learningSets' and validationSets' sizes should be the same
        while (learningSets.hasNext()) {
            // iterate through folds

            Instances learn = learningSets.next();
            double[] output = new double[learn.numInstances()];
            for(int i = 0; i < learn.numInstances(); ++i)   {
                output[i] = networkOutput(learn.instance(i).toDoubleArray(), fold);
            }
            // trainsetOutput is empty, so we can use add.
            trainSetOutput.add(output);

            Instances valid = validationSets.next();
            output = new double[valid.numInstances()];
            for(int i = 0; i < valid.numInstances(); ++i)   {
                output[i] = networkOutput(valid.instance(i).toDoubleArray(), fold);
            }
            // validationSetOutput should be empty here
            validationSetOutput.add(output);

            ++fold;
        }

    }
}
