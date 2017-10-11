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
import java.util.Iterator;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;

/**
 * a modified multilayered-combinatorial GMDH network class. the idea is to
 * force an increase of the polynomial order of exactly one when growing it
 * by one layer. to be used with TwoInputModelMkIIFactory. otherwise we won't
 * have this nice property
 *
 * @author ivek
 */
public class MultiSelectCombiMkII extends MultiSelectCombi {

    private static final long serialVersionUID = -7268330695981979403L;

    public String path = "./";

    public MultiSelectCombiMkII(DatasetSupervised h, ModelFactory f, NodeFilter selector) {

        super(h, f, selector);
    }

    /*
     * we'll store models-candidates per each layer
     */
    public ArrayList<ArrayList<Model>> modelsThatProducedLayer =
            new ArrayList<ArrayList<Model>>();

    /**
     *
     * Grow network by one layer; in multilayered version of
     * selectional-combinatorial algorithm. When growing layer k, takes into
     * account models from layers 0, and k-1.
     *
     * The thing is that we'd like to have models of controlledly increasing
     * complexity from layer to layer, moreover, we'd like to have polynomials
     * of order n+1 on the n-th layer. They will get constructed in the
     * following way:
     *  if n is odd (oder of polynomial is even), the candidates will be built
     *      by combining polynomials from layer n/2 and by combining polynomials
     *      from layer n-1 and 0-th layer
     *  if n is even (oder of polynomial is odd), the candidates will be built
     *      by combining polynomials from layer n/2 and by combining polynomials
     *      from layer n-1 and 0-th layer
     *
     * this way we've slightly biased the selection mechanism but nevermind -
     * it's more stable than the usual thing because the complexity of models is
     * much smoother as a function of layer.
     *
     * Note: start counting the iteration-1s from 1.
     *
     * @return index of newly grown top node
     * @param iteration-1
     * @return
     * @throws wGmdh.jGmdh.exceptions.ExpressionEqualToZero
     * @throws wGmdh.jGmdh.exceptions.TooBig
     * @throws wGmdh.jGmdh.exceptions.TooSmall
     */
    @Override
    public int mscGrow(int iteration) throws ExpressionEqualToZero, TooBig, TooSmall {

        ArrayList<Model> newLayer = new ArrayList<Model>();


        if (iteration - 1 < 2) {
            if (iteration - 1 == 0) {
                // straightforward stuff

                // candidates have already been through filtering in initAttributeLayer()
                for (int i = 0; i < candidates.size(); ++i) {
                    Node input1 = candidates.get(i);
                    for (int j = i + 1; j < candidates.size(); ++j) {
                        Node input2 = candidates.get(j);

                        Model youngster = null;
                        try {
                            youngster = getMaker().instantiate(
                                    getSelectionPerformance(), getTrainingPerformance(),
                                    input1, input2);
                        } catch (Exception ex) {
                            Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        youngster.coeffsAndErrors(getDataset());
                        newLayer.add(youngster);
                    }
                }

            } else {
                selector.reset();
                // AttributeNodes..
                for (int i = 0; i < selectedLayers.get(0).size(); ++i) {
                    selector.feed(selectedLayers.get(0).get(i));

                }
                // ...and the first layer
                for (int i = 0; i < selectedLayers.get(iteration - 1).size(); ++i) {
                    Node rejectedNode = selector.feed(selectedLayers.get(iteration - 1).get(i));
                    if (rejectedNode != null) {
                        if (rejectedNode instanceof Model) {
                            Model rejectedModel = (Model) rejectedNode;

                            rejectedModel.trainSetOutput = null;
                            rejectedModel.validationSetOutput = null;
                        }   // else do nothing
                    }
                }

                ArrayList<Node> result = selector.getResult();
                candidates = (ArrayList<Node>) result.clone();
                boolean hasModel = false;
                for (Node n : candidates) {
                    if (n instanceof Model) {
                        hasModel = true;
                        break;
                    }
                }

                /* Technically, a NodeFilter doesn't have to return a Model, but
                 * we'll insist on it
                 */
                if (!hasModel) {
                    // an outstanding situation
                    ArrayList<Model> models;
                    System.out.println("empty");
                    models = selector.getSortedModels();
                    Iterator<Model> m = models.iterator();
                    while (m.hasNext()) {
                        System.out.println(m.next().getIdentifier());
                    }
                    Model intervention = models.get(models.size() - 1);
                    candidates.add(intervention);
                }

                ArrayList<Model> dest = new ArrayList<Model>();
                modelsThatProducedLayer.add(dest);

                for (int i = 0; i < candidates.size(); ++i) {
                    Node input1 = candidates.get(i);
                    // store the Models involved in producing the next Layer
                    if (input1 instanceof Model) {
                        dest.add((Model) input1);
                    }
                    for (int j = i + 1; j < candidates.size(); ++j) {
                        Node input2 = candidates.get(j);


                        if ((input1 instanceof Model) &&
                                (input2 instanceof Model)) {
                            continue;
                        }

                        Model youngster = null;
                        try {
                            youngster = getMaker().instantiate(
                                    getSelectionPerformance(), getTrainingPerformance(),
                                    input1, input2);
                        } catch (Exception ex) {
                            Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        youngster.coeffsAndErrors(getDataset());
                        newLayer.add(youngster);
                    }
                }
            /* traverse dest and kill the outputs of Models contained, to
             * save memory. we'll calculate those outputs when needed
             */
            for (Model m : dest) {
                try {
                    // i'll serialize those, it'll be faster.
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " train "+ ".gmdh", m.trainSetOutput);
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " valid "+ ".gmdh", m.validationSetOutput);
                    //System.out.println("Serializing " + m.getIdentifier() + ".gmdh");
                    m.trainSetOutput.clear();
                    m.validationSetOutput.clear();
                } catch (Exception ex) {
                    Logger.getLogger(MultiSelectCombiMkII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            }
        // this branching sucks but works.
        } else if (iteration - 1 % 2 == 1) {

            // first decide who the candidates will be
            selector.reset();
            // AttributeNodes..
            for (int i = 0; i < selectedLayers.get(0).size(); ++i) {
                selector.feed(selectedLayers.get(0).get(i));

            }
            // ...and the (iteration-1)-th layer
            for (int i = 0; i < selectedLayers.get(iteration - 1).size(); ++i) {
                Node rejectedNode = selector.feed(selectedLayers.get(iteration - 1).get(i));
                if (rejectedNode != null) {
                    if (rejectedNode instanceof Model) {
                        Model rejectedModel = (Model) rejectedNode;

                        rejectedModel.trainSetOutput = null;
                        rejectedModel.validationSetOutput = null;
                    }   // else do nothing
                }
            }

            ArrayList<Node> result = selector.getResult();
            candidates = (ArrayList<Node>) result.clone();
            boolean hasModel = false;
            for (Node n : candidates) {
                if (n instanceof Model) {
                    hasModel = true;
                    break;
                }
            }

            /* Technically, a NodeFilter doesn't have to return a Model, but
             * we'll insist on it
             */
            if (!hasModel) {
                // an outstanding situation
                ArrayList<Model> models;
                System.out.println("empty");
                models = selector.getSortedModels();
                Iterator<Model> m = models.iterator();
                while (m.hasNext()) {
                    System.out.println(m.next().getIdentifier());
                }
                Model intervention = models.get(models.size() - 1);
                candidates.add(intervention);
            }

            // filtering stuff done

            /* to make things nicer to look at, we'll leave candidates only in
             * selectedLayers
             */
            ArrayList<Model> selectedModels = new ArrayList<Model>();
            for (Node n : candidates) {
                if (n instanceof Model) {
                    selectedModels.add((Model) n);
                }
            }
            selectedLayers.set(selectedLayers.size() - 1, selectedModels);

            selector.reset();

            ArrayList<Model> dest = new ArrayList<Model>();
            modelsThatProducedLayer.add(dest);

            for (int i = 0; i < candidates.size(); ++i) {
                Node input1 = candidates.get(i);
                // store the Models involved in producing the next Layer
                if (input1 instanceof Model) {
                    dest.add((Model) input1);
                }
                for (int j = i + 1; j < candidates.size(); ++j) {
                    Node input2 = candidates.get(j);

                    /*AttributeNode-AttributeNode combis don't interest
                     * us on higher layers
                     */
                    if ((input1 instanceof AttributeNode) &&
                            (input2 instanceof AttributeNode)) {
                        continue;
                    }

                    if ((input1 instanceof Model) &&
                            (input2 instanceof Model)) {
                        continue;
                    }
                    Model youngster = null;
                    try {
                        youngster = getMaker().instantiate(
                                getSelectionPerformance(), getTrainingPerformance(),
                                input1, input2);
                    } catch (Exception ex) {
                        Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    youngster.coeffsAndErrors(getDataset());
                    newLayer.add(youngster);
                }
            }

            /* traverse dest and kill the outputs of Models contained, to
             * save memory. we'll calculate those outputs when needed
             */
            for (Model m : dest) {
                try {
                    // i'll serialize those, it'll be faster.
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " train "+ ".gmdh", m.trainSetOutput);
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " valid "+ ".gmdh", m.validationSetOutput);
                    //System.out.println("Serializing " + m.getIdentifier() + ".gmdh");
                    m.trainSetOutput.clear();
                    m.validationSetOutput.clear();
                } catch (Exception ex) {
                    Logger.getLogger(MultiSelectCombiMkII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        } else {
            // iteration-1 is even
            // start by filtering out Models to find the candidates
            selector.reset();
            // AttributeNodes..
            for (int i = 0; i < selectedLayers.get(0).size(); ++i) {
                selector.feed(selectedLayers.get(0).get(i));
            }
            // ...and the (iteration-1)-th layer (currently topmost layer)
            for (int i = 0; i < selectedLayers.get(iteration - 1).size(); ++i) {
                Node rejectedNode = selector.feed(selectedLayers.get(iteration - 1).get(i));
                if (rejectedNode != null) {
                    if (rejectedNode instanceof Model) {
                        Model rejectedModel = (Model) rejectedNode;

                        rejectedModel.trainSetOutput = null;
                        rejectedModel.validationSetOutput = null;
                    }   // else do nothing
                }
            }

            /* At this point we' re done with feeding the selector and we can
             * go for the candidates
             */
            ArrayList<Node> result = selector.getResult();
            candidates = (ArrayList<Node>) result.clone();

            boolean hasModel = false;
            for (Node n : candidates) {
                if (n instanceof Model) {
                    hasModel = true;
                    break;
                }
            }

            /* Technically, a NodeFilter doesn't have to return a Model, but
             * we'll insist on it
             */
            if (!hasModel) {
                // an outstanding situation
                ArrayList<Model> models;
                System.out.println("empty");
                models = selector.getSortedModels();
                Iterator<Model> m = models.iterator();
                while (m.hasNext()) {
                    System.out.println(m.next().getIdentifier());
                }
                Model intervention = models.get(models.size() - 1);
                candidates.add(intervention);
            }

            // filtering stuff done

            /* to make things nicer to look at, we'll leave candidates only in
             * selectedLayers
             */
            ArrayList<Model> selectedModels = new ArrayList<Model>();
            for (Node n : candidates) {
            if (n instanceof Model) {
            Model m = (Model)n;
            // improve coding style. optimize. weak TODO.
            selectedModels.add((Model) n);
            }
            }
            selectedLayers.set(selectedLayers.size() - 1, selectedModels);
            
            selector.reset();

            ArrayList<Model> dest = new ArrayList<Model>();
            modelsThatProducedLayer.add(dest);
            for (int i = 0; i < candidates.size(); ++i) {
                Node input1 = candidates.get(i);
                // store the Models involved in producing the next Layer
                if (input1 instanceof Model) {
                    dest.add((Model) input1);
                }
                for (int j = i + 1; j < candidates.size(); ++j) {
                    Node input2 = candidates.get(j);

                    /*AttributeNode-AttributeNode combis don't interest
                     * us on higher layers
                     */
                    if ((input1 instanceof AttributeNode) &&
                            (input2 instanceof AttributeNode)) {
                        continue;
                    }
                    if ((input1 instanceof Model) &&
                            (input2 instanceof Model)) {
                        continue;
                    }
                    // this conditional branching can be made more efficient.


                    Model youngster = null;
                    try {
                        youngster = getMaker().instantiate(
                                getSelectionPerformance(), getTrainingPerformance(),
                                input1, input2);
                    } catch (Exception ex) {
                        Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    youngster.coeffsAndErrors(getDataset());
                    newLayer.add(youngster);
                }


            }
            /* traverse dest and kill the outputs of Models contained, to
             * save memory. we'll calculate those outputs when needed
             */
            for (Model m : dest) {
                try {
                    // i'll serialize those, it'll be faster.
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " train "+ ".gmdh", m.trainSetOutput);
                    weka.core.SerializationHelper.write(path+
                            m.getIdentifier() + " valid "+ ".gmdh", m.validationSetOutput);
                    //System.out.println("Serializing " + m.getIdentifier() + ".gmdh");
                    m.trainSetOutput.clear();
                    m.validationSetOutput.clear();
                } catch (Exception ex) {
                    Logger.getLogger(MultiSelectCombiMkII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }




            /* and there are some models of the order (iteration-1+2) that we
             * still need to calculate. they are made by combining 2 Models
             * from (iteration-1/2)-th layer, stored at
             * modelsThatProducedLayer.get(((iteration-1)/2)-1).
             */
            ArrayList<Model> candidateModels = modelsThatProducedLayer.get(((iteration - 1) / 2 - 1));

            /* we need to calculate their outputs
             * (we deleted those before, remember?)
             */
            for (Model m : candidateModels) {
                // i'm expecting to have TwoInputModelMkII Models here exclusively
                // - bad coding style, a TODO:
                //((TwoInputModelMkII) m).calculateOutputs(dataset);

                // simply deserialize model's outputs
                try {
                    //System.out.println("Deserializing " + m.getIdentifier() + ".gmdh");
                    m.trainSetOutput = (ArrayList<double[]>) weka.core.SerializationHelper.read(path+
                            m.getIdentifier() + " train "+ ".gmdh");
                    m.validationSetOutput = (ArrayList<double[]>) weka.core.SerializationHelper.read(path+
                            m.getIdentifier() + " valid "+ ".gmdh");

                } catch (Exception ex) {
                    Logger.getLogger(MultiSelectCombiMkII.class.getName()).log(Level.SEVERE, null, ex);
                }
            }


            for (int i = 0; i < candidateModels.size(); ++i) {
                Model input1 = candidateModels.get(i);
                for (int j = i + 1; j < candidateModels.size(); ++j) {
                    Model input2 = candidateModels.get(j);
                    Model youngster = null;
                    try {
                        youngster = getMaker().instantiate(
                                getSelectionPerformance(), getTrainingPerformance(),
                                input1, input2);
                    } catch (Exception ex) {
                        Logger.getLogger(MultiSelectCombi.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    youngster.coeffsAndErrors(getDataset());
                    newLayer.add(youngster);
                }
            }
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
}

