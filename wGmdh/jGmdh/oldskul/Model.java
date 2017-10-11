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

import wGmdh.jGmdh.oldskul.measures.Performance;
import java.util.ArrayList;
import java.util.Arrays;
import wGmdh.jGmdh.exceptions.*;
import java.util.Iterator;
import java.util.List;
import wGmdh.jGmdh.util.supervised.DatasetHandlerSupervised;
import weka.core.Instances;

/**
 * Contains basic functionality of a GMDH model, such as  connectivity
 * information and obtaining data at the inputs.
 * If you want to customize the GMDH node (its output function or whatever),
 * inherit from this class.
 *
 * TODO: Model could extend classifier directly? We could use Evaluation then
 * and all related weka built-in functions
 *
 * @author ivek
 */
public abstract class Model extends Node {

    public ArrayList<Node> links;
    /* Coefficients of the model. coeffs.get(n) is a set of
     * coefficients obtained when working with trainSetOutput.get(n) and
     * validationSetOutput.get(n);
     *
     * TODO: coefficients per fold and outputs per fold can be calculate in paralell
     */
    protected ArrayList<double[]> coeffs;
    private double errorStructureLearningSet;
    private double errorStructureValidationSet;

    /*
     * sortedLinkIds is used to determine whether two models are same.
     * TODO: implement the compare operation so we sort ArrayList<Node> links
     * and determine equality based on it, making sortedLinkIds obsolete.
     * And additionaly free some heap space.
     */
    protected long[] sortedLinkIds;
    protected Performance structureSelectionCriterion = null;
    protected Performance errorMeasure = null;

    public Model() {
        this.links = null;
        this.sortedLinkIds = null;
        coeffs = new ArrayList<double[]>();
    }

    public Model(Node... links) {
        this.links = new ArrayList<Node>(Arrays.asList(links));
        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
        coeffs = new ArrayList<double[]>();
    }

    public Model(ArrayList<Node> links) {
        this.links = links;

        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
        coeffs = new ArrayList<double[]>();
    }

    /**
     *
     * @param regressionGoals        each entry in the ArrayList coresponds to
     *                              one fold
     * @param validationGoals
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    @Deprecated
    public Model(ArrayList<double[]> regressionGoals,
            ArrayList<double[]> validationGoals,
            Performance selectionCriterion,
            Performance errorMeasure, ArrayList<Node> links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = links;
        coeffs = new ArrayList<double[]>(regressionGoals.size());

        // we suppose that regressionGoal.size() is equal to validationGoals.size()
        // (and equal to number of folds used)
        errorStructureLearningSet = 0;
        for (int i = 0; i < regressionGoals.size(); ++i) {
            coeffs.add(i, coeffsFromData(regressionGoals.get(i), this.collectTrainingData(i)));
            this.trainSetOutput.add(localOuputOnArray(this.collectTrainingData(i), coeffs.get(i)));
            System.out.println("Creating model " + this.getIdentifier());
            errorStructureLearningSet += errorMeasure.calculate(this, i, regressionGoals.get(i));
            validationSetOutput.add(i,
                    localOuputOnArray(this.collectValidationData(i), coeffs.get(i)));
            errorStructureValidationSet +=
                    getSelectionCriterion().calculate(this, i, validationGoals.get(i));
        }
        errorStructureLearningSet /= regressionGoals.size();
        errorStructureValidationSet /= regressionGoals.size();
        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
    }

    /**
     *
     * @param handler
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws wGmdh.jGmdh.exceptions.TooBig
     * @throws wGmdh.jGmdh.exceptions.ExpressionEqualToZero
     * @throws wGmdh.jGmdh.exceptions.TooSmall
     */
    public Model(DatasetHandlerSupervised handler,
            Performance selectionCriterion,
            Performance errorMeasure, ArrayList<Node> links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = links;
        coeffs = new ArrayList<double[]>();

        errorStructureLearningSet = 0;
        errorStructureValidationSet = 0;

        coeffsAndErrors(handler);

        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
    }

