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
package ieeei2010;

import ieeei2010.InstancesResultListenerDebug;
import ieeei2010.CrossValidationResultProducerWgmdh;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import wGmdh.Msc;
import wGmdh.jGmdh.hybrid.CfsFilter;
import wGmdh.jGmdh.hybrid.RandomFilter;
import wGmdh.jGmdh.oldskul.NodeFilter;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.measures.Rrse;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.experiment.CSVResultListener;

/**
 * CrossValidationResultProducer passed on to CSV ResultListener
 *
 * @author ivek
 */
public class ParameterSetTesting {

    public static class DatasetFilter implements FilenameFilter {

        private static String noname = "weka_experiment.arff";

        public boolean accept(File dir, String name) {
            return !name.equals(noname);
        }
    }

    public static void main(String[] args) throws IOException, Exception {

        // experiment setup... REMEMBER TO KILL THE FILES
        int runs = 10;
        int folds = 10;
        float[] percentageSplits = {((float)2/3)*100};
        int[] slideFilterSizes = {20, 40, 60, 80, 100};
        int[] cmsFilterSizes = {20, 40, 60};
        int[] randomFilterSizes = {};
        int maxLayer = 120;
        
        // test setup
        /*int runs = 1;
        int folds = 10;
        float[] percentageSplits = {((float) 2 / 3) * 100};
        int[] slideFilterSizes = {50};     // slide filter
        int[] cmsFilterSizes = {10, 15};     // CMS filter
        int[] randomFilterSizes = {};
        int maxLayer = 15;
*/
        // list all datasets from folder
//        String path = "../Selected Datasets, parameter testing 1/";
        //String path = "../Selected Datasets, IEEEI2010/";
        String path = "C:/Users/vekk/Documents/NetBeansProjects/Selected Datasets, IEEEI2010/";
//        String path = "../Selected Datasets, test/";
        File folder = new File(path);
        File[] datasets = folder.listFiles(new DatasetFilter());

        // Initialize listener and producer
        InstancesResultListenerDebug listener = new InstancesResultListenerDebug();
        CrossValidationResultProducerWgmdh producer = new CrossValidationResultProducerWgmdh();
        producer.setNumFolds(folds);

        // deserialize listener and producer
        // UPDATE: won't work
        /*InstancesResultListenerDebug listener = (InstancesResultListenerDebug) weka.core.SerializationHelper.read("/listener.list");

        CrossValidationResultProducerWgmdh producer =
        (CrossValidationResultProducerWgmdh) weka.core.SerializationHelper.read(
        "/producer.prod");
         */
        String[] additionalMeasures = null;

        producer.setResultListener(listener);
        producer.setAdditionalMeasures(additionalMeasures);
        producer.preProcess();


        // constrain the additional measures to be only those allowable
        // by the ResultListener
        String[] columnConstraints = listener.determineColumnConstraints(producer);

        if (columnConstraints != null) {
            producer.setAdditionalMeasures(columnConstraints);
        }

        /* cleanup serialized *.gmdh files
         */
        boolean success = producer.getSplitEvaluator().deleteSerialized();


        /* to have the keys sorted, i'll feed producer with dummy classifiers in
         * order that suits analysis to come.
         */
        System.out.println("Dummy classifiers...");
        double dummyTime = System.currentTimeMillis();
        ArffLoader loader = new ArffLoader();
        loader.setURL(ParameterSetTesting.class.getResource("/Resources/dummy.arff").toString());
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);   // set class

        producer.setInstances(data);

        // instantiate a classifier
        Msc dummy = new Msc();
        PercentageSplitHandler dataProvider = new PercentageSplitHandler();
        dummy.setDataProvider(dataProvider);
        dummy.setMaxLayers(1);
        dummy.setRelearn(false);
        NodeFilter selector = new SlidingFilter(10);
        dummy.setSelector(selector);
        dummy.setStructureValidationPerformanceMeasure(new Rrse());
        dummy.setStructureLearningPerformanceMeasure(new Rrse());
        dummy.setVisualize(false);
        dummy.buildClassifier(data);

        for (int i = 0; i < folds; ++i) {
            weka.core.SerializationHelper.write(
                    RegressionSplitEvaluatorWgmdh.prefix + i +
                    RegressionSplitEvaluatorWgmdh.extension, dummy);
        }

