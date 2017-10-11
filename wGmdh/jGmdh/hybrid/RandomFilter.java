/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wGmdh.jGmdh.hybrid;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;
import wGmdh.jGmdh.hybrid.CfsFilter.CorrsAndDevsAndMeans;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.ModelComparator;
import wGmdh.jGmdh.oldskul.Node;
import wGmdh.jGmdh.oldskul.NodeFilter;
import weka.core.Option;
import weka.core.Utils;

/**
 *
 * @author vekk
 */
public class RandomFilter extends NodeFilter {

    private static final long serialVersionUID = -4730729817538160185L;
    private static int defaultSetsize = 20;
    protected Vector<Node> set;
    protected CorrsAndDevsAndMeans storage;
    private int setsize;

    public RandomFilter() {
        super();
        set = new Vector<Node>();
        this.setSetsize(defaultSetsize);
    //storage = new CorrsAndDevsAndMeans();
    }

    public RandomFilter(int size) {
        super();
        set = new Vector<Node>();
        setsize = size;
    //storage = new CorrsAndDevsAndMeans();
    }

    @Override
    public Node feed(Node newNode) {
        /*
         * Wrap and store models.
         */

        set.add(newNode);
        return null;
    }

    @Override
    public ArrayList<Node> getResult() {
        // randomly select nodes out of set
        int totalsize = set.size();
        Vector<Integer> rawIndices = new Vector<Integer>(totalsize);
        int limit = (setsize < set.size() ? setsize : set.size());
        int[] selectedIndices = new int[limit];

        for (int i = 0; i < totalsize; ++i) {
            rawIndices.add(i);
        }

        // add the best model(if there is one).
        ArrayList<Node> selected = new ArrayList<Node>(setsize);
        int bestIndex = 0;
        Model best = null;
        int i = 0;
        for (i = 0; i < set.size(); ++i) {
            Node n = set.get(i);
            if (n instanceof Model) {
                best = (Model) n;
                bestIndex = i;
            }
        }
        for (; i < set.size(); ++i) {
            Node n = set.get(i);
            if (n instanceof Model) {
                Model current = (Model) n;
                if (current.getErrorStructureValidationSet() < best.getErrorStructureValidationSet()) {
                    best = current;
                    bestIndex = i;
                }
            }
        }
        int iter;
        if (best != null) {
            selectedIndices[0] = bestIndex;
            rawIndices.remove(bestIndex);
            iter = 1;
        } else {
            iter = 0;
        }

        // add other nodes randomly
        Random r = new Random();
        for (; iter < limit; ++iter) {
            int randomInt = r.nextInt(rawIndices.size());
            selectedIndices[iter] = rawIndices.get(randomInt);
            rawIndices.remove(randomInt);
        }


        for (i = 0; i < selectedIndices.length; ++i) {
            //System.out.println("adding index " + selectedIndices[i]);
            selected.add(set.get(selectedIndices[i]));
        }

        return selected;
    }

    @Override
    public void reset() {
        set.clear();
        storage = null; // we should do it more elegantly
    }

    /**
     * @return the setsize
     */
    public int getSetsize() {
        return setsize;
    }

    /**
     * @param setsize the setsize to set
     */
    public void setSetsize(int setsize) {
        this.setsize = setsize;
    }

    @Override
    public ArrayList<Model> getSortedModels() {
        ArrayList<Model> models = new ArrayList<Model>();
        for (Node n : set) {
            //System.out.println(n.getIdentifier());
            if (n instanceof Model) {
                Model m = (Model) n;
                models.add(m);

            }
        }
        java.util.Collections.sort(models, new ModelComparator());
        return models;
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
            setSetsize((new Integer(string)).intValue());
        } else {
            setSetsize(defaultSetsize);
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

        options.add("-S");
        options.add("" + this.getSetsize());

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
}