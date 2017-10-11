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
import java.util.HashMap;
import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import weka.core.Instances;

/**
 * A class used to:
 *  - hard-copy models
 *  - retrain the model with prefixed structure
 *
 * @author ivek
 */
public class CopyCat {

    private ModelFactory factory;
    private Model prototype;

    private CopyCat(){};

    public CopyCat(Model m, ModelFactory f)   {
        factory = f;
        prototype = m;
    }

    /**
     * Trains a new model that has the same structure as this. Its coefficients
     * will be calculated from the {inputs, goal} dataset.
     * Similar approach can be used to make a deep copy of a model (everything
     * the same, except the coefficients get copied, no need to calculate them).
     * Outputs of all submodels will be nullified.
     *
     * @param trainInsts
     * @return
     */
    public Model copyStructureAndPurify(Instances trainInsts) throws TooBig, ExpressionEqualToZero, TooSmall, Exception {
        /* We'll use a hashmap with submodels' IDs as keys. Traversal will
         * be made using a traversal list, but we
         * want to leave the entries in the list, not pop them out, to make
         * the second traversal faster.
         */

//        System.out.println("Rebuilding structure");
        ArrayList<Node> traversalList = new ArrayList<Node>();

        traversalList.add(prototype);
        int iterator = 0;
        Node currentNode;
        while (iterator < traversalList.size()) {
            currentNode = traversalList.get(iterator++);

            if (currentNode.visited.isVisited() == false) {
                currentNode.visited.onVisit();
                // work goes here

                if (currentNode instanceof Model) {
                    Model visitedModel = (Model) currentNode;
                    for (int i = 0; i < visitedModel.links.size(); ++i) {
                        traversalList.add(visitedModel.links.get(i));
                    }
                }
            }//else go to next iteration
        }
        // we may have cycles in our graph. this means that a situation may
        // occur where we will visit a node from a lower layer before
        // visiting the one from a higher layer. to avoid this we will simply
        // sort the list
        java.util.Collections.sort(traversalList);

        /* Now we can start copying the structure, bottom-up, i.e. from the end
         * of traversalList to its begining
         */
        double[] goal =
                trainInsts.attributeToDoubleArray(trainInsts.classIndex());
        HashMap copyHm = new HashMap();
        for (int i = 0; i < traversalList.size(); ++i) {
            currentNode = traversalList.get(i);
            if (currentNode instanceof AttributeNode) {
                AttributeNode attr = (AttributeNode) currentNode;
                if (!copyHm.containsKey(attr.getIdentifier())) {
                    copyHm.put(attr.getIdentifier(),
                            new AttributeNode(trainInsts, null, attr.index));
//                    System.out.println("copyHm put:" + attr.getIdentifier());
//                    System.out.println("    our ID: " +
//                            ((AttributeNode) copyHm.get(attr.getIdentifier())).getIdentifier());
                //System.out.println("instances:");
                /*                    double[] out = ((AttributeNode)copyHm.get(attr.getIdentifier())).trainSetOutput;
                for(int j = 0; j<out.length; ++j)   {
                System.out.println("    " + out[j]);
                }
                 */
                }
            } else {
                // it's an M
                Model mod = (Model) currentNode;
                long keyOfFirstChild = mod.links.get(0).getIdentifier();
                long keyOfSecondChild = mod.links.get(1).getIdentifier();

                if (!copyHm.containsKey(mod.getIdentifier())) {
                    Model tim = factory.instantiate(
                            goal, null, null,
                            (Node) copyHm.get(keyOfFirstChild),
                            (Node) copyHm.get(keyOfSecondChild));
                    copyHm.put(mod.getIdentifier(),
                            tim);
/*                    System.out.println(currentNode.getIdentifier() + " => " +
                            tim.getIdentifier());
                    System.out.println("    children: " + ((Node) copyHm.get(keyOfFirstChild)).getIdentifier() + ", " +
                            ((Node) copyHm.get(keyOfSecondChild)).getIdentifier());
*/                }
            }
        }
        /* One more traversal - kill the outputs of submodels and reset the
         * currrentNode flag
         */
        for (int i = 0; i < traversalList.size(); ++i) {
            currentNode = traversalList.get(i);
            // reset its traversal flag
            currentNode.visited.resetVisited();
            currentNode.trainSetOutput = null;
            currentNode.validationSetOutput = null;
        }

        return (Model) copyHm.get(prototype.getIdentifier());
    }


