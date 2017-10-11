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
import weka.core.converters.ArffLoader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;
import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import wGmdh.jGmdh.gui.GuiManager;
import wGmdh.jGmdh.hybrid.CfsFilter;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.MultiSelectCombiMkII;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.TwoInputModel;
import wGmdh.jGmdh.oldskul.TwoInputModelFactory;
import wGmdh.jGmdh.oldskul.TwoInputModelMkIIFactory;
import wGmdh.jGmdh.oldskul.measures.Sse;
import wGmdh.jGmdh.oldskul.measures.StructureLearningPerformance;
import wGmdh.jGmdh.oldskul.measures.StructureValidationPerformance;
import wGmdh.jGmdh.util.supervised.CvHandler;

/**
 *
 * @author ivek
 */
public class Toy {

    private Toy() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
            throws IOException, ExpressionEqualToZero, TooBig {

        /*Instances data = new Instances(
                new BufferedReader(
                new FileReader("./Resources/regressionTest.arff")));
         */
    	ArffLoader loader = new ArffLoader();
    	loader.setURL(Toy.class.getResource("/Resources/cpu_short.arff").toString());
        //loader.setURL(Toy.class.getResource("/Resources/cpu_short.arff").toString());
    	Instances data = loader.getDataSet();                		
        data.setClassIndex(data.numAttributes() - 1);   // set class

        /* Instantiate a GMDH network object and assign error measure and selection
         * criterion to it
         */
        CvHandler handler = new CvHandler(data, 6);
        SlidingFilter filter = new SlidingFilter(50);
        filter.initialize(handler);
       /* MultiSelectCombi gmdhTest = new MultiSelectCombi(
                handler, new TwoInputModelFactory(), filter  );
        */
        MultiSelectCombi gmdhTest = new MultiSelectCombi(
                handler, new TwoInputModelMkIIFactory(), filter  );
        
        try {
            gmdhTest.initAttributeLayer();
        } catch (Exception ex) {
            Logger.getLogger(Toy.class.getName()).log(Level.SEVERE, null, ex);
        }
        gmdhTest.setTrainingPerformance(new StructureLearningPerformance(new Sse()));
        gmdhTest.setSelectionPerformance(new StructureValidationPerformance(new Sse()));

        try {
            // set layer of attribute nodes
            gmdhTest.multiSelectCombi(10); // initiate MSC algorithm
        } catch (TooSmall ex) {
            Logger.getLogger(Toy.class.getName()).log(Level.SEVERE, null, ex);
        }
        TwoInputModel best = gmdhTest.bestModel(null).model;
        System.out.println(best.getIdentifier());
        System.out.println(MultiSelectCombi.polynomialExpressionGlobal(best, 0));

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
