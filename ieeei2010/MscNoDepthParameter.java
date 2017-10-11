/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ieeei2010;

import java.util.Enumeration;
import java.util.Vector;
import wGmdh.Msc;
import wGmdh.jGmdh.oldskul.NodeFilter;
import wGmdh.jGmdh.oldskul.SlidingFilter;
import wGmdh.jGmdh.oldskul.measures.Measure;
import wGmdh.jGmdh.oldskul.measures.Rrse;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;
import weka.core.Option;
import weka.core.Utils;

/**
 * ... so we can trick weka (ResultProducer, ResultListener...) into treating
 * Mscs with differerent deph parameters as classifiers with same parameter setup
 *
 * @author vekk
 */
public class MscNoDepthParameter extends Msc{

    private static final long serialVersionUID = -1582605096248719898L;

    /**
     * Returns an enumeration describing the available options.
     *
     * @return an enumeration of all the available options.
     */
    @Override
    public Enumeration listOptions() {

        Vector newVector = new Vector(5);

/*        newVector.addElement(new Option(
                "\tNumber of layers.\n" + "\tTraining stops when the algorithm reaches totalLayers layers." + "If totalLayers is set to zero, learning will proceed until error" + "on validation set begins to deteriorate.",
                "L", 1, "-L <totalLayers>"));
*/
        newVector.addElement(new Option(
                "\tNumber of models retained per layer, i.e. the number of models " + "with best structure validation score that take part in creating" + "more complex, higher-layer models.\n",
                "N", 1, "-N <models>"));

        newVector.addElement(new Option(
                "\tIf set to true, visualize calculated GMDH models." + "Otherwise, don't.\n",
                "V", 1, "-V <visualise>"));

        newVector.addElement(new Option(
                "\tThe misfit measure on structure learning set to be used.\n" + "\t(default: jGMDH.raw.oldskul.measures.Rrse)",
                "E", 1, "-E <classname and parameters>"));

        newVector.addElement(new Option(
                "\tThe Misfit Measure on structure validation set to be used.\n" + "\t(default: jGMDH.raw.oldskul.measures.Rrse)",
                "S", 1, "-S <classname and parameters>"));

        newVector.addElement(new Option(
                "\tNode selector to be used.\n" + "\t(default: jGMDH.raw.oldskul.CfsFilter)",
                "F", 1, "-F <classname and parameters>"));

        newVector.addElement(new Option(
                "\tDetermines whether percentage split or CV will be used to" + "determine model structure.\n" + "",
                "P", 1, "-P <seed>"));

        newVector.addElement(new Option(
                "\tSeed used to randomize data.\n" + "",
                "R", 1, "-R <seed>"));

        newVector.addElement(new Option(
                "\tHaving found the optimal structure for the problem, if" + "set to true, model coefficients will be recalculated " + "using the entire dataset.\n" + "",
                "T", 1, "-T <relearn>"));

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

  /*      String layers = Utils.getOption('L', options);
        if (layers.length() != 0) {
            setMaxLayers((new Integer(layers)).intValue());
        } else {
            setMaxLayers(0);
        }
*/
        String visualize = Utils.getOption('V', options);
        if (visualize.length() != 0) {
            if (visualize.equals("true")) {
                setVisualize(true);
            } else {
                setVisualize(false);
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

        name = Utils.getOption('P', options);
        additionalOptions = Utils.splitOptions(name);
        if (additionalOptions.length != 0) {
            name = additionalOptions[0];
            additionalOptions[0] = "";
            this.setDataProvider((DatasetSupervised) Utils.forName(
                    DatasetSupervised.class, name, additionalOptions));
        } else {
            setDataProvider(new PercentageSplitHandler());
        }

        String seed = Utils.getOption('R', options);
        if (seed.length() != 0) {
            setRandomSeed((new Long(seed)).longValue());
        } else {
            setRandomSeed(0);
        }

        String relearning = Utils.getOption('T', options);
        if (relearning.length() != 0) {
            if (relearning.equals("true")) {
                setRelearn(true);
            } else {
                setRelearn(false);
            }
        } else {
            setRelearn(false);
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

        options.add("-F");
        options.add("" + getSelector().getClass().getName() + " " + Utils.joinOptions(getSelector().getOptions()));

     /*   options.add("-L");
        options.add("" + this.getMaxLayers());
*/
        options.add("-V");
        options.add("" + this.getVisualize());

        options.add("-E");
        options.add("" + structureLearningPerformance.getMeasure().getClass().getName() + " " + Utils.joinOptions(structureLearningPerformance.getMeasure().getOptions()));
        options.add("-S");
        options.add("" + structureValidationPerformance.getMeasure().getClass().getName() + " " + Utils.joinOptions(structureValidationPerformance.getMeasure().getOptions()));

        options.add("-P");
        options.add("" + getDataProvider().getClass().getName() + " " + Utils.joinOptions(getDataProvider().getOptions()));
        options.add("-R");
        options.add("" + this.getRandomSeed());

        options.add("-T");
        options.add("" + this.isRelearn());


        return (String[]) options.toArray(new String[options.size()]);
    }
}
