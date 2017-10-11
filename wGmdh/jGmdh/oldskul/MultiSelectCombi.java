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

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import wGmdh.jGmdh.exceptions.*;
import wGmdh.jGmdh.oldskul.measures.StructureValidationPerformance;
import wGmdh.jGmdh.oldskul.measures.StructureLearningPerformance;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import wGmdh.jGmdh.hybrid.SseRegressionEquations;
import wGmdh.jGmdh.hybrid.SseRegressionEquations.Summand;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instances;

/**
 * a multilayered-combinatorial GMDH network class
 *
 * @author ivek
 */
public class MultiSelectCombi implements Serializable {

    private static final long serialVersionUID = -5049609784850297177L;

    public static double errorChangeTolerance = 1e-5;
    //protected Instances dataset;
    //protected int numFolds;
    protected DatasetSupervised dataset;
    protected double trainPercentage; // used in case of percentage split of input data
    /* Data we regress to and its corresponding Attribute class
     */
    protected Attribute attrRegress;
    protected StructureValidationPerformance selectionPerformance;
    protected StructureLearningPerformance trainingPerformance;

    // nodes used as building-blocks when building next layer
    protected ArrayList<Node> candidates;

    /*
     * Container that stores references to Models selected by the GMDH algorithm;
     * they are the best ones in their layer. Each layer is represented by an
     * ArrayList of ? extends Node objects. ArttributeNodes can be found at
     * the zeroth layer. The algorithm could actually do without it, but it's
     * used for the sake of graphically depicting the AttributeNodes and
     * calculated Models -
     * everything the algorithm needs are the AttributeNodes and the Nodefilter
     * instance
     */
    public ArrayList<ArrayList<? extends Node>> selectedLayers =
            new ArrayList<ArrayList<? extends Node>>();

    /* A factory to spawn Models of appropriate class
     */
    protected ModelFactory maker;
    protected NodeFilter selector;

    public MultiSelectCombi(DatasetSupervised h, ModelFactory f, NodeFilter selector) {

        //numFolds = nrFolds;
        maker = f;
        /* uncommenting will randomize data
         *
         try {
        // hard copy. now we can do randomization without messing up the
        // original dataset
        dataset = (Instances) new SerializedObject(data).getObject();
        } catch (Exception ex) {
        Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
        }
         */
        this.dataset = h;
        this.selector = selector;
        this.attrRegress = dataset.getInstances().attribute(dataset.getInstances().classIndex());
    }

    public void setSelectionPerformance(StructureValidationPerformance crit) {
        this.selectionPerformance = crit;
    }

    public StructureValidationPerformance getSelectionPerformance() {
        return this.selectionPerformance;
    }

    public void setTrainingPerformance(StructureLearningPerformance meas) {
        this.trainingPerformance = meas;
    }

    public StructureLearningPerformance getTrainingPerformance() {
        return this.trainingPerformance;
    }

    /**
     * Initializes the layer of AttributeNodes and presents it to selector
     *
     * @throws java.lang.Exception
     */
    public void initAttributeLayer() throws Exception {

        ArrayList<Node> layAttrib = new ArrayList<Node>();

        for (int i = 0; i < getDataset().getInstances().classIndex(); ++i) {
            AttributeNode newborn = new AttributeNode(getDataset(), i);
            layAttrib.add(newborn);
            selector.feed(newborn);
        //System.out.println("feeding selector with " + newborn.getIdentifier());
        }
        for (int i = getDataset().getInstances().classIndex() + 1;
                i < getDataset().getInstances().numAttributes(); ++i) {
            AttributeNode newborn = new AttributeNode(getDataset(), i);
            layAttrib.add(newborn);
            selector.feed(newborn);
        //System.out.println("feeding selector with " + newborn.getIdentifier());
        }

        this.selectedLayers.add(layAttrib);
        candidates = (ArrayList<Node>) selector.getResult().clone();

    /* debugging stuff
     */
    /*for (Iterator<Node> iter = layAttrib.iterator(); iter.hasNext();) {
    AttributeNode attr = (AttributeNode) iter.next();
    for (Iterator<double[]> arrayiter = attr.validationSetOutput.iterator();
    arrayiter.hasNext();) {
    double[] set = arrayiter.next();
    for (int i = 0; i < set.length; ++i) {
    System.out.print(set[i] + ", ");
    }
    System.out.println("next");
    }
    }
     */

    }

