package ieeei2010;

import ieeei2010.ParameterSetTesting.DatasetFilter;
import wGmdh.jGmdh.playground.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;
import wGmdh.Msc;
import wGmdh.jGmdh.hybrid.CfsFilter;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.measures.Sse;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.experiment.CSVResultListener;
import weka.experiment.CrossValidationResultProducer;
import weka.experiment.RegressionSplitEvaluator;

/**
 * Train and evaluate a whole bunch of classifiers. As simple as that.
 *
 * Really, i'd like to compare their convergence properties, on the control set
 * and on validation set
 * @author ivek
 */
public class SlideAndCMSComparisonMkII {

    public static void main(String[] args) throws IOException, Exception {

        int runs = 2;
        int folds = 10;
        float[] percentageSplits = {66};
        int[] filterSizes = {10};     // slide filter

        //float[] percentageSplits = {10, 50, 90};
        //int[] filterSizes = {10, 20};     // slide filter
        int maxDepth = 3;

        // list all datasets from folder
        String path = "../Selected Datasets, parameter testing 1/";
        //String path = "../Selected Datasets, test/";
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
        // constrain the additional measures to be only those allowable
        // by the ResultListener
        String[] columnConstraints = listener.determineColumnConstraints(producer);

        if (columnConstraints != null) {
            producer.setAdditionalMeasures(columnConstraints);
        }
        producer.setResultListener(listener);
        producer.preProcess();




        // producer and listener stuff ends

        for (File f : datasets) {

            System.out.println("Dataset: " + f.getName());

            // load original dataset: splits[0] + ".arff" is its name
            ArffLoader loader = new ArffLoader();
            loader.setFile(f);
            Instances dataset = loader.getDataSet();
            //Random random = new Random(1);
            //dataset.randomize(random);
            dataset.setClassIndex(dataset.numAttributes() - 1);

            for (int run = 0; run < runs; ++run) {

                for (Float percentage : percentageSplits) {
                    for (Integer length : filterSizes) {
                                        // instantiate classifier
                            // use setters to set parameters
                            Msc msc = new Msc();
                            PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                            dataProvider.setTrainPercentage(percentage);
                            msc.setDataProvider(dataProvider);
                            msc.setMaxLayers(0);
                            //  msc.setRandomSeed(0);
                            msc.setRelearn(false);
// sliding filter or CMS gets chosen here
                            SlidingFilter selector = new SlidingFilter();
                            selector.setTargetLength(length);
                            msc.setSelector(selector);
                            msc.setStructureValidationPerformanceMeasure(new Sse());
                            msc.setVisualize(false);
                            //msc.buildClassifier(dataset);
                            producer.setInstances(dataset);
                            ((RegressionSplitEvaluator) producer.getSplitEvaluator()).setClassifier(msc);
                            producer.doRun(run);

                        for (int depth = 0; depth < maxDepth; ++depth) {
  //wotizadis                          Integer optimalDepth = depthIter.next();

                            System.out.println("percentage = " + percentage +
                                    "; size = " + length + "; depth = " + depth);


                        }
                    }
                }
            }
        }
        // cleanup
        producer.postProcess();
        listener.postProcess(producer);
    }
}