    /**
     * This constructor calculates only one set of coefficients from one set of
     * regressors(it acts as if one fold only exists, instead of n-fold CV).
     * Does not calculate how good the structure performs on some other dataset
     * (validationSetOutput and errorStructureValidationSet). If needed,
     * initialize Performance members and calculate performance after
     * constructor call.
     *
     * @param regressionGoals
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    @Deprecated
    public Model(double[] regressionGoals,
            Performance selectionCriterion,
            Performance errorMeasure,
            Node... links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = new ArrayList<Node>(Arrays.asList(links));
        this.coeffs = new ArrayList<double[]>(regressionGoals.length);

        this.collectTrainingData(0);
        coeffs.add(0, coeffsFromData(regressionGoals, this.collectTrainingData(0)));
        this.trainSetOutput.add(localOuputOnArray(this.collectTrainingData(0), coeffs.get(0)));

        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
    }

    /**
     *
     * @param regressionGoals
     * @param validationGoals
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    @Deprecated
    public Model(ArrayList<double[]> regressionGoals,
            ArrayList<double[]> validationGoals,
            Performance selectionCriterion,
            Performance errorMeasure, Node... links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = new ArrayList<Node>(Arrays.asList(links));
        coeffs = new ArrayList<double[]>(regressionGoals.size());

        // we suppose that regressionGoal.size() is equal to validationGoals.size()
        // (and equal to number of folds used)
        errorStructureLearningSet = 0;
        for (int i = 0; i < regressionGoals.size(); ++i) {
            coeffs.add(i, coeffsFromData(regressionGoals.get(i), this.collectTrainingData(i)));
            this.trainSetOutput.add(localOuputOnArray(this.collectTrainingData(i), coeffs.get(i)));
            errorStructureLearningSet += errorMeasure.calculate(this, i, regressionGoals.get(i));
            validationSetOutput.add(i,
                    localOuputOnArray(this.collectValidationData(i), coeffs.get(i)));
            errorStructureValidationSet +=
                    getSelectionCriterion().calculate(this, i, validationGoals.get(i));
        }
        errorStructureLearningSet /= regressionGoals.size();
        errorStructureValidationSet /= regressionGoals.size();
        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
    }

    /**
     * No regression or model evaluation takes place in this constructor.
     *
     * @param selectionCriterion
     * @param errorMeasure
     * @param links
     * @throws jGMDH.exceptions.TooBig
     * @throws jGMDH.exceptions.ExpressionEqualToZero
     * @throws jGMDH.exceptions.TooSmall
     */
    public Model(Performance selectionCriterion,
            Performance errorMeasure, Node... links)
            throws TooBig, ExpressionEqualToZero, TooSmall {

        this.structureSelectionCriterion = selectionCriterion;
        this.errorMeasure = errorMeasure;
        this.links = new ArrayList<Node>(Arrays.asList(links));
        coeffs = new ArrayList<double[]>();

        this.sortedLinkIds = new long[this.links.size()];
        for (int i = 0; i < this.links.size(); ++i) {
            this.sortedLinkIds[i] = this.links.get(i).getIdentifier();
        }
        Arrays.sort(this.sortedLinkIds);
    }

    /**
     * Folds dataSetToFold. For each fold it fits the model to find its
     * coefficients and calculates average error
     *
     * @param datasetToFold
     * @param numFolds
     */
    @Deprecated
    public void coeffsAndErrors(
            Instances datasetToFold, int numFolds)
            throws TooBig, TooSmall, ExpressionEqualToZero {
        errorStructureLearningSet = 0;
        errorStructureValidationSet = 0;
        for (int i = 0; i < numFolds; ++i) {
            // learning stuff
            double[] learningGoals =
                    getStructureLearningGoals(datasetToFold, numFolds, i);
            getCoeffs().add(i, coeffsFromData(
                    learningGoals, collectTrainingData(i)));
            trainSetOutput.add(
                    localOuputOnArray(collectTrainingData(i), getCoeffs().get(i)));
            errorStructureLearningSet += errorMeasure.calculate(this, i, learningGoals);

            // validation stuff
            double[] validationGoals =
                    getStructureValidationGoals(datasetToFold, numFolds, i);
            validationSetOutput.add(i,
                    localOuputOnArray(collectValidationData(i), getCoeffs().get(i)));
            errorStructureValidationSet +=
                    getSelectionCriterion().calculate(this, i, validationGoals);
        }
        errorStructureLearningSet /= numFolds;
        errorStructureValidationSet /= numFolds;
    }


