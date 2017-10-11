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

package wGmdh.jGmdh.oldskul;

import java.util.Iterator;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import wGmdh.jGmdh.util.supervised.DatasetHandlerSupervised;
import weka.core.Attribute;
import weka.core.Instances;

/**
 *
 * @author ivek
 */
public class AttributeNode extends Node {

    private static final long serialVersionUID = 3688286550783249651L;
    public Attribute attr;
    public int index;

    /**
     * Appropriate when only two datasets are used: one for building the
     * network structure and one one dataset to approximate how well this
     * structure would generalize
     *
     * @param instancesTraining
     * @param instancesValidation
     * @param attrIndex
     */
    @Deprecated
    public AttributeNode(Instances instancesTraining,
            Instances instancesValidation, int attrIndex) {
        super(1);
        this.index = attrIndex;
        if (instancesTraining != null) {
            trainSetOutput.add(
                    instancesTraining.attributeToDoubleArray(attrIndex));
        }
        if (instancesValidation != null) {
            validationSetOutput.add(
                    instancesValidation.attributeToDoubleArray(attrIndex));
        }
        this.attr = instancesTraining.attribute(attrIndex);
    }

    /**
     * Appropriate when n-fold crossvalidation will be used to determine
     * the network structure: one dataset is provided. dataset is randomized at
     * each fold.
     *
     * @param dataset
     * @param attrIndex
     * @param nrFolds
     */
    @Deprecated
    public AttributeNode(Instances dataset, int attrIndex, int nrFolds) {
        super(nrFolds);
        this.index = attrIndex;

        for (int fold = 0; fold < nrFolds; ++fold) {
            trainSetOutput.add(fold,
                    dataset.trainCV(nrFolds, fold).attributeToDoubleArray(attrIndex));
            validationSetOutput.add(fold,
                    dataset.testCV(nrFolds, fold).attributeToDoubleArray(attrIndex));
        }

        this.attr = dataset.attribute(attrIndex);
    }

    /**
     *
     * @param provider
     * @param attrIndex
     */
    public AttributeNode(DatasetSupervised provider, int attrIndex) {

        // we'll need learning Instances and validation Instances to instantiate
        // AttributeNode
        Iterator<Instances> learningInputs = provider.getLearningSets();
        Iterator<Instances> validationInputs = provider.getValidationSets();

        // TODO: check number of learning sets and validation sets
        while (learningInputs.hasNext()) {
            // iterate through folds

            // To create AttributeNodes, we need learning and validation inputs
            Instances learningIn = learningInputs.next();
            Instances validationIn = validationInputs.next();

            trainSetOutput.add(learningIn.attributeToDoubleArray(attrIndex));
            validationSetOutput.add(validationIn.attributeToDoubleArray(attrIndex));
        }
        this.attr = provider.getInstances().attribute(attrIndex);
        this.index = attrIndex;
    }

    /**
     * Attribute node passes the same output whatever the fold.
     * 
     * @param inputs
     * @param fold      doesn't matter
     * @return
     */
    @Override
    public double networkOutput(double[] inputs, int fold) {
        return inputs[index];
    }

    /**
     *
     * @param inputs
     * @param fold  ...doesn't matter
     * @return
     */
    @Override
    public double networkOutputNoFlags(double[] inputs, int fold) {
        //System.out.println("ID: " + this.getIdentifier() + " outputs: " + inputs[index]);
        return inputs[index];
    }
}