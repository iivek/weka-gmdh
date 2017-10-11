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

package wGmdh.jGmdh.hybrid;

import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import wGmdh.jGmdh.oldskul.AttributeNode;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import wGmdh.jGmdh.oldskul.TwoInputModel;
import wGmdh.jGmdh.oldskul.measures.Performance;

/**
 * A two-input building block of additive GMDH
 *
 * @author ivek
 */
public class ErrorPropagatingModel extends TwoInputModel {

    private static final long serialVersionUID = -4976617095992800170L;
    private static int referentIndex = 0;

    /**
     * Don't allow this constructor.
     */
    private ErrorPropagatingModel() {
    }

    /**
     *
     * @param links
     */
    public ErrorPropagatingModel(Node... links) {
        super(links);
    }

    /**
     * Sets up performance criteria and links. Does not determine coefficients
     * and measure quality of fit.
     *
     * @param selectionCriterion
     * @param errorMeasure
     * @param inputL
     * @param inputR
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    public ErrorPropagatingModel(Performance selectionCriterion,
            Performance errorMeasure, Node inputL, Node inputR) throws TooBig, ExpressionEqualToZero, TooSmall {

        super(selectionCriterion, errorMeasure, inputL, inputR);
    }

    private ErrorPropagatingModel(double[] goal, Object object, Object object0, Node node, Node node0) {
        throw new UnsupportedOperationException("Not yet implemented");
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
    public ErrorPropagatingModel(double[] regressionGoals,
            Performance selectionCriterion,
            Performance errorMeasure,
            Node... links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

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
    public ErrorPropagatingModel(Performance selectionCriterion,
            Performance errorMeasure, Node... inputs) throws TooBig, ExpressionEqualToZero, TooSmall {

        // delegate parameters upward
        super(selectionCriterion, errorMeasure, inputs);
    }

    /**
     * Fits the coefficients to residual of the existing fit.
     *
     * @param regressTo             regression goal
     * @param trainingData          node inputs
     * @return
     * @throws wGmdh.jGmdh.exceptions.TooBig
     */
    @Override
    protected double[] coeffsFromData(
            double[] regressTo, double[][] trainingData)
            throws TooBig {
        int len = regressTo.length;
        // currentlyModeled should be of same size as regressTo.
        double[] currentlyModeled = trainingData[referentIndex];
        double[] newGoal;
        double[][] newTrainingData;
        if (links.get(referentIndex) instanceof AttributeNode) {
            newTrainingData = trainingData;
            newGoal = regressTo;
        } else if (links.get(referentIndex) instanceof Model) {
            newGoal = new double[len];
            for (int i = 0; i < newGoal.length; ++i) {
                newGoal[i] = regressTo[i] - currentlyModeled[i];
            }

            newTrainingData = new double[trainingData.length][trainingData[0].length];
            for (int i = 0; i < trainingData.length; ++i) {
                for (int j = 0; j < trainingData[i].length; ++j) {
                    newTrainingData[i][j] = regressTo[j] - trainingData[i][j];
                }
            }
        } else {
            try {
                throw new Exception("only TwoInputModels and AttributeNodes expected here");
            } catch (Exception ex) {
                Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return null;
        }


        return super.coeffsFromData(newGoal, newTrainingData);
    }

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
    @Override
    public double networkOutput(double inputs[], int fold) {

        visitinfo.setInputs(inputs);

        double[] local = new double[2];

        if (links.get(0) instanceof Model) {
            local[0] = links.get(0).networkOutputNoFlags(inputs, fold) -
                    ((Model) links.get(0)).links.get(referentIndex).networkOutputNoFlags(inputs, fold);
        } else if (links.get(0) instanceof AttributeNode) {
            local[0] = links.get(0).networkOutputNoFlags(inputs, fold);
        } else {
            try {
                throw new Exception("only TwoInputModels and AttributeNodes expected here");
            } catch (Exception ex) {
                Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return Double.NaN;
        }

        if (links.get(1) instanceof Model) {
            local[1] = links.get(1).networkOutputNoFlags(inputs, fold) -
                    ((Model) links.get(1)).links.get(referentIndex).networkOutputNoFlags(inputs, fold);
        } else if (links.get(1) instanceof AttributeNode) {
            local[1] = links.get(1).networkOutputNoFlags(inputs, fold);
        } else {
            try {
                throw new Exception("only TwoInputModels and AttributeNodes expected here");
            } catch (Exception ex) {
                Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
            }
            return Double.NaN;
        }

        /* additive model
         */
        double output = localOutput(local, getCoeffs().get(fold)) + local[0];

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
                    throw new Exception("only TwoInputModels and AttributeNodes expected here");
                } catch (Exception ex) {
                    Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        return output;
    }

    /**
     * Invoked by networkOutput.
     *
     * @param inputs
     * @param fold
     * @return
     */
    @Override
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
            if (links.get(0) instanceof Model) {
                local[0] = links.get(0).networkOutputNoFlags(inputs, fold) -
                        ((Model) links.get(0)).links.get(referentIndex).networkOutputNoFlags(inputs, fold);
            } else if (links.get(0) instanceof AttributeNode) {
                local[0] = links.get(0).networkOutputNoFlags(inputs, fold);
            } else {
                try {
                    throw new Exception("only TwoInputModels and AttributeNodes expected here");
                } catch (Exception ex) {
                    Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
                }
                return Double.NaN;
            }

            if (links.get(1) instanceof Model) {
                local[1] = links.get(1).networkOutputNoFlags(inputs, fold) -
                        ((Model) links.get(1)).links.get(referentIndex).networkOutputNoFlags(inputs, fold);
            } else if (links.get(1) instanceof AttributeNode) {
                local[1] = links.get(1).networkOutputNoFlags(inputs, fold);
            } else {
                try {
                    throw new Exception("only TwoInputModels and AttributeNodes expected here");
                } catch (Exception ex) {
                    Logger.getLogger(TwoInputModel.class.getName()).log(Level.SEVERE, null, ex);
                }
                return Double.NaN;
            }

            double output = localOutput(local, getCoeffs().get(fold)) + local[0];
//            System.out.println(getIdentifier() + "'s output: " + output);
            visitinfo.setOutput(output);
            visitinfo.onVisit();
            return output;
        }
    }
}
