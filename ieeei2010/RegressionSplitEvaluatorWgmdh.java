/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ieeei2010;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.Msc;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.TwoInputModel;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.core.AdditionalMeasureProducer;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Summarizable;
import weka.experiment.RegressionSplitEvaluator;

/**
 *
 * @author ivek
 */
public final class RegressionSplitEvaluatorWgmdh extends RegressionSplitEvaluator {

    private static final long serialVersionUID = -5735699236657522857L;
    /** The length of a result */
    private static final int RESULT_SIZE = 21;
    private int crossvalidationFold = 0;
    private int crossvalidationFolds = 10;
    public int postFirstMinimumGrowing = 10;
    public static String prefix = "/net";
//    public static String prefixNoDepth = "/netNoDepth";
    public static String extension = ".gmdh";

    // we want 10 more layers after hitting the first minimum
    public boolean deleteSerialized() {
        boolean succeeded = true;
        for (int i = 0; i < crossvalidationFolds; ++i) {
            String name = prefix + i + extension;
            File toDelete = new File(name);
            if (toDelete.delete() == false) {
                succeeded = false;
            }
        }
        System.out.println("deletingSucceeded = " + succeeded);
        return succeeded;
    }

    /**
     * A change is made where buildClassifier is called in RegressionSplitEvaluator's
     * method
     * 
     * @param train
     * @param test
     * @return
     * @throws Exception
     */
    @Override
    public Object[] getResult(Instances train, Instances test)
            throws Exception {

        if (train.classAttribute().type() != Attribute.NUMERIC) {
            throw new Exception("Class attribute is not numeric!");
        }
        if (m_Template == null) {
            throw new Exception("No classifier has been specified");
        }
        ThreadMXBean thMonitor = ManagementFactory.getThreadMXBean();
        boolean canMeasureCPUTime = thMonitor.isThreadCpuTimeSupported();
        if (!thMonitor.isThreadCpuTimeEnabled()) {
            thMonitor.setThreadCpuTimeEnabled(true);
        }

        int addm = (m_AdditionalMeasures != null) ? m_AdditionalMeasures.length : 0;
        Object[] result = new Object[RESULT_SIZE + addm];
        long thID = Thread.currentThread().getId();
        long CPUStartTime = -1, trainCPUTimeElapsed = -1, testCPUTimeElapsed = -1,
                trainTimeStart, trainTimeElapsed, testTimeStart, testTimeElapsed;
        Evaluation eval = new Evaluation(train);
//        m_Classifier = Classifier.makeCopy(m_Template);

        trainTimeStart = System.currentTimeMillis();
        if (canMeasureCPUTime) {
            CPUStartTime = thMonitor.getThreadUserTime(thID);
        }

        /*
         * CHANGES START
         */
        //m_Classifier.buildClassifier(train);
        // load gmdh whose growth we would like to continue
        String path = prefix + getCrossvalidationFold() + extension;
        File f = new File(path);

        if (!f.exists()) {
            // this indicates that we have to initialize our classifier
            //System.out.println("going for the template");
            m_Classifier = Classifier.makeCopy(m_Template);
            m_Classifier.buildClassifier(train);
//            System.out.println("initializinig classifier: " + path);
        } else {
            //System.out.println("    deserializing classifier");
            double deserializationElapsed = System.currentTimeMillis();
            Msc currentClassifier =
                    (Msc) weka.core.SerializationHelper.read(path);
            deserializationElapsed = System.currentTimeMillis() - deserializationElapsed;
            //System.out.println("    deserializing took " + deserializationElapsed + "ms");

            if (currentClassifier.currentlyBestStructure.layersWithoutImprovement <
                    postFirstMinimumGrowing) {

                /* grow the network
                 */
                // reconnect dataset
                //  currentClassifier.gmdhNet.setAttributeLayer(
                //          currentClassifier.gmdhNet.getDataset());
                currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                currentClassifier.next(currentClassifier.iterations++);

                /* we'd like to see how the algorithm behaves after convergence,
                 * so we'll continue to grow the net and track and save eval outputs
                 * for several layers more...
                 */
                if (currentClassifier.currentlyBestStructure.layersWithoutImprovement > 0) {
                    //System.out.println("    after-first-minimum-learning for " + currentClassifier.currentlyBestStructure.layersWithoutImprovement + " layers");
                    int topLayer = currentClassifier.gmdhNet.selectedLayers.size() - 1;
                    int bestLocation = currentClassifier.gmdhNet.selectedLayers.get(topLayer).size() - 1;
                    currentClassifier.modelToOutput =
                            (TwoInputModel) currentClassifier.gmdhNet.selectedLayers.get(topLayer).get(bestLocation);
                    ++currentClassifier.currentlyBestStructure.layersWithoutImprovement;
                } else {
                    currentClassifier.currentlyBestStructure = currentClassifier.gmdhNet.bestModel(
                            currentClassifier.currentlyBestStructure);
                    currentClassifier.modelToOutput =
                            currentClassifier.currentlyBestStructure.model;
                /*if (currentClassifier.currentlyBestStructure.layersWithoutImprovement == 1) {
                // first minimum hit.
                System.out.println("first minimum hit");

                //serialize the classifier as a MscNoDepthParameter
                //                        String pathNoDepth = prefixNoDepth + getCrossvalidationFold() + extension;
                //                        weka.core.SerializationHelper.write(pathNoDepth, currentClassifier);
                }
                 */
                }

            } else {
                return null;
            }

            m_Classifier = currentClassifier;
        }
        weka.core.SerializationHelper.write(
                prefix + getCrossvalidationFold() + extension, m_Classifier);

        //
        /*
         * CHANGES END
         */

        if (canMeasureCPUTime) {
            trainCPUTimeElapsed = thMonitor.getThreadUserTime(thID) - CPUStartTime;
        }
        trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
        testTimeStart = System.currentTimeMillis();
        if (canMeasureCPUTime) {
            CPUStartTime = thMonitor.getThreadUserTime(thID);
        }
        eval.evaluateModel(m_Classifier, test);
        if (canMeasureCPUTime) {
            testCPUTimeElapsed = thMonitor.getThreadUserTime(thID) - CPUStartTime;
        }
        testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        thMonitor = null;

        m_result = eval.toSummaryString();

        /*
         * CHANGES BEGIN
         */

        weka.core.SerializationHelper.write(
                prefix + this.getCrossvalidationFold() + extension, m_Classifier);

        // we'll save the eval stuff organised in files, to be able to track convergence of gmdh
        // error on the left-out validation set
        appendToFile(filenameMarker + " validation corr " + ".txt", "" + eval.correlationCoefficient());
        appendToFile(filenameMarker +" validation mae " + ".txt", "" + eval.meanAbsoluteError());
        appendToFile(filenameMarker +" validation rmse " + ".txt", "" + eval.rootMeanSquaredError());
        appendToFile(filenameMarker +" validation rae "  + ".txt", "" + eval.relativeAbsoluteError());
        appendToFile(filenameMarker +" validation rrse "  + ".txt", "" + eval.rootRelativeSquaredError());

        Msc currentMsc = (Msc) m_Classifier;
        // error on the algorithm's control set
        appendToFile(filenameMarker +" learning error" + ".txt", "" +
                currentMsc.currentlyBestStructure.model.getErrorStructureLearningSet());
        appendToFile(filenameMarker +" control error" +".txt", "" +
                currentMsc.currentlyBestStructure.model.getErrorStructureValidationSet());


        /*
         * CHANGES END
         */

        // The results stored are all per instance -- can be multiplied by the
        // number of instances to get absolute numbers
        int current = 0;
        result[current++] = new Double(train.numInstances());
        result[current++] = new Double(eval.numInstances());

        result[current++] = new Double(eval.meanAbsoluteError());
        result[current++] = new Double(eval.rootMeanSquaredError());
        result[current++] = new Double(eval.relativeAbsoluteError());
        result[current++] = new Double(eval.rootRelativeSquaredError());
        result[current++] = new Double(eval.correlationCoefficient());

        result[current++] = new Double(eval.SFPriorEntropy());
        result[current++] = new Double(eval.SFSchemeEntropy());
        result[current++] = new Double(eval.SFEntropyGain());
        result[current++] = new Double(eval.SFMeanPriorEntropy());
        result[current++] = new Double(eval.SFMeanSchemeEntropy());
        result[current++] = new Double(eval.SFMeanEntropyGain());

        // Timing stats
        result[current++] = new Double(trainTimeElapsed / 1000.0);
        result[current++] = new Double(testTimeElapsed / 1000.0);
        if (canMeasureCPUTime) {
            result[current++] = new Double((trainCPUTimeElapsed / 1000000.0) / 1000.0);
            result[current++] = new Double((testCPUTimeElapsed / 1000000.0) / 1000.0);
        } else {
            result[current++] = new Double(Instance.missingValue());
            result[current++] = new Double(Instance.missingValue());
        }

        // sizes
        ByteArrayOutputStream bastream = new ByteArrayOutputStream();
        ObjectOutputStream oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(m_Classifier);
        result[current++] = new Double(bastream.size());
        bastream = new ByteArrayOutputStream();
        oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(train);
        result[current++] = new Double(bastream.size());
        bastream = new ByteArrayOutputStream();
        oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(test);
        result[current++] = new Double(bastream.size());

        if (m_Classifier instanceof Summarizable) {
            result[current++] = ((Summarizable) m_Classifier).toSummaryString();
        } else {
            result[current++] = null;
        }

        for (int i = 0; i < addm; i++) {
            if (m_doesProduce[i]) {
                try {
                    double dv = ((AdditionalMeasureProducer) m_Classifier).getMeasure(m_AdditionalMeasures[i]);
                    if (!Instance.isMissingValue(dv)) {
                        Double value = new Double(dv);
                        result[current++] = value;
                    } else {
                        result[current++] = null;
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            } else {
                result[current++] = null;
            }
        }

        if (current != RESULT_SIZE + addm) {
            throw new Error("Results didn't fit RESULT_SIZE");
        }
        return result;
    }

    /**
     *
     * @param train
     * @param test
     * @return
     * @throws java.lang.Exception
     */
    public Object[] getResultDepthPreset(Instances train, Instances test)
            throws Exception {

        if (train.classAttribute().type() != Attribute.NUMERIC) {
            throw new Exception("Class attribute is not numeric!");
        }
        if (m_Template == null) {
            throw new Exception("No classifier has been specified");
        }
        ThreadMXBean thMonitor = ManagementFactory.getThreadMXBean();
        boolean canMeasureCPUTime = thMonitor.isThreadCpuTimeSupported();
        if (!thMonitor.isThreadCpuTimeEnabled()) {
            thMonitor.setThreadCpuTimeEnabled(true);
        }

        int addm = (m_AdditionalMeasures != null) ? m_AdditionalMeasures.length : 0;
        Object[] result = new Object[RESULT_SIZE + addm];
        long thID = Thread.currentThread().getId();
        long CPUStartTime = -1, trainCPUTimeElapsed = -1, testCPUTimeElapsed = -1,
                trainTimeStart, trainTimeElapsed, testTimeStart, testTimeElapsed;
        Evaluation eval = new Evaluation(train);
//        m_Classifier = Classifier.makeCopy(m_Template);

        trainTimeStart = System.currentTimeMillis();
        if (canMeasureCPUTime) {
            CPUStartTime = thMonitor.getThreadUserTime(thID);
        }

        /*
         * CHANGES START
         */
        //m_Classifier.buildClassifier(train);

        // load gmdh whose growth we would like to continue
        //String path = prefixNoDepth + getCrossvalidationFold() + extension;
        String path = prefix + getCrossvalidationFold() + extension;
        File f = new File(path);

        // an empty shell to be filled up with meaningful members.
        // m_Template should be MscNoDepthParameter
        m_Classifier = Classifier.makeCopy(m_Template);

        double deserializationElapsed = System.currentTimeMillis();
        Msc currentClassifier =
                (Msc) weka.core.SerializationHelper.read(path);
        deserializationElapsed = System.currentTimeMillis() - deserializationElapsed;
        //System.out.println("    deserializing took " + deserializationElapsed + "ms");

        currentClassifier.currentlyBestStructure = null;
        currentClassifier.currentlyBestStructure = currentClassifier.gmdhNet.bestModel(
                null);
        currentClassifier.modelToOutput =
                currentClassifier.currentlyBestStructure.model;

        //MscNoDepthParameter noDepth = new MscNoDepthParameter();
/*        MscNoDepthParameter noDepth = (MscNoDepthParameter) m_Classifier;
        System.out.println(noDepth.modelToOutput.getIdentifier());
        noDepth.gmdhNet = currentClassifier.gmdhNet;
        noDepth.currentlyBestStructure = currentClassifier.currentlyBestStructure;
        noDepth.modelToOutput = noDepth.currentlyBestStructure.model;
        System.out.println("minimum occured at layer " + noDepth.currentlyBestStructure.layerIndex);
         */
        m_Classifier = currentClassifier;

        //
        /*
         * CHANGES END
         */

        if (canMeasureCPUTime) {
            trainCPUTimeElapsed = thMonitor.getThreadUserTime(thID) - CPUStartTime;
        }
        trainTimeElapsed = System.currentTimeMillis() - trainTimeStart;
        testTimeStart = System.currentTimeMillis();
        if (canMeasureCPUTime) {
            CPUStartTime = thMonitor.getThreadUserTime(thID);
        }
        eval.evaluateModel(m_Classifier, test);
        if (canMeasureCPUTime) {
            testCPUTimeElapsed = thMonitor.getThreadUserTime(thID) - CPUStartTime;
        }
        testTimeElapsed = System.currentTimeMillis() - testTimeStart;
        thMonitor = null;

        m_result = eval.toSummaryString();

        // The results stored are all per instance -- can be multiplied by the
        // number of instances to get absolute numbers
        int current = 0;
        result[current++] = new Double(train.numInstances());
        result[current++] = new Double(eval.numInstances());

        result[current++] = new Double(eval.meanAbsoluteError());
        result[current++] = new Double(eval.rootMeanSquaredError());
        result[current++] = new Double(eval.relativeAbsoluteError());
        result[current++] = new Double(eval.rootRelativeSquaredError());
        result[current++] = new Double(eval.correlationCoefficient());

        result[current++] = new Double(eval.SFPriorEntropy());
        result[current++] = new Double(eval.SFSchemeEntropy());
        result[current++] = new Double(eval.SFEntropyGain());
        result[current++] = new Double(eval.SFMeanPriorEntropy());
        result[current++] = new Double(eval.SFMeanSchemeEntropy());
        result[current++] = new Double(eval.SFMeanEntropyGain());

        // Timing stats
        result[current++] = new Double(trainTimeElapsed / 1000.0);
        result[current++] = new Double(testTimeElapsed / 1000.0);
        if (canMeasureCPUTime) {
            result[current++] = new Double((trainCPUTimeElapsed / 1000000.0) / 1000.0);
            result[current++] = new Double((testCPUTimeElapsed / 1000000.0) / 1000.0);
        } else {
            result[current++] = new Double(Instance.missingValue());
            result[current++] = new Double(Instance.missingValue());
        }

        // sizes
        ByteArrayOutputStream bastream = new ByteArrayOutputStream();
        ObjectOutputStream oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(m_Classifier);
        result[current++] = new Double(bastream.size());
        bastream = new ByteArrayOutputStream();
        oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(train);
        result[current++] = new Double(bastream.size());
        bastream = new ByteArrayOutputStream();
        oostream = new ObjectOutputStream(bastream);
        oostream.writeObject(test);
        result[current++] = new Double(bastream.size());

        if (m_Classifier instanceof Summarizable) {
            result[current++] = ((Summarizable) m_Classifier).toSummaryString();
        } else {
            result[current++] = null;
        }

        for (int i = 0; i < addm; i++) {
            if (m_doesProduce[i]) {
                try {
                    double dv = ((AdditionalMeasureProducer) m_Classifier).getMeasure(m_AdditionalMeasures[i]);
                    if (!Instance.isMissingValue(dv)) {
                        Double value = new Double(dv);
                        result[current++] = value;
                    } else {
                        result[current++] = null;
                    }
                } catch (Exception ex) {
                    System.err.println(ex);
                }
            } else {
                result[current++] = null;
            }
        }

        if (current != RESULT_SIZE + addm) {
            throw new Error("Results didn't fit RESULT_SIZE");
        }
        return result;
    }

    /**
     * @return the crossvalidationFold
     */
    public int getCrossvalidationFold() {
        return crossvalidationFold;
    }

    /**
     * @param crossvalidationFold the crossvalidationFold to set
     */
    public void setCrossvalidationFold(int crossvalidationFold) {
        this.crossvalidationFold = crossvalidationFold;
    }

    /**
     * @return the crossvalidationFolds
     */
    public int getCrossvalidationFolds() {
        return crossvalidationFolds;
    }

    /**
     * @param crossvalidationFolds the crossvalidationFolds to set
     */
    public void setCrossvalidationFolds(int crossvalidationFolds) {
        this.crossvalidationFolds = crossvalidationFolds;
    }
    String filenameMarker = null;

    /**
     * Sets the filename part that will be unique to this dataset, run and fold.
     * @param marker
     */
    public void setFilenameMarker(String marker) {
        filenameMarker = marker;
/*        if (m_Classifier.getOptions()[3].equals("1")) {
            String options = new String();
            // we're interested in first option only
            for (int i = 0; i < 2; ++i) {
                String s = m_Classifier.getOptions()[i];
                options += s + " ";
            }
            appendEntryToLogFiles(options);
        }
*/
    }

    public String getFilenameMarker() {
        return filenameMarker;
    }

    public static void appendToFile(String filename, String s) {
        FileWriter fstream = null;
        try {
            fstream = new FileWriter(filename, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(s);
            out.newLine();
            out.flush();
        } catch (IOException ex) {
            Logger.getLogger(RegressionSplitEvaluatorWgmdh.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                fstream.close();
            } catch (IOException ex) {
                Logger.getLogger(RegressionSplitEvaluatorWgmdh.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Classifier getClassifier() {
        return this.m_Classifier;
    }
}
