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

import wGmdh.jGmdh.gui.NodeGraphics;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * TODO: delegate all data folding and splitting to DatasetHandler (
 * networkOutput, networkOutputNoFlags)
 *
 * @author ivek
 */
public abstract class Node implements Comparable<Node>, Serializable {

    private static long currentIdentifier = 0;
    /* Unique node ID... makes debugging easier, is also used when
     * determining equality of 2 models, regardless of their links' ordering.
     * Used, altough its memory address can be regarded as a unique identifier
     */
    private long identifier;
    /* Next two members enable the use of n-fold crossvalidation to determine
     * the network structure. The i-th entries correspont to i-th fold. Each
     */
    public ArrayList<double[]> trainSetOutput;
    public ArrayList<double[]> validationSetOutput;
    public transient NodeGraphics graphics;

    //public boolean visited;
    /**
     * An interface used to check if node has been visited during a graph
     * traversal.
     * Intended mechanism: after onVisit() gets called, a call to
     * isVisited() returns true until resetVisited() gets called - after this
     * isVisited() returns false. Starting conditions may vary.
     */
    interface CheckVisited {

        boolean isVisited();

        void onVisit();

        void resetVisited();
    }

    /**
     * Big cyclic graphs are expected that have significantly less nodes than
     * vertices. So, when traversing each node once, it's beneficial to use a
     * traversal flag. 1 extra boolean per Node... not too much memory.
     * Not synchronized or anything, this is just a wrapper around a bool flag,
     * so have in mind who else might be using it.
     */
    public class Visited implements CheckVisited, Serializable {

        private static final long serialVersionUID = -627226309338313738L;
        private boolean flag;

        private Visited() {
        }
        ;

        Visited(boolean b) {
            flag = b;
        }

        public boolean isVisited() {
            return flag;
        }

        public void onVisit() {
            flag = true;
        }

        public void resetVisited() {
            flag = false;
        }
    }
    public Visited visited;

    Node() {
        this.identifier = ++currentIdentifier;
        visited = new Visited(false);
        trainSetOutput = new ArrayList<double[]>();
        validationSetOutput = new ArrayList<double[]>();

    /* Note that graphics class hasn't been initialized in this constructor
     */
    }

    Node(int nrFolds) {
        this.identifier = ++currentIdentifier;
        visited = new Visited(false);
        trainSetOutput = new ArrayList<double[]>(nrFolds);
        validationSetOutput = new ArrayList<double[]>(nrFolds);

    /* Note that graphics class hasn't been initialized in this constructor
     */
    }

    public int getNrFolds() throws Exception {
        //if (trainSetOutput.size() != validationSetOutput.size()) {
        //}     // TODO: throw exception
        return trainSetOutput.size();
    }

    static public void setGlobalIdentifier(long l) {
        currentIdentifier = l;
    }
    ;

    static public long getGlobalIdentifier() {
        return currentIdentifier;
    }
    ;

    public void InitializeGraphics(NodeGraphics graphics) {
        this.graphics = graphics;
    }

    public long getIdentifier() {
        return identifier;
    }

    public int compareTo(Node n) {
        if (this.identifier < n.identifier) {
            return -1;
        }
        if (this.identifier == n.identifier) {
            return 0;
        } else {
            return +1;
        }
    }


    // TODO: deprecate networkOutput and networkOutputNoFlags by making equivalent
    // methods that do the same thing with DatasetHandlers
    /**
     * To systematically enable crossvalidation to help decide which structure
     * to go with, data and coefficients learned are organized in ArrayLists -
     * each entry is related to one fold of CV.
     * Call this one when you need to calculate entire network
     *
     * @param inputs    ...the Model is fed with
     * @param index     ... of CV fold
     * @return output   the node gives when given an input
     */
    abstract public double networkOutput(
            double inputs[], int index);

    /**
     * This one gets invoked by networkOutput. Flags are a must, performance issues
     * otherwise.
     *
     * @param inputs
     * @param index
     * @return
     */
    abstract public double networkOutputNoFlags(
            double inputs[], int index);

    /**
     * A Node may propagate its output function or a residual or whatever, but,
     * basically, what it propagates doesn't have to be the output of entire
     * structure beneath it.
     *
     * @param inputs
     * @param index
     * @return
     */
    //abstract public double nodeOutput(double inputs[], int index);
}
