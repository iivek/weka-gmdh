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

import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import wGmdh.jGmdh.gui.GuiManager;
import wGmdh.jGmdh.gui.NodeGraphics;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.MultiSelectCombi.ModelAndLayer;
import wGmdh.jGmdh.oldskul.Node;
import wGmdh.jGmdh.oldskul.TwoInputModel;
import wGmdh.jGmdh.oldskul.measures.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.oldskul.CopyCat;
import wGmdh.jGmdh.oldskul.ModelComparator;
import wGmdh.jGmdh.oldskul.NodeFilter;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import weka.classifiers.Classifier;
import weka.classifiers.IterativeClassifier;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SerializedObject;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.UnsupportedAttributeTypeException;
import weka.core.UnsupportedClassTypeException;
import weka.core.Utils;

/**
 * It will randomize the provided dataset
 *
 * @author ivek
 */
@Deprecated
public abstract class MscPrototype extends Classifier
        implements OptionHandler, IterativeClassifier, Drawable,
        TechnicalInformationHandler {

    private static final long serialVersionUID = -8428396965804218962L;

    private static double errorChangeTolerance = 1E-6;
    protected MultiSelectCombi gmdhNet;
    /* Common parameters
     */
    protected int nrLayers = 0;
    protected boolean visualizeNet = false;
    protected String filterSelection;
    protected long randomSeed = 0;     // seed for dataset randomization
    protected int divergenceTolerance = 0;  // otherwise it may promote overfitting
    protected ModelAndLayer currentlyBestStructure;
    protected TwoInputModel modelToOutput; // TODO: serialize this as a tree of objects
    protected NodeFilter selector = new SlidingFilter();
    public StructureLearningPerformance structureLearningPerformance =
            new StructureLearningPerformance(new Omitted());
    public StructureValidationPerformance structureValidationPerformance =
            new StructureValidationPerformance(new Sse());
    private transient GuiManager gui;
    //private transient Runnable swingRunnable;
    //private transient Thread guiThread;
    public int iterations;  // contains current number of layers

    public void setStructureLearningPerformanceMeasure(Measure m) {
        structureLearningPerformance.setMeasure(m);
    }

    public Measure getStructureLearningPerformanceMeasure() {
        return structureLearningPerformance.getMeasure();
    }

    public String structureLearningPerformanceMeasureTipText() {
        return "Performance measure on training subset for determining model structure. Not needed by " +
                "the algorithm - choose Omitted for speed.";
    }

    public void setStructureValidationPerformanceMeasure(Measure m) {
        structureValidationPerformance.setMeasure(m);
    }

    public Measure getStructureValidationPerformanceMeasure() {
        return structureValidationPerformance.getMeasure();
    }

    public String structureValidationMeasureTipText() {
        return "Performance measure on structure validation subset, used for model comparison" +
                "i.e. to determine which models will be used to build more complex ones.";
    }

    /**
     * Attribute layer included in the count.
     *
     * @return
     */
    public int getNumberOfLayers() {
        return gmdhNet.selectedLayers.size();
    }

    /**
     *
     * @param layerIndex
     * @return  best model on the layerIndex-th layer
     */
    protected TwoInputModel getBest(int layerIndex) {
        int bestLocation = gmdhNet.selectedLayers.get(layerIndex).size() - 1;
        return (TwoInputModel) gmdhNet.selectedLayers.get(layerIndex).get(bestLocation);
    }

    public int getMaxLayers() {
        return this.nrLayers;
    }

    public void setMaxLayers(int nrLayers) {
        this.nrLayers = nrLayers;
    }

    public String maxLayersTipText() {
        return "Training stops when the algorithm reaches totalLayers layers. " +
                "If totalLayers is set to zero, learning will proceed until " +
                "validation error begins to deteriorate.";
    }

    public boolean getVisualize() {
        return this.visualizeNet;
    }

    public void setVisualize(boolean visualizeNet) {
        this.visualizeNet = visualizeNet;
    }

    public String visualizeTipText() {
        return "If set to true, visualize all calculated GMDH models." +
                "Otherwise, don't.";
    }

    public abstract void initClassifier(Instances instances) throws Exception;

    public void next(int nrIteration)
            throws ExpressionEqualToZero, TooBig, TooSmall {
        gmdhNet.mscGrow(nrIteration);
        System.out.println("Growing: top layer = " + nrIteration);
    }

    public void done() throws Exception {

        return;
    }

    @Override
    public Object clone() {
        /*
         * Simply return a copy of our entire classifier
         */
        SerializedObject so;
        try {
            so = new SerializedObject(this);
            return so.getObject();
        } catch (Exception ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    @Override
    public void buildClassifier(Instances instances) throws Exception {

        getCapabilities().testWithFail(instances);
        /* All instances need to have a class.
         */
        instances.deleteWithMissingClass();
        /* Check number of attributes
         */
        if (instances.numAttributes() < 3) {
            throw new Exception("At least 2 attributes and a class" +
                    "are required to build classifier.");
        }

        if (!instances.classAttribute().isNumeric()) {
            throw new UnsupportedClassTypeException("Numeric class only");
        }

        Random random = new Random(randomSeed);
        instances.randomize(random);
        initClassifier(instances);

        /* Determine model structure
         */
        // If maxLayers is set to zero, layers will be added until selection
        // performance begins to deteriorate for divergenceTolerance layers.
        //
        if (nrLayers == 0) {
            iterations = 1;
            // currentlyBestStructure is null here
            next(iterations);
            currentlyBestStructure = gmdhNet.bestModel(currentlyBestStructure);
            do {
                next(++iterations);
                currentlyBestStructure = gmdhNet.bestModel(currentlyBestStructure);

            } while (currentlyBestStructure.layersWithoutImprovement <= divergenceTolerance);

        } else {
            // currentlyBestStructure is null here
            for (iterations = 1; iterations <= nrLayers; ++iterations) {
                next(iterations);
            }
            currentlyBestStructure = gmdhNet.bestModel(currentlyBestStructure);

            // return the best model from the highest layer

            // make sure highest layer is sorted (we'll sort all layers)
            Comparator comp = new ModelComparator();
            for (int i = 1; i < gmdhNet.selectedLayers.size(); ++i) {
                Collections.sort(gmdhNet.selectedLayers.get(i), comp);
            }

            int highest = gmdhNet.selectedLayers.size() - 1;
            int bestLocation = gmdhNet.selectedLayers.get(highest).size() - 1;
            currentlyBestStructure = new MultiSelectCombi.ModelAndLayer(
                    (TwoInputModel) gmdhNet.selectedLayers.get(highest).get(bestLocation), highest,
                    errorChangeTolerance);
            currentlyBestStructure.layersWithoutImprovement = 0;
        }
        /* TODO: Save the structures (by serialization), if required by user.
         * That way the user will be able to continue to build from what is
         * available now some other time. After that unneeded structures can
         * be discarded.
         */

        /* Pass through all nodes and force freeing some memory by killing
         * all the large stuff we don't need anymore.
         */
        Runtime r = Runtime.getRuntime();
        System.out.println("memory before array cleanup: " + r.freeMemory());
        gmdhNet.arrayCleanup();
        r.gc();
        System.out.println("after cleanup: " + r.freeMemory());

        /* Now rebuild the best model by keeping the optimal structure and
         * using the entire training set as regression goal.
         */
        Node.setGlobalIdentifier(0);   // small integers as IDs. nice. as long
        // as we don't mix the old Models with
        // the new ones.

        // train model on entire dataset
        CopyCat copier = new CopyCat(
                currentlyBestStructure.model, gmdhNet.getMaker());
        modelToOutput = (TwoInputModel) copier.copyStructureAndPurify(instances);
        //modelToOutput = currentlyBestStructure.model;

        System.out.println(currentlyBestStructure.model.getIdentifier());

        // force garbage collection
        r.gc();
        System.out.println("buildClassifier returns with : " + r.freeMemory() +
                " free memory");

        System.out.println("stepping out of build classifier");

        return;
    }

    /**
     * Returns the revision string.
     *
     * @return        the revision
     */
    public String getRevision() {
        return RevisionUtils.extract("$Revision: 1.0 $");
    }

    @Override
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();

        /* attributes
         */
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        /* class
         */
        result.enable(Capability.NUMERIC_CLASS);

        return result;
    }

    /**
     * Classifies the given test instance. The instance has to belong to a
     * dataset when it's being classified.
     *
     * @param instance the instance to be classified
     * @return the predicted most likely class for the instance or
     * Instance.missingValue() if no prediction is made
     * @exception Exception if an error occurred during the prediction
     */
    @Override
    public double classifyInstance(Instance instance) throws Exception {

        /*     double[] dist = distributionForInstance(instance);
        if (dist == null) {
        throw new Exception("Null distribution predicted");
        }
         * */
        switch (instance.classAttribute().type()) {
            case Attribute.NOMINAL:
                throw new UnsupportedAttributeTypeException(
                        "jGMDH not capable of dealing with nominal attributes");
            case Attribute.NUMERIC:
                /* If we're here, we already have built a model and found the
                 * best one.
                 */
                // TODO: check if instance is OK
                // coeffs are fixed at index 0 - when you train a fresh model on
                // entire dataset, that's where you'll find them
                double result = modelToOutput.networkOutput(instance.toDoubleArray(), 0);
                return result;

            default:
                return Instance.missingValue();
        }
    }

    @Override
    public double[] distributionForInstance(Instance instance) throws Exception {
        throw new Exception("wGMDH: distributionForInstance is not implemented");
    }

    public String globalInfo() {
        return "A GMDH prototype. Seems like you forgot to override this :)";
    }

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration listOptions() {

        Vector newVector = new Vector(5);

        newVector.addElement(new Option(
                "\tNumber of layers.\n" +
                "\tTraining stops when the algorithm reaches totalLayers layers." +
                "If totalLayers is set to zero, learning will proceed until error" +
                "on validation set begins to deteriorate.",
                "L", 1, "-L <totalLayers>"));

        newVector.addElement(new Option(
                "\tNumber of models retained per layer, i.e. the number of models " +
                "with best structure validation score that take part in creating" +
                "more complex, higher-layer models.\n",
                "N", 1, "-N <models>"));

        newVector.addElement(new Option(
                "\tIf set to true, visualize calculated GMDH models." +
                "Otherwise, don't.\n",
                "V", 1, "-V <visualise>"));

        newVector.addElement(new Option(
                "\tThe misfit measure on structure learning set to be used.\n" +
                "\t(default: jGMDH.raw.oldskul.measures.Rrse)",
                "E", 1, "-E <classname and parameters>"));

        newVector.addElement(new Option(
                "\tThe Misfit Measure on structure validation set to be used.\n" +
                "\t(default: jGMDH.raw.oldskul.measures.Rrse)",
                "S", 1, "-S <classname and parameters>"));

        newVector.addElement(new Option(
                "\tNode selector to be used.\n" +
                "\t(default: jGMDH.raw.oldskul.CfsFilter)",
                "F", 1, "-F <classname and parameters>"));

        newVector.addElement(new Option(
                "\tSeed used to randomize data.\n" +
                "",
                "R", 1, "-R <seed>"));

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

        String layers = Utils.getOption('L', options);
        if (layers.length() != 0) {
            setMaxLayers((new Integer(layers)).intValue());
        } else {
            setMaxLayers(0);
        }

        String visualize = Utils.getOption('V', options);
        if (visualize.length() != 0) {
            if (visualize.equals("true")) {
                setVisualize(true);
            }
        } else {
            setVisualize(false);
        }

        String name = Utils.getOption('E', options);
        String[] additionalOptions = Utils.splitOptions(name);
        if (additionalOptions.length != 0) {
            name = additionalOptions[0];
            additionalOptions[0] = "";
            structureLearningPerformance.setMeasure((Measure) Utils.forName(
                    Measure.class, name, additionalOptions));
        } else {
            structureLearningPerformance.setMeasure(new Rrse());
        }

        name = Utils.getOption('S', options);
        additionalOptions = Utils.splitOptions(name);
        if (additionalOptions.length != 0) {
            name = additionalOptions[0];
            additionalOptions[0] = "";
            structureValidationPerformance.setMeasure((Measure) Utils.forName(
                    Measure.class, name, additionalOptions));
        } else {
            structureValidationPerformance.setMeasure(new Rrse());
        }

        name = Utils.getOption('F', options);
        additionalOptions = Utils.splitOptions(name);
        if (additionalOptions.length != 0) {
            name = additionalOptions[0];
            additionalOptions[0] = "";
            this.setSelector((NodeFilter) Utils.forName(
                    NodeFilter.class, name, additionalOptions));
        } else {
            setSelector(new SlidingFilter());
        }

        String seed = Utils.getOption('R', options);
        if (seed.length() != 0) {
            setRandomSeed((new Long(seed)).longValue());
        } else {
            setRandomSeed(0);
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

        options.add("-L");
        options.add("" + this.getMaxLayers());

        options.add("-E");
        options.add("" + structureLearningPerformance.getMeasure().getClass().getName() + "" +
                Utils.joinOptions(structureLearningPerformance.getMeasure().getOptions()));
        options.add("-S");
        options.add("" + structureValidationPerformance.getMeasure().getClass().getName() +
                Utils.joinOptions(structureValidationPerformance.getMeasure().getOptions()));

        options.add("-F");
        options.add("" + getSelector().getClass().getName() +
                Utils.joinOptions(getSelector().getOptions()));
        options.add("-R");
        options.add("" + this.getRandomSeed());

        options.add("-V");
        options.add("" + this.getVisualize());

        return (String[]) options.toArray(new String[options.size()]);
    }

    /**
     * @return string that describes the model.
     */
    @Override
    public String toString() {
        String output = null;

        try {
            output = MultiSelectCombi.polynomialExpressionGlobal(
                    modelToOutput, 0);
        } catch (TooBig ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (getVisualize()) {
            output += "";
        }
        return output;
    }

    public int graphType() {
        return -1;
    }

    public String graph() throws Exception {
        /* Visualize all calculated models, if required
         */
        System.gc();
        if (visualizeNet) {
            int maxPerLayer = 0;
            for (int i = 0; i < gmdhNet.selectedLayers.size(); ++i) {
                if (gmdhNet.selectedLayers.get(i).size() > maxPerLayer) {
                    maxPerLayer = gmdhNet.selectedLayers.get(i).size();
                }
            }

            gui = new GuiManager(gmdhNet.selectedLayers,
                    (int) (maxPerLayer * NodeGraphics.buttonWidth * 0.4 + 2 * 25), (iterations + 1) * NodeGraphics.buttonHeight * 3, 25, 25);

            Runnable swingRunnable = new Runnable() {

                public void run() {
                    ArrayList<Model> l = new ArrayList<Model>();
                    l.add(currentlyBestStructure.model);
                    //gui.launchGUI(l);
                    gui.launchGUI(null);
                }
            };
            javax.swing.SwingUtilities.invokeLater(swingRunnable);

        /*     guiThread = new Thread() {

        @Override
        public void run() {
        gui.launchGUI();
        }
        };
        guiThread.start();
        }
         * */


        //    gui.launchGUI();
        }

        return null;
    }

    public TechnicalInformation getTechnicalInformation() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Chooses the best model structure on layer, trains it on insts and
     * prepares the classifier for evaluating it.
     *
     * @param layer
     */
    public void setOutputModel(int layer, Instances insts) {
        // we want to refit this.getBestLayer();
        CopyCat copier = new CopyCat(
                getBest(layer), gmdhNet.getMaker());
        try {
            modelToOutput = (TwoInputModel) copier.copyStructureAndPurify(insts);
        } catch (TooBig ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ExpressionEqualToZero ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TooSmall ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(MscWithCv.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /**
     * @return the selector
     */
    public NodeFilter getSelector() {
        return selector;
    }

    /**
     * @param selector the selector to set
     */
    public void setSelector(NodeFilter selector) {
        this.selector = selector;
    }

    /**
     * @return the randomSeed
     */
    public long getRandomSeed() {
        return randomSeed;
    }

    /**
     * @param randomSeed the randomSeed to set
     */
    public void setRandomSeed(long randomSeed) {
        this.randomSeed = randomSeed;
    }
}