        for (int run = 0; run < 1; ++run) {
            /* Percentage splits traversal
             */
            int seed = 0;
            dummy.setRandomSeed(seed);
            for (int percentageIter = 0;
                    percentageIter < percentageSplits.length;
                    ++percentageIter) {
                dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                /* Filter sizes traversal
                 */
                for (int filterIter = 0;
                        filterIter < slideFilterSizes.length;
                        ++filterIter) {

                    ((SlidingFilter) selector).setTargetLength(slideFilterSizes[filterIter]);
                    dummy.setSelector(selector);

                    /* a loop that grows the gmdh by one layer - traverse
                     * depth parameter
                     */

                    for (int layer = 1; layer <= maxLayer; ++layer) {
                        dummy.setMaxLayers(layer);
                        producer.getSplitEvaluator().setClassifier(dummy);

                        producer.doRunDepthPreset(run);
                    }
                }
            }
        }

        selector = new CfsFilter(5);
        dummy.setSelector(selector);
        for (int run = 0; run < 1; ++run) {
            /* Percentage splits traversal
             */
            int seed = 0;
            dummy.setRandomSeed(seed);
            for (int percentageIter = 0;
                    percentageIter < percentageSplits.length;
                    ++percentageIter) {
                dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                /* Filter sizes traversal
                 */
                for (int filterIter = 0;
                        filterIter < cmsFilterSizes.length;
                        ++filterIter) {
                    ((CfsFilter) selector).setSetsize(cmsFilterSizes[filterIter]);
                    dummy.setSelector(selector);
                    data.setClassIndex(data.numAttributes() - 1);

                    /* a loop that grows the gmdh by one layer - traverse
                     * depth parameter
                     */
                    for (int layer = 1; layer <= maxLayer; ++layer) {
                        dummy.setMaxLayers(layer);
                        producer.getSplitEvaluator().setClassifier(dummy);
                        producer.doRunDepthPreset(run);
                    }
                }
            }
        }

        selector = new RandomFilter(5);
        dummy.setSelector(selector);
        for (int run = 0; run < 1; ++run) {
            /* Percentage splits traversal
             */
            int seed = 0;
            dummy.setRandomSeed(seed);
            for (int percentageIter = 0;
                    percentageIter < percentageSplits.length;
                    ++percentageIter) {
                dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                /* Filter sizes traversal
                 */
                for (int filterIter = 0;
                        filterIter < randomFilterSizes.length;
                        ++filterIter) {
                    ((RandomFilter) selector).setSetsize(randomFilterSizes[filterIter]);
                    dummy.setSelector(selector);
                    data.setClassIndex(data.numAttributes() - 1);

                    /* a loop that grows the gmdh by one layer - traverse
                     * depth parameter
                     */
                    for (int layer = 1; layer <= maxLayer; ++layer) {
                        dummy.setMaxLayers(layer);
                        producer.getSplitEvaluator().setClassifier(dummy);
                        producer.doRunDepthPreset(run);
                    }
                }
            }
        }

        dummyTime = System.currentTimeMillis() - dummyTime;
        System.out.println("Dummies took " + dummyTime + "ms");
        dummy = null;
        System.gc();