    /**
     * Reconnect inputs of AttributeNodes. Useful when serializing/deserializing
     * because references to input dataset are lost.
     *
     * @param dataProvider
     */
    public void setAttributeLayer(DatasetSupervised dataProvider) {

        this.setDataset(dataProvider);
        Instances data = dataProvider.getInstances();

        ArrayList<AttributeNode> layAttrib = (ArrayList<AttributeNode>) selectedLayers.get(0);
        for (int j = 0; j < layAttrib.size(); ++j) {
            for (int i = 0; i < data.classIndex(); ++i) {

                Iterator<Instances> learningInputs = dataset.getLearningSets();
                Iterator<Instances> validationInputs = dataset.getValidationSets();
                AttributeNode attrNode = (AttributeNode) layAttrib.get(j);
                /*attrNode.trainSetOutput.clear();
                attrNode.validationSetOutput.clear();
                 */
                int setIndex = 0;
                while (learningInputs.hasNext()) {

                    // To reconnest AttributeNodes, we need learning and
                    // validation inputs
                    Instances learningIn = learningInputs.next();
                    Instances validationIn = validationInputs.next();

                    attrNode.trainSetOutput.add(setIndex,
                            learningIn.attributeToDoubleArray(attrNode.index));
                    attrNode.validationSetOutput.add(setIndex,
                            validationIn.attributeToDoubleArray(attrNode.index));

                    ++setIndex;
                }
            }
            for (int i = data.classIndex() + 1; i < data.numAttributes(); ++i) {
                Iterator<Instances> learningInputs = dataset.getLearningSets();
                Iterator<Instances> validationInputs = dataset.getValidationSets();
                AttributeNode attrNode = (AttributeNode) layAttrib.get(j);
                /* attrNode.trainSetOutput.clear();
                attrNode.validationSetOutput.clear();
                 */
                int setIndex = 0;
                while (learningInputs.hasNext()) {

                    // To reconnect AttributeNodes, we need learning and
                    // validation inputs
                    Instances learningIn = learningInputs.next();
                    Instances validationIn = validationInputs.next();

                    attrNode.trainSetOutput.add(setIndex,
                            learningIn.attributeToDoubleArray(attrNode.index));
                    attrNode.validationSetOutput.add(setIndex,
                            validationIn.attributeToDoubleArray(attrNode.index));

                    ++setIndex;
                }
            }
        }
    }

    public void multiSelectCombi(int nrLayers)
            throws ExpressionEqualToZero, TooBig, TooSmall {
        /*
         * Outer loop: one pass -> growth by one layer
         */
        for (int i = 1; i <= nrLayers; ++i) {
            int top = mscGrow(i);
            System.out.println("Growth: top layer is " + top);
        }
    }

    protected void copyInstances(
            Instances source, Instances dest, int from, int num) {

        for (int i = 0; i < num; i++) {
            dest.add(source.instance(from + i));
        }
    }

