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

import wGmdh.jGmdh.oldskul.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.gui.UsefulButNonethelessStupidBoolWrapper;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import weka.core.Option;
import weka.core.Utils;

/**
 * Correlation-based Model selection
 * 
 * (Adapted from
 * A.Hall: Correlation Based Feature Selection for Machine Learning)
 *
 * @author ivek
 */
public class CfsFilter extends NodeFilter {

    private static final long serialVersionUID = -8850647944618555368L;
    private static int defaultSetsize = 20;
    protected Vector<Node> set;
    protected CorrsAndDevsAndMeans storage;
    private int setsize;

    public CfsFilter() {
        super();
        set = new Vector<Node>();
        this.setSetsize(defaultSetsize);
    //storage = new CorrsAndDevsAndMeans();
    }

    public CfsFilter(int size) {
        super();
        set = new Vector<Node>();
        setsize = size;
    //storage = new CorrsAndDevsAndMeans();
    }

    @Override
    public Node feed(Node newNode) {
        /*
         * Wrap and store models.
         */

        set.add(newNode);
        return null;
    }

    @Override
    public ArrayList<Node> getResult() {
        try {
            storage = new CorrsAndDevsAndMeans(set, dataset);
        } catch (Exception ex) {
            Logger.getLogger(CfsFilter.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            return (ArrayList<Node>) forwardSelection(getSetsize());
            //return (ArrayList<Node>) backwardElimination(getSetsize());
        } catch (Exception ex) {
            Logger.getLogger(CfsFilter.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    @Override
    public void reset() {
        set.clear();
        storage = null; // we should do it more elegantly
    }

    /**
     * @return the setsize
     */
    public int getSetsize() {
        return setsize;
    }

    /**
     * @param setsize the setsize to set
     */
    public void setSetsize(int setsize) {
        this.setsize = setsize;
    }

    /**
     * A class that manages correlation matrix and standard deviations of feats.
     */
    public class CorrsAndDevsAndMeans implements Serializable {

        private static final long serialVersionUID = -6411372142822578528L;

        //private int fold;
        public Vector<Node> set;
        private DatasetSupervised handler;
        /* Correlation matrices, means and standard deviations. Indexing is as
         * follows:
         * correlationMatrices[ indexOfModel1 ][ IndexOfModel2 ][ foldIndex ]
         * stdDeviations[ indexOfModel ][ foldIndex ]
         * means[ indexOfModel ][ foldIndex ]
         */
        private double[][][] correlationMatrices;
        private double[][] stdDeviations;
        private double[][] means;

        private CorrsAndDevsAndMeans() {
        }

        public CorrsAndDevsAndMeans(Vector<Node> modelSet,
                DatasetSupervised dataset) throws Exception {
            handler = dataset;
            set = modelSet;
            int numFeats = modelSet.size();

            /* initialize matrices. An entry of -Inf means we haven't calculated
             * it yet.
             */

            correlationMatrices = new double[numFeats + 1][][];
            stdDeviations = new double[numFeats + 1][handler.getDimension()];
            means = new double[numFeats + 1][handler.getDimension()];

            int i;
            for (i = 0; i < numFeats; ++i) {
                correlationMatrices[i] = new double[i + 1][handler.getDimension()];
                for (int f = 0; f < handler.getDimension(); ++f) {
                    stdDeviations[i][f] = Double.NEGATIVE_INFINITY;
                    means[i][f] = Double.NEGATIVE_INFINITY;
                    for (int j = 0; j < i; ++j) {
                        correlationMatrices[i][j][f] = Double.NEGATIVE_INFINITY;
                    }
                }
            }
            // i == numFeats -> class-feature correlations
            correlationMatrices[i] = new double[numFeats][handler.getDimension()];
            for (int f = 0; f < handler.getDimension(); ++f) {
                stdDeviations[i][f] = Double.NEGATIVE_INFINITY;
                means[i][f] = Double.NEGATIVE_INFINITY;
                for (int j = 0; j < numFeats; ++j) {
                    correlationMatrices[i][j][f] = Double.NEGATIVE_INFINITY;
                }
            }

        }

        /**
         *
         * @param i     index of model in set
         * @param j     index of model in set
         * @return      array of correlation coefficients, one for entry for
         *              each set of outputs/inputs/coefficients a Model has
         *
         * @throws java.lang.Exception
         */
        public double[] getCorr(int i, int j) throws Exception {
            int smaller, larger;
            if (i < j) {
                smaller = i;
                larger = j;
            } else {
                larger = i;
                smaller = j;
            }

            if (correlationMatrices[larger][smaller][0] ==
                    Double.NEGATIVE_INFINITY) {
                /* sufficient indication that we haven't calculated correlations
                 * yet, correlations for all folds. let's do the calculation
                 */
                if (larger == correlationMatrices.length - 1) {
                    // we need correlation with class
                    Iterator<double[]> validationOutSmaller =
                            handler.getValidationOutputs(set.get(smaller));
                    Iterator<double[]> validationGoals =
                            handler.getValidationGoals();
                    int counter = 0;

                    while (validationOutSmaller.hasNext()) {// && validationGoals.hasNext()) {

                        CorrResults res = corr(validationOutSmaller.next(), validationGoals.next());
                        correlationMatrices[larger][smaller][counter] = res.corr;
                        stdDeviations[smaller][counter] = res.std1;
                        stdDeviations[larger][counter] = res.std2;
                        means[smaller][counter] = res.mean1;
                        means[larger][counter] = res.mean2;

                        ++counter;
                    }
                } else {
                    // we need between-model xcorr or a model autocorr
                    Iterator<double[]> validationOutSmaller =
                            handler.getValidationOutputs(set.get(smaller));
                    Iterator<double[]> validationOutLarger =
                            handler.getValidationOutputs(set.get(larger));
                    int counter = 0;

                    while (validationOutSmaller.hasNext()) {// && validationlarger.hasNext()) {
                        CorrResults res = corr(validationOutSmaller.next(),
                                validationOutLarger.next());
                        correlationMatrices[larger][smaller][counter] = res.corr;
                        stdDeviations[smaller][counter] = res.std1;
                        stdDeviations[larger][counter] = res.std2;
                        means[smaller][counter] = res.mean1;
                        means[larger][counter] = res.mean2;

                        ++counter;
                    }
                }
            }
            return correlationMatrices[larger][smaller];
        }

        public double[] getMean(int index) {
            if (means[index][0] == Double.NEGATIVE_INFINITY) {
                // sufficient to know we have to calculate the whole batch
                Iterator<double[]> validationOut =
                        handler.getValidationOutputs(set.get(index));
                int counter = 0;

                while (validationOut.hasNext()) {
                    double[] data = validationOut.next();
                    double mean = 0;
                    for (int i = 0; i < data.length; ++i) {
                        mean += data[i];
                    }
                    mean /= data.length;
                    means[index][counter] = mean;
                }
                ++counter;
            }
            return means[index];
        }

        public double[] getDeviation(int index) {

            if (stdDeviations[index][0] == Double.NEGATIVE_INFINITY) {
                // sufficient to know we have to calculate the whole batch
                Iterator<double[]> validationOut =
                        handler.getValidationOutputs(set.get(index));
                int counter = 0;
                double[] meanVals = getMean(index);
                while (validationOut.hasNext()) {

                    double[] data = validationOut.next();
                    double std = 0;
                    for (int i = 0; i < data.length; ++i) {
                        std += Math.pow((data[i] - meanVals[counter]), 2);
                    }
                    std = Math.sqrt(std / (data.length - 1));

                    //std = Math.sqrt(std / (data.length));
                    stdDeviations[index][counter] = std;
                }
                ++counter;
            }
            // else we already have itc
            return stdDeviations[index];
        }
    }

    /**
     * Calculates and returns the merit of a set of Models
     *
     * @return
     */
    protected double evaluateSubset(Vector<Integer> subset) throws Exception {

        int numFeats = subset.size();
        //System.out.println("numFeats" + numFeats);

        // Numerator -> correlations with class (goal)
        double numerator[] = new double[dataset.getDimension()];
        for (int i = 0; i < numFeats; ++i) {
            int modelIndex = subset.get(i);
            int classIndex = set.size();
            //System.out.println("modelIndex = " + modelIndex);
            //System.out.println("set.size = " + set.size());

            double[] deviations = storage.getDeviation(modelIndex);
            double[] correlations = storage.getCorr(classIndex, modelIndex);
            // deviations.length == storage.length, we won't check it

            for (int f = 0; f < dataset.getDimension(); ++f) {
                numerator[f] += Math.abs(deviations[f] * correlations[f]);
            }
        }

        // Denominator -> intercorrelations
        double denominator[] = new double[dataset.getDimension()];
        for (int i = 0; i < numFeats; ++i) {
            int modelIndex1 = subset.get(i);
            double[] deviations1 = storage.getDeviation(modelIndex1);
            for (int f = 0; f < dataset.getDimension(); ++f) {
                denominator[f] += 1.0 * Math.pow(deviations1[f], 2);
            }
            for (int j = 0; j < numFeats; ++j) {
                int modelIndex2 = subset.get(j);
                double[] deviations2 = storage.getDeviation(modelIndex2);
                double[] correlations = storage.getCorr(modelIndex1, modelIndex2);
                for (int f = 0; f < dataset.getDimension(); ++f) {
                    denominator[f] += 2.0 * Math.abs(deviations1[f] * deviations2[f] *
                            correlations[f]);
                }
            }
        }

        Double avgMerit = 0.0;
        for (int f = 0; f < dataset.getDimension(); ++f) {
            avgMerit += numerator[f] / Math.sqrt(denominator[f]);
        }
        avgMerit /= dataset.getDimension();

        /* An extra precaution - if all Nodes in our filter have a NaN merit,
         * we won't have anything to output (we want to output at least some
         * Attribute nodes, to give the algorithm something to work with).
         * 0/0 is NaN. Denominator can in fact be zero, take for instance a set
         * with one point only which we would like to calculate merit on.
         */
        if (avgMerit.isNaN()) {
            avgMerit = -1.0;
        }
        return avgMerit;
    }

    protected class CorrResults {

        public double mean1,  mean2,  std1,  std2,  corr;
    }

    /**
     * Pearson's coefficient.
     *
     * @param var1
     * @param var2
     * @return
     */
    public CorrResults corr(double[] var1, double[] var2) {

        CorrResults res = new CorrResults();
        // first get the means
        for (int i = 0; i < var1.length; ++i) {
            // var1 and var1 should have the same lengths
            res.mean1 += var1[i];
            res.mean2 += var2[i];
        }
        res.mean1 /= var1.length;
        res.mean2 /= var2.length;

        // then the std.devs.
        for (int i = 0; i < var1.length; ++i) {
            res.std1 += Math.pow((var1[i] - res.mean1), 2);
            res.std2 += Math.pow((var2[i] - res.mean2), 2);
        }
        int num = (var1.length - 1);
        //int num = var1.length;
        res.std1 = Math.sqrt(res.std1 / num);
        res.std2 = Math.sqrt(res.std2 / num);

        res.corr = 0;
        for (int i = 0; i < var1.length; ++i) {
            res.corr += (var1[i] - res.mean1) * (var2[i] - res.mean2);
        }

        res.corr /= num * res.std1 * res.std2;

        return res;
    }

    /**
     * Selects a subset of models that maximize CFS merit using forward selection.
     *
     * @param setSize
     * @return
     */
    protected ArrayList<Node> forwardSelection(int nrSelected) throws Exception {
        //System.out.println("forwardselecting");
        ArrayList<Node> selectedNodes = new ArrayList<Node>();
        // mask: false -> not selected, true -> selected
        Vector<UsefulButNonethelessStupidBoolWrapper> selected =
                new Vector<UsefulButNonethelessStupidBoolWrapper>(set.size());
        Vector<Integer> selectedIndices = new Vector<Integer>(nrSelected);
        for (int i = 0; i < set.size(); ++i) {
            selected.add(new UsefulButNonethelessStupidBoolWrapper(false));
        }

        int bestIndex = 0;
        double bestMerit = -1;

        int limit = (nrSelected < set.size() ? nrSelected : set.size());
        for (int j = 0; j < limit; ++j) {
            bestMerit = -1;
            for (int i = 0; i < set.size(); ++i) {
                if (selected.get(i).b == false) {
                    int nextindex = selectedIndices.size();

                    selectedIndices.add(nextindex, i);
                    double merit = evaluateSubset(selectedIndices);
//                    System.out.println("    index = " + i + "; merit = " + merit);
                    if (merit > bestMerit) {
                        bestMerit = merit;
                        bestIndex = i;
                    }
                    selectedIndices.remove(nextindex);
                }
            }

            if (selected.get(bestIndex).b == false) {
                // it should be false, but make sure anyway
//                System.out.println(" index = " + bestIndex + "; ID = " + set.get(bestIndex).getIdentifier());
                selected.set(bestIndex, new UsefulButNonethelessStupidBoolWrapper(true));
                selectedIndices.add(bestIndex);
                selectedNodes.add(set.get(bestIndex));
            }
        }
        /*System.out.println("    Selected indices");
        for (int i = 0; i < selectedIndices.size(); ++i) {
        System.out.print(selectedIndices.get(i) + ", ");
        }
        System.out.println();
         */
        return selectedNodes;
    }

    /**
     * Selects a subset of models that maximize CFS merit using backward
     * elimination.
     *
     * @param setSize
     * @return
     */
    protected ArrayList<Node> backwardElimination(int nrSelected) throws Exception {
        ArrayList<Node> selectedNodes = new ArrayList<Node>();
        // mask: false -> not selected, true -> selected
        Vector<UsefulButNonethelessStupidBoolWrapper> selected =
                new Vector<UsefulButNonethelessStupidBoolWrapper>(set.size());
        Vector<Integer> selectedIndices = new Vector<Integer>(set.size());
        for (int i = 0; i < set.size(); ++i) {
            selectedNodes.add(set.get(i));
        }
        for (int i = 0; i < set.size(); ++i) {
            selectedIndices.add(i);
        }
        for (int i = 0; i < set.size(); ++i) {
            selected.add(new UsefulButNonethelessStupidBoolWrapper(true));
        }

        int bestIndex = 0;  // to remove
        double bestMerit = -1;


        int limit = (nrSelected < set.size() ? nrSelected : set.size());
        int toRemove = set.size() - limit;
        System.out.println("toremove = " + toRemove);
        for (int j = 0; j < toRemove; ++j) {
            bestMerit = -1;
            // prolazim ja tako kroz set...
            for (int i = 0; i < selectedIndices.size(); ++i) {
                if (selected.get(i).b == true) {
                    int index = selectedIndices.get(i);
                    selectedIndices.remove(i);
                    double merit = evaluateSubset(selectedIndices);
                    //System.out.println("    index = " + i + "; merit = " + merit);
                    if (merit > bestMerit) {
                        bestMerit = merit;
                        bestIndex = i;
                    }
                    selectedIndices.add(i, index);
                }   // else it's already marked as removed from the set
            }

            if (selected.get(bestIndex).b == true) {
                // it should be true, but make sure anyway
                //System.out.println(" index = " + bestIndex + "; ID = " + set.get(bestIndex).getIdentifier());
                selected.set(bestIndex, new UsefulButNonethelessStupidBoolWrapper(false));
                selectedIndices.remove(bestIndex);
                selectedNodes.remove(set.get(bestIndex));
            }
        }


        /*System.out.println("    Selected indices");
        for (int i = 0; i < selectedIndices.size(); ++i) {
        System.out.print(selectedIndices.get(i) + ", ");
        }
        System.out.println();
         */
        return selectedNodes;
    }

    /**
     *
     * @param options
     * @throws java.lang.Exception
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String string;
        string = Utils.getOption('S', options);
        if (string.length() != 0) {
            setSetsize((new Integer(string)).intValue());
        } else {
            setSetsize(defaultSetsize);
        }

        super.setOptions(options);
    }

    @Override
    public String[] getOptions() {

        Vector options = new Vector();
        String[] superOptions = super.getOptions();
        for (int i = 0; i < superOptions.length; i++) {
            options.add(superOptions[i]);
        }

        options.add("-S");
        options.add("" + this.getSetsize());

        return (String[]) options.toArray(new String[options.size()]);
    }

    @Override
    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        Vector newVector = new Vector(1);
        newVector.addElement(new Option(
                "\tNumber of models to be selecetd",
                "S", 1, "-S <number of selected models>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return options.elements();
    }

    @Override
    public ArrayList<Model> getSortedModels() {
        ArrayList<Model> models = new ArrayList<Model>();
        for (Node n : set) {
            //System.out.println(n.getIdentifier());
            if (n instanceof Model) {
                Model m = (Model) n;
                models.add(m);

            }
        }
        java.util.Collections.sort(models, new ModelComparator());
        return models;
    }
}
