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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import wGmdh.jGmdh.util.supervised.DatasetSupervised;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * The idea is to pass every Model and Node you have in consideration for
 * building upper-layer models to this filter class and use its (reduced)
 * output to build them
 *
 * @author ivek
 */
public abstract class NodeFilter implements Serializable, OptionHandler {

    private boolean debug = false;

    /**
     * DatasetSupervised models have been built on
     */
    protected DatasetSupervised dataset = null;

    /**
     * Present filter with a Node it needs to process.
     *
     * @param newNode
     * @return
     */
    public abstract Node feed(Node newNode);

    public NodeFilter() {
    };

    /**
     * Obtain results once you're done with feeding the filter
     * 
     * @return
     */
    public abstract ArrayList<Node> getResult();

    /**
     * Filter initialization
     *
     * @param dataset   dataset models have been build upon
     */
    public void initialize(DatasetSupervised dataset) {
        this.dataset = dataset;
    }

    /**
     * Resets filter state, e.g. when you want to start filtering a new layer.
     */
    public void reset() {
    }

    // OptionHandler overrides

    /**
     *
     * @param options
     * @throws java.lang.Exception
     */
    public void setOptions(String[] options) throws Exception {

        setDebug(Utils.getFlag('D', options));
        Utils.checkForRemainingOptions(options);
    }


    public String[] getOptions() {
        Vector result;

        result = new Vector();

        if (getDebug()) {
            result.add("-D");
        }

        return (String[]) result.toArray(new String[result.size()]);
    }

    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        options.addElement(new Option(
                "\tStandard debugging switch.\n",
                "D", 0, "-D"));

        return options.elements();
    }
    // OptionHandler overrides end

        public void setDebug(boolean b) {
        debug = b;
    }

    public boolean getDebug() {
        return debug;
    }

    /**
     * It's useful to have all models that have been presented to us at disposal,
     * furthermore sorted
     *
     * @return  sorted models
     */
    public abstract ArrayList<Model> getSortedModels();
}