    /**
     *
     * Grow network by one layer; in multilayered version of
     * selectional-combinatorial algorithm. When growing layer k, takes into
     * account models from layers 0, and k-1
     *
     * @return index of newly grown top node
     * @param iteration
     * @return
     * @throws wGmdh.jGmdh.exceptions.ExpressionEqualToZero
     * @throws wGmdh.jGmdh.exceptions.TooBig
     * @throws wGmdh.jGmdh.exceptions.TooSmall
     */
    public int mscGrow(int iteration) throws ExpressionEqualToZero, TooBig, TooSmall {

        /* For ArrayLists, working with get() is faster then working with
         * iterators, so get() will be used here exclusively
         */
        /* Create Models out of the nodeSelector output
         */
        selector.reset();   // prepare it to accept new nodes
        /* feed nodeSelector with AttributeNodes
         */
        for (int i = 0; i < selectedLayers.get(0).size(); ++i) {
            selector.feed(selectedLayers.get(0).get(i));
//            System.out.println("feeding" + selectedLayers.get(0).get(i).getIdentifier());
        // never mind the selector's output
        }
        /* Uncomment to randomize tranining and validation sets
         *
        // let's randomize training and validation sets each time a new layer
        // is being built. It is actually unneccesary to randomize the data before
        // growing the first layer, but it's done anyway.
        //
        System.out.print("randomizing...");
        Iterator<Instances> learningInputs = dataset.getLearningSets();
        Iterator<Instances> validationInputs = dataset.getValidationSets();
        // these two iterators are expected to be iterating over structures of
        // same sizes

        while (learningInputs.hasNext()) {
        Instances learningIn = learningInputs.next();
        Instances validationIn = validationInputs.next();
        int learnsize = learningIn.numInstances();
        int validationsize = validationIn.numInstances();
        // first we glue them together, then do randomization
        Instances glued;
        Instances toAdd;
        if (learnsize > validationsize) {
        glued = new Instances(learningIn);
        toAdd = validationIn;
        } else {
        glued = new Instances(validationIn);
        toAdd = learningIn;
        }
        for (int i = 0; i < toAdd.numInstances(); ++i) {
        glued.add(toAdd.instance(i));
        }
        glued.randomize(new Random(iteration));
        learningIn.delete();
        validationIn.delete();
        copyInstances(glued, learningIn, 0, learnsize);
        copyInstances(glued, validationIn, learnsize, validationsize);
        }
        dataset.initialize();
        setAttributeLayer(dataset);

        // now do the same thing with Models-candidates - their outputs
        for (int i = 0; i < candidates.size(); ++i) {
        Node curr = candidates.get(i);
        if (curr instanceof Model) {
        Model currm = (Model) curr;
        Iterator<double[]> trainIter = currm.trainSetOutput.iterator();
        Iterator<double[]> validationIter = currm.validationSetOutput.iterator();
        // these two iterators are expected to be iterating over structures of
        // same sizes
        while (trainIter.hasNext()) {
        double[] trainPart = trainIter.next();
        double[] validationPart = validationIter.next();

        // by merging them together, entire dataset is obtained
        int trainSize = trainPart.length;
        int validationSize = validationPart.length;

        double[] merged = new double[trainSize + validationSize];
        int start = 0;
        System.arraycopy(
        trainPart, 0, merged, start, trainSize);
        start += trainSize;
        System.arraycopy(
        validationPart, 0, merged, start, validationSize);
        randomizeArray(new Random(iteration), merged);
        // having randomized the merged array, split it back
        start = 0;
        System.arraycopy(
        merged, start, trainPart, 0, trainSize);
        start += trainSize;
        System.arraycopy(
        merged, start, validationPart, 0, validationSize);
        }
        }
        }
        System.out.println("done");
        // having randomized the dataset, proceed with algorithm
         */

        for (int i = 0; i < candidates.size(); ++i) {
            Node input1 = candidates.get(i);
            for (int j = i + 1; j < candidates.size(); ++j) {
                Node input2 = candidates.get(j);

                /* we will only consider combinations that have top-layer-models
                 * in them (i.e. AttributeNode-AttributeNode don't interest
                 * us on higher layers)
                 */
                if (iteration > 1) {
                    if ((input1 instanceof AttributeNode) &&
                            (input2 instanceof AttributeNode)) {
                        continue;
                    }
                }

                /*
                 * Create a new model, pass it on to the selector.
                 * If the selector outputs a model, set its data
                 * member to null - it won't be used anymore so let the
                 * garbage collextor do its job
                 * all new models will share the selection criterion and
                 * error measure objects
                 */
                Model youngster = null;
                try {
                    youngster = getMaker().instantiate(
                            getSelectionPerformance(), getTrainingPerformance(),
                            input1, input2);
                } catch (Exception ex) {
                    Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                }
                youngster.coeffsAndErrors(getDataset());

                // we can freely feed the selector with a model that resulted
                // from an ill-conditioned system because the ones we're using
                // are made in a way that can handle those; such models will
                // have their performances set to NaNs, and the filters treat
                // NaNs as worst performances
                Node rejectedNode = selector.feed(
                        youngster);
                if (rejectedNode != null) {
                    if (rejectedNode instanceof Model) {
                        Model rejectedModel = (Model) rejectedNode;

                        rejectedModel.trainSetOutput = null;
                        rejectedModel.validationSetOutput = null;
                    }   // else do nothing
                }
            }
        }

        ArrayList<Model> newLayer = new ArrayList<Model>();

        ArrayList<Node> result = selector.getResult();
        for (int i = 0; i < result.size(); ++i) {
            if (result.get(i) instanceof Model) {
                newLayer.add((Model) result.get(i));
            }
        }
        candidates = (ArrayList<Node>) result.clone();

        /* Technically, a NodeFilter doesn't have to return a Model, which would
         * result with an empty layer on top.
         */
        if (newLayer.isEmpty()) {
            // an outstanding situation
            ArrayList<Model> models;
            System.out.println("empty");
            models = selector.getSortedModels();
            Iterator<Model> m = models.iterator();
            while (m.hasNext()) {
                System.out.println(m.next().getIdentifier());
            }
            Model intervention = models.get(models.size() - 1);
            newLayer.add(intervention);
            candidates.add(candidates.size() - 1, intervention);
        }

        selectedLayers.add(newLayer);

        Runtime r = Runtime.getRuntime();
//        System.out.println("available memory: " + r.freeMemory());
        r.gc();
//        System.out.println("after cleanup: " + r.freeMemory());
        /* Return index of the new top layer
         */

        return selectedLayers.size() - 1;
    }

