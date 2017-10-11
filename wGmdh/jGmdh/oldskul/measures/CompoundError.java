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
package wGmdh.jGmdh.oldskul.measures;

import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * error = cw*(Errs/Errs0)^2 + (1-cw)*(Texe-Texe0)
 *
 * To be used with jGMDH.raw.oldskul.ModelClassic.
 * To use it with jGMDH.raw.hybrid.ModelMultiple you will need to calculate
 * number of additions and multiplications in the polynomial separately for
 * each ModelMultiple because they need not be the same.
 *
 * @author ivek
 */
public class CompoundError extends Measure implements TechnicalInformationHandler{

    private static final long serialVersionUID = 6424604954426035737L;
    private HashMap hm = new HashMap();
    private static double cwDefault = 0.5;
    private static double executionTimeThresholdDefault = 25;
    private static double rrseThresholdDefault = 1;
    private static double multiplicationTimeDefault = 25;
    private static double additionTimeDefault = 25;
    private double cw = cwDefault;
    private double executionTimeThreshold = executionTimeThresholdDefault; //Texe0
    private double rrseThreshold = rrseThresholdDefault;   //Errs0
    private double multiplicationTime = multiplicationTimeDefault;
    private double additionTime = additionTimeDefault;
    private double singlePolynomialexecutionTime;
    private Rrse rrse = new Rrse();

    public CompoundError() {
        calculateSinglePolynomialExecutionTime();
    }

    /* Because we have a top-to-bottom data structure that describe models, it's
     * not possible to count the subnodes without repetition directly. A hashmap
     * will be used - key will be equal to modelID and we will simply count the
     * entries.
     */
    /**
     * Preorder traversal: add node ID to hashmap at each visit. Iteration
     * returns when we hit an AttributeNode.
     *
     * @param node
     */
    private void preorderTraversal(Model model) {
        hm.put(model.getIdentifier(), new Double(1));

        for (Iterator<Node> iter = model.links.iterator(); iter.hasNext();) {
            Node visited = iter.next();
            if (visited instanceof Model) {
                preorderTraversal((Model) visited);
            }
        }
    }

    public int countSubmodels(Model model) {
        hm.clear(); // clear hashmap
        preorderTraversal(model);   // fill hashmap
        return hm.size();  // count entries
    }

    public double calculate(Model model, double[] modeled, double[] goal) throws Exception {
        if (modeled.length != goal.length) {
            throw new TooBig("Array lengths differ.");
        } else {
            int nrSubmodels = countSubmodels(model);
            return getCw() * (rrse.calculate(model, modeled, goal)
                    / getRrseThreshold())
                    + (1 - getCw()) * (nrSubmodels
                    * getSinglePolynomialexecutionTime()
                    / getExecutionTimeThreshold());
        }
    }

    /**
     *
     * @param options
     * @throws java.lang.Exception
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String string;
        string = Utils.getOption('C', options);
        if (string.length() != 0) {
            setCw((new Double(string)).doubleValue());
        } else {
            setCw(cwDefault);
        }

        string = Utils.getOption('E', options);
        if (string.length() != 0) {
            setExecutionTimeThreshold((new Double(string)).doubleValue());
        } else {
            setExecutionTimeThreshold(executionTimeThresholdDefault);
        }

        string = Utils.getOption('R', options);
        if (string.length() != 0) {
            setRrseThreshold((new Double(string)).doubleValue());
        } else {
            setRrseThreshold(rrseThresholdDefault);
        }

        string = Utils.getOption('M', options);
        if (string.length() != 0) {
            setMultiplicationTime((new Double(string)).doubleValue());
        } else {
            setMultiplicationTime(multiplicationTimeDefault);
        }

        string = Utils.getOption('A', options);
        if (string.length() != 0) {
            setAdditionTime((new Double(string)).doubleValue());
        } else {
            setAdditionTime(additionTimeDefault);
        }

        super.setOptions(options);
    }

    /**
     *
     * @return
     */
    @Override
    public String[] getOptions() {
        Vector result;

        Vector options = new Vector();
        String[] superOptions = super.getOptions();
        for (int i = 0; i < superOptions.length; i++) {
            options.add(superOptions[i]);
        }

        options.add("-C");
        options.add("" + this.getCw());
        options.add("-E");
        options.add("" + this.getExecutionTimeThreshold());
        options.add("-R");
        options.add("" + this.getRrseThreshold());
        options.add("-M");
        options.add("" + this.getMultiplicationTime());
        options.add("-A");
        options.add("" + this.getAdditionTime());

        return (String[]) options.toArray(new String[options.size()]);
    }

