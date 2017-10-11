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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import wGmdh.GmdhDepthSearch;
import wGmdh.Msc;
import wGmdh.jGmdh.hybrid.CfsFilter;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.measures.Sse;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;
import weka.core.Instances;
import weka.core.converters.ArffLoader;

/**
 *
 * @author ivek
 */
public class DepthOptimizationAndRenaming {

    public static void main(String[] args) throws IOException, Exception {

        float[] percentageSplits = {((float)2/3)*100};
        //float[] percentageSplits = {50, 90};
        int[] filterSizes = {10, 20, 40, 80, 160, 320}; // sliding
        //int[] filterSizes = {10, 20, 40, 80}; // CFS

        //int[] filterSizes = {10, 20, 40, 80, 160}; // sliding
        

        /* List all datasets
         */
        /*        String[] datasetPaths = {
        "regressiontest.arff"
        };
         */
        
        String path = "../CMS paper mkII, slide/";
        //String path = "../CMS paper, CMS, CV with randomization/";
        //String path = "../CMS paper, slide, test/";
        
        File folder = new File(path);
        File[] datasets = folder.listFiles();

        // for each dataset
        for (int i = 0; i < datasets.length; ++i) {
            String nameSuffix = new String("");
            File f = datasets[i];
            System.out.println("Dataset: " + f.getName());

            ArffLoader loader = new ArffLoader();
            loader.setFile(f);
            Instances fullSet = loader.getDataSet();
            fullSet.setClassIndex(fullSet.numAttributes() - 1);

            nameSuffix += "_";
            for (int percentageIter = 0;
                    percentageIter < percentageSplits.length;
                    ++percentageIter) {
                nameSuffix += "_" + String.valueOf(percentageSplits[percentageIter]);
            }
            nameSuffix += "_";
            for (int filterIter = 0;
                    filterIter < filterSizes.length;
                    ++filterIter) {
                nameSuffix += "_" + String.valueOf(filterSizes[filterIter]);
            }

            nameSuffix += "_";

            // iterate through listed percentage splits
            int seed = 0;
            for (int percentageIter = 0;
                    percentageIter < percentageSplits.length;
                    ++percentageIter) {
                // iterate through listed filter sizes
                for (int filterIter = 0;
                        filterIter < filterSizes.length;
                        ++filterIter) {

                    fullSet = loader.getDataSet();
                    fullSet.setClassIndex(fullSet.numAttributes() - 1);

                    System.out.println(percentageSplits[percentageIter] + ", " + filterSizes[filterIter]);
                    // instantiate our metaclassifier
                    GmdhDepthSearch meta = new GmdhDepthSearch();
                    // use setters to set parameters
                    meta.setFolds(10);
                    //meta.setSeed(seed++);
                    Msc msc = new Msc();
                    PercentageSplitHandler dataProvider = new PercentageSplitHandler();
                    dataProvider.setTrainPercentage(percentageSplits[percentageIter]);
                    msc.setDataProvider(dataProvider);
                    msc.setMaxLayers(0);
                    //msc.setRandomSeed(0);
                    msc.setRelearn(false);
                    SlidingFilter selector = new SlidingFilter();
                    //CfsFilter selector = new CfsFilter();
                    selector.setTargetLength(filterSizes[filterIter]);
                    //selector.setSetsize(filterSizes[filterIter]);
                    msc.setSelector(selector);
                    msc.setStructureValidationPerformanceMeasure(new Sse());
                    msc.setVisualize(false);
                    meta.setClassifier(msc);

                    meta.buildClassifier(fullSet);

                    String output = meta.toString();
                    String indicator = "Training with suggested depth: ";
                    String[] tokens = output.split(indicator);
                    String suggestedDepth = tokens[tokens.length - 1];
                    System.out.println(suggestedDepth);
//                    nameSuffix += "__" + String.valueOf(percentageSplits[percentageIter]);
//                    nameSuffix += "_" + String.valueOf(filterSizes[filterIter]);
                    nameSuffix += "_" + String.valueOf(suggestedDepth);
                //Integer.parseInt(tokens[tokens.length-1]);
                }
            }
            String[] tokens = f.getName().split("\\.");
            System.out.println(path + tokens[0] + nameSuffix + "." + tokens[1]);
            copyFile(f, new File(path + tokens[0] + nameSuffix + "." + tokens[1]));
        }
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
}