    public void randomizeArray(Random random, double[] dbls) {

        for (int j = dbls.length - 1; j > 0; j--) {
            // swap(j, random.nextInt(j + 1));
            int randomLocation = random.nextInt(j + 1);
            double temp = dbls[j];
            dbls[j] = dbls[randomLocation];
            dbls[randomLocation] = temp;
        }
    }

    /**
     * @return the dataset
     */
    public DatasetSupervised getDataset() {
        return dataset;
    }

    /**
     * @param dataset the dataset to set
     */
    public void setDataset(DatasetSupervised dataset) {
        this.dataset = dataset;
    }

    /**
     * @return the maker
     */
    public ModelFactory getMaker() {
        return maker;
    }

    /**
     * @param maker the maker to set
     */
    public void setMaker(ModelFactory maker) {
        this.maker = maker;
    }

    /**
     * A class used to describe the best model trained.
     */
    public static class ModelAndLayer implements Serializable {

        private static final long serialVersionUID = -2310837029007635423L;
        public TwoInputModel model;
        public int layerIndex;
        public int layersWithoutImprovement;   // see MultiSelectCombi.bestModel
        public double difference;   // made to store the error change from last
                                    // layer
        /*
         * if percentile improvement of error from layer to layer is below
         * percentileTolerance, it will be treated as no improvement
         */
        private double percentileTolerance = 0;

        /* Shallow copies only.
         */
        public ModelAndLayer(ModelAndLayer mal, double tol) {
            model = mal.model;
            layerIndex = mal.layerIndex;
            layersWithoutImprovement = mal.layersWithoutImprovement;
            setPercentileTolerance(tol);
        }

        public ModelAndLayer(TwoInputModel m, int l, double tol) {
            model = m;
            layerIndex = l;
            layersWithoutImprovement = 0;
            setPercentileTolerance(tol);
        }

        /**
         * @return the percentileTolerance
         */
        public double getPercentileTolerance() {
            return percentileTolerance;
        }

        /**
         * @param percentileTolerance the percentileTolerance to set
         */
        public void setPercentileTolerance(double percentileTolerance) {
            this.percentileTolerance = percentileTolerance;
        }
    }