    @Override
    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        Vector newVector = new Vector(4);
        newVector.addElement(new Option(
                "\tWeighting coefficient",
                "C", 1, "-C <Cw>"));
        newVector.addElement(new Option(
                "\tExecution Time Threshold",
                "E", 1, "-E <Texe0>"));
        newVector.addElement(new Option(
                "\tRoot relative Squared Error Threshold",
                "R", 1, "-R <Errse0>"));
        newVector.addElement(new Option(
                "\tMultiplication time",
                "M", 1, "-M <Tmul>"));
        newVector.addElement(new Option(
                "\tAddition time",
                "A", 1, "-A <Tadd>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return options.elements();
    }

    /**
     * @return the cw
     */
    public double getCw() {
        return cw;
    }

    /**
     * @param cw the cw to set
     */
    public void setCw(double cw) {
        this.cw = cw;
    }

    /**
     * @return the executionTimeThreshold
     */
    public double getExecutionTimeThreshold() {
        return executionTimeThreshold;
    }

    /**
     * @param executionTimeThreshold the executionTimeThreshold to set
     */
    public void setExecutionTimeThreshold(double executionTimeThreshold) {
        this.executionTimeThreshold = executionTimeThreshold;
    }

    /**
     * @return the rrseThreshold
     */
    public double getRrseThreshold() {
        return rrseThreshold;
    }

    /**
     * @param rrseThreshold the rrseThreshold to set
     */
    public void setRrseThreshold(double rrseThreshold) {
        this.rrseThreshold = rrseThreshold;
    }

    /**
     * @return the multiplicationTime
     */
    public double getMultiplicationTime() {
        return multiplicationTime;
    }

    /**
     * @param multiplicationTime the multiplicationTime to set
     */
    public void setMultiplicationTime(double multiplicationTime) {
        this.multiplicationTime = multiplicationTime;
    }

    /**
     * @return the additionTime
     */
    public double getAdditionTime() {
        return additionTime;
    }

    /**
     * @param additionTime the additionTime to set
     */
    public void setAdditionTime(double additionTime) {
        this.additionTime = additionTime;
    }

    /**
     * @return the singlePolynomialexecutionTime
     */
    public double getSinglePolynomialexecutionTime() {
        return singlePolynomialexecutionTime;
    }

    /**
     * In our 2 variable polynomial we have 5 additions and 5 multiplications
     */
    public void calculateSinglePolynomialExecutionTime() {
        singlePolynomialexecutionTime = 5 * (getAdditionTime() + getMultiplicationTime());
    }

    /**
     * Returns a string describing classifier
     * @return a description suitable for
     * displaying in the explorer/experimenter gui
     */
    public String globalInfo() {

        return "Compound Squared Relative Error:\n"
                + " E = cw*(Errs/Errs0)^2 + (1-cw)*(Texe/Texe0)\n "
                + "For more information see listed publications.\n\n";
    }

    public TechnicalInformation getTechnicalInformation() {
        TechnicalInformation result;

        result = new TechnicalInformation(Type.ARTICLE);
        result.setValue(Field.AUTHOR, "Marić, I.; Ivek, I.");
        result.setValue(Field.TITLE, "Compensation for Joule-Thompson Effect" +
                "in Flowrate Measurements by GMDH Polynomial");
        result.setValue(Field.YEAR, "2010");
        result.setValue(Field.JOURNAL, "Flow Measurement and Instrumentation");

        return result;
    }
}
