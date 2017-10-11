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

package wGmdh;

import java.util.Random;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.RandomizableSingleClassifierEnhancer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.UnsupportedClassTypeException;

/**
 * A meta-learner that finds optimal network depth for
 * MscWithCv using 10 fold cross-validation WITH RANDOMIZATION.
 * In each iteration of the CV, a GMDH classifier is run and
 * performances of best models per each layer get stored. At the end, we're
 * left with 10*perLayer performance scores and the resulting output is
 * the network depth that performed best accross the 10 iterations of the CV.
 *
 * @author ivek
 */
public class GmdhDepthSearch extends RandomizableSingleClassifierEnhancer
        implements OptionHandler, TechnicalInformationHandler {

    private static final long serialVersionUID = -7054485337985853969L;
    private int folds = 10;
    /* Root relative squared error is fixed as a performance norm.
     */
    protected String outputString;
    //MscWithCv prototype;
    Msc prototype;

    public GmdhDepthSearch() {
        super();
        //setClassifier(new MscWithCv_mk2());
        setClassifier(new Msc());
    }

    /**
     * Set the learner. It has to be one of the GMDH classifiers.
     *
     * @param newClassifier the classifier to use.
     */
    @Override
    public void setClassifier(Classifier newClassifier) {
        /*        if (!(newClassifier instanceof MscWithCv)) {
        try {
        throw new Exception("wrong classifier");
        } catch (Exception ex) {
        Logger.getLogger(GmdhDepthSearch.class.getName()).log(Level.SEVERE, null, ex);
        }
        } else {
        m_Classifier = newClassifier;
        prototype = (MscWithCv) m_Classifier;
        }
         */
        m_Classifier = newClassifier;
        prototype = (Msc) m_Classifier;
    }

    /*    @Override
    public void buildClassifier(Instances data) throws Exception {

    Random random = new Random(m_Seed);
    data.randomize(random);

    ArrayList<Evaluation> evals = new ArrayList<Evaluation>();
    ArrayList<Integer> nrProcessed = new ArrayList<Integer>();
     */
    /* for each pass of the CV...
     */
    /*        for (int fold = 0; fold < folds; ++fold) {

    MscWithCv current =
    (MscWithCv) Classifier.makeCopy(m_Classifier);
    Instances trainData = data.trainCV(folds, fold);
    Instances testData = data.testCV(folds, fold);

    current.buildClassifier(trainData);

    if (current.getNumberOfLayers() > maxDepth) {
    maxDepth = current.getNumberOfLayers();
    }

    for (int layer = 1; // avoid the attribute layer
    layer < current.getNumberOfLayers();
    ++layer) {
    if (evals.size() < layer) {
    evals.add(new Evaluation(trainData));
    nrProcessed.add(new Integer(0));
    }

    current.setOutput(layer, trainData);
    // evaluate classifier on test data.
    int processed = nrProcessed.get(layer - 1).intValue();
    for (int inst = 0; inst < testData.numInstances(); ++inst) {
    evals.get(layer - 1).evaluateModelOnceAndRecordPrediction(
    current, testData.instance(inst));
    ++processed;
    }
    nrProcessed.add(layer - 1, processed);
    }
    }

    outputString = new String();
    for (int i = 0; i < maxDepth - 1; ++i) {
    outputString += ("Layer: ") + i + " rrse = " +
    evals.get(i).rootRelativeSquaredError() +
    " processedInstances = " + nrProcessed.get(i) + "\n";
    }

    }
     */
    @Override
    public void buildClassifier(Instances data) throws Exception {

        Random random = new Random(m_Seed);
        data.randomize(random);
        outputString = new String();
        outputString += ("Layer 0: Attribute layer;\n");

//        ArrayList<Evaluation> evals = new ArrayList<Evaluation>();
//        ArrayList<Integer> nrProcessed = new ArrayList<Integer>();

        // each fold will have its classifier. we will use serialization to
        // store and access them. they get initialized and serialized here

        for (int fold = 0; fold < getFolds(); ++fold) {
//            MscWithCv current =
//                    (MscWithCv) Classifier.makeCopy(m_Classifier);
            Msc current =
                    (Msc) Classifier.makeCopy(m_Classifier);
            Instances trainData = data.trainCV(getFolds(), fold, random);
            Instances testData = data.testCV(getFolds(), fold);

            /* building classifier
             */
            //current.buildClassifier(trainData);
            current.getCapabilities().testWithFail(trainData);
            /* All instances need to have a class.
             */
            trainData.deleteWithMissingClass();
            /* Check number of attributes
             */
            if (trainData.numAttributes() < 3) {
                throw new Exception("At least 2 attributes and a class" +
                        "are required to build classifier.");
            }

            if (!trainData.classAttribute().isNumeric()) {
                throw new UnsupportedClassTypeException("Numeric class only");
            }

//            Random random2 = new Random();
//            trainData.randomize(random2);
            current.initClassifier(trainData);
            weka.core.SerializationHelper.write(
                    "/net" + fold + ".gmdh", current);
        }

        // here we differentiate between cases where maxLayers is set to 0 and
        // otherwise
        int iteration = 0;
        Evaluation finalEval = null;

        if (prototype.getMaxLayers() == 0) {
            double lastEvaluated = Double.POSITIVE_INFINITY;
            /* Continue to grow layers
             */
            while (true) {
                Evaluation eval = new Evaluation(data);
                int nrProcessed = 0;
                /* for each pass of the CV / classifier
                 */
                for (int fold = 0; fold < getFolds(); ++fold) {

                    Instances trainData = data.trainCV(getFolds(), fold);
                    Instances testData = data.testCV(getFolds(), fold);

//                    MscWithCv current =
//                            (MscWithCv) weka.core.SerializationHelper.read(
//                            "/net" + fold + ".gmdh");
                    Msc current =
                            (Msc) weka.core.SerializationHelper.read(
                            "/net" + fold + ".gmdh");
                    current.gmdhNet.setAttributeLayer(
                            current.gmdhNet.getDataset());

                    current.next(++current.iterations);
                    current.currentlyBestStructure = current.gmdhNet.bestModel(
                            current.currentlyBestStructure);

                    weka.core.SerializationHelper.write(
                            "/net" + fold + ".gmdh", current);

                    // take the best model, retrain it...
                    current.setOutput(current.iterations, trainData);
                    // ...and evaluate it on test data.
                    for (int inst = 0; inst < testData.numInstances(); ++inst) {
                        eval.evaluateModelOnceAndRecordPrediction(
                                current, testData.instance(inst));
                        ++nrProcessed;
                    }
                    iteration = current.iterations;
                }
                outputString += ("Layer ") + iteration + ": rmse = " +
                        eval.rootMeanSquaredError() +
                        ", processedInstances = " + nrProcessed + ";\n";
                System.out.println(eval.rootMeanSquaredError());
                if (eval.rootMeanSquaredError() < lastEvaluated) {
                    lastEvaluated = eval.rootMeanSquaredError();
                    finalEval = eval;
                } else {
                    if (finalEval == null) {
                        finalEval = eval;
                    }
                    // we choose to break instead of going all the way with
                    // the growth
                    break;
                }
            }

        } else {
            double lastEvaluated = Double.POSITIVE_INFINITY;

            /* Loop that grows layers
             */
            for (int iter = 1; iter <= prototype.getMaxLayers(); ++iter) {
                Evaluation eval = new Evaluation(data);
                int nrProcessed = 0;
                /* for each pass of the CV / classifier
                 */
                for (int fold = 0; fold < getFolds(); ++fold) {

                    Instances trainData = data.trainCV(getFolds(), fold);
                    Instances testData = data.testCV(getFolds(), fold);

//                    MscWithCv current =
//                            (MscWithCv) weka.core.SerializationHelper.read(
//                            "/net" + fold + ".gmdh");
                    Msc current =
                            (Msc) weka.core.SerializationHelper.read(
                            "/net" + fold + ".gmdh");
                    current.gmdhNet.setAttributeLayer(
                            current.gmdhNet.getDataset());

                    current.next(++current.iterations);
                    current.currentlyBestStructure = current.gmdhNet.bestModel(
                            current.currentlyBestStructure);

                    weka.core.SerializationHelper.write(
                            "/net" + fold + ".gmdh", current);

                    // take the best model, retrain it...
                    current.setOutput(current.iterations, trainData);
                    // ...and evaluate it on test data.
                    for (int inst = 0; inst < testData.numInstances(); ++inst) {
                        eval.evaluateModelOnceAndRecordPrediction(
                                current, testData.instance(inst));
                        ++nrProcessed;
                    }
                    iteration = current.iterations;
                }
                outputString += ("Layer ") + iteration + ": rrse = " +
                        eval.rootRelativeSquaredError() +
                        ", processedInstances = " + nrProcessed + ";\n";
                if (eval.rootRelativeSquaredError() < lastEvaluated) {
                    lastEvaluated = eval.rootRelativeSquaredError();
                    finalEval = eval;
                } else {
                    if (finalEval == null) {
                        finalEval = eval;
                    }
                    // we choose to break instead of going all the way with
                    // the growth                    
                    break;
                }
            }
        }
        int depth = (iteration - 1);
        outputString += "\nOptimal layer averages: ";
        outputString += "\ncorr = ";
        outputString += finalEval.correlationCoefficient();
        outputString += "\nmae = ";
        outputString += finalEval.meanAbsoluteError();
        outputString += "\nrmse = ";
        outputString += finalEval.rootMeanSquaredError();
        outputString += "\nrae = ";
        outputString += finalEval.relativeAbsoluteError();
        outputString += "\nrrse = ";
        outputString += finalEval.rootRelativeSquaredError();
        outputString += "\n\nTraining with suggested depth: " + depth;

        System.out.println(outputString);

        // training with the parameter we found
        prototype.setMaxLayers(depth);
        prototype.buildClassifier(data);
    }

    @Override
    public double classifyInstance(Instance instance) throws Exception {
        return prototype.classifyInstance(instance);
    }

    @Override
    public String toString() {

        return outputString;
    }

    public String getRevision() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public TechnicalInformation getTechnicalInformation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the folds
     */
    public int getFolds() {
        return folds;
    }

    /**
     * @param folds the folds to set
     */
    public void setFolds(int folds) {
        this.folds = folds;
    }
}