    /**
     * Incrementaly finds the best model according to structure validation score.
     * If the structure is trained on one subset and validated on some other and
     * structure validation measure is the same as structure learning
     * score (sse), structure validation score converges. However, because we
     * are averaging the scores on several datasets (obtained by some version of
     * cross-validation), this convergence is only an expected behavior but
     * does not have to occur. Therefore we want to be able to tolerate a number
     * of layers where improvement of structure validation score doesn't have to
     * take place, i.e. to stop training only if that score hasn't improved for
     * a number of predefined layers.
     * Note: all decision making regarding
     * modelAndLayer.layersWithoutImprovement is to be made from the caller;
     * this method only takes care of updating modelAndLayer.layersWithoutImprovement
     *
     * @param currentBest   to initialize search, pass null. To build upon a
     *                  prexisting search (e.g. you know the best one from the
     *                  3rd layer and you've grown your layers in the meantime
     *                  and now you have 10 of them so you can continue from
     *                  the 4th layer), pass the best model found in last
     *                  iteration and the index of layer it belongs to, wrapped
     *                  inside a ModelAndLayer. Remains unchanged.
     * @return  model with best performance (on our internal dataset for
     *          selection) together with index of layer it belongs to
     */
    public ModelAndLayer bestModel(ModelAndLayer currentBest) {

        int bestLocation;
        TwoInputModel bestModel;
        ModelAndLayer output;

        if (currentBest == null) {
            /* Initialization
             */
            // make sure Models are sorted
            Collections.sort(selectedLayers.get(1), new ModelComparator());
            bestLocation = selectedLayers.get(1).size() - 1;
            if (bestLocation == -1) {
                return null;
            }
            bestModel = (TwoInputModel) selectedLayers.get(1).get(bestLocation);
            output = new ModelAndLayer(bestModel, 1, errorChangeTolerance);
        } else {
            output = new ModelAndLayer(currentBest, errorChangeTolerance);
        }

        /* Nodes beyond attribute layer should all be TwoInputModel instances
         * so we won't check that.
         */
        for (int i = output.layerIndex + 1 + output.layersWithoutImprovement;
                i < selectedLayers.size(); ++i) {
            // make sure Models are sorted
            Collections.sort(selectedLayers.get(i), new ModelComparator());

            bestLocation = selectedLayers.get(i).size() - 1;
            TwoInputModel currentModel =
                    (TwoInputModel) selectedLayers.get(i).get(bestLocation);
            double relativeChange = (output.model.getErrorStructureValidationSet() -
                    currentModel.getErrorStructureValidationSet())/
                    output.model.getErrorStructureValidationSet();
            //System.out.println("rel change " + relativeChange);
            if (relativeChange > output.getPercentileTolerance()) {
                output.layerIndex = i;
                output.difference = output.model.getErrorStructureValidationSet() -
                        currentModel.getErrorStructureValidationSet();
                output.model = currentModel;
                output.layersWithoutImprovement = 0;   // reset tolerance

            } else {
                ++output.layersWithoutImprovement;
                break;
            }
        }

        //System.out.println("stepping out of best model");

        return output;
    }

    /**
     * Submodels will be marked by their uniqueID. Traversal flags are used
     * here
     *
     * @param target
     * @param fold      coefficients obtained from fold-th fold will be used
     * @return
     * @throws jGMDH.raw.exceptions.TooBig
     */
    public static String polynomialExpressionGlobal(Model target, int fold)
            throws TooBig {

        String chainedPolynomial = new String();
        /* queue is preferred to stack to enable breadth-first traversal.
         * which is nice.
         */
        LinkedList<Node> traversalQueue = new LinkedList<Node>();

        Node current = target;

        /* Traverse all used polynomials
         */
        while (current != null) {
            if (current.visited.isVisited() == true) {
                current = (Node) traversalQueue.poll();
                continue;
            }
            current.visited.onVisit();
            /* current has been visited - do work here
             */
            if (current instanceof Model) {
                chainedPolynomial = chainedPolynomial + ((Model) current).toString(0);

                /* Enqueue current's children
                 */
                Model currentModelBase = (Model) current;
                for (int i = 0; i < currentModelBase.links.size(); ++i) {

                    traversalQueue.add(currentModelBase.links.get(i));
                }
            }

            current = (Node) traversalQueue.poll();
        }

        /* Traverse all nodes visited in the above loop and reset their flags
         */
        while (current != null) {
            if (current.visited.isVisited() == false) {
                continue;
            }
            current.visited.onVisit();

            /* Enqueue current's children
             */
            if (current instanceof Model) {
                Model currentModelBase = (Model) current;
                for (int i = 0; i < currentModelBase.links.size(); ++i) {

                    traversalQueue.add(currentModelBase.links.get(i));
                }
            }
            current = (Node) traversalQueue.poll();
        }

        return chainedPolynomial;
    }