        success = producer.getSplitEvaluator().deleteSerialized();
        /* Datasets traversal
         */
        for (int i = 0; i < datasets.length; ++i) {
            File f = datasets[i];

            /* Cross-validation run traversal
             */
            for (int run = 0; run < runs; ++run) {
                dummyTime = System.currentTimeMillis();
                /* Percentage splits traversal
                 */
                int seed = 0;
                for (int percentageIter = 0;
                        percentageIter < percentageSplits.length;
                        ++percentageIter) {
                    /* Filter sizes traversal
                     */
                    for (int filterIter = 0;
                            filterIter < slideFilterSizes.length;
                            ++filterIter) {
                        double elapsed = System.currentTimeMillis();

                        System.out.println("Dataset: " + f.getName());
                        System.out.println("run = " + run + " size = " + slideFilterSizes[filterIter]);

                        // reload out Instances object from file
                        //ArffLoader loader = new ArffLoader();
                        loader.setFile(f);
                        Instances fullSet = loader.getDataSet();
                        fullSet.setClassIndex(fullSet.numAttributes() - 1);

                        // instantiate a classifier with needed parameters
                        Msc msc = new Msc();
                        //PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                        dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                        msc.setDataProvider(dataProvider);
                        msc.setMaxLayers(1);
                        msc.setRandomSeed(seed);
                        msc.setRelearn(false);
                        selector = new SlidingFilter();
                        ((SlidingFilter) selector).setTargetLength(slideFilterSizes[filterIter]);
                        //CfsFilter selector = new CfsFilter();
                        //selector.setSetsize(filterSizes[filterIter]);
                        msc.setSelector(selector);
                        msc.setStructureValidationPerformanceMeasure(new Rrse());
                        msc.setStructureLearningPerformanceMeasure(new Rrse());
                        msc.setVisualize(false);

                        producer.setInstances(fullSet);

                        /* a loop that grows the gmdh by one layer - traverse
                         * depth parameter
                         */
                        int layer = 1;
                        //System.out.println("    layer = " + layer + ", calling doRun()");
                        producer.getSplitEvaluator().setClassifier(msc);
                        producer.callMeBeforeStartingWithIterativeGrowing();
                        producer.passFilenameClassifierPart("slideFilter " + slideFilterSizes[filterIter] +
                                " percentageSplit " + percentageSplits[percentageIter]);
                        producer.doRun(run);

                        for (layer = 2; layer <= maxLayer; ++layer) {
                            if (producer.allDone) {
                                producer.allDone = false;
                                break;
                            }

                            /* load a serialized .gmdh classifier and pass it on
                             * as a template to our SplitEvaluator. doesn't
                             * matter which one is chosen - SplitEvaluator
                             * takes care of loading the needed ones on its own.
                             */
                            Msc currentClassifier =
                                    (Msc) weka.core.SerializationHelper.read(
                                    RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                            currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                            currentClassifier.setSelector(selector);

                            producer.getSplitEvaluator().setClassifier(currentClassifier);
                            //System.out.println("    layer = " + layer + ", calling doRun()");
                            producer.doRun(run);
                        }

                        // ... and pass custom Msc to producer that will
                        // allow us to find the best classifier across datasets
                        // regardless of the depth parameter
                        /* load a serialized .gmdh classifier and pass it on
                         * as a template to our SplitEvaluator. doesn't
                         * matter which one is chosen - SplitEvaluator
                         * takes care of loading the needed ones on its own.
                         */
                        Msc currentClassifier =
                                (Msc) weka.core.SerializationHelper.read(
                                RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                        currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                        MscNoDepthParameter noDepth = new MscNoDepthParameter();
                        noDepth.gmdhNet = currentClassifier.gmdhNet;
                        noDepth.currentlyBestStructure = currentClassifier.gmdhNet.bestModel(null);
                        noDepth.modelToOutput = noDepth.currentlyBestStructure.model;
                        noDepth.setSelector(selector);
                        noDepth.setMaxLayers(0);

                        producer.getSplitEvaluator().setClassifier(noDepth);
                        //currentClassifier.setMaxLayers(99999);
                        //currentClassifier.setSelector(new CfsFilter(99999));
                        //producer.getSplitEvaluator().setClassifier(currentClassifier);
                        producer.doRunDepthPreset(run);

                        /* now we have to delete the serialized gmdhs to allow
                         * for initialization
                         */
                        success = producer.getSplitEvaluator().deleteSerialized();

                        elapsed = System.currentTimeMillis() - elapsed;
                        System.out.println("                 This build lasted " + elapsed + "ms");

                    // for different parameters different randomizations will be obtained
                    }
                }
                dummyTime = System.currentTimeMillis() - dummyTime;
                System.out.println("            This run lasted " + dummyTime + "ms");
            }

        // serialize the producer and listener, to be able to continue if
        // the experiment decides to die for whichever reason
        // UPDATE: this doesn't help - the most important objects in listener and
        // producer don't get serialized
            /*weka.core.SerializationHelper.write(
        "/listener.list", listener);
        weka.core.SerializationHelper.write(
        "/producer.prod", producer);
         */
        }

        // The other filter. too lazy to make a loop.

        /* cleanup serialized *.gmdh files
         */
        success = producer.getSplitEvaluator().deleteSerialized();

        /* Datasets traversal
         */
        for (int i = 0; i < datasets.length; ++i) {
            File f = datasets[i];

            /* Cross-validation run traversal
             */
            for (int run = 0; run < runs; ++run) {
                dummyTime = System.currentTimeMillis();
                /* Percentage splits traversal
                 */
                int seed = 0;
                for (int percentageIter = 0;
                        percentageIter < percentageSplits.length;
                        ++percentageIter) {
                    /* Filter sizes traversal
                     */
                    for (int filterIter = 0;
                            filterIter < cmsFilterSizes.length;
                            ++filterIter) {

                        double elapsed = System.currentTimeMillis();

                        System.out.println("Dataset: " + f.getName());
                        System.out.println("run = " + run + " size = " + cmsFilterSizes[filterIter]);

                        // reload out Instances object from file
                        loader = new ArffLoader();
                        loader.setFile(f);
                        Instances fullSet = loader.getDataSet();
                        fullSet.setClassIndex(fullSet.numAttributes() - 1);

                        // instantiate a classifier with needed parameters
                        Msc msc = new Msc();
                        //PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                        dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                        msc.setDataProvider(dataProvider);
                        msc.setMaxLayers(1);
                        msc.setRandomSeed(seed);
                        msc.setRelearn(false);
                        selector = new CfsFilter();
                        ((CfsFilter) selector).setSetsize(cmsFilterSizes[filterIter]);
                        msc.setSelector(selector);
                        msc.setStructureValidationPerformanceMeasure(new Rrse());
                        msc.setStructureLearningPerformanceMeasure(new Rrse());
                        msc.setVisualize(false);

                        producer.setInstances(fullSet);

                        /* a loop that grows the gmdh by one layer - traverse
                         * depth parameter
                         */
                        int layer = 1;
                        //System.out.println("    layer = " + layer + ", calling doRun()");
                        producer.getSplitEvaluator().setClassifier(msc);
                        producer.callMeBeforeStartingWithIterativeGrowing();
                        producer.passFilenameClassifierPart("cmsFilter " + cmsFilterSizes[filterIter] +
                                " percentageSplit " + percentageSplits[percentageIter]);
                        producer.doRun(run);

                        for (layer = 2; layer <= maxLayer; ++layer) {
                            if (producer.allDone) {
                                producer.allDone = false;
                                break;
                            }

                            /* load a serialized .gmdh classifier and pass it on
                             * as a template to our SplitEvaluator. doesn't
                             * matter which one is chosen - SplitEvaluator
                             * takes care of loading the needed ones on its own.
                             */
                            Msc currentClassifier =
                                    (Msc) weka.core.SerializationHelper.read(
                                    RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                            currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                            currentClassifier.setSelector(selector);

                            producer.getSplitEvaluator().setClassifier(currentClassifier);
                            //System.out.println("    layer = " + layer + ", calling doRun()");
                            producer.doRun(run);
                        }
                        // ... and pass custom Msc to producer that will
                        // allow us to find the best classifier across datasets
                        // regardless of the depth parameter
                                                    /* load a serialized .gmdh classifier and pass it on
                         * as a template to our SplitEvaluator. doesn't
                         * matter which one is chosen - SplitEvaluator
                         * takes care of loading the needed ones on its own.
                         */
                        Msc currentClassifier =
                                (Msc) weka.core.SerializationHelper.read(
                                RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                        currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                        MscNoDepthParameter noDepth = new MscNoDepthParameter();
                        noDepth.gmdhNet = currentClassifier.gmdhNet;
                        noDepth.currentlyBestStructure = currentClassifier.gmdhNet.bestModel(null);
                        noDepth.modelToOutput = noDepth.currentlyBestStructure.model;
                        noDepth.setSelector(selector);
                        noDepth.setMaxLayers(0);

                        producer.getSplitEvaluator().setClassifier(noDepth);
                        //currentClassifier.setMaxLayers(99999);
                        //currentClassifier.setSelector(new CfsFilter(99999));
                        //producer.getSplitEvaluator().setClassifier(currentClassifier);
                        producer.doRunDepthPreset(run);

                        /* now we have to delete the serialized gmdhs to allow
                         * for initialization
                         */
                        success = producer.getSplitEvaluator().deleteSerialized();

                        elapsed = System.currentTimeMillis() - elapsed;
                        System.out.println("                 This build lasted " + elapsed + "ms");

                    // for different parameters different randomizations will be obtained
                    }
                }
                dummyTime = System.currentTimeMillis() - dummyTime;
                System.out.println("            This run lasted " + dummyTime + "ms");
            }
        }

        // The other filter. too lazy to make a loop.

        /* cleanup serialized *.gmdh files
         */
        success = producer.getSplitEvaluator().deleteSerialized();

        /* Datasets traversal
         */
        for (int i = 0; i < datasets.length; ++i) {
            File f = datasets[i];

            /* Cross-validation run traversal
             */
            for (int run = 0; run < runs; ++run) {
                dummyTime = System.currentTimeMillis();
                /* Percentage splits traversal
                 */
                int seed = 0;
                for (int percentageIter = 0;
                        percentageIter < percentageSplits.length;
                        ++percentageIter) {
                    /* Filter sizes traversal
                     */
                    for (int filterIter = 0;
                            filterIter < randomFilterSizes.length;
                            ++filterIter) {

                        double elapsed = System.currentTimeMillis();

                        System.out.println("Dataset: " + f.getName());
                        System.out.println("run = " + run + " size = " + cmsFilterSizes[filterIter]);

                        // reload out Instances object from file
                        loader = new ArffLoader();
                        loader.setFile(f);
                        Instances fullSet = loader.getDataSet();
                        fullSet.setClassIndex(fullSet.numAttributes() - 1);

                        // instantiate a classifier with needed parameters
                        Msc msc = new Msc();
                        //PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                        dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                        msc.setDataProvider(dataProvider);
                        msc.setMaxLayers(1);
                        msc.setRandomSeed(seed);
                        msc.setRelearn(false);
                        selector = new RandomFilter();
                        ((RandomFilter) selector).setSetsize(randomFilterSizes[filterIter]);
                        msc.setSelector(selector);
                        msc.setStructureValidationPerformanceMeasure(new Rrse());
                        msc.setStructureLearningPerformanceMeasure(new Rrse());
                        msc.setVisualize(false);

                        producer.setInstances(fullSet);

                        /* a loop that grows the gmdh by one layer - traverse
                         * depth parameter
                         */
                        int layer = 1;
                        //System.out.println("    layer = " + layer + ", calling doRun()");
                        producer.getSplitEvaluator().setClassifier(msc);
                        producer.callMeBeforeStartingWithIterativeGrowing();
                        producer.passFilenameClassifierPart("randomFilter " + randomFilterSizes[filterIter] +
                                " percentageSplit " + percentageSplits[percentageIter]);
                        producer.doRun(run);

                        for (layer = 2; layer <= maxLayer; ++layer) {
                            if (producer.allDone) {
                                producer.allDone = false;
                                break;
                            }

                            /* load a serialized .gmdh classifier and pass it on
                             * as a template to our SplitEvaluator. doesn't
                             * matter which one is chosen - SplitEvaluator
                             * takes care of loading the needed ones on its own.
                             */
                            Msc currentClassifier =
                                    (Msc) weka.core.SerializationHelper.read(
                                    RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                            currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                            currentClassifier.setSelector(selector);

                            producer.getSplitEvaluator().setClassifier(currentClassifier);
                            //System.out.println("    layer = " + layer + ", calling doRun()");
                            producer.doRun(run);
                        }
                        // ... and pass custom Msc to producer that will
                        // allow us to find the best classifier across datasets
                        // regardless of the depth parameter
                                                    /* load a serialized .gmdh classifier and pass it on
                         * as a template to our SplitEvaluator. doesn't
                         * matter which one is chosen - SplitEvaluator
                         * takes care of loading the needed ones on its own.
                         */
                        Msc currentClassifier =
                                (Msc) weka.core.SerializationHelper.read(
                                RegressionSplitEvaluatorWgmdh.prefix + "1" + RegressionSplitEvaluatorWgmdh.extension);
                        currentClassifier.setMaxLayers(currentClassifier.getMaxLayers() + 1);
                        MscNoDepthParameter noDepth = new MscNoDepthParameter();
                        noDepth.gmdhNet = currentClassifier.gmdhNet;
                        noDepth.currentlyBestStructure = currentClassifier.gmdhNet.bestModel(null);
                        noDepth.modelToOutput = noDepth.currentlyBestStructure.model;
                        noDepth.setSelector(selector);
                        noDepth.setMaxLayers(0);

                        producer.getSplitEvaluator().setClassifier(noDepth);
                        //currentClassifier.setMaxLayers(99999);
                        //currentClassifier.setSelector(new CfsFilter(99999));
                        //producer.getSplitEvaluator().setClassifier(currentClassifier);
                        producer.doRunDepthPreset(run);

                        /* now we have to delete the serialized gmdhs to allow
                         * for initialization
                         */
                        success = producer.getSplitEvaluator().deleteSerialized();

                        elapsed = System.currentTimeMillis() - elapsed;
                        System.out.println("                 This build lasted " + elapsed + "ms");

                    // for different parameters different randomizations will be obtained
                    }
                }
                dummyTime = System.currentTimeMillis() - dummyTime;
                System.out.println("            This run lasted " + dummyTime + "ms");
            }
        }
        // serialize the producer and listener, to be able to continue if
        // the experiment decides to die for whichever reason
        // UPDATE: this doesn't help - the most important objects in listener and
        // producer don't get serialized
            /*weka.core.SerializationHelper.write(
        "/listener.list", listener);
        weka.core.SerializationHelper.write(
        "/producer.prod", producer);
         */

        //izvadi output od listenera
        File resultFile = ((CSVResultListener) listener).getOutputFile();

        // cleanup
        producer.postProcess();
        listener.postProcess(producer);
    }

    static void copyFile(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        byte[] buff = new byte[1024];
        int len;
        while ((len = in.read(buff)) > 0) {
            out.write(buff, 0, len);
        }
        in.close();
        out.close();
    }
}/* TODO: constructing selectors in the loop. setting stuff to same value in the
 * loop. redundant.
 */