    /**
     * Fits the model to find its coefficients and calculates its error on
     * validation set
     *
     * @param dataset
     * @param trainPercentage
     * @throws wGmdh.jGmdh.exceptions.TooBig
     * @throws wGmdh.jGmdh.exceptions.TooSmall
     * @throws wGmdh.jGmdh.exceptions.ExpressionEqualToZero
     * @deprecated
     */
    @Deprecated
    public void coeffsAndErrors(
            Instances dataset, double trainPercentage)
            throws TooBig, TooSmall, ExpressionEqualToZero {

        // learning stuff
        int i = 0;  // one training set and one validation set, indexed as 0
        double[] learningGoals =
                getStructureLearningGoals(dataset, trainPercentage);
        getCoeffs().add(i, coeffsFromData(
                learningGoals, collectTrainingData(i)));
        trainSetOutput.add(
                localOuputOnArray(collectTrainingData(i), getCoeffs().get(i)));
        errorStructureLearningSet = errorMeasure.calculate(this, i, learningGoals);

        // validation stuff
        double[] validationGoals =
                getStructureValidationGoals(dataset, trainPercentage);
        validationSetOutput.add(i,
                localOuputOnArray(collectValidationData(i), getCoeffs().get(i)));
        errorStructureValidationSet =
                getSelectionCriterion().calculate(this, i, validationGoals);
    }

    /**
     * Folds dataSetToFold. For each fold it fits the model to find its
     * coefficients and calculates average error
     *
     * @param handler
     */
    public void coeffsAndErrors(DatasetHandlerSupervised handler)
            throws TooBig, TooSmall, ExpressionEqualToZero {
        errorStructureLearningSet = 0;
        errorStructureValidationSet = 0;

        Iterator<double[]> learningGoals = handler.getLearningGoals();
        Iterator<double[][]> learningInputs = handler.getLearningInputs(this);
        Iterator<double[]> validationGoals = handler.getValidationGoals();
        Iterator<double[][]> validationInputs = handler.getValidationInputs(this);
        // in case everythig is OK,
        // learningGoals.hasNext() == true && learningInputs.hasNext() == true
        int fold = 0;
        while (learningGoals.hasNext()) {
            // iterate through folds

            //learning stuff
            double[] goals = learningGoals.next();
            double[][] inputs = learningInputs.next();
            getCoeffs().add(fold, coeffsFromData(goals, inputs));
            trainSetOutput.add(
                    localOuputOnArray(inputs, getCoeffs().get(fold)));
            errorStructureLearningSet +=
                    errorMeasure.calculate(this, fold, goals);
            // TODO: make Measure classes work with DatasetHandlers

            //validation stuff
            goals = validationGoals.next();
            inputs = validationInputs.next();
            validationSetOutput.add(
                    localOuputOnArray(inputs, getCoeffs().get(fold)));
            errorStructureValidationSet +=
                    structureSelectionCriterion.calculate(this, fold, goals);

            ++fold;
        }

        errorStructureLearningSet /= fold;
        errorStructureValidationSet /= fold;
    }

    public void setSelectionCriterion(Performance criterion) {
        this.structureSelectionCriterion = criterion;
    }

    public Performance getSelectionCriterion() {
        return structureSelectionCriterion;
    }

    public void setErrorMeasure(Performance criterion) {
        this.errorMeasure = criterion;
    }

    public Performance getErrorMeasure() {
        return errorMeasure;
    }

    /**
     * Obtains coefficients by regression.
     *
     * @param regressTo        regression goal
     * @param trainingData     array of regressor values
     */
    abstract protected double[] coeffsFromData(
            double[] regressTo, double[][] trainingData)
            throws TooBig, TooSmall, ExpressionEqualToZero;

    /**
     * Calculates value that a GMDH polynomial takes in a point, for an array
     * of points, using coeffs as coefficients.
     *
     * @param inputs    array of polynomial inputs
     * @param coeffs    coefficients
     */
    abstract public double[] localOuputOnArray(double[][] inputs, double[] coeffs)
            throws TooBig, TooSmall, ExpressionEqualToZero;

