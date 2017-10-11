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
import wGmdh.jGmdh.oldskul.Node.CheckVisited;
import wGmdh.jGmdh.oldskul.measures.Performance;
import wGmdh.jGmdh.exceptions.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.hybrid.SseRegressionEquations;
import wGmdh.jGmdh.hybrid.SseRegressionEquations.Summand;

/**
 * Polynomial GMDH node with two inputs, by L2 fitting
 *
 * @author ivek
 */
public class TwoInputModel extends Model {

    private static final long serialVersionUID = -7477320438539630486L;

    // marks if the linear system that produced this model had some kind of
    // stability issues (ill - conditioned or a singularity)
    private boolean illConditioned = false;

    private TwoInputModel() {
    }
    ;

    /**
     *
     * @param links
     */
    public TwoInputModel(Node... links) {
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
    public TwoInputModel(ArrayList<double[]> regressionGoal, Performance selectionCriterion,
            Performance errorMeasure, Node inputL, Node inputR) throws TooBig, ExpressionEqualToZero, TooSmall {

        super(regressionGoal, null, selectionCriterion, errorMeasure, inputL, inputR);
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
    public TwoInputModel(double[] regressionGoals,
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
    public TwoInputModel(Performance selectionCriterion,
            Performance errorMeasure, Node... inputs) throws TooBig, ExpressionEqualToZero, TooSmall {

        // delegate parameters upward
        super(selectionCriterion, errorMeasure, inputs);
    }

    /**
     * Least squares fitting of basic GMDH building-block, a (second-order)
     * polynomial P(trainingData[0],trainingData[1]).
     * Coefficients of the polynomial are treated as unknowns.
     *
     * @param regressTo       values we are fitting to
     * @param trainingData
     * @return coefficients of polynomial
     */
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
        double[][] mA = new double[6][6];
        double[] mB_tr = new double[6];
        /*
         * Transposed vector of coefficients of the polynomial
         */
        double[] mX_tr = new double[6];

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
            mB_tr[3] += regressTo[i] * xx;
            mB_tr[4] += regressTo[i] * xy;
            mB_tr[5] += regressTo[i] * yy;

            mA[0][1] += trainingData[0][i];
            mA[0][2] += trainingData[1][i];
            mA[0][3] += xx;
            mA[0][4] += xy;
            mA[0][5] += yy;
            mA[1][3] += xx * trainingData[0][i];
            mA[1][4] += xx * trainingData[1][i];
            mA[1][5] += xy * trainingData[1][i];
            mA[2][5] += yy * trainingData[1][i];
            mA[3][3] += xx * xx;
            mA[3][4] += xx * xy;
            mA[3][5] += xx * yy;
            mA[4][5] += xy * yy;
            mA[5][5] += yy * yy;
        }
        mA[1][0] = mA[0][1];
        mA[1][1] = mA[0][3];
        mA[1][2] = mA[0][4];
        mA[2][0] = mA[0][2];
        mA[2][1] = mA[1][2];
        mA[2][2] = mA[0][5];
        mA[2][3] = mA[1][4];
        mA[2][4] = mA[1][5];
        mA[3][0] = mA[0][3];
        mA[3][1] = mA[1][3];
        mA[3][2] = mA[2][3];
        mA[4][0] = mA[0][4];
        mA[4][1] = mA[1][4];
        mA[4][2] = mA[2][4];
        mA[4][3] = mA[3][4];
        mA[4][4] = mA[3][5];
        mA[5][0] = mA[0][5];
        mA[5][1] = mA[1][5];
        mA[5][2] = mA[2][5];
        mA[5][3] = mA[3][5];
        mA[5][4] = mA[4][5];


        /*
         * Now we have a system of linear equations to solve
         */
        /*        LinearEqSystem regressionSystem =
        new LinearEqSystem(mA, mB_tr, mX_tr);

        return regressionSystem.gaussElimination();
         */

        /* let's use JAMA instead
         */
        Matrix mA_matrix = new Matrix(mA);
        double[][] mB_tr_2D = new double[6][1];
        mB_tr_2D[0][0] = mB_tr[0];
        mB_tr_2D[1][0] = mB_tr[1];
        mB_tr_2D[2][0] = mB_tr[2];
        mB_tr_2D[3][0] = mB_tr[3];
        mB_tr_2D[4][0] = mB_tr[4];
        mB_tr_2D[5][0] = mB_tr[5];
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
            mX_tr[4] = solution[4][0];
            mX_tr[5] = solution[5][0];
        } else {
            // mark the coeffs as NaNs
            mX_tr[0] = Double.NaN;
            mX_tr[1] = Double.NaN;
            mX_tr[2] = Double.NaN;
            mX_tr[3] = Double.NaN;
            mX_tr[4] = Double.NaN;
            mX_tr[5] = Double.NaN;
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
     *                 coefficients[3] * inputs[0][i] * inputs[0][i] +
     *                 coefficients[4] * inputs[0][i] * inputs[1][i] +
     *                 coefficients[5] * inputs[1][i] * inputs[1][i];, where
     *                 i ranges from 0 to trainingData[0].length-1 == x2.length-1
     */
    public double[] localOuputOnArray(double[][] inputs, double[] coefficients)
            throws TooBig {

        /*        if (coeffs.length != 6) {
        throw new TooBig("coeffs.length != 6");
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
                    coefficients[3] * inputs[0][i] * inputs[0][i] +
                    coefficients[4] * inputs[0][i] * inputs[1][i] +
                    coefficients[5] * inputs[1][i] * inputs[1][i];
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

        /*if (coeffs.length != 6) {
        throw new TooBig("coeffs.length != 6");
        }

        if (inputs.length != 2) {
        throw new TooBig("need exactly 2 inputs");
        }
         */

        double polyValue = coefficients[0] +
                coefficients[1] * inputs[0] +
                coefficients[2] * inputs[1] +
                coefficients[3] * inputs[0] * inputs[0] +
                coefficients[4] * inputs[0] * inputs[1] +
                coefficients[5] * inputs[1] * inputs[1];

        return polyValue;
    }

    /**
     * @return the illConditioned
     */
    public boolean isIllConditioned() {
        return illConditioned;
    }

    /**
     * @param illConditioned the illConditioned to set
     */
    public void setIllConditioned(boolean illConditioned) {
        this.illConditioned = illConditioned;
    }

    /**
     * Uses a Hashtable to store pairs of Nodes and inputs for which they
     * have calculated the output, to avoid multiple calculations of the same
     * thing. Hashtable enables calculation if the outputs in paralell,
     * on different Nodes, and for different inputs. (In contrast, take a single
     * boolean flag per Node. This would allow us to mark visited subnodes in a
     * single traversal, for a single input point, making computation in
     * paralell impossible.)
     */
    public class VisitedHt implements CheckVisited, Serializable {

        private static final long serialVersionUID = 7697389884829550429L;

        /* Used to store and distribute outputs given an input. Use it if you
         * want to compute the outputs in parallel. More heap used up tho. Sry but jbg.
         */
        private Hashtable pairs = new Hashtable(1);
        private double[] inputs;
        private double output;

        /**
         * @return  true if this.inputs and this.outputs exist as an entry
         * in our hashmap
         */
        public boolean isVisited() {
            return pairs.containsKey(inputs);
        }

        /**
         * Store the pair obtained in feed() to our Hashtable
         */
        public void onVisit() {
            pairs.put(inputs, new Double(output));
        }

        /**
         * Makes us forget everything we calculated.
         */
        public void resetVisited() {
            pairs.clear();
            setInputs(null);
        }

        public boolean isReset() {
            return pairs.isEmpty();

        }

        /**
         * Call it before checking with isVisited() or calling onVisit() or
         * returnOutput() to get  to know the inputs we're delaing with
         *
         * @param inputs the inputs to set
         */
        public void setInputs(double[] inputs) {
            this.inputs = inputs;
        }

        /**
         * Call it before checking with calling onVisit() to get
         * to know the outputs we're delaing with

         * @param output the output to set
         */
        public void setOutput(double output) {
            this.output = output;
        }

        /**
         * In case we already have been visited, return the output given
         * this.inputs.
         *
         * @return
         */
        public double returnOutput() {
            return (Double) pairs.get(inputs);
        }
    }
    public VisitedHt visitinfo = new VisitedHt();

    /**
     * Calculate total output of this GMDH model, recursively, given an instance.
     *
     * Note: Models that share the same layer can be have their outputs
     * calculated in parallel. Procesing of an array of inputs can also be
     * optimized. (Athough, Weka doesn't have a classifyInstance(Instance[]), only
     * classifyInstance(Instance)).
     *
     * @param inputs    instance data organized as an array
     * @param fold      index of fold. if there are no multiple folds, pass 0
     * @return
     */
    public double networkOutput(double inputs[], int fold) {

        visitinfo.setInputs(inputs);
//        if (visitinfo.isVisited()) {
        // we already computed this output - recursion stops
//            return visitinfo.returnOutput();
//        } else {

        double[] local = new double[2];
        local[0] = links.get(0).networkOutputNoFlags(inputs, fold);
        local[1] = links.get(1).networkOutputNoFlags(inputs, fold);
        /*System.out.println("input0 = " + local[0]);
        System.out.println("input1 = " + local[1]);

        double[] l = inputs;
        System.out.println("instance");
        for (int i = 0; i < l.length; ++i) {
        System.out.println(l[i] + ", ");
        }

        double[] c = getCoeffs().get(fold);
        System.out.println("coeffszis");
        for (int i = 0; i < c.length; ++i) {
        System.out.println(c[i] + ", ");
        }

        System.out.println();
         */

        double output = localOutput(local, getCoeffs().get(fold));
        // Debugging stuffz
/*         System.out.println("we're evaluating: " + getIdentifier());
        System.out.println("inputs: ");
        for (int i = 0; i < inputs.length; ++i) {
        System.out.print(inputs[i] + " ");
        }
        System.out.println();
        System.out.println("coeffs: ");
        for (int i = 0; i < getCoeffs().get(fold).length; ++i) {
        System.out.print(getCoeffs().get(fold)[i] + " ");
        }
        System.out.println();
        System.out.println("output: ");
        System.out.println(output);
         */
//            visitinfo.setOutput(output);
//            visitinfo.onVisit();

        /* before returning, clean visitinfos of the entire graph
         */
        Stack<Node> stack = new Stack();
        stack.push(this);
        while (!stack.isEmpty()) {
            Node current = stack.pop();
            if (current instanceof TwoInputModel) {
                TwoInputModel two = (TwoInputModel) current;
                if (two.visitinfo.isReset()) {
                    break;
                }
                two.visitinfo.resetVisited();
                stack.push(two.links.get(0));
                stack.push(two.links.get(1));
            } else if (!(current instanceof AttributeNode)) {
                try {
                    throw new Exception("only TwoInputModel and AttributeNode expected here");
                } catch (Exception ex) {
                    Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return output;
//        }
    }

    /**
     * Invoked by networkOutput.
     *
     * @param inputs
     * @param fold
     * @return
     */
    public double networkOutputNoFlags(double inputs[], int fold) {

        visitinfo.setInputs(inputs);
        if (visitinfo.isVisited()) {
            // we already computed this output - recursion stops
//            System.out.println("Stopping recursion");
            return visitinfo.returnOutput();
        } else {

            /*double[] local = new double[links.size()];
            for (int i = 0; i < links.size(); ++i) {
            local[i] = links.get(i).networkOutput(inputs);
            }*/
            double[] local = new double[2];
            local[0] = links.get(0).networkOutputNoFlags(inputs, fold);
            local[1] = links.get(1).networkOutputNoFlags(inputs, fold);

            double output = localOutput(local, getCoeffs().get(fold));
//            System.out.println(getIdentifier() + "'s output: " + output);
            visitinfo.setOutput(output);
            visitinfo.onVisit();
            return output;
        }
    }

    /**
     *
     * @param fold
     * @return algebraic form of the model, HTML
     */
    @Override
    public String toHTML(int fold) {
        String output = new String();
        double[] coeffsToShow = this.getCoeffs().get(fold);
        Iterator<Summand> summandIter =
                SseRegressionEquations.generateSummands(this, coeffsToShow).iterator();

        output = output.concat(
                "<html><body><font size = 2><b>P<sub<html>" + getIdentifier() +
                "</sub></b> = ");
        /* summandIter definitely has next
         */
        output = output.concat(String.valueOf(summandIter.next().coefficient));
        while (summandIter.hasNext()) {
            Summand currentSummand = summandIter.next();
            output = output.concat(" + " + currentSummand.coefficient);
            Iterator<Node> variableIter =
                    currentSummand.variables.iterator();
            while (variableIter.hasNext()) {
                Node next = variableIter.next();
                if (next instanceof AttributeNode) {
                    output = output.concat("*P<sub>" +
                            ((AttributeNode) next).attr.name() +
                            "</sub>");
                } else {
                    output = output.concat("*P<sub>" +
                            next.getIdentifier() +
                            "</sub>");
                }
            }
        }
        output = output.concat("</font><br>");
        return output;
    }

    @Override
    public String toString(int fold) {

        String output = new String();
        double[] coeffsToShow = this.getCoeffs().get(fold);
        Iterator<Summand> summandIter =
                SseRegressionEquations.generateSummands(this, coeffsToShow).iterator();

        output = output.concat(
                "P" + getIdentifier() +
                " = ");
        // summandIter definitely has next
        output = output.concat(String.valueOf(summandIter.next().coefficient));
        while (summandIter.hasNext()) {
            Summand currentSummand = summandIter.next();
            output = output.concat(" + " + currentSummand.coefficient);
            Iterator<Node> variableIter =
                    currentSummand.variables.iterator();
            while (variableIter.hasNext()) {
                Node next = variableIter.next();
                if (next instanceof AttributeNode) {
                    output = output.concat("*" +
                            ((AttributeNode) next).attr.name());
                } else {
                    output = output.concat("*P" +
                            next.getIdentifier());
                }
            }
        }
        output = output.concat("\n");
        return output;
    }
} // TwoInputModel ends