    /**
     * Trains a new model that has the same structure as this. Its coefficients
     * will be calculated from the {inputs, goal} dataset.
     * Similar approach can be used to make a deep copy of a model (everything
     * the same, except the coefficients get copied, no need to calculate them).
     *
     * @param trainInsts
     * @return
     */
    public TwoInputModel copyStructure(Instances trainInsts) throws TooBig, ExpressionEqualToZero, TooSmall {
        /* We'll use a hashmap with submodels' IDs as keys. Traversal will
         * be made using a traversal list, but we
         * want to leave the entries in the list, not pop them out, to make
         * the second traversal faster.
         */
        ArrayList<Node> traversalList = new ArrayList<Node>();

        traversalList.add(prototype);
        int iterator = 0;
        Node currentNode;
        while (iterator < traversalList.size()) {
            currentNode = traversalList.get(iterator++);

            if (currentNode.visited.isVisited() == false) {
                currentNode.visited.onVisit();
                // work goes here

                if (currentNode instanceof Model) {
                    Model visitedModel = (Model) currentNode;
                    for (int i = 0; i < visitedModel.links.size(); ++i) {
                        traversalList.add(visitedModel.links.get(i));
                    }
                }
            }//else go to next iteration
        }
        // we may have cycles in our graph. this means that a situation may
        // occur where we will visit a node from a lower layer before
        // visiting the one from a higher layer. to avoid this we will simply
        // sort the list
        java.util.Collections.sort(traversalList);

        /* Now we can start copying the structure, bottom-up, i.e. from the end
         * of traversalList to its begining
         */
        double[] goal =
                trainInsts.attributeToDoubleArray(trainInsts.classIndex());
        HashMap copyHm = new HashMap();
        for (int i = 0; i < traversalList.size(); ++i) {
            currentNode = traversalList.get(i);
            if (currentNode instanceof AttributeNode) {
                AttributeNode attr = (AttributeNode) currentNode;
                if (!copyHm.containsKey(attr.getIdentifier())) {
                    copyHm.put(attr.getIdentifier(),
                            new AttributeNode(trainInsts, null, attr.index));
//                    System.out.println("copyHm put:" + attr.getIdentifier());
//                    System.out.println("    our ID: " +
//                            ((AttributeNode) copyHm.get(attr.getIdentifier())).getIdentifier());
                //System.out.println("instances:");
                /*                    double[] out = ((AttributeNode)copyHm.get(attr.getIdentifier())).trainSetOutput;
                for(int j = 0; j<out.length; ++j)   {
                System.out.println("    " + out[j]);
                }
                 */
                }
            } else {
                // it's a TwoInputModel
                TwoInputModel mod = (TwoInputModel) currentNode;
                long keyOfFirstChild = mod.links.get(0).getIdentifier();
                long keyOfSecondChild = mod.links.get(1).getIdentifier();

                if (!copyHm.containsKey(mod.getIdentifier())) {
                    TwoInputModel tim = new TwoInputModel(
                            goal, null, null,
                            (Node) copyHm.get(keyOfFirstChild),
                            (Node) copyHm.get(keyOfSecondChild));
                    copyHm.put(mod.getIdentifier(),
                            tim);
/*                    System.out.println(currentNode.getIdentifier() + " => " +
                            tim.getIdentifier());
                    System.out.println("    children: " + ((Node) copyHm.get(keyOfFirstChild)).getIdentifier() + ", " +
                            ((Node) copyHm.get(keyOfSecondChild)).getIdentifier());
*/                }
            }
        }
        /* One more traversal - kill the outputs of submodels and reset the
         * currrentNode flag
         */
        for (int i = 0; i < traversalList.size(); ++i) {
            currentNode = traversalList.get(i);
            // reset its traversal flag
            currentNode.visited.resetVisited();
        }

        return (TwoInputModel) copyHm.get(prototype.getIdentifier());
    }
}