    /**
     * Passes through all nodes we have and set to null their trainSetOutput
     * and validationSetOutput.
     */
    public void arrayCleanup() {

        for (int i = 0; i < selectedLayers.size(); ++i) {
            for (int j = 0; j < selectedLayers.get(i).size(); ++j) {
                selectedLayers.get(i).get(j).trainSetOutput = null;
                selectedLayers.get(i).get(j).validationSetOutput = null;
            }
        }
    }
    /**
     * Submodels will be marked by their uniqueID. We don't use traversal flags
     * here so we will have as many loop iterations as there are vertices,
     * making our queue may grow rapidly. Perhaps it's better to use traversal
     * flags because GMDH structures typically have more nodes than vertices and
     * our goal is to traverse each node, not each vertex.
     * @param target
     * @return
     * @throws jGMDH.raw.exceptions.TooBig
     */
    /*-public static String polynomialExpressionGlobal(Model target) throws TooBig {

    String chainedPolynomial = new String();
    TreeSet usedPolynomials = new TreeSet();
    /* queue is preferred to stack to enable breadth-first traversal.
     * which is nice.
     */
    /*- LinkedList<Node> traversalQueue = new LinkedList<Node>();

    Node current = target;

    /* Traverse all used polynomials and put them in a TreeSet
     */
    /*-while (current != null) {
    /* Add current to the treeSet
     */
    /*- System.out.println("working on " + current.identifier);
    usedPolynomials.add(current);
    /* Enqueue current's children
     */
    /*-if (current instanceof Model) {
    Model currentModelBase = (Model) current;
    for (int i = 0; i < currentModelBase.links.size(); ++i) {

    traversalQueue.add(currentModelBase.links.get(i));
    }
    }
    current = (Node) traversalQueue.poll();
    }
    /* Now traverse usedPolynomials
     */
    /*-Iterator<Node> it = usedPolynomials.iterator();
    while (it.hasNext()) {
    current = it.next();
    if (current instanceof Model) {
    Model currentModel = ((Model) current);
    Iterator<Summand> summandIter =
    ((Model) current).generateSummands().iterator();

    chainedPolynomial = chainedPolynomial.concat(
    "P" + currentModel.getIdentifier() +
    " = ");
    /* summandIter definitely has next
     */
    /*-     chainedPolynomial = chainedPolynomial.concat(String.valueOf(summandIter.next().coefficient));
    while (summandIter.hasNext()) {
    Summand currentSummand = summandIter.next();
    chainedPolynomial = chainedPolynomial.concat(" + " + currentSummand.coefficient);
    Iterator<Node> variableIter =
    currentSummand.variables.iterator();
    while (variableIter.hasNext()) {
    Node next = variableIter.next();
    if (next instanceof AttributeNode) {
    chainedPolynomial = chainedPolynomial.concat("*" +
    ((AttributeNode) next).attr.name());
    } else {
    chainedPolynomial = chainedPolynomial.concat("*P" +
    next.getIdentifier());
    }
    }
    }
    chainedPolynomial = chainedPolynomial.concat("\n");
    }
    }

    System.out.println("stepping into represent models");

    return chainedPolynomial;
    }-*/
}