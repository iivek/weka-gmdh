/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ieeei2010;

import java.io.File;
import java.io.IOException;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

/**
 * Won't help with sorting the keys. bummer
 *
 * @author ivek
 */
public class SortProducerOutput {

    public static void main(String[] args) throws IOException, Exception {

        /* load
         */
        ArffLoader loader = new ArffLoader();
        loader.setFile(new File("../CMS_experiment.arff"));
        Instances data = loader.getDataSet();
        System.out.println("original");
        System.out.println(data);
        if (false) {            /* sort
             */
            data.sort(4);
            System.out.println("processed");
            System.out.println(data);
        }
        /* remove the y dummy dataset
         */


        /* save
         */
        ArffSaver saver = new ArffSaver();
        saver.setInstances(data);
        saver.setFile(new File("../CMS_experiment_sorted.arff"));
        saver.writeBatch();

    }
}
