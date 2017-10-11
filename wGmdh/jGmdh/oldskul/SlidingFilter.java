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
import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Option;
import weka.core.Utils;

/**
 *
 * @author ivek
 *
 * A class used for on-the-go determining and storing references to N best
 * models according to the selected criterion. In case it's fed with
 * AttributeNodes, each gets accepted.
 */
public class SlidingFilter extends NodeFilter {
    /* A class that stores references to ModelBases, sorted by error measure.
     * It's implemented by a sorted ArrayList - not a bottleneck because
     * the number of stored references is small.
     */

    private static final long serialVersionUID = 9049856552876526824L;
    private static int defaultTargetLength = 100;
    protected ArrayList<Model> models;
    protected ArrayList<AttributeNode> attributeNodes;

    public int getSize() {
        return models.size();
    }
    private int targetLength,  currentLength;    // Window lengths
    private Boolean isTransitional;
    private ModelComparator comparator;

    public SlidingFilter() {
        super();
        models = new ArrayList<Model>();
        attributeNodes = new ArrayList<AttributeNode>();
        comparator = new ModelComparator();

        this.targetLength = defaultTargetLength;
        this.currentLength = 0;

        isTransitional = true;

    }

    public SlidingFilter(int targetLength) {
        super();
        models = new ArrayList<Model>();
        attributeNodes = new ArrayList<AttributeNode>();
        comparator = new ModelComparator();

        /*if (targetLength == 0) {
        throw new ExpressionEqualToZero("targetLength == 0");
        }
         */
        this.targetLength = targetLength;
        this.currentLength = 0;

        isTransitional = true;
    }

    /**
     * Adds a model to the when the current length of models is
     * smaller than its target length
     *
     * @param newModel  input Model class
     */
    private void feedTransitional(Model newModel) {
        models.add(newModel);
        ++currentLength;
        Collections.sort(models, comparator);
        if (currentLength == getTargetLength()) {
            isTransitional = false;
        /* transitional period over - slideRegular will be called in next
         * iteration
         */
        }
    }

    /**
     * Updates the models array when its current length is equal to its
     * target length. Models array either remains unchanged or
     * a new reference replaces an old one
     *
     * @param newModel  input Model class
     * @return          Model removed out of trainingSetOutput container
     */
    private Model feedRegular(Model newModel) {
        models.add(newModel);
        Collections.sort(models, comparator);
        return (Model) models.remove(0);
    }

    /**
     * Updates the models array. It decides internally whether to call
     * slideTransitional or slideRegular
     *
     * @param newNode   input Node object
     * @return          either a Model that gets replaced by another one
     *                  or null if none got replaced
     */
    public Node feed(Node newNode) {

        // if newModel is an attributeNode, just store it - no filtering
        if (newNode instanceof AttributeNode) {
            attributeNodes.add((AttributeNode) newNode);
        } else if (newNode instanceof Model) {
            /*
             * Check if the same Model already exists
             */
            for (Model n : this.models) {
                if (newNode.equals((Model) n)) {
                    return newNode;
                }
            }

            /* TODO: It would be faster if we used a delegate here rather than
             * conditional branching with condition evaluated each time this method
             * is called
             */
            if (isTransitional) {
                feedTransitional((Model) newNode);
                return null;
            } else {
                return feedRegular((Model) newNode);
            }
        }
        // leave it alone
        return newNode;
    }

    @Override
    public ArrayList<Node> getResult() {
        // return merged models and attributeNodes
        ArrayList<Node> output = new ArrayList<Node>();
        output.addAll((ArrayList<AttributeNode>)attributeNodes.clone());
        output.addAll((ArrayList<Model>)models.clone());
        return output;
    }

    @Override
    public void reset() {
        models.clear();
        attributeNodes.clear();
        this.currentLength = 0;
        isTransitional = true;
    }

    /**
     *
     * @param options
     * @throws java.lang.Exception
     */
    @Override
    public void setOptions(String[] options) throws Exception {

        String string;
        string = Utils.getOption('S', options);
        if (string.length() != 0) {
            setTargetLength((new Integer(string)).intValue());
        } else {
            setTargetLength(defaultTargetLength);
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

        options.add("-S");
        options.add("" + this.getTargetLength());

        return (String[]) options.toArray(new String[options.size()]);
    }

    @Override
    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        Vector newVector = new Vector(1);
        newVector.addElement(new Option(
                "\tNumber of models to be selecetd",
                "S", 1, "-S <number of selected models>"));

        Enumeration parent = super.listOptions();
        while (parent.hasMoreElements()) {
            newVector.addElement(parent.nextElement());
        }

        return options.elements();
    }

    /**
     * @return the targetLength
     */
    public int getTargetLength() {
        return targetLength;
    }

    /**
     * @param targetLength the targetLength to set
     */
    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }

    @Override
    public ArrayList<Model> getSortedModels() {
        ArrayList<Model> sorted = (ArrayList<Model>) models.clone();
        java.util.Collections.sort(sorted, new ModelComparator());
        return sorted;
    }
}   // SlidingFilter ends
