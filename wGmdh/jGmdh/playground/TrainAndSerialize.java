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
package wGmdh.jGmdh.playground;

import java.io.IOException;
import java.util.logging.Level;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import wGmdh.jGmdh.gui.GuiManager;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.TwoInputModelFactory;
import wGmdh.jGmdh.oldskul.measures.Rrse;
import wGmdh.jGmdh.oldskul.measures.Sse;
import wGmdh.jGmdh.oldskul.measures.StructureLearningPerformance;
import wGmdh.jGmdh.oldskul.measures.StructureValidationPerformance;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;

/**
 * Isentropic exponent model, to be webstarted
 *
 * @author ivek
 */
public class TrainAndSerialize {

    private TrainAndSerialize() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
            throws IOException, ExpressionEqualToZero, TooBig, Exception {

        Instances data = new Instances(
                new BufferedReader(
                //        new FileReader("C:/Users/vekk/Desktop/wGmdh output/Datasets/ISO-20765-1/Isentropic_Exponent/Ie1_6_20k.arff")));
//                new FileReader("C:/Users/vekk/Desktop/wGmdh output/Datasets/ISO-20765-1/Joule_Thomson_Coefficient/JTc1_6_20k.arff")));
//                new FileReader("C:/Users/vekk/Desktop/wGmdh output/Datasets/ISO-20765-1/Specific_Heat_Cap_at_Const_Pressure/CPm1_6_20k.arff")));
                new FileReader("C:/Users/vekk/Documents/NetBeansProjects/franovi datasets/Drug_datasets/small_svmParamsIncl/benzo32_paramsInRelation.arff")));
        data.setClassIndex(data.numAttributes() - 1);   // set class

        /* Instantiate a GMDH network object and assign error measure and selection
         * criterion to it
         */
        PercentageSplitHandler handler;
        handler = new PercentageSplitHandler(data, (float) 66.6);
        SlidingFilter filter = new SlidingFilter(100);
        filter.initialize(handler);
        MultiSelectCombi gmdhTest = new MultiSelectCombi(
                handler, new TwoInputModelFactory(), filter);
        try {
            gmdhTest.initAttributeLayer();
        } catch (Exception ex) {
            Logger.getLogger(Toy.class.getName()).log(Level.SEVERE, null, ex);
        }
        gmdhTest.setTrainingPerformance(new StructureLearningPerformance(new Rrse()));
        gmdhTest.setSelectionPerformance(new StructureValidationPerformance(new Rrse()));

        try {
            // set layer of attribute nodes
//            gmdhTest.multiSelectCombi(6); // initiate MSC algorithm
//            gmdhTest.multiSelectCombi(9); // initiate MSC algorithm
//            gmdhTest.multiSelectCombi(9); // initiate MSC algorithm
              gmdhTest.multiSelectCombi(3); // initiate MSC algorithm
        } catch (TooSmall ex) {
            Logger.getLogger(Toy.class.getName()).log(Level.SEVERE, null, ex);
        }

        // kill arrays - we won't be needing them
        gmdhTest.arrayCleanup();
        System.gc();

        weka.core.SerializationHelper.write(
//                    "Ie1_6_20k.gmdh", gmdhTest);
//                "JTc1_6_20k.gmdh", gmdhTest);
//                  "CPm1_6_20k.gmdh", gmdhTest);
                    "benzo32.gmdh", gmdhTest);
        /* Graphical stuff
                 */
        final GuiManager drawModel = new GuiManager(gmdhTest.selectedLayers,
                1000, 800, 50, 50);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                drawModel.launchGUI(null);
            }
        });
    }
}
