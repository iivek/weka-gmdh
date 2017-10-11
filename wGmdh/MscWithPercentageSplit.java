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

import java.util.Enumeration;
import java.util.Vector;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.Node;
import wGmdh.jGmdh.oldskul.TwoInputModelFactory;
import wGmdh.jGmdh.util.supervised.PercentageSplitHandler;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;

/**
 *
 * @author ivek
 */
@Deprecated
public class MscWithPercentageSplit extends MscPrototype {

    private static final long serialVersionUID = -9214038961969748826L;
    protected float trainPercentage = 66;

    @Override
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

        gmdhNet = new MultiSelectCombi(new PercentageSplitHandler(
                instances, trainPercentage), new TwoInputModelFactory(),
                getSelector());

        /* Provide gmdhNet with chosen performance classes
         */
        gmdhNet.setTrainingPerformance(structureLearningPerformance);
        gmdhNet.setSelectionPerformance(structureValidationPerformance);
        selector.initialize(gmdhNet.getDataset());
    }

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

        String percent = Utils.getOption('P', options);
        if (percent.length() != 0) {
            setTrainPercentage((new Float(percent)).floatValue());
        } else {
            setTrainPercentage(66);
        }

        super.setOptions(options);
    }

    @Override
    public String[] getOptions() {

        Vector options = new Vector();
        String[] superOptions = super.getOptions();
        for (int i = 0; i < superOptions.length; i++) {
            options.add(superOptions[i]);
        }

        options.add("-P");
        options.add("" + getTrainPercentage());

        return (String[]) options.toArray(new String[options.size()]);
    }

    public String trainPercentageTipText() {
        return "If set to true, visualize all calculated GMDH models." +
                "Otherwise, don't.";
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
}
