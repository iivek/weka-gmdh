package ieeei2010;

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
 * Parses .arff's filename to extract optimal parameter setup, trains Mscs
 * using those parameters and feeds
 *
 * @author ivek
 */
public class SlideAndCMSComparison {

    public static void main(String[] args) throws IOException, Exception {

        int runs = 2;
        int folds = 10;

        // this path leads to datasets that have optimal parameters embedded
        // in their names, as well
        String pathEmbedded = "C:/Users/vekk/Documents/NetBeansProjects/CMS paper, slide, CV with randomization, JAMA/";
                //String pathEmbedded = "C:/Users/vekk/Documents/NetBeansProjects/CMS paper, slide, test/";
        File folderEmbedded = new File(pathEmbedded);
        File[] datasetsEmbedded = folderEmbedded.listFiles();
        // this path leads to the original datasets
        String pathOriginal = "C:/Users/vekk/Documents/NetBeansProjects/Selected Datasets, raw/";

        // producer and listener stuff
        InstancesResultListenerDebug listener = new InstancesResultListenerDebug();
        CrossValidationResultProducer producer = new CrossValidationResultProducer();
        // listener.setOutputFileName(pathOriginal + "results.arff");
        //System.out.println(pathOriginal + "results.arff");

        producer.setNumFolds(folds);

        String[] additionalMeasures = null;

        producer.setResultListener(listener);
        producer.setAdditionalMeasures(additionalMeasures);
        producer.setSplitEvaluator(new RegressionSplitEvaluator());

        // constrain the additional measures to be only those allowable
        // by the ResultListener
        String[] columnConstraints = listener.determineColumnConstraints(producer);

        if (columnConstraints != null) {
            producer.setAdditionalMeasures(columnConstraints);
        }
        producer.preProcess();
        
        // producer and listener stuff ends

        for (File f : datasetsEmbedded) {
            // parsing filenames
            String filename = f.getName();
            System.out.println("    " + filename);

            String[] splits = filename.split("__");
            // remove the ".arff" from the last (third) split

            splits[3] = (splits[3].split("\\."))[0];

            Vector<Float> percentageSplits = new Vector<Float>();
            // splits[1]    contains percentages
            String[] toParse = splits[1].split("_");
            for (String s : toParse) {
                percentageSplits.add(Float.parseFloat(s));
            }
            Vector<Integer> filterSizes = new Vector<Integer>();
            // splits[2]    contains filter sizes
            toParse = splits[2].split("_");
            for (String s : toParse) {
                filterSizes.add(Integer.parseInt(s));
            }
            // splits[3]    contains optimal depths
            Vector<Integer> depths = new Vector<Integer>();
            toParse = splits[3].split("_");
            for (String s : toParse) {
                depths.add(Integer.parseInt(s));
            }
            // parsing finished


            // load original dataset: splits[0] + ".arff" is its name
            ArffLoader loader = new ArffLoader();
            File original = new File(pathOriginal + splits[0] + ".arff");
            System.out.println(pathOriginal + splits[0] + ".arff");
            loader.setFile(original);
            Instances dataset = loader.getDataSet();
            //Random random = new Random(1);
            //dataset.randomize(random);
            dataset.setClassIndex(dataset.numAttributes() - 1);

            for (int run = 0; run < runs; ++run) {

                Iterator<Integer> depthIter = depths.iterator();
                for (Float percentage : percentageSplits) {
                    for (Integer length : filterSizes) {
                        Integer optimalDepth = depthIter.next();

                        System.out.println("percentage = " + percentage +
                                "; size = " + length + "; depth = " + optimalDepth);

                        // instantiate classifier
                        // use setters to set parameters
                        Msc msc = new Msc();
                        PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                        dataProvider.setTrainPercentage(percentage);
                        msc.setDataProvider(dataProvider);
                        msc.setMaxLayers(optimalDepth);
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
                        ((RegressionSplitEvaluator)producer.getSplitEvaluator()).setClassifier(msc);
                        producer.doRun(run);
                    }
                }
            }
        }

        /////////////////////////////////// now everything once more, for CMS
        //pathEmbedded = "C:/Users/vekk/Documents/NetBeansProjects/CMS paper, CMS, CV with randomization, JAMA/";
/*        pathEmbedded = "C:/Users/vekk/Documents/NetBeansProjects/CMS paper, CMS, test/";
        folderEmbedded = new File(pathEmbedded);
        datasetsEmbedded = folderEmbedded.listFiles();
                for (File f : datasetsEmbedded) {
            // parsing filenames
            String filename = f.getName();
            System.out.println("    " + filename);

            String[] splits = filename.split("__");
            // remove the ".arff" from the last (third) split

            splits[3] = (splits[3].split("\\."))[0];

            Vector<Float> percentageSplits = new Vector<Float>();
            // splits[1]    contains percentages
            String[] toParse = splits[1].split("_");
            for (String s : toParse) {
                percentageSplits.add(Float.parseFloat(s));
            }
            Vector<Integer> filterSizes = new Vector<Integer>();
            // splits[2]    contains filter sizes
            toParse = splits[2].split("_");
            for (String s : toParse) {
                filterSizes.add(Integer.parseInt(s));
            }
            // splits[3]    contains optimal depths
            Vector<Integer> depths = new Vector<Integer>();
            toParse = splits[3].split("_");
            for (String s : toParse) {
                depths.add(Integer.parseInt(s));
            }

            System.out.println(splits[0]);
            // parsing finished


            // load original dataset: splits[0] + ".arff" is its name
            ArffLoader loader = new ArffLoader();
            File original = new File(pathOriginal + splits[0] + ".arff");
            loader.setFile(original);
            Instances dataset = loader.getDataSet();
            //Random random = new Random(1);
            //dataset.randomize(random);
            dataset.setClassIndex(dataset.numAttributes() - 1);

            for (int run = 0; run < runs; ++run) {

                Iterator<Integer> depthIter = depths.iterator();
                for (Float percentage : percentageSplits) {
                    for (Integer length : filterSizes) {
                        Integer optimalDepth = depthIter.next();

                        System.out.println("percentage = " + percentage +
                                "; size = " + length + "; depth = " + optimalDepth);

                        // instantiate classifier
                        // use setters to set parameters
                        Msc msc = new Msc();
                        PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                        dataProvider.setTrainPercentage(percentage);
                        msc.setDataProvider(dataProvider);
                        msc.setMaxLayers(optimalDepth);
                        //msc.setRandomSeed(0);
                        msc.setRelearn(false);
// sliding filter or CMS gets chosen here
                        CfsFilter selector = new CfsFilter();
                        selector.setSetsize(length);
                        msc.setSelector(selector);
                        msc.setStructureValidationPerformanceMeasure(new Sse());
                        msc.setVisualize(false);
                        //msc.buildClassifier(dataset);

                        producer.setInstances(dataset);
                        ((RegressionSplitEvaluator)producer.getSplitEvaluator()).setClassifier(msc);
                        producer.doRun(run);
                    }
                }
            }
        }*/
        // cleanup
        producer.postProcess();
        listener.postProcess(producer);
    }
}