    /**
     * Check if Model instance's links member is null
     *
     * @return  yea or nay
     */
    public boolean noLinks() {
        if (this.links == null) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check if models m1 and m2 are one and the same
     *
     * @param m1
     * @param m2
     * @return
     */
//    public boolean equalsTo(Model m) {
        /*
     * Two Models are taken as equal if they have the same links (regardless
     * of their links' ordering). Other feats, such as coeffs, may differ
     * due to computer numerics.
     */
//        if (this.links.size() == m.links.size()) {
//            if (Arrays.equals(this.sortedLinkIds, m.sortedLinkIds)) {
//                return true;
//            }
//       }

//        return false;
//    }
    /**
     * Collects output of fold-th fold from all Nodes this Model is linked
     * to and stores it reorganized in  matrix form, suitable for forming
     * the system of linear equations for regressionGoal.
     *
     * @param fold      fold index
     * @return
     */
    @Deprecated
    public double[][] collectTrainingData(int fold) {

        double[][] dataLocal = new double[links.size()][];
        for (int i = 0; i < links.size(); ++i) {
            dataLocal[i] = links.get(i).trainSetOutput.get(fold);
        }

        return dataLocal;
    }

    /**
     * Collects validationSetOutput of fold-th fold from all Nodes this Model is linked
     * to and stores it reorganized in  matrix form, suitable for forming
     * the system of linear equations for regressionGoal.
     *
     * @param fold  fold index
     * @return
     */
    @Deprecated
    public double[][] collectValidationData(int fold) {

        double[][] dataLocal = new double[links.size()][];
        for (int i = 0; i < links.size(); ++i) {
            dataLocal[i] = links.get(i).validationSetOutput.get(fold);
        }

        return dataLocal;
    }

    /**
     * @return the errorStructureLearningSet
     */
    public double getErrorStructureLearningSet() {
        return errorStructureLearningSet;
    }

    /**
     * @return the errorStructureValidationSet
     */
    public double getErrorStructureValidationSet() {
        return errorStructureValidationSet;
    }

    /**
     * @return the coeffs
     */
    public ArrayList<double[]> getCoeffs() {
        return coeffs;
    }

    @Deprecated
    public Instances getStructureValidationInstances(
            Instances dataset, int numfolds, int fold) {
        return dataset.testCV(numfolds, fold);
    }

    @Deprecated
    public double[] getStructureValidationGoals(
            Instances dataset, int numfolds, int fold) {
        return dataset.testCV(numfolds, fold).attributeToDoubleArray(dataset.classIndex());
    }

    @Deprecated
    public Instances getStructureLearningInstances(
            Instances dataset, int numfolds, int fold) {
        return dataset.trainCV(numfolds, fold);
    }

    @Deprecated
    public double[] getStructureLearningGoals(
            Instances dataset, int numfolds, int fold) {
        return dataset.trainCV(numfolds, fold).attributeToDoubleArray(dataset.classIndex());
    }

    @Deprecated
    public Instances getStructureValidationInstances(
            Instances dataset, double trainPercentage) {

        // percentage split
        int learnSetsize = (int) Math.round(
                trainPercentage / 100.0 * dataset.numInstances());
        int validationSetsize = dataset.numInstances() - learnSetsize;

        /*if (validationSetsize < 1) {
        throw new Exception("No instances in structure validation set.");
        }
        if (learnSetsize < 1) {
        throw new Exception("No instances in structure learning set.");
        }
         */
        Instances validationSet = new Instances(
                dataset, learnSetsize, validationSetsize);

        return validationSet;
    }

    @Deprecated
    public double[] getStructureValidationGoals(
            Instances dataset, double trainPercentage) {

        int learnSetsize = (int) Math.round(
                trainPercentage / 100.0 * dataset.numInstances());
        int validationSetsize = dataset.numInstances() - learnSetsize;

        /*if (validationSetsize < 1) {
        throw new Exception("No instances in structure validation set.");
        }
        if (learnSetsize < 1) {
        throw new Exception("No instances in structure learning set.");
        }
         */
        //Instances learnSet = new Instances(dataset, 0, learnSetsize);
        Instances validationSet = new Instances(
                dataset, learnSetsize, validationSetsize);

        return validationSet.attributeToDoubleArray(dataset.classIndex());
    }

    @Deprecated
    public Instances getStructureLearningInstances(
            Instances dataset, double trainPercentage) {

        int learnSetsize = (int) Math.round(
                trainPercentage / 100.0 * dataset.numInstances());
        /*
        if (learnSetsize < 1) {
        throw new Exception("No instances in structure learning set.");
        }
         */
        Instances learnSet = new Instances(dataset, 0, learnSetsize);

        return learnSet;
    }

    @Deprecated
    public double[] getStructureLearningGoals(
            Instances dataset, double trainPercentage) {

        int learnSetsize = (int) Math.round(
                trainPercentage / 100.0 * dataset.numInstances());
        /*
        if (learnSetsize < 1) {
        throw new Exception("No instances in structure learning set.");
        }
         */
        Instances learnSet = new Instances(dataset, 0, learnSetsize);

        return learnSet.attributeToDoubleArray(dataset.classIndex());
    }

    /**
     *
     * @return  the algebraic form of the model
     */
    public String toString(int fold)    {
        return new String();
    }

        /**
     *
     * @return  the algebraic form of the model, HTML
     */
    public abstract String toHTML(int fold);
}   // Model class ends