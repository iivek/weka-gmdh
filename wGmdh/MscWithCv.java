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

import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.Node;
import java.util.Enumeration;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.oldskul.NodeFilter;
import wGmdh.jGmdh.oldskul.TwoInputModelFactory;
import wGmdh.jGmdh.util.supervised.CvHandler;
import weka.classifiers.IterativeClassifier;
import weka.core.Drawable;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * It will randomize the provided dataset
 *
 * @author ivek
 */
 @Deprecated
public class MscWithCv extends MscPrototype
        implements OptionHandler, IterativeClassifier, Drawable,
        TechnicalInformationHandler {

    private static final long serialVersionUID = -1327851584443758469L;
    
    /* Parameters
     */
    // nrFolds-crossvalidation will be used to optimize network structure.
    protected int nrFolds = 10;

    /**
     * @return the nrFolds
     */
    public int getNrFolds() {
        return nrFolds;
    }

    /**
     * @param nrFolds the nrFolds to set
     */
    public void setNrFolds(int nrFolds) {
        this.nrFolds = nrFolds;
    }

    public String nrFoldsTipText() {
        return "Model structure gets evaluated by n-fold crossvalidation. " +
                "This sets number of folds for CV.";
    }

    
    public void initClassifier(Instances instances) throws Exception {

        Node.setGlobalIdentifier(0);
        // this is optional, it's here simply to start from
        // zero (a single overflow doesn't
        // matter but more than one should never
        // happen)

        currentlyBestStructure = null;
        /* Initialize our GMDH algorithm with provided datasets.
         */
        gmdhNet = null;
        Runtime.getRuntime().gc();

        gmdhNet = new MultiSelectCombi(
                new CvHandler(instances, nrFolds), new TwoInputModelFactory(), getSelector());

        /* Provide gmdhNet with chosen performance classes
         */
        gmdhNet.setTrainingPerformance(structureLearningPerformance);
        gmdhNet.setSelectionPerformance(structureValidationPerformance);
        selector.initialize(gmdhNet.getDataset());
    }

    @Override
    public String globalInfo() {
        return "A GMDH regression implementation";
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration listOptions() {

        Vector newVector = new Vector(1);
        newVector.addElement(new Option(
                "\tThe algorithm will use N-fold crossvalidation to determine optimal structure, followed by relearning of the optimal structure on entire dataset.",
                "P", 1, "-P <numberOfFolds>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return newVector.elements();
    }

    /**
     * Parses a given list of options.
     * - take a peek in listOptions for valid options.
     * Default parameters can be found here and are set if not provided or valid.
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String folds = Utils.getOption('P', options);
        if (folds.length() != 0) {
            setNrFolds((new Integer(folds)).intValue());
        } else {
            setNrFolds(10);
        }

        super.setOptions(options);
    }

    /**
     * Gets the current settings of our Classifier.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    @Override
    public String[] getOptions() {

        Vector options = new Vector();
        String[] superOptions = super.getOptions();
        for (int i = 0; i < superOptions.length; i++) {
            options.add(superOptions[i]);
        }

        options.add("-P");
            options.add("" + getNrFolds());

        return (String[]) options.toArray(new String[options.size()]);
    }

    /**
     * @return string describing the model.
     */
    @Override
    public String toString() {
        String output = null;

        try {
            // trainedOnEntireDataset is treated as if it has one(!) fold only
            output = MultiSelectCombi.polynomialExpressionGlobal(
                    modelToOutput, 0);
        } catch (TooBig ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (getVisualize()) {
            output += "\nNote:\nmodels and their coefficients listed in the " +
                    "GMDH graphical representation have been obtained on structure " +
                    "learning subset of the training set\nand the listed scores " +
                    " on structure validation subset of the " +
                    "training set, during the structure determination phase " +
                    "of learning." +
                    "\nThe model listed above is built on the entire training " +
                    "set, using the determined structure, and is used as " +
                    "classifier output.";
        }
        return output;
    }


    @Override
    public TechnicalInformation getTechnicalInformation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * @return the selector
     */
    @Override
    public NodeFilter getSelector() {
        return selector;
    }

    /**
     * @param selector the selector to set
     */
    @Override
    public void setSelector(NodeFilter selector) {
        this.selector = selector;
    }

    /**
     * @return the randomSeed
     */
    @Override
    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * @param randomSeed the randomSeed to set
     */
    @Override
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
