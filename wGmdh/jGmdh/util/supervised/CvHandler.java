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
 * A class used to feed Models when using the crossvalidation scheme to optimize
 * structure.
 *
 * @author ivek
 */
public class CvHandler extends DatasetSupervised  {

    private static final long serialVersionUID = 2074067266313078216L;

    public CvHandler()  {
        setDimension(10);
    }


    @Override
    public void initialize() {
        if(getInstances() == null || getDimension() == 0)    {
            try {
                throw new Exception("Use setInstances() and setDimension()" +
                        "to set handler parameters");
            } catch (Exception ex) {
                Logger.getLogger(CvHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        validationSets = new Vector<Instances>(getDimension());
        learningSets = new Vector<Instances>(getDimension());
        validationGoals = new Vector<double[]>(getDimension());
        learningGoals = new Vector<double[]>(getDimension());

        for (int i = 0; i < getDimension(); ++i) {
            Instances v = getInstances().testCV(getDimension(), i);
            Instances l = getInstances().trainCV(getDimension(), i);
            validationSets.add(i, v);
            learningSets.add(i, l);
            validationGoals.add(i, v.attributeToDoubleArray(getInstances().classIndex()));
            learningGoals.add(i, l.attributeToDoubleArray(getInstances().classIndex()));
        }
    }


    public CvHandler(Instances dataset, int folds) {
        setInstances(dataset);
        setDimension(folds);

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
        for (int f = 0; f < getDimension(); ++f) {
            // for each fold
            double[][] dataLocal = new double[m.links.size()][];
            for (int l = 0; l < m.links.size(); ++l) {
                // for each link
                dataLocal[l] = m.links.get(l).validationSetOutput.get(f);
            }
            inputs.add(dataLocal);
        }
        return inputs.iterator();
    }

    public Iterator<double[][]> getLearningInputs(Model m) {
        Vector<double[][]> inputs = new Vector<double[][]>(getDimension());
        for (int f = 0; f < getDimension(); ++f) {
            // for each fold
            double[][] dataLocal = new double[m.links.size()][];
            for (int l = 0; l < m.links.size(); ++l) {
                // for each link
                dataLocal[l] = m.links.get(l).trainSetOutput.get(f);
            }
            inputs.add(dataLocal);
        }
        return inputs.iterator();
    }

    public Iterator<double[]> getValidationOutputs(Node n) {
        Vector<double[]> out = new Vector<double[]>(getDimension());
        for (int f = 0; f < getDimension(); ++f) {
            // for each fold
            double[] local = n.validationSetOutput.get(f);
            out.add(local);
        }
        return out.iterator();
    }

    public Iterator<double[]> getLearningOutputs(Node n) {
        Vector<double[]> out = new Vector<double[]>(getDimension());
        for (int f = 0; f < getDimension(); ++f) {
            // for each fold
            double[] local = n.trainSetOutput.get(f);
            out.add(local);
        }
        return out.iterator();
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
            setDimension((new Integer(string)).intValue());
        } else {
            setDimension(10);
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
        options.add("" + this.getDimension());

        return (String[]) options.toArray(new String[options.size()]);
    }

    @Override
    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        Vector newVector = new Vector(1);
        newVector.addElement(new Option(
                "\tNumber of CV folds when optimizing structure",
                "P", 1, "-P <number of folds>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return options.elements();
    }

    @Override
    public void setDimension(int dimension) {
        this.folds = dimension;
    }
}
