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

package wGmdh.jGmdh.util.supervised;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;

/**
 * A class used to feed Models when using the percentage split scheme to
 * optimize structure.
 *
 * @author ivek
 */
public class PercentageSplitHandler extends DatasetSupervised {

    private static final long serialVersionUID = 6231460024521083324L;
    private float trainPercentage = 66;

    public PercentageSplitHandler() {
        setDimension(1);
    }

    @Override
    public void initialize() {

        if (getInstances() == null || getTrainPercentage() == 0) {
            try {
                throw new Exception("Use setInstances() and" + "setTrainPercentage() to set handler parameters.");
            } catch (Exception ex) {
                Logger.getLogger(PercentageSplitHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        validationSets = new Vector<Instances>(getDimension());
        learningSets = new Vector<Instances>(getDimension());
        validationGoals = new Vector<double[]>(getDimension());
        learningGoals = new Vector<double[]>(getDimension());

        // percentage split
        int learnSetsize = (int) Math.round(
                trainPercentage / 100.0 * getInstances().numInstances());
        int validationSetsize = getInstances().numInstances() - learnSetsize;

        if (validationSetsize < 1) {
            try {
                throw new Exception("No instances in structure validation set.");
            } catch (Exception ex) {
                Logger.getLogger(PercentageSplitHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if (learnSetsize < 1) {
            try {
                throw new Exception("No instances in structure learning set.");
            } catch (Exception ex) {
                Logger.getLogger(PercentageSplitHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        Instances learnSet = new Instances(getInstances(), 0, learnSetsize);
        Instances validationSet = new Instances(
                getInstances(), learnSetsize, validationSetsize);

        validationSets.add(validationSet);
        learningSets.add(learnSet);
        validationGoals.add(validationSet.attributeToDoubleArray(getInstances().classIndex()));
        learningGoals.add(learnSet.attributeToDoubleArray(getInstances().classIndex()));
    }

    public PercentageSplitHandler(Instances dataset, float trainPercentage) throws Exception {
        this.setInstances(dataset);
        this.trainPercentage = trainPercentage;
        setDimension(1);

        initialize();
    }

    public Iterator<Instances> getValidationSets() {
        return validationSets.iterator();
    }

    public Iterator<Instances> getLearningSets() {
        return learningSets.iterator();
    }

    public Iterator<double[]> getValidationGoals() {
        return validationGoals.iterator();
    }

    public Iterator<double[]> getLearningGoals() {
        return learningGoals.iterator();
    }

    public Iterator<double[][]> getValidationInputs(Model m) {
        Vector<double[][]> inputs = new Vector<double[][]>(getDimension());

        double[][] dataLocal = new double[m.links.size()][];
        for (int l = 0; l < m.links.size(); ++l) {
            // for each link
            dataLocal[l] = m.links.get(l).validationSetOutput.get(0);
        }
        inputs.add(dataLocal);

        return inputs.iterator();
    }

    public Iterator<double[][]> getLearningInputs(Model m) {
        Vector<double[][]> inputs = new Vector<double[][]>(getDimension());
        double[][] dataLocal = new double[m.links.size()][];
        for (int l = 0; l < m.links.size(); ++l) {
            // for each link
            dataLocal[l] = m.links.get(l).trainSetOutput.get(0);
        }
        inputs.add(dataLocal);
        return inputs.iterator();
    }

    public Iterator<double[]> getValidationOutputs(Node n) {
        Vector<double[]> out = new Vector<double[]>(getDimension());
        out.add(n.validationSetOutput.get(0));
        return out.iterator();
    }

    public Iterator<double[]> getLearningOutputs(Node n) {
        Vector<double[]> out = new Vector<double[]>(getDimension());
        out.add(n.trainSetOutput.get(0));
        return out.iterator();
    }

    /**
     * @return the trainPercentage
     */
    public float getTrainPercentage() {
        return trainPercentage;
    }

    /**
     * @param trainPercentage the trainPercentage to set
     */
    public void setTrainPercentage(float trainPercentage) {
        this.trainPercentage = trainPercentage;
    }

/**
     *
     * @param options
     * @throws java.lang.Exception
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String string;
        string = Utils.getOption('P', options);
        if (string.length() != 0) {
            setTrainPercentage((new Float(string)).floatValue());
        } else {
            setTrainPercentage(66);
        }

        super.setOptions(options);
    }

    /**
     *
     * @return
     */
    @Override
    public String[] getOptions() {

        Vector options = new Vector();
        String[] superOptions = super.getOptions();
        for (int i = 0; i < superOptions.length; i++) {
            options.add(superOptions[i]);
        }

        options.add("-P");
        options.add(" " + this.getTrainPercentage());

        return (String[]) options.toArray(new String[options.size()]);
    }

    @Override
    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        Vector newVector = new Vector(1);
        newVector.addElement(new Option(
                "\tPercentage of dataset that will be used for coefficient fitting;\n" +
                "the rest will be used to validate the GMDH models.",
                "P", 1, "-P <train percentage>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return options.elements();
    }
}